package com.carparking.app.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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

/**
 * Bannière qui vérifie automatiquement (au montage) s'il existe une nouvelle
 * version sur GitHub Releases, et propose de la télécharger + l'installer.
 */
@Composable
fun UpdateBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var downloadPercent by remember { mutableStateOf<Int?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        updateInfo = UpdateChecker.checkForUpdate(context)
    }

    val info = updateInfo ?: return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.SystemUpdate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Mise à jour disponible — v${info.versionName}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (info.releaseNotes.isNotBlank()) {
                        Text(
                            info.releaseNotes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 3
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            when {
                error != null -> {
                    Text(
                        "Échec : $error",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { error = null; downloadPercent = null },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Réessayer") }
                }
                downloadPercent != null -> {
                    LinearProgressIndicator(
                        progress = { downloadPercent!! / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Téléchargement… $downloadPercent%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                else -> {
                    Button(
                        onClick = {
                            if (!UpdateInstaller.canRequestInstall(context)) {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                )
                                return@Button
                            }
                            scope.launch {
                                downloadPercent = 0
                                val file = UpdateInstaller.download(context, info.downloadUrl) { progress ->
                                    when (progress) {
                                        is UpdateInstaller.Progress.Downloading -> downloadPercent = progress.percent
                                        is UpdateInstaller.Progress.Error -> error = progress.message
                                        UpdateInstaller.Progress.ReadyToInstall -> {}
                                    }
                                }
                                if (file != null) {
                                    UpdateInstaller.launchInstall(context, file)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.SystemUpdate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Télécharger et installer")
                    }
                }
            }
        }
    }
}
