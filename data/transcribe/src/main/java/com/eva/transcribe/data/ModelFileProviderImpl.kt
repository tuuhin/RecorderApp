package com.eva.transcribe.data

import android.content.Context
import android.util.Log
import com.eva.transcribe.domain.LanguageModel
import com.eva.transcribe.domain.ModelFileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.VisibleForTesting
import java.io.File
import java.io.IOException

private const val TAG = "MODEL_FILE_PROVIDER"

internal class ModelFileProviderImpl(
	private val context: Context
) : ModelFileProvider {

	private val languageToFileMap = mapOf(LanguageModel.EN_US to "vosk-model-small-en-us-0.15")

	private val modelFolder by lazy {
		File(context.filesDir, "ml-models").apply { mkdirs() }
	}

	override suspend fun provideModelFile(language: LanguageModel): File? {
		val fileName = languageToFileMap[language] ?: return null
		val probableFile = File(modelFolder, fileName)
		// if file exists and content is not null
		if (probableFile.exists() && probableFile.listFiles()?.isNotEmpty() == true) {
			Log.i(TAG, "FILE ALREADY PRESENT")
			return probableFile
		}
		Log.i(TAG, "FILE NOT FOUND COPYING CONTENT FROM ASSETS")
		// copy the whole thing
		withContext(Dispatchers.IO) {
			copyAssetsParallel(fileName, probableFile)
		}
		Log.d(TAG, "DONE COPYING THE CONTENT")
		return probableFile
	}

	private fun collectAssetFiles(
		assetPath: String,
		destDir: File,
		result: MutableList<Pair<String, File>>
	) {
		val items = context.assets.list(assetPath) ?: return

		for (item in items) {
			val inPath = "$assetPath/$item"
			val outFile = File(destDir, item)

			val inner = context.assets.list(inPath)
			if (inner != null && inner.isNotEmpty()) {
				outFile.mkdirs()
				collectAssetFiles(inPath, outFile, result)
			} else {
				result += inPath to outFile
			}
		}
	}

	private suspend fun copyAssetsParallel(assetPath: String, destPath: File) = coroutineScope {
		if (!destPath.exists()) destPath.mkdirs()

		// generate a graph for the files
		val filesToCopy = mutableListOf<Pair<String, File>>()
		collectAssetFiles(assetPath, destPath, filesToCopy)

		Log.i(TAG, "COPYING FILES COUNT : ${filesToCopy.size}")
		Log.d(TAG, "COPYING FILES NAMES: ${filesToCopy.map { it.first }}")

		// copy the file contents
		val deferred = filesToCopy.map { (inPath, outFile) ->
			async(Dispatchers.IO) {
				context.assets.open(inPath).use { input ->
					Log.d(TAG, "COPYING FILE CONTENT :$inPath")
					outFile.outputStream().use { output -> input.copyTo(output) }
				}
			}
		}
		deferred.awaitAll()
	}

	@VisibleForTesting
	suspend fun deleteCopiedFile(file: File) {
		withContext(Dispatchers.IO) {
			try {
				file.deleteRecursively()
			} catch (e: IOException) {
				e.printStackTrace()
				Log.w(TAG, "UNABLE TO DELETE THE CONTENT OF THE FILE")
			}
		}
	}
}