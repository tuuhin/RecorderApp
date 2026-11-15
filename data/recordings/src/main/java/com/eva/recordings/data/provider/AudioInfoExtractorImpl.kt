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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

class AudioInfoExtractorImpl(
    private val context: Context,
    private val addressProvider: LocationAddressProvider,
) : AudioInfoExtractor {

    override suspend fun extractMediaData(uri: String): MediaMetaDataInfo? {
        val audioFileUri = uri.toUri()

        return withContext(Dispatchers.IO) {
            val extractor = MediaExtractor()
            val retriever = MediaMetadataRetriever()
            try {
                // set source
                extractor.setDataSource(context, audioFileUri, null)
                retriever.setDataSource(context, audioFileUri)
                // file without any tracks
                if (extractor.trackCount <= 0) return@withContext null
                // its accountable that there is a single track
                val mediaFormat = extractor.getTrackFormat(0)
                val locationAsString = async { retriever.extractLocation() }

                MediaMetaDataInfo(
                    channelCount = mediaFormat.channelCount,
                    sampleRate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) retriever.keySampleRate
                    else mediaFormat.sampleRate,
                    bitRate = retriever.bitRate ?: mediaFormat.bitrate,
                    artists = retriever.artist,
                    author = retriever.author,
                    locationString = locationAsString.await(),
                )

            } catch (e: Exception) {
                if (e is CancellationException) throw e
                e.printStackTrace()
                null
            } finally {
                retriever.release()
                extractor.release()
            }
        }
    }

    private val MediaFormat.channelCount: Int
        get() = if (containsKey(MediaFormat.KEY_CHANNEL_COUNT))
            getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        else 0

    private val MediaFormat.sampleRate: Int
        get() = if (containsKey(MediaFormat.KEY_SAMPLE_RATE))
            getInteger(MediaFormat.KEY_SAMPLE_RATE)
        else 0

    private val MediaFormat.bitrate: Int
        get() = if (containsKey(MediaFormat.KEY_BIT_RATE))
            getInteger(MediaFormat.KEY_BIT_RATE)
        else 0

    private val MediaMetadataRetriever.keySampleRate: Int
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)?.toIntOrNull() ?: 0
        else 0

    private val MediaMetadataRetriever.bitRate: Int?
        get() = extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull()

    private val MediaMetadataRetriever.artist: String?
        get() = extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)

    private val MediaMetadataRetriever.author: String?
        get() = extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR)

    private suspend fun MediaMetadataRetriever.extractLocation(): String? {
        val locationString = extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION)
        return parseLocationFromString(locationString)
            ?.let { addressProvider.invoke(it).getOrNull() }
    }
}
