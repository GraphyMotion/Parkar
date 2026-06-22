package com.carparking.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.carparking.app.data.model.Car
import com.carparking.app.ui.viewmodel.CarViewModel
import com.carparking.app.utils.ImageUtils
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCarScreen(navController: NavController, carId: Int?) {
    val vm: CarViewModel = viewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isEdit = carId != null

    var name by remember { mutableStateOf("") }
    var photoPath by remember { mutableStateOf<String?>(null) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var tempImageFile by remember { mutableStateOf<File?>(null) }
    var nameError by remember { mutableStateOf(false) }

    // Load car data if editing
    LaunchedEffect(carId) {
        if (carId != null) {
            vm.getCarById(carId)?.let { car ->
                name = car.name
                photoPath = car.photoPath
            }
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempImageFile?.let { file ->
                photoPath = file.absolutePath
                photoUri = null
            }
        }
    }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val newPath = ImageUtils.copyUriToFile(context, uri, "CAR_${timestamp}.jpg")
            if (newPath != null) {
                photoPath = newPath
                photoUri = null
            }
        }
    }

    var showPhotoMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEdit) "Modifier la voiture" else "Nouvelle voiture",
                        fontWeight = FontWeight.Bold
                    )
                },
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            // Photo picker
            Box(contentAlignment = Alignment.BottomEnd) {
                if (photoPath != null && File(photoPath!!).exists()) {
                    AsyncImage(
                        model = File(photoPath!!),
                        contentDescription = "Photo de la voiture",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .clickable { showPhotoMenu = true },
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .clickable { showPhotoMenu = true }
                            .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), CircleShape),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Icon(
                            Icons.Filled.DirectionsCar,
                            contentDescription = null,
                            modifier = Modifier.padding(28.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(34.dp).clickable { showPhotoMenu = true }
                ) {
                    Icon(
                        Icons.Filled.CameraAlt,
                        contentDescription = "Ajouter photo",
                        modifier = Modifier.padding(7.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "Appuyez pour ajouter une photo",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(32.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; nameError = false },
                label = { Text("Nom de la voiture") },
                placeholder = { Text("Ex: Peugeot 208, Voiture de Marie...") },
                leadingIcon = { Icon(Icons.Filled.DirectionsCar, contentDescription = null) },
                isError = nameError,
                supportingText = if (nameError) {{ Text("Le nom est obligatoire") }} else null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            Spacer(Modifier.height(40.dp))

            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                        return@Button
                    }
                    scope.launch {
                        if (isEdit && carId != null) {
                            val existing = vm.getCarById(carId)
                            if (existing != null) {
                                vm.updateCar(existing.copy(name = name.trim(), photoPath = photoPath))
                            }
                        } else {
                            vm.addCar(Car(name = name.trim(), photoPath = photoPath))
                        }
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(if (isEdit) Icons.Filled.Check else Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (isEdit) "Enregistrer les modifications" else "Ajouter la voiture",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // Photo source menu
    if (showPhotoMenu) {
        AlertDialog(
            onDismissRequest = { showPhotoMenu = false },
            title = { Text("Choisir une photo") },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text("Prendre une photo") },
                        leadingContent = { Icon(Icons.Filled.CameraAlt, contentDescription = null) },
                        modifier = Modifier.clickable {
                            showPhotoMenu = false
                            val file = ImageUtils.createImageFile(context)
                            tempImageFile = file
                            val uri = ImageUtils.getUriForFile(context, file)
                            cameraLauncher.launch(uri)
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Choisir depuis la galerie") },
                        leadingContent = { Icon(Icons.Filled.Photo, contentDescription = null) },
                        modifier = Modifier.clickable {
                            showPhotoMenu = false
                            galleryLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        }
                    )
                    if (photoPath != null) {
                        ListItem(
                            headlineContent = { Text("Supprimer la photo", color = MaterialTheme.colorScheme.error) },
                            leadingContent = { Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            modifier = Modifier.clickable {
                                showPhotoMenu = false
                                photoPath = null
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPhotoMenu = false }) { Text("Annuler") }
            }
        )
    }
}
