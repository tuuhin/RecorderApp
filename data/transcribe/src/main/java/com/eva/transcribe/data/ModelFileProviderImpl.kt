package com.eva.transcribe.data

import android.content.Context
import android.util.Log
import com.eva.transcribe.domain.ModelFileProvider
import com.eva.transcribe.domain.models.LanguageModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "MODEL_FILE_PROVIDER"

internal class ModelFileProviderImpl(private val context: Context) : ModelFileProvider {

	private val modelFolder: File
		get() = File(context.filesDir, ModelConstants.MODEL_FILES_DIR)
			.apply { mkdirs() }

	override suspend fun provideModelFile(language: LanguageModel): File {

		val fileName = language.savedModelPath
		val probableFile = File(modelFolder, fileName)
		// if file exists and content is not null
		if (probableFile.exists() && probableFile.listFiles()?.isNotEmpty() == true) {
			Log.i(TAG, "FILE ALREADY PRESENT")
			return probableFile
		}
		Log.i(TAG, "FILE NOT FOUND COPYING CONTENT FROM ASSETS")
		// copy the whole thing
		copyAssetsParallel(fileName, probableFile)
		Log.d(TAG, "DONE COPYING THE CONTENT")
		return probableFile
	}

	override suspend fun deleteModelInfo(languageModel: LanguageModel) {
		withContext(Dispatchers.IO) {
			val fileName = languageModel.savedModelPath
			val probableFile = File(modelFolder, fileName)
			if (probableFile.exists()) {
				// delete the file else
				probableFile.deleteRecursively()
				Log.i(TAG, "FILE ALREADY PRESENT")
				return@withContext
			} else {
				Log.d(TAG, "LANGUAGE MODEL IS NOT PRESENT")
			}
		}
	}

	private suspend fun collectAssetFiles(
		assetPath: String,
		destDir: File,
		result: MutableList<Pair<String, File>>
	) {
		withContext(Dispatchers.IO) {
			val items = context.assets.list(assetPath) ?: return@withContext

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
}