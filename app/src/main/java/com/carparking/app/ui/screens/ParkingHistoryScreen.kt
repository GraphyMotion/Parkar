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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.carparking.app.data.model.ParkingRecord
import com.carparking.app.ui.theme.ParkingGreen
import com.carparking.app.ui.theme.ParkingOrange
import com.carparking.app.ui.viewmodel.CarViewModel
import com.carparking.app.ui.viewmodel.ParkingViewModel
import com.carparking.app.utils.ExportUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParkingHistoryScreen(navController: NavController, carId: Int) {
    val context = LocalContext.current
    val carVm: CarViewModel = viewModel()
    val parkingVm: ParkingViewModel = viewModel()
    val cars by carVm.cars.collectAsStateWithLifecycle()
    val car = cars.firstOrNull { it.id == carId }
    val records by parkingVm.getParkingsByCar(carId).collectAsStateWithLifecycle(initialValue = emptyList())

    var query by remember { mutableStateOf("") }
    val filteredRecords = remember(records, query) {
        if (query.isBlank()) records
        else records.filter {
            it.address?.contains(query, ignoreCase = true) == true ||
            it.note?.contains(query, ignoreCase = true) == true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Historique", fontWeight = FontWeight.Bold)
                        car?.let {
                            Text(it.name, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "Retour")
                    }
                },
                actions = {
                    if (records.isNotEmpty()) {
                        IconButton(onClick = {
                            ExportUtils.shareAsCsv(context, car?.name ?: "voiture", filteredRecords)
                        }) {
                            Icon(Icons.Filled.Share, "Exporter en CSV",
                                tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        if (records.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.History, null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.3f))
                    Spacer(Modifier.height(16.dp))
                    Text("Aucun historique", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Bloc stats
                item { StatsCard(records = records) }

                item {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Rechercher par adresse ou note…") },
                        leadingIcon = { Icon(Icons.Filled.Search, null) },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(Icons.Filled.Clear, "Effacer")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                item {
                    Text(
                        if (query.isBlank()) "Tous les stationnements (${filteredRecords.size})"
                        else "Résultats (${filteredRecords.size}/${records.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (filteredRecords.isEmpty()) {
                    item {
                        Text("Aucun résultat pour « $query »",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    items(filteredRecords, key = { it.id }) { record ->
                        HistoryItem(record = record)
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun StatsCard(records: List<ParkingRecord>) {
    val total = records.size
    val active = records.count { it.isActive }
    val withPhoto = records.count { !it.photoPath.isNullOrEmpty() }
    val withNote = records.count { !it.note.isNullOrEmpty() }
    val mostFrequentAddress = records
        .filter { !it.address.isNullOrEmpty() }
        .groupBy { it.address }
        .maxByOrNull { it.value.size }
        ?.key

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(0.dp)) {
            // En-tête dégradé
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.BarChart, null, tint = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(10.dp))
                    Text("Statistiques", fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleMedium)
                }
            }

            // Chiffres clés
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatChip(value = "$total", label = "Parkings")
                StatChip(value = "$withPhoto", label = "Photos")
                StatChip(value = "$withNote", label = "Notes")
            }

            if (!mostFrequentAddress.isNullOrEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Star, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Zone favorite", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(mostFrequentAddress, style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HistoryItem(record: ParkingRecord) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.getDefault()) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            // Indicateur actif/inactif
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (record.isActive) ParkingGreen else ParkingOrange)
                )
                Spacer(Modifier.height(4.dp))
                // Ligne verticale (timeline)
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(36.dp)
                        .background(
                            if (record.isActive) ParkingGreen.copy(alpha = 0.3f)
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Badge
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (record.isActive) ParkingGreen.copy(0.15f) else MaterialTheme.colorScheme.outline.copy(0.1f)
                ) {
                    Text(
                        if (record.isActive) "En cours" else "Terminé",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (record.isActive) ParkingGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(4.dp))
                Text(dateFormat.format(Date(record.parkedAt)),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium)

                if (!record.address.isNullOrEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocationOn, null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(3.dp))
                        Text(record.address, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                if (!record.note.isNullOrEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Notes, null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(3.dp))
                        Text(record.note, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            // Icônes indicateurs
            Column(horizontalAlignment = Alignment.End) {
                if (!record.photoPath.isNullOrEmpty()) {
                    Icon(Icons.Filled.Photo, null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary)
                }
                if (record.reminderAt != null) {
                    Spacer(Modifier.height(4.dp))
                    Icon(Icons.Filled.Alarm, null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
