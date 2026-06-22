package com.carparking.app.ui.screens

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.carparking.app.bluetooth.BluetoothMonitorService
import com.carparking.app.bluetooth.BluetoothPreferences
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun BluetoothSettingsScreen(navController: NavController) {
    val context = LocalContext.current
    var btEnabled by remember { mutableStateOf(BluetoothPreferences.isBluetoothAutoParkEnabled(context)) }
    var savedDeviceName by remember { mutableStateOf(BluetoothPreferences.getSavedDeviceName(context)) }
    var savedDeviceAddress by remember { mutableStateOf(BluetoothPreferences.getSavedDeviceAddress(context)) }
    var showDeviceList by remember { mutableStateOf(false) }

    // BLUETOOTH_CONNECT est une permission runtime sur Android 12+ (API 31+)
    val btPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        rememberPermissionState(android.Manifest.permission.BLUETOOTH_CONNECT)
    } else null
    val hasBtPermission = btPermission?.status?.isGranted ?: true
    var pendingEnable by remember { mutableStateOf(false) }

    // Localisation en arrière-plan ("Tout le temps") — indispensable pour capturer le GPS
    // quand la voiture se déconnecte alors que l'appli est fermée (Android 10+)
    val bgLocPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        rememberPermissionState(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    } else null
    val hasBgLocation = bgLocPermission?.status?.isGranted ?: true

    // Exemption d'optimisation batterie — sans elle, beaucoup de téléphones (Xiaomi, etc.)
    // empêchent le receiver Bluetooth de se réveiller
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    var ignoresBatteryOptim by remember {
        mutableStateOf(powerManager.isIgnoringBatteryOptimizations(context.packageName))
    }

    // Journal de diagnostic : dernier événement Bluetooth reçu
    var lastBtEvent by remember { mutableStateOf(BluetoothPreferences.getLastEvent(context)) }

    // Rafraîchit l'état au retour des paramètres système
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                ignoresBatteryOptim = powerManager.isIgnoringBatteryOptimizations(context.packageName)
                lastBtEvent = BluetoothPreferences.getLastEvent(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Dès que la permission est accordée, continuer l'activation si l'utilisateur avait basculé le toggle
    LaunchedEffect(hasBtPermission) {
        if (hasBtPermission && pendingEnable) {
            pendingEnable = false
            if (savedDeviceAddress == null) showDeviceList = true
            else {
                BluetoothPreferences.setBluetoothAutoParkEnabled(context, true)
                btEnabled = true
                BluetoothMonitorService.start(context)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bluetooth Auto-Park", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Bannière permission Bluetooth manquante (Android 12+)
            if (!hasBtPermission) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.BluetoothDisabled, null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Permission Bluetooth requise",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer)
                            Text(
                                if (btPermission?.status?.shouldShowRationale == true)
                                    "La permission a été refusée. Accordez-la dans les Paramètres de votre téléphone."
                                else
                                    "Appuyez sur le toggle pour autoriser l'accès Bluetooth.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Bluetooth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "Parking automatique via Bluetooth",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "Parkar détecte automatiquement quand vous coupez le contact de votre voiture (déconnexion Bluetooth) et sauvegarde votre position. Quand vous rallumez, le parking est archivé.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // â"€â"€ Toggle â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.PowerSettingsNew,
                        contentDescription = null,
                        tint = if (btEnabled) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Activer le Bluetooth Auto-Park",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)
                        if (!btEnabled) {
                            Text("Désactivé",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Text("Actif",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Switch(
                        checked = btEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                if (!hasBtPermission) {
                                    pendingEnable = true
                                    btPermission?.launchPermissionRequest()
                                } else if (savedDeviceAddress == null) {
                                    showDeviceList = true
                                } else {
                                    BluetoothPreferences.setBluetoothAutoParkEnabled(context, true)
                                    btEnabled = true
                                    BluetoothMonitorService.start(context)
                                }
                            } else {
                                BluetoothPreferences.setBluetoothAutoParkEnabled(context, false)
                                btEnabled = false
                                BluetoothMonitorService.stop(context)
                            }
                        }
                    )
                }
            }

            // â"€â"€ Appareil sÃ©lectionnÃ© â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€
            if (btEnabled && savedDeviceAddress != null) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.DirectionsCar, null,
                                tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Appareil voiture",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(savedDeviceName ?: "Inconnu",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold)
                                Text(savedDeviceAddress ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { showDeviceList = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.SwapHoriz, contentDescription = null,
                                modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Changer l'appareil")
                        }
                    }
                }
            } else if (!btEnabled) {
                // Instructions
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Comment ça marche ?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        InstructionStep("1", "Activez le Bluetooth Auto-Park")
                        InstructionStep("2", "Sélectionnez l’appareil Bluetooth de votre voiture")
                        InstructionStep("3", "Coupez le contact : votre position est sauvegardée")
                        InstructionStep("4", "Rallumez le moteur : le parking est archivé")

                    }
                }
            }

            // Étapes indispensables pour que la détection fonctionne en arrière-plan
            if (btEnabled) {
                if (!hasBgLocation) {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.LocationOff, null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("Localisation \"Tout le temps\" requise",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer)
                                    Text("Sans elle, le GPS ne peut pas être capturé quand vous coupez le contact avec l'appli fermée.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer)
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = { bgLocPermission?.launchPermissionRequest() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.LocationOn, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Autoriser \"Tout le temps\"")
                            }
                        }
                    }
                }

                if (!ignoresBatteryOptim) {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.BatteryAlert, null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("Optimisation batterie active",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer)
                                    Text("Votre téléphone peut empêcher Parkar de détecter le Bluetooth en arrière-plan. Désactivez l'optimisation pour Parkar.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer)
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    context.startActivity(
                                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                            Uri.parse("package:${context.packageName}"))
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.BatteryChargingFull, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Désactiver l'optimisation")
                            }
                        }
                    }
                }

                // Journal de diagnostic
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Receipt, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Dernier événement Bluetooth détecté",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(lastBtEvent ?: "Aucun pour l'instant — coupez/rallumez le contact pour tester",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (lastBtEvent != null) FontWeight.Medium else FontWeight.Normal)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // â"€â"€ Dialog de sÃ©lection d'appareil â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€
    if (showDeviceList) {
        PairedDeviceListDialog(
            onSelect = { address, name ->
                BluetoothPreferences.saveDevice(context, address, name)
                savedDeviceName = name
                savedDeviceAddress = address
                btEnabled = true
                showDeviceList = false
                BluetoothMonitorService.start(context)
            },
            onDismiss = {
                showDeviceList = false
                // Si on annule sans sÃ©lectionner, dÃ©sactiver
                if (savedDeviceAddress == null) {
                    BluetoothPreferences.setBluetoothAutoParkEnabled(context, false)
                    btEnabled = false
                }
            }
        )
    }
}

@Composable
private fun InstructionStep(number: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            modifier = Modifier.size(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(number, style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun PairedDeviceListDialog(
    onSelect: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val adapter = btManager.adapter

    val pairedDevices = remember {
        mutableStateOf<List<Pair<String, String>>>(  
            if (adapter != null && adapter.isEnabled) {
                adapter.bondedDevices?.map { (it.name ?: "Sans nom") to it.address }?.sortedBy { it.first.lowercase() }
                    ?: emptyList()
            } else emptyList()
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Appareils Bluetooth") },
        text = {
            if (pairedDevices.value.isEmpty()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.BluetoothDisabled, null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f))
                    Spacer(Modifier.height(8.dp))
                    Text("Aucun appareil appairé",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Assurez-vous que le Bluetooth est activé et que votre voiture est appairée.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Text("Sélectionnez l'appareil Bluetooth de votre voiture :",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    pairedDevices.value.forEach { (name, address) ->
                        ListItem(
                            headlineContent = { Text(name) },
                            supportingContent = { Text(address) },
                            leadingContent = {
                                Icon(Icons.Filled.Bluetooth, contentDescription = null)
                            },
                            modifier = Modifier.fillMaxWidth().clickable { onSelect(address, name) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}