package com.eva.recordings.data.provider

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.os.Build
import androidx.core.net.toUri
import com.eva.location.domain.repository.LocationAddressProvider
import com.eva.location.domain.utils.parseLocationFromString
import com.eva.recordings.domain.models.MediaMetaDataInfo
import com.eva.recordings.domain.provider.AudioInfoExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

class AudioInfoExtractorImpl(
	private val context: Context,
	private val addressProvider: LocationAddressProvider,
) : AudioInfoExtractor {

	override suspend fun extractMediaData(uri: String): MediaMetaDataInfo? {
		val extractor = MediaExtractor()
		val retriever = MediaMetadataRetriever()
		try {
			val audioFileUri = uri.toUri()
			return withContext(Dispatchers.IO) {// set source
				extractor.setDataSource(context, audioFileUri, null)
				retriever.setDataSource(context, audioFileUri)
				// its accountable that there is a single track
				val mediaFormat = extractor.getTrackFormat(0)
				val channelCount = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

				val sampleRate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
					retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)
						?.toIntOrNull() ?: 0
				} else mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)

				// we will try to read location from the metadata if it's available
				val locationAsString = async {
					val locationString =
						retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION)
					parseLocationFromString(locationString)
						?.let { addressProvider.invoke(it).getOrNull() }
				}
				val bitRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
					?.toIntOrNull() ?: mediaFormat.getInteger(MediaFormat.KEY_BIT_RATE)

				val artists = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
				val author = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR)

				MediaMetaDataInfo(
					channelCount = channelCount,
					sampleRate = sampleRate,
					bitRate = bitRate,
					artists = artists,
					author = author,
					locationString = locationAsString.await()
				)
			}
		} catch (e: Exception) {
			e.printStackTrace()
			return null
		} finally {
			retriever.release()
			extractor.release()
		}
	}
}