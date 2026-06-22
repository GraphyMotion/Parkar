package com.carparking.app.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ImageUtils {

    fun createImageFile(context: Context): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = context.getExternalFilesDir("images")
        storageDir?.mkdirs()
        return File.createTempFile("IMG_${timestamp}_", ".jpg", storageDir)
    }

    fun getUriForFile(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    fun copyUriToFile(context: Context, uri: Uri, destFileName: String): String? {
        return try {
            val destDir = context.getExternalFilesDir("images")
            destDir?.mkdirs()
            val destFile = File(destDir, destFileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            }
            destFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }
}
