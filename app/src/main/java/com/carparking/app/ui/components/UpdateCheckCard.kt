package com.carparking.app.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.carparking.app.update.UpdateChecker
import com.carparking.app.update.UpdateInfo
import com.carparking.app.update.UpdateInstaller
import kotlinx.coroutines.launch

private sealed class CheckState {
    object Idle : CheckState()
    object Checking : CheckState()
    object UpToDate : CheckState()
    data class Available(val info: UpdateInfo) : CheckState()
    data class Downloading(val percent: Int) : CheckState()
    data class Failed(val message: String) : CheckState()
}

/**
 * Carte "Mises à jour" pour l'écran Mes voitures : affiche la version
 * installée et permet de vérifier manuellement s'il y en a une nouvelle.
 */
@Composable
fun UpdateCheckCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<CheckState>(CheckState.Idle) }

    val currentVersion = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
        } catch (e: Exception) { "?" }
    }

    fun check() {
        state = CheckState.Checking
        scope.launch {
            val info = UpdateChecker.checkForUpdate(context)
            state = if (info != null) CheckState.Available(info) else CheckState.UpToDate
        }
    }

    fun startDownload(info: UpdateInfo) {
        if (!UpdateInstaller.canRequestInstall(context)) {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
            )
            return
        }
        scope.launch {
            state = CheckState.Downloading(0)
            val file = UpdateInstaller.download(context, info.downloadUrl) { progress ->
                when (progress) {
                    is UpdateInstaller.Progress.Downloading -> state = CheckState.Downloading(progress.percent)
                    is UpdateInstaller.Progress.Error -> state = CheckState.Failed(progress.message)
                    UpdateInstaller.Progress.ReadyToInstall -> {}
                }
            }
            if (file != null) UpdateInstaller.launchInstall(context, file)
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.SystemUpdate, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Mises à jour", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold)
                    Text("Version installée : $currentVersion",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(12.dp))

            when (val s = state) {
                CheckState.Idle -> {
                    OutlinedButton(onClick = { check() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Vérifier les mises à jour")
                    }
                }
                CheckState.Checking -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("Vérification en cours…", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                CheckState.UpToDate -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Vous avez la dernière version", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                is CheckState.Available -> {
                    Text("Nouvelle version disponible : v${s.info.versionName}",
                        style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    if (s.info.releaseNotes.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(s.info.releaseNotes, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 4)
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = { startDownload(s.info) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.SystemUpdate, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Télécharger et installer")
                    }
                }
                is CheckState.Downloading -> {
                    LinearProgressIndicator(
                        progress = { s.percent / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("Téléchargement… ${s.percent}%", style = MaterialTheme.typography.labelMedium)
                }
                is CheckState.Failed -> {
                    Text("Échec : ${s.message}", color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { check() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Réessayer")
                    }
                }
            }
        }
    }
}
