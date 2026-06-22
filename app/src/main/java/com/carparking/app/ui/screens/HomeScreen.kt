package com.carparking.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.carparking.app.data.model.Car
import com.carparking.app.data.model.ParkingRecord
import com.carparking.app.ui.navigation.Screen
import com.carparking.app.bluetooth.BluetoothPreferences
import com.carparking.app.ui.theme.GoldAccent
import com.carparking.app.ui.theme.ParkingGreen
import com.carparking.app.ui.theme.ParkingOrange
import com.carparking.app.ui.viewmodel.CarViewModel
import com.carparking.app.ui.viewmodel.ParkingViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val carVm: CarViewModel = viewModel()
    val parkingVm: ParkingViewModel = viewModel()
    val cars by carVm.cars.collectAsStateWithLifecycle()
    val activeParkings by parkingVm.allActiveParkings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.DirectionsCar,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Parkar",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.BluetoothSettings.route) }) {
                        Icon(Icons.Filled.Bluetooth, contentDescription = "Bluetooth Auto-Park")
                    }
                    IconButton(onClick = { navController.navigate(Screen.CarList.route) }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Gérer les voitures")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        if (cars.isEmpty()) {
            EmptyHomeState(
                modifier = Modifier.padding(padding),
                onAddCar = { navController.navigate(Screen.AddCar.route) }
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text(
                        "Mes voitures",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(cars, key = { it.id }) { car ->
                    val activeParking = activeParkings.firstOrNull { it.carId == car.id }
                    CarParkingCard(
                        car = car,
                        parking = activeParking,
                        onParkHere = { navController.navigate(Screen.SaveParking.createRoute(car.id)) },
                        onViewParking = { navController.navigate(Screen.ParkingDetail.createRoute(car.id)) }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun EmptyHomeState(modifier: Modifier = Modifier, onAddCar: () -> Unit) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.DirectionsCar,
            contentDescription = null,
            modifier = Modifier.size(90.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "Aucune voiture enregistrée",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Ajoutez votre première voiture pour\ncommencer à sauvegarder vos parkings",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onAddCar,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.height(52.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Ajouter une voiture", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun CarParkingCard(
    car: Car,
    parking: ParkingRecord?,
    onParkHere: () -> Unit,
    onViewParking: () -> Unit
) {
    val isParked = parking != null
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isParked) 6.dp else 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(0.dp)) {
            // Header gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            if (isParked)
                                listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
                            else
                                listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant)
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Car photo
                    if (car.photoPath != null && File(car.photoPath).exists()) {
                        AsyncImage(
                            model = File(car.photoPath),
                            contentDescription = car.name,
                            modifier = Modifier.size(54.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(
                            modifier = Modifier.size(54.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        ) {
                            Icon(
                                Icons.Filled.DirectionsCar,
                                contentDescription = null,
                                modifier = Modifier.padding(12.dp),
                                tint = if (isParked) MaterialTheme.colorScheme.onPrimary
                                       else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            car.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isParked) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        StatusBadge(isParked = isParked)
                    }
                }
            }

            // Body
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                if (isParked && parking != null) {
                    if (!parking.address.isNullOrEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                parking.address,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Garé ${formatDuration(parking.parkedAt)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!parking.note.isNullOrEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                Icons.Filled.Notes,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                parking.note,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onViewParking,
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Voir sur la carte", fontSize = 13.sp)
                        }
                        OutlinedButton(
                            onClick = onParkHere,
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Re-garer ici", fontSize = 13.sp)
                        }
                    }
                } else {
                    Text(
                        "Position non enregistrée",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onParkHere,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.AddLocation, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Je me gare ici !", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(isParked: Boolean) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (isParked) ParkingGreen.copy(alpha = 0.2f)
                else ParkingOrange.copy(alpha = 0.2f),
        modifier = Modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (isParked) ParkingGreen else ParkingOrange)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                if (isParked) "Garée" else "Non garée",
                style = MaterialTheme.typography.labelMedium,
                color = if (isParked) ParkingGreen else ParkingOrange,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun formatDuration(parkedAtMs: Long): String {
    val diff = System.currentTimeMillis() - parkedAtMs
    val minutes = diff / 60_000
    val hours = minutes / 60
    val days = hours / 24
    return when {
        days > 0   -> "il y a ${days}j ${hours % 24}h"
        hours > 0  -> "il y a ${hours}h ${minutes % 60}min"
        minutes > 0 -> "il y a ${minutes}min"
        else       -> "à l'instant"
    }
}
