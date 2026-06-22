package com.carparking.app.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.carparking.app.data.model.ParkingRecord
import com.carparking.app.ui.components.CompassToParking
import com.carparking.app.ui.components.OsmMapView
import com.carparking.app.ui.components.PhotoPickerRow
import com.carparking.app.ui.navigation.Screen
import com.carparking.app.ui.theme.ParkingGreen
import com.carparking.app.ui.viewmodel.CarViewModel
import com.carparking.app.ui.viewmodel.ParkingViewModel
import com.carparking.app.utils.LocationUtils
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ParkingDetailScreen(navController: NavController, carId: Int) {
    val context = LocalContext.current
    val carVm: CarViewModel = viewModel()
    val parkingVm: ParkingViewModel = viewModel()

    val cars by carVm.cars.collectAsStateWithLifecycle()
    val car = cars.firstOrNull { it.id == carId }
    val parking by parkingVm.getActiveParkingByCar(carId)
        .collectAsStateWithLifecycle(initialValue = null)

    // Photos : primaryPhoto (photoPath) + photos supplémentaires depuis la table
    val extraPhotos by remember(parking) {
        if (parking != null)
            parkingVm.getPhotosForParking(parking!!.id)
        else
            kotlinx.coroutines.flow.flowOf(emptyList())
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    val allPhotos = remember(parking, extraPhotos) {
        buildList {
            parking?.photoPath?.let { add(it) }
            extraPhotos.forEach { add(it.photoPath) }
        }.distinct()
    }

    var showArriveDialog by remember { mutableStateOf(false) }
    var showCompass by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    // Permission GPS — demandée quand on bascule sur l'onglet Boussole
    // La boussole gère elle-même les mises à jour continues
    val locationPerms = rememberMultiplePermissionsState(
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    )

    fun shareParking(p: ParkingRecord) {
        val mapsUrl = "https://maps.google.com/?q=${p.latitude},${p.longitude}"
        val text = buildString {
            appendLine("🚗 Ma voiture est garée ici :")
            if (!p.address.isNullOrEmpty()) appendLine("📍 ${p.address}")
            appendLine(mapsUrl)
            if (!p.note.isNullOrEmpty()) append("💬 ${p.note}")
        }
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                },
                "Partager la position"
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(car?.name ?: "Voiture", fontWeight = FontWeight.Bold)
                        Text("Position enregistrée", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "Retour")
                    }
                },
                actions = {
                    // Corriger la position
                    parking?.let {
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(Icons.Filled.EditLocationAlt, "Corriger la position",
                                tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    // Partager
                    parking?.let { p ->
                        IconButton(onClick = { shareParking(p) }) {
                            Icon(Icons.Filled.Share, "Partager", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    // Historique
                    IconButton(onClick = { navController.navigate(Screen.ParkingHistory.createRoute(carId)) }) {
                        Icon(Icons.Filled.History, "Historique", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        if (parking == null) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.LocationOff, null, Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f))
                    Spacer(Modifier.height(16.dp))
                    Text("Aucun parking actif", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(onClick = { navController.navigate(Screen.ParkingHistory.createRoute(carId)) }) {
                        Icon(Icons.Filled.History, null); Spacer(Modifier.width(8.dp))
                        Text("Voir l'historique")
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding)
                    .verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ── Onglets Carte / Boussole ─────────────────────────────────
                TabRow(selectedTabIndex = if (showCompass) 1 else 0,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clip(RoundedCornerShape(14.dp))) {
                    Tab(selected = !showCompass, onClick = { showCompass = false }) {
                        Row(Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Map, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Carte", fontWeight = FontWeight.Medium)
                        }
                    }
                    Tab(selected = showCompass, onClick = {
                        showCompass = true
                        if (!locationPerms.allPermissionsGranted) locationPerms.launchMultiplePermissionRequest()
                    }) {
                        Row(Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Explore, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Boussole", fontWeight = FontWeight.Medium)
                        }
                    }
                }

                // ── Carte ou Boussole ────────────────────────────────────────
                Card(shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(4.dp),
                    modifier = Modifier.fillMaxWidth()) {
                    if (showCompass) {
                        Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) {
                            CompassToParking(
                                carLat = parking!!.latitude,
                                carLng = parking!!.longitude
                            )
                        }
                    } else {
                        OsmMapView(parking!!.latitude, parking!!.longitude,
                            Modifier.fillMaxWidth().height(270.dp), interactive = true)
                    }
                }

                // ── Infos ────────────────────────────────────────────────────
                Card(shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (!parking!!.address.isNullOrEmpty())
                            InfoRow(Icons.Filled.LocationOn, "Adresse", parking!!.address!!)
                        InfoRow(Icons.Filled.Schedule, "Garé le",
                            SimpleDateFormat("dd/MM/yyyy à HH:mm", Locale.getDefault())
                                .format(Date(parking!!.parkedAt)))
                        InfoRow(Icons.Filled.MyLocation, "Coordonnées",
                            "%.6f, %.6f".format(parking!!.latitude, parking!!.longitude))
                        if (!parking!!.note.isNullOrEmpty())
                            InfoRow(Icons.Filled.Notes, "Note", parking!!.note!!)
                    }
                }

                // ── Photos ───────────────────────────────────────────────────
                if (allPhotos.isNotEmpty()) {
                    Card(shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Photo, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text("Photos (${allPhotos.size})", fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(Modifier.height(10.dp))
                            PhotoPickerRow(
                                photos = allPhotos,
                                onAddClick = {},
                                onDeleteClick = { idx ->
                                    if (idx == 0) { /* photo principale: géré autrement */ }
                                    else {
                                        val extraIdx = idx - if (parking!!.photoPath != null) 1 else 0
                                        if (extraIdx < extraPhotos.size)
                                            parkingVm.deletePhoto(extraPhotos[extraIdx])
                                    }
                                },
                                readOnly = true,     // pas d'ajout depuis le détail
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // ── Boutons ──────────────────────────────────────────────────
                Button(
                    onClick = {
                        val geoUri = Uri.parse(
                            "geo:${parking!!.latitude},${parking!!.longitude}?" +
                            "q=${parking!!.latitude},${parking!!.longitude}(Ma+voiture)")
                        val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
                            .apply { setPackage("com.google.android.apps.maps") }
                        if (mapIntent.resolveActivity(context.packageManager) != null)
                            context.startActivity(mapIntent)
                        else context.startActivity(Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://maps.google.com/?q=${parking!!.latitude},${parking!!.longitude}")))
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.Navigation, null); Spacer(Modifier.width(10.dp))
                    Text("Naviguer vers ma voiture", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { shareParking(parking!!) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.Share, null); Spacer(Modifier.width(10.dp))
                    Text("Partager la position", fontWeight = FontWeight.Medium)
                }

                OutlinedButton(
                    onClick = { showArriveDialog = true },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ParkingGreen)
                ) {
                    Icon(Icons.Filled.CheckCircle, null); Spacer(Modifier.width(10.dp))
                    Text("J'ai retrouvé ma voiture !", fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showArriveDialog) {
        AlertDialog(
            onDismissRequest = { showArriveDialog = false },
            icon = { Icon(Icons.Filled.CheckCircle, null, tint = ParkingGreen) },
            title = { Text("J'ai retrouvé ma voiture !") },
            text = { Text("Voulez-vous archiver ce stationnement ?") },
            confirmButton = {
                TextButton(onClick = {
                    parkingVm.deactivateParking(carId, context)
                    showArriveDialog = false
                    navController.popBackStack()
                }) { Text("Oui, archiver", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showArriveDialog = false }) { Text("Garder actif") }
            }
        )
    }

    if (showEditDialog && parking != null) {
        EditParkingDialog(
            parking = parking!!,
            carName = car?.name ?: "votre voiture",
            onDismiss = { showEditDialog = false },
            onSave = { lat, lng, address, note ->
                parkingVm.correctParking(
                    parking = parking!!,
                    newLatitude = lat,
                    newLongitude = lng,
                    newAddress = address,
                    newNote = note,
                    context = context,
                    carName = car?.name ?: "votre voiture"
                )
                showEditDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditParkingDialog(
    parking: ParkingRecord,
    carName: String,
    onDismiss: () -> Unit,
    onSave: (lat: Double, lng: Double, address: String?, note: String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var latitude by remember { mutableStateOf(parking.latitude) }
    var longitude by remember { mutableStateOf(parking.longitude) }
    var address by remember { mutableStateOf(parking.address ?: "") }
    var note by remember { mutableStateOf(parking.note ?: "") }
    var isRelocating by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.EditLocationAlt, contentDescription = null) },
        title = { Text("Corriger le stationnement") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "GPS actuel : %.6f, %.6f".format(latitude, longitude),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = {
                        isRelocating = true
                        scope.launch {
                            val loc = LocationUtils.getCurrentLocation(context)
                            if (loc != null) {
                                latitude = loc.latitude
                                longitude = loc.longitude
                                val geo = withContext(Dispatchers.IO) {
                                    LocationUtils.getAddressFromCoordinates(context, loc.latitude, loc.longitude)
                                }
                                if (!geo.isNullOrBlank()) address = geo
                            }
                            isRelocating = false
                        }
                    },
                    enabled = !isRelocating,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isRelocating) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Localisation…")
                    } else {
                        Icon(Icons.Filled.MyLocation, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Utiliser ma position actuelle")
                    }
                }
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Adresse") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2, maxLines = 4
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(latitude, longitude, address.trim().ifEmpty { null }, note.trim().ifEmpty { null })
            }) { Text("Enregistrer", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}
