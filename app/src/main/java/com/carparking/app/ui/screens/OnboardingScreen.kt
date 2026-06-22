package com.carparking.app.ui.screens

import android.Manifest
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState

/** Données d'affichage pour chaque permission */
private data class PermissionInfo(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val required: Boolean,
    val color: Color
)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun OnboardingScreen(onComplete: () -> Unit) {

    // Liste des permissions à demander selon la version Android
    val permissionList = remember {
        buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            add(Manifest.permission.CAMERA)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
                add(Manifest.permission.READ_MEDIA_IMAGES)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
            }
        }
    }

    val permsState = rememberMultiplePermissionsState(permissionList)

    // Informations affichées pour chaque permission
    val permInfos = remember {
        listOf(
            PermissionInfo(
                icon        = Icons.Filled.LocationOn,
                title       = "Localisation GPS",
                description = "Indispensable pour enregistrer la position exacte de votre voiture et afficher la carte.",
                required    = true,
                color       = Color(0xFF1A237E)
            ),
            PermissionInfo(
                icon        = Icons.Filled.CameraAlt,
                title       = "Appareil photo",
                description = "Pour prendre des photos de l'emplacement de stationnement comme repère visuel.",
                required    = false,
                color       = Color(0xFF00695C)
            ),
            PermissionInfo(
                icon        = Icons.Filled.Notifications,
                title       = "Notifications",
                description = "Pour vous envoyer un rappel quand votre stationnement arrive à expiration.",
                required    = false,
                color       = Color(0xFFE65100)
            ),
            PermissionInfo(
                icon        = Icons.Filled.Photo,
                title       = "Galerie photos",
                description = "Pour choisir une photo existante comme photo de l'emplacement ou de votre voiture.",
                required    = false,
                color       = Color(0xFF6A1B9A)
            )
        )
    }

    // État : l'utilisateur a appuyé sur "Accorder"
    var hasRequested by remember { mutableStateOf(false) }

    // Vérifie si les permissions obligatoires sont accordées
    val locationGranted = permsState.permissions.any {
        it.permission == Manifest.permission.ACCESS_FINE_LOCATION && it.status.isGranted
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // ── En-tête dégradé ──────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF1A237E), Color(0xFF283593))
                        )
                    )
                    .padding(top = 56.dp, bottom = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Icône app
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier.size(88.dp)
                    ) {
                        Icon(
                            Icons.Filled.DirectionsCar,
                            contentDescription = null,
                            modifier = Modifier.padding(20.dp),
                            tint = Color.White
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    Text(
                        "Bienvenue dans\nParkar",
                        style     = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color     = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        "Pour fonctionner correctement,\nl'application a besoin de quelques autorisations.",
                        style     = MaterialTheme.typography.bodyMedium,
                        color     = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        modifier  = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }

            // ── Liste des permissions ────────────────────────────────────────
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Autorisations requises",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onBackground
                )

                permInfos.forEach { info ->
                    // Détermine si cette permission est accordée
                    val isGranted = when (info.title) {
                        "Localisation GPS" -> permsState.permissions.any {
                            (it.permission == Manifest.permission.ACCESS_FINE_LOCATION ||
                             it.permission == Manifest.permission.ACCESS_COARSE_LOCATION) &&
                            it.status.isGranted
                        }
                        "Appareil photo" -> permsState.permissions.any {
                            it.permission == Manifest.permission.CAMERA && it.status.isGranted
                        }
                        "Notifications" -> Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                            permsState.permissions.any {
                                it.permission == Manifest.permission.POST_NOTIFICATIONS && it.status.isGranted
                            }
                        "Galerie photos" -> Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                            permsState.permissions.any {
                                it.permission == Manifest.permission.READ_MEDIA_IMAGES && it.status.isGranted
                            }
                        else -> false
                    }

                    PermissionCard(
                        info      = info,
                        isGranted = isGranted,
                        shown     = hasRequested
                    )
                }

                Spacer(Modifier.height(8.dp))

                // ── Bouton principal ─────────────────────────────────────────
                AnimatedContent(
                    targetState = when {
                        !hasRequested   -> "request"
                        locationGranted -> "done"
                        else            -> "retry"
                    },
                    transitionSpec = {
                        fadeIn(tween(300)) togetherWith fadeOut(tween(300))
                    },
                    label = "button_state"
                ) { state ->
                    when (state) {
                        "request" -> Button(
                            onClick = {
                                hasRequested = true
                                permsState.launchMultiplePermissionRequest()
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape    = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Filled.Security, contentDescription = null)
                            Spacer(Modifier.width(10.dp))
                            Text("Accorder les autorisations", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        "done" -> Button(
                            onClick  = onComplete,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape    = RoundedCornerShape(14.dp),
                            colors   = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2E7D32)
                            )
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null)
                            Spacer(Modifier.width(10.dp))
                            Text("Commencer !", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Localisation refusée : bloquant
                            Card(
                                shape  = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.Warning, null,
                                        tint     = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        "La localisation est indispensable.\nActivez-la dans les Paramètres → Autorisations.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(
                                    onClick   = { permsState.launchMultiplePermissionRequest() },
                                    modifier  = Modifier.weight(1f).height(50.dp),
                                    shape     = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Réessayer")
                                }
                                TextButton(
                                    onClick  = onComplete,
                                    modifier = Modifier.weight(1f).height(50.dp)
                                ) {
                                    Text("Ignorer", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                // Message informatif
                Spacer(Modifier.height(4.dp))
                Text(
                    "Vous pouvez modifier ces autorisations à tout moment dans les paramètres de votre téléphone.",
                    style     = MaterialTheme.typography.bodySmall,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                )

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun PermissionCard(
    info: PermissionInfo,
    isGranted: Boolean,
    shown: Boolean
) {
    Card(
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icône colorée
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = info.color.copy(alpha = 0.12f),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    info.icon,
                    contentDescription = null,
                    tint     = info.color,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(info.title, fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyLarge)
                    if (!info.required) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        ) {
                            Text("Optionnel",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(info.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.width(10.dp))

            // Indicateur accordé / en attente
            AnimatedVisibility(
                visible = shown,
                enter   = fadeIn() + scaleIn()
            ) {
                if (isGranted) {
                    Icon(Icons.Filled.CheckCircle, "Accordé",
                        tint     = Color(0xFF2E7D32),
                        modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Filled.Cancel, "Refusé",
                        tint     = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}
