package com.carparking.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.carparking.app.data.model.Car
import com.carparking.app.ui.components.CrashLogsCard
import com.carparking.app.ui.components.UpdateCheckCard
import com.carparking.app.ui.navigation.Screen
import com.carparking.app.ui.viewmodel.CarViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarListScreen(navController: NavController) {
    val vm: CarViewModel = viewModel()
    val cars by vm.cars.collectAsStateWithLifecycle()
    var carToDelete by remember { mutableStateOf<Car?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mes voitures", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.BluetoothSettings.route) }) {
                        Icon(Icons.Filled.Bluetooth, contentDescription = "Bluetooth Auto-Park",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate(Screen.AddCar.route) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Ajouter une voiture", fontWeight = FontWeight.SemiBold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { padding ->
        if (cars.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
            ) {
                CrashLogsCard(modifier = Modifier.padding(bottom = 16.dp))
                UpdateCheckCard()
                Spacer(Modifier.height(16.dp))
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.DirectionsCar,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Aucune voiture",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Appuyez sur + pour en ajouter une",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { CrashLogsCard() }
                item { UpdateCheckCard() }
                items(cars, key = { it.id }) { car ->
                    CarListItem(
                        car = car,
                        onEdit = { navController.navigate(Screen.EditCar.createRoute(car.id)) },
                        onDelete = { carToDelete = car }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    carToDelete?.let { car ->
        AlertDialog(
            onDismissRequest = { carToDelete = null },
            icon = { Icon(Icons.Filled.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Supprimer ${car.name} ?") },
            text = { Text("Cette action supprimera également tous les historiques de stationnement de cette voiture.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteCar(car)
                    carToDelete = null
                }) {
                    Text("Supprimer", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { carToDelete = null }) { Text("Annuler") }
            }
        )
    }
}

@Composable
private fun CarListItem(car: Car, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (car.photoPath != null && File(car.photoPath).exists()) {
                AsyncImage(
                    model = File(car.photoPath),
                    contentDescription = car.name,
                    modifier = Modifier.size(52.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ) {
                    Icon(
                        Icons.Filled.DirectionsCar,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Text(
                car.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "Modifier", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
