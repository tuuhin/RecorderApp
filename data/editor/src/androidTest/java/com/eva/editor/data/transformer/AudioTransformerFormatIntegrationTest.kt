package com.eva.editor.data.transformer

import android.content.Context
import android.media.MediaMetadataRetriever
import androidx.core.net.toUri
import androidx.media3.common.MimeTypes
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.bytes
import assertk.assertions.exists
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isSuccess
import com.eva.editor.domain.AudioTransformer
import com.eva.editor.domain.model.AudioClipConfig
import com.eva.editor.domain.model.AudioEditAction
import com.eva.recordings.domain.models.AudioFileModel
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.io.File
import java.time.LocalDateTime
import javax.inject.Inject
import kotlin.test.BeforeTest
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.datetime.toKotlinLocalDateTime
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class AudioTransformerFormatIntegrationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val temporaryFolder = TemporaryFolder()

    @Inject
    lateinit var transformer: AudioTransformer
    lateinit var context: Context

    @BeforeTest
    fun init() {
        context = ApplicationProvider.getApplicationContext()
        hiltRule.inject()
    }

    @Test
    fun given_flac_when_cut_and_crop_then_exports_readable_edited_audio() =
        performCutAndCrop("editor_flac.flac", "audio/flac", "flac")


    @Test
    fun given_custom_wav_when_cut_and_crop_then_exports_readable_edited_audio() =
        performCutAndCrop("editor_wav.wav", "audio/x-wav", "wav")

    @Test
    fun given_mp3_when_cut_and_crop_then_exports_readable_edited_audio() =
        performCutAndCrop("editor_mp3.mp3", MimeTypes.AUDIO_MPEG, "mp3")

    @Test
    fun given_ogg_when_cut_and_crop_then_exports_readable_edited_audio() =
        performCutAndCrop("editor_ogg.ogg", MimeTypes.AUDIO_OGG, "oga")

    private fun performCutAndCrop(assetName: String, mimeType: String, expectedExtension: String) = runBlocking {
        val source = copyAssetToTemporaryFile(assetName)

        val operations = listOf(
            Triple(AudioEditAction.CUT, AudioClipConfig(3.seconds, 5.seconds), 10L),
            Triple(AudioEditAction.CROP, AudioClipConfig(3.seconds, 7.seconds), 4L),
        )

        for ((action, clip, expectedDuration) in operations) {
            var output: File? = null
            try {
                val model = AudioFileModel(
                    id = 0L,
                    displayName = assetName,
                    size = source.length(),
                    duration = FIXTURE_DURATION,
                    lastModified = LocalDateTime.now().toKotlinLocalDateTime(),
                    fileUri = source.toUri().toString(),
                    mimeType = mimeType,
                )

                val fileOperation = withContext(Dispatchers.Main.immediate) {
                    transformer.transformAudio(model, listOf(clip to action))
                }
                assertThat(fileOperation).isSuccess()

                val exportedFile = fileOperation.getOrThrow()
                output = exportedFile

                assertThat(exportedFile).exists()
                assertThat(exportedFile).bytes().isNotEmpty()
                assertThat(exportedFile.extension).isEqualTo(expectedExtension)

                val (outDuration, mime) = extractDurationAndMime(exportedFile)

                assertThat(outDuration).isNotNull()
                assertThat(mime).isNotNull()
                    .isNotEmpty()

            } finally {
                withContext(NonCancellable) {
                    if (output?.exists() == true) output.delete()
                    withContext(Dispatchers.Main) {
                        transformer.cleanUp()
                    }
                }
            }
        }
    }

    private suspend fun copyAssetToTemporaryFile(assetName: String): File {
        return withContext(Dispatchers.IO) {
            val destination = temporaryFolder.newFile(assetName)
            context.assets.open(assetName).use { input ->
                destination.outputStream().use(input::copyTo)
            }
            destination
        }
    }

    private suspend fun extractDurationAndMime(file: File): Pair<Long?, String?> {
        return withContext(Dispatchers.Default) {
            val extractor = MediaMetadataRetriever()
            try {
                extractor.setDataSource(file.absolutePath)
                val seconds =
                    extractor.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()?.div(1000) ?: 0
                val mime = extractor.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
                seconds to mime
            } finally {
                extractor.release()
            }
        }
    }

    companion object {
        private val FIXTURE_DURATION = 12.seconds
    }
}
