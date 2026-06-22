package com.carparking.app.update

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val versionName: String,
    val downloadUrl: String,
    val releaseNotes: String
)

/**
 * Interroge l'API GitHub Releases pour savoir si une nouvelle version
 * de Parkar est disponible, et fournit le lien direct de l'APK à télécharger.
 */
object UpdateChecker {

    private const val REPO = "GraphyMotion/Parkar"
    private const val API_URL = "https://api.github.com/repos/$REPO/releases/latest"

    suspend fun checkForUpdate(context: Context): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val connection = (URL(API_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json")
                connectTimeout = 8000
                readTimeout = 8000
            }
            val code = connection.responseCode
            if (code != 200) return@withContext null

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val tagName = json.getString("tag_name") // ex: "v1.1.0"
            val remoteVersion = tagName.removePrefix("v")

            val currentVersion = currentVersionName(context)
            if (!isNewer(remoteVersion, currentVersion)) return@withContext null

            val assets = json.getJSONArray("assets")
            var apkUrl: String? = null
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.getString("name").endsWith(".apk")) {
                    apkUrl = asset.getString("browser_download_url")
                    break
                }
            }
            apkUrl ?: return@withContext null

            UpdateInfo(
                versionName = remoteVersion,
                downloadUrl = apkUrl,
                releaseNotes = json.optString("body", "").trim()
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun currentVersionName(context: Context): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0"
        } catch (e: Exception) {
            "0.0"
        }
    }

    /** Compare deux versions "x.y.z" — retourne true si [remote] > [current]. */
    private fun isNewer(remote: String, current: String): Boolean {
        val r = remote.split(".").map { it.toIntOrNull() ?: 0 }
        val c = current.split(".").map { it.toIntOrNull() ?: 0 }
        val len = maxOf(r.size, c.size)
        for (i in 0 until len) {
            val rv = r.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (rv != cv) return rv > cv
        }
        return false
    }
}
