package com.carparking.app.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.carparking.app.data.model.ParkingRecord
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Exporte l'historique de stationnement en CSV et lance le partage système.
 */
object ExportUtils {

    fun shareAsCsv(context: Context, carName: String, records: List<ParkingRecord>) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val csv = buildString {
            appendLine("Date,Statut,Adresse,Latitude,Longitude,Note")
            records.forEach { r ->
                val date = dateFormat.format(Date(r.parkedAt))
                val statut = if (r.isActive) "En cours" else "Terminé"
                val adresse = (r.address ?: "").csvEscape()
                val note = (r.note ?: "").csvEscape()
                appendLine("$date,$statut,$adresse,${r.latitude},${r.longitude},$note")
            }
        }

        val fileName = "Parkar_historique_${carName.replace(Regex("[^A-Za-z0-9]"), "_")}.csv"
        val file = File(context.cacheDir, fileName)
        file.writeText(csv)

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Exporter l'historique"))
    }

    internal fun String.csvEscape(): String =
        if (contains(",") || contains("\"") || contains("\n"))
            "\"${replace("\"", "\"\"")}\""
        else this
}
