package com.carparking.app.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import com.carparking.app.ui.components.OsmMapView
import com.carparking.app.ui.components.PhotoPickerRow
import com.carparking.app.ui.viewmodel.CarViewModel
import com.carparking.app.ui.viewmodel.ParkingViewModel
import com.carparking.app.utils.ImageUtils
import com.carparking.app.utils.LocationUtils
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun SaveParkingScreen(navController: NavController, carId: Int) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val carVm: CarViewModel = viewModel()
    val parkingVm: ParkingViewModel = viewModel()
    val cars by carVm.cars.collectAsStateWithLifecycle()
    val car = cars.firstOrNull { it.id == carId }

    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var address by remember { mutableStateOf("") }
    var gpsAddress by remember { mutableStateOf("") }
    var addressEdited by remember { mutableStateOf(false) }
    var isGeocodingAddress by remember { mutableStateOf(false) }
    var geocodeError by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    var photos by remember { mutableStateOf<List<String>>(emptyList()) }  // toutes les photos
    var isLocating by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf(false) }
    var reminderHours by remember { mutableStateOf(0f) }
    var showPhotoMenu by remember { mutableStateOf(false) }
    var tempFile by remember { mutableStateOf<File?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val locationPerms = rememberMultiplePermissionsState(
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    )
    val notifPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        rememberMultiplePermissionsState(listOf(Manifest.permission.POST_NOTIFICATIONS))
    else null

    fun fetchLocation() = scope.launch {
        isLocating = true; locationError = false
        val loc = LocationUtils.getCurrentLocation(context)
        if (loc != null) {
            latitude = loc.latitude; longitude = loc.longitude
            val geo = withContext(Dispatchers.IO) {
                LocationUtils.getAddressFromCoordinates(context, loc.latitude, loc.longitude) ?: ""
            }
            gpsAddress = geo
            if (!addressEdited) address = geo
        } else locationError = true
        isLocating = false
    }

    /** Géocode l'adresse saisie manuellement → met à jour latitude, longitude et la carte */
    fun geocodeManualAddress() = scope.launch {
        if (address.isBlank()) return@launch
        isGeocodingAddress = true
        geocodeError = false
        val result = withContext(Dispatchers.IO) {
            LocationUtils.getCoordinatesFromAddress(context, address)
        }
        if (result != null) {
            latitude  = result.latitude
            longitude = result.longitude
            geocodeError = false
        } else {
            geocodeError = true
        }
        isGeocodingAddress = false
    }

    LaunchedEffect(locationPerms.allPermissionsGranted) {
        if (locationPerms.allPermissionsGranted) fetchLocation()
    }

    fun addPhoto(path: String) { photos = photos + path }
    fun removePhoto(index: Int) { photos = photos.toMutableList().also { it.removeAt(index) } }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) tempFile?.let { addPhoto(it.absolutePath) }
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            ImageUtils.copyUriToFile(context, uri, "PARK_${ts}.jpg")?.let { addPhoto(it) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Enregistrer le parking", fontWeight = FontWeight.Bold)
                        car?.let { Text(it.name, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── GPS ─────────────────────────────────────────────────────────
            Card(shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.MyLocation, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Position GPS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        if (locationPerms.allPermissionsGranted) {
                            IconButton(onClick = { fetchLocation() }, enabled = !isLocating) {
                                Icon(Icons.Filled.Refresh, "Relocaliser", tint = MaterialTheme.colorScheme.primary)
                            }
                        } else {
                            TextButton(onClick = { locationPerms.launchMultiplePermissionRequest() }) { Text("Autoriser") }
                        }
                    }
                    when {
                        !locationPerms.allPermissionsGranted -> {
                            Spacer(Modifier.height(8.dp))
                            Text("La permission de localisation est requise.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { locationPerms.launchMultiplePermissionRequest() }) {
                                Icon(Icons.Filled.LocationOn, null); Spacer(Modifier.width(8.dp))
                                Text("Autoriser la localisation")
                            }
                        }
                        isLocating -> {
                            Spacer(Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(12.dp))
                                Text("Localisation en cours…")
                            }
                            Spacer(Modifier.height(16.dp))
                        }
                        locationError -> {
                            Spacer(Modifier.height(8.dp))
                            Text("Impossible d'obtenir la position. GPS activé ?",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = { fetchLocation() }) { Text("Réessayer") }
                        }
                        latitude != null && longitude != null -> {
                            Spacer(Modifier.height(12.dp))

                            // Champ adresse
                            OutlinedTextField(
                                value = address,
                                onValueChange = {
                                    address = it
                                    addressEdited = it != gpsAddress
                                    geocodeError = false
                                },
                                label = { Text("Adresse") },
                                placeholder = { Text("Ex : 12 Rue de la Paix, Paris") },
                                leadingIcon = {
                                    Icon(Icons.Filled.LocationOn, null,
                                        tint = if (geocodeError) MaterialTheme.colorScheme.error
                                               else MaterialTheme.colorScheme.primary)
                                },
                                trailingIcon = {
                                    when {
                                        isGeocodingAddress -> CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp).padding(2.dp),
                                            strokeWidth = 2.dp
                                        )
                                        addressEdited -> IconButton(onClick = {
                                            address = gpsAddress; addressEdited = false; geocodeError = false
                                        }) {
                                            Icon(Icons.Filled.Refresh, "Rétablir adresse GPS",
                                                tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                },
                                isError = geocodeError,
                                supportingText = {
                                    when {
                                        geocodeError -> Text(
                                            "Adresse introuvable — vérifiez l'orthographe ou soyez plus précis",
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                        addressEdited -> Text(
                                            "Modifiée manuellement  •  ↺ pour rétablir le GPS",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                singleLine = true
                            )

                            // Bouton "Localiser cette adresse" — visible uniquement si modifiée manuellement
                            if (addressEdited && address.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Button(
                                    onClick = { geocodeManualAddress() },
                                    enabled = !isGeocodingAddress,
                                    modifier = Modifier.fillMaxWidth().height(44.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor   = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                ) {
                                    if (isGeocodingAddress) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text("Localisation en cours…")
                                    } else {
                                        Icon(Icons.Filled.Search, null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Localiser cette adresse sur la carte",
                                            style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                            }

                            Spacer(Modifier.height(6.dp))
                            Text(
                                "GPS : %.6f, %.6f".format(latitude, longitude),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(10.dp))
                            OsmMapView(
                                latitude  = latitude!!,
                                longitude = longitude!!,
                                modifier  = Modifier.fillMaxWidth().height(180.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                interactive = false
                            )
                        }
                    }
                }
            }

            // ── Note ────────────────────────────────────────────────────────
            OutlinedTextField(
                value = note, onValueChange = { note = it },
                label = { Text("Note (optionnel)") },
                placeholder = { Text("Ex: Devant la boulangerie…") },
                leadingIcon = { Icon(Icons.Filled.Notes, null) },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                minLines = 2, maxLines = 4
            )

            // ── Photos multiples ─────────────────────────────────────────────
            Card(shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.PhotoCamera, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Photos de l'emplacement", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        Text("${photos.size}/6", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(12.dp))
                    PhotoPickerRow(
                        photos = photos,
                        onAddClick = { showPhotoMenu = true },
                        onDeleteClick = { removePhoto(it) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ── Rappel ───────────────────────────────────────────────────────
            Card(shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Alarm, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Rappel", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        Text(if (reminderHours == 0f) "Désactivé" else "Dans ${fmtReminder(reminderHours)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (reminderHours > 0) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(8.dp))
                    Slider(value = reminderHours, onValueChange = { reminderHours = it },
                        valueRange = 0f..12f, steps = 23, modifier = Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Aucun", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("12h",  style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // ── Sauvegarder ──────────────────────────────────────────────────
            Button(
                onClick = {
                    val lat = latitude ?: return@Button
                    val lng = longitude ?: return@Button
                    if (isSaving) return@Button
                    isSaving = true
                    if (notifPerm != null && !notifPerm.allPermissionsGranted && reminderHours > 0)
                        notifPerm.launchMultiplePermissionRequest()

                    val primaryPhoto = photos.firstOrNull()
                    val extraPhotos  = if (photos.size > 1) photos.drop(1) else emptyList()

                    parkingVm.saveParking(
                        parking = ParkingRecord(
                            carId     = carId,
                            latitude  = lat,
                            longitude = lng,
                            address   = address.trim().ifEmpty { null },
                            note      = note.trim().ifEmpty { null },
                            photoPath = primaryPhoto
                        ),
                        carName         = car?.name ?: "votre voiture",
                        reminderMinutes = if (reminderHours > 0) (reminderHours * 60).toLong() else null,
                        context         = context,
                        extraPhotoPaths = extraPhotos
                    )
                    navController.popBackStack()
                },
                enabled = latitude != null && !isLocating && !isSaving,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isSaving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary)
                else {
                    Icon(Icons.Filled.Save, null); Spacer(Modifier.width(10.dp))
                    Text("Sauvegarder ma position", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    // ── Menu source photo ─────────────────────────────────────────────────────
    if (showPhotoMenu) {
        AlertDialog(
            onDismissRequest = { showPhotoMenu = false },
            title = { Text("Ajouter une photo") },
            text = {
                Column {
                    ListItem(headlineContent = { Text("Prendre une photo") },
                        leadingContent = { Icon(Icons.Filled.CameraAlt, null) },
                        modifier = Modifier.clickable {
                            showPhotoMenu = false
                            val f = ImageUtils.createImageFile(context)
                            tempFile = f
                            cameraLauncher.launch(ImageUtils.getUriForFile(context, f))
                        })
                    ListItem(headlineContent = { Text("Choisir depuis la galerie") },
                        leadingContent = { Icon(Icons.Filled.Photo, null) },
                        modifier = Modifier.clickable {
                            showPhotoMenu = false
                            galleryLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly))
                        })
                }
            },
            confirmButton = { TextButton(onClick = { showPhotoMenu = false }) { Text("Annuler") } }
        )
    }
}

private fun fmtReminder(h: Float): String {
    val total = (h * 60).toInt()
    return when { total / 60 == 0 -> "${total}min"; total % 60 == 0 -> "${total / 60}h"
                  else -> "${total / 60}h${total % 60}min" }
}
