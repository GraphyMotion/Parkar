package com.carparking.app.ui.components

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.carparking.app.crash.CrashHandler

/**
 * Affichée uniquement s'il existe au moins un journal de plantage —
 * permet de le partager (par email, etc.) pour signaler le problème.
 */
@Composable
fun CrashLogsCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var logs by remember { mutableStateOf(CrashHandler.listLogs(context)) }

    if (logs.isEmpty()) return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.BugReport, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Un plantage a été détecté",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer)
                    Text("${logs.size} journal(aux) disponible(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        val latest = logs.firstOrNull() ?: return@Button
                        val uri = FileProvider.getUriForFile(
                            context, "${context.packageName}.fileprovider", latest
                        )
                        context.startActivity(
                            Intent.createChooser(
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                },
                                "Partager le journal de plantage"
                            )
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Partager") }
                OutlinedButton(
                    onClick = {
                        CrashHandler.clearLogs(context)
                        logs = emptyList()
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Effacer") }
            }
        }
    }
}
