package com.carparking.app.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Télécharge l'APK d'une mise à jour et lance l'installeur système.
 */
object UpdateInstaller {

    sealed class Progress {
        data class Downloading(val percent: Int) : Progress()
        object ReadyToInstall : Progress()
        data class Error(val message: String) : Progress()
    }

    suspend fun download(context: Context, url: String, onProgress: (Progress) -> Unit): File? =
        withContext(Dispatchers.IO) {
            try {
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = true
                    connectTimeout = 10000
                    readTimeout = 15000
                }
                val totalSize = connection.contentLength
                val outFile = File(context.cacheDir, "parkar-update.apk")

                connection.inputStream.use { input ->
                    outFile.outputStream().use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var downloaded = 0
                        var lastPercent = -1
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            downloaded += read
                            if (totalSize > 0) {
                                val percent = (downloaded * 100 / totalSize)
                                if (percent != lastPercent) {
                                    lastPercent = percent
                                    onProgress(Progress.Downloading(percent))
                                }
                            }
                        }
                    }
                }
                onProgress(Progress.ReadyToInstall)
                outFile
            } catch (e: Exception) {
                onProgress(Progress.Error(e.message ?: "Erreur de téléchargement"))
                null
            }
        }

    fun launchInstall(context: Context, apkFile: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun canRequestInstall(context: Context): Boolean {
        return context.packageManager.canRequestPackageInstalls()
    }
}
