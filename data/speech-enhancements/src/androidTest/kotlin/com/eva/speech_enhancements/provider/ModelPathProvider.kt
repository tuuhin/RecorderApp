package com.eva.speech_enhancements.provider

import android.content.Context
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

class ModelPathProvider(private val context: Context) {

    suspend fun copyAssetsToModelDir(parentDir: File): Unit = withContext(Dispatchers.IO) {

        val op1 = async { copyToFile(CONFIG_FILE_NAME, parentDir) }
        val op2 = async { copyToFile(ENCODER_FILE_NAME, parentDir) }
        val op3 = async { copyToFile(ERB_DECODER_FILE_NAME, parentDir) }
        val op4 = async { copyToFile(DECODER_FILE_NAME, parentDir) }

        awaitAll(op1, op2, op3, op4)
    }


    private fun copyToFile(name: String, parentDir: File) {
        val file = File(parentDir, name.substringAfterLast('/'))
        if (!file.exists()) file.createNewFile()
        // create the file then copy content
        context.assets.open(name).use { inp ->
            file.outputStream().use { out -> inp.copyTo(out) }
        }
    }

    companion object {
        private const val CONFIG_FILE_NAME = "model/config.ini"
        private const val DECODER_FILE_NAME = "model/df_dec.onnx"
        private const val ENCODER_FILE_NAME = "model/enc.onnx"
        private const val ERB_DECODER_FILE_NAME = "model/erb_dec.onnx"
    }
}
