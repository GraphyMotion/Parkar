package com.carparking.app.crash

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Capture les plantages non gérés dans un fichier local lisible/partageable
 * depuis l'appli (écran "Mes voitures" → Journaux de plantage), sans dépendre
 * d'un service tiers (pas de Firebase, pas de tracking externe).
 */
class CrashHandler(
    private val context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    companion object {
        private const val DIR_NAME = "crash_logs"
        private const val MAX_LOGS = 10

        fun logsDir(context: Context): File =
            File(context.filesDir, DIR_NAME).apply { mkdirs() }

        fun listLogs(context: Context): List<File> =
            logsDir(context).listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()

        fun clearLogs(context: Context) {
            listLogs(context).forEach { it.delete() }
        }
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            writeCrashLog(throwable)
        } catch (e: Exception) {
            // Ne jamais planter dans le gestionnaire de plantage lui-même
        }
        defaultHandler?.uncaughtException(thread, throwable)
    }

    private fun writeCrashLog(throwable: Throwable) {
        val dir = logsDir(context)
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(dir, "crash_$timestamp.txt")

        val stackTrace = StringWriter().also { throwable.printStackTrace(PrintWriter(it)) }.toString()
        val versionName = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: Exception) { "?" }

        file.writeText(
            buildString {
                appendLine("Parkar version : $versionName")
                appendLine("Date : ${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())}")
                appendLine("Appareil : ${Build.MANUFACTURER} ${Build.MODEL}")
                appendLine("Android : ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                appendLine()
                append(stackTrace)
            }
        )

        // Limite le nombre de logs conservés
        val logs = listLogs(context)
        if (logs.size > MAX_LOGS) {
            logs.drop(MAX_LOGS).forEach { it.delete() }
        }
    }
}
