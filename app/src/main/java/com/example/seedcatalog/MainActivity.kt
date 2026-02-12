package com.example.seedcatalog

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.example.seedcatalog.data.local.AppDatabase
import com.example.seedcatalog.data.local.PacketLot
import com.example.seedcatalog.data.local.PacketLotWithPhotos
import com.example.seedcatalog.data.local.Photo
import com.example.seedcatalog.data.local.PhotoType
import com.example.seedcatalog.data.local.Plant
import com.example.seedcatalog.data.local.PlantFilterOptions
import com.example.seedcatalog.data.repository.OfflineSeedRepository
import com.example.seedcatalog.data.BackupManager
import com.example.seedcatalog.ui.SeedViewModel
import com.example.seedcatalog.ui.SeedViewModelFactory
import com.example.seedcatalog.ui.theme.SeedCatalogTheme
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class Screen(val route: String) {
    data object SeedList : Screen("seedList")
    data object SeedDetail : Screen("seedDetail/{plantId}") {
        fun createRoute(plantId: Int) = "seedDetail/$plantId"
    }
    data object Settings : Screen("settings")
}

class MainActivity : ComponentActivity() {
    private val viewModel: SeedViewModel by viewModels {
        val db = AppDatabase.getInstance(applicationContext)
        SeedViewModelFactory(
            OfflineSeedRepository(
                plantDao = db.plantDao(),
                packetLotDao = db.packetLotDao(),
                photoDao = db.photoDao()
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SeedCatalogTheme {
                SeedCatalogApp(viewModel)
            }
        }
    }
}

@Composable
private fun SeedCatalogApp(seedViewModel: SeedViewModel = viewModel()) {
    val navController = rememberNavController()
    val plants by seedViewModel.plants.collectAsStateWithLifecycle()
    val filterOptions by seedViewModel.filterOptions.collectAsStateWithLifecycle()
    val searchQuery by seedViewModel.uiSearchQuery.collectAsStateWithLifecycle()
    val selectedPlantType by seedViewModel.uiPlantType.collectAsStateWithLifecycle()
    val selectedLightRequirement by seedViewModel.uiLightRequirement.collectAsStateWithLifecycle()
    val selectedIndoorOutdoor by seedViewModel.uiIndoorOutdoor.collectAsStateWithLifecycle()

    NavHost(navController = navController, startDestination = Screen.SeedList.route) {
        composable(Screen.SeedList.route) {
            SeedListScreen(
                plants = plants,
                filterOptions = filterOptions,
                searchQuery = searchQuery,
                selectedPlantType = selectedPlantType,
                selectedLightRequirement = selectedLightRequirement,
                selectedIndoorOutdoor = selectedIndoorOutdoor,
                onSearchQueryChange = seedViewModel::updateSearchQuery,
                onPlantTypeFilterChange = seedViewModel::updatePlantTypeFilter,
                onLightRequirementFilterChange = seedViewModel::updateLightRequirementFilter,
                onIndoorOutdoorFilterChange = seedViewModel::updateIndoorOutdoorFilter,
                onOpenSettings = { navController.navigate(Screen.Settings.route) },
                onSeedClick = { navController.navigate(Screen.SeedDetail.createRoute(it)) },
                onSavePlant = { botanicalName, commonName, variety, plantType, lightRequirement, indoorOutdoor, description, medicinalUses, culinaryUses, growingInstructions, notes ->
                    seedViewModel.createPlant(
                        botanicalName,
                        commonName,
                        variety,
                        plantType,
                        lightRequirement,
                        indoorOutdoor,
                        description,
                        medicinalUses,
                        culinaryUses,
                        growingInstructions,
                        notes
                    )
                },
                onUpdatePlant = { seedViewModel.updatePlant(it) },
                onDeletePlant = { seedViewModel.deletePlant(it) }
            )
        }
        composable(
            route = Screen.SeedDetail.route,
            arguments = listOf(navArgument("plantId") { type = NavType.IntType })
        ) { backStackEntry ->
            val plantId = backStackEntry.arguments?.getInt("plantId") ?: -1
            val plant by seedViewModel.plant(plantId).collectAsStateWithLifecycle(initialValue = null)
            val lots by seedViewModel.lotsWithPhotos(plantId).collectAsStateWithLifecycle(initialValue = emptyList())
            SeedDetailScreen(
                plant = plant,
                lots = lots,
                onBack = { navController.popBackStack() },
                onCreateLot = { lotCode, quantity, notes ->
                    seedViewModel.createPacketLot(plantId, lotCode, quantity, notes)
                },
                onUpdateLot = { seedViewModel.updatePacketLot(it) },
                onDeleteLot = { seedViewModel.deletePacketLot(it) },
                onAttachPhotos = { lotId, type, uris ->
                    uris.forEach { uri -> seedViewModel.addPhoto(lotId, uri, type) }
                },
                onDeletePhoto = { seedViewModel.deletePhoto(it) }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SeedListScreen(
    plants: List<Plant>,
    filterOptions: PlantFilterOptions,
    searchQuery: String,
    selectedPlantType: String,
    selectedLightRequirement: String,
    selectedIndoorOutdoor: String,
    onSearchQueryChange: (String) -> Unit,
    onPlantTypeFilterChange: (String) -> Unit,
    onLightRequirementFilterChange: (String) -> Unit,
    onIndoorOutdoorFilterChange: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onSeedClick: (Int) -> Unit,
    onSavePlant: (String, String, String, String, String, String, String, String, String, String, String) -> Unit,
    onUpdatePlant: (Plant) -> Unit,
    onDeletePlant: (Plant) -> Unit
) {
    var editingPlant by remember { mutableStateOf<Plant?>(null) }
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Plants") },
            actions = {
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }
        )
    }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(onClick = { showCreateDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Add Plant")
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search plants") },
                singleLine = true
            )

            Text("Plant type")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedPlantType.isBlank(),
                    onClick = { onPlantTypeFilterChange("") },
                    label = { Text("All") }
                )
                filterOptions.plantTypes.forEach { option ->
                    FilterChip(
                        selected = selectedPlantType == option,
                        onClick = { onPlantTypeFilterChange(option) },
                        label = { Text(option) }
                    )
                }
            }

            Text("Light")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedLightRequirement.isBlank(),
                    onClick = { onLightRequirementFilterChange("") },
                    label = { Text("All") }
                )
                filterOptions.lightRequirements.forEach { option ->
                    FilterChip(
                        selected = selectedLightRequirement == option,
                        onClick = { onLightRequirementFilterChange(option) },
                        label = { Text(option) }
                    )
                }
            }

            Text("Environment")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedIndoorOutdoor.isBlank(),
                    onClick = { onIndoorOutdoorFilterChange("") },
                    label = { Text("All") }
                )
                filterOptions.indoorOutdoorOptions.forEach { option ->
                    FilterChip(
                        selected = selectedIndoorOutdoor == option,
                        onClick = { onIndoorOutdoorFilterChange(option) },
                        label = { Text(option) }
                    )
                }
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(plants, key = { it.id }) { plant ->
                    Card(modifier = Modifier.fillMaxWidth(), onClick = { onSeedClick(plant.id) }) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(plant.commonName.ifBlank { plant.botanicalName }, fontWeight = FontWeight.Bold)
                            Text("Botanical: ${plant.botanicalName.ifBlank { "N/A" }}")
                            Text("Type: ${plant.plantType}")
                            Text("Light: ${plant.lightRequirement}")
                            Text("Environment: ${plant.indoorOutdoor}")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { editingPlant = plant }) { Text("Edit") }
                                TextButton(onClick = { onDeletePlant(plant) }) { Text("Delete") }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        PlantDialog(
            title = "Add Plant",
            onDismiss = { showCreateDialog = false },
            onSave = { botanicalName, commonName, variety, plantType, lightRequirement, indoorOutdoor, description, medicinalUses, culinaryUses, growingInstructions, notes ->
                onSavePlant(
                    botanicalName,
                    commonName,
                    variety,
                    plantType,
                    lightRequirement,
                    indoorOutdoor,
                    description,
                    medicinalUses,
                    culinaryUses,
                    growingInstructions,
                    notes
                )
                showCreateDialog = false
            }
        )
    }

    editingPlant?.let { plant ->
        PlantDialog(
            title = "Edit Plant",
            initialBotanicalName = plant.botanicalName,
            initialCommonName = plant.commonName,
            initialVariety = plant.variety,
            initialPlantType = plant.plantType,
            initialLightRequirement = plant.lightRequirement,
            initialIndoorOutdoor = plant.indoorOutdoor,
            initialDescription = plant.description,
            initialMedicinalUses = plant.medicinalUses,
            initialCulinaryUses = plant.culinaryUses,
            initialGrowingInstructions = plant.growingInstructions,
            initialNotes = plant.notes,
            onDismiss = { editingPlant = null },
            onSave = { botanicalName, commonName, variety, plantType, lightRequirement, indoorOutdoor, description, medicinalUses, culinaryUses, growingInstructions, notes ->
                onUpdatePlant(
                    plant.copy(
                        botanicalName = botanicalName,
                        commonName = commonName,
                        variety = variety,
                        plantType = plantType,
                        lightRequirement = lightRequirement,
                        indoorOutdoor = indoorOutdoor,
                        description = description,
                        medicinalUses = medicinalUses,
                        culinaryUses = culinaryUses,
                        growingInstructions = growingInstructions,
                        notes = notes
                    )
                )
                editingPlant = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeedDetailScreen(
    plant: Plant?,
    lots: List<PacketLotWithPhotos>,
    onBack: () -> Unit,
    onCreateLot: (String, Int, String) -> Unit,
    onUpdateLot: (PacketLot) -> Unit,
    onDeleteLot: (PacketLot) -> Unit,
    onAttachPhotos: (Int, String, List<String>) -> Unit,
    onDeletePhoto: (Photo) -> Unit
) {
    val context = LocalContext.current
    var showLotDialog by rememberSaveable { mutableStateOf(false) }
    var editingLot by remember { mutableStateOf<PacketLot?>(null) }
    var lotForPhotoPicker by remember { mutableStateOf<PacketLot?>(null) }
    var pendingPhotoType by rememberSaveable { mutableStateOf(PhotoType.FRONT.dbValue) }
    var showPhotoTypeDialog by rememberSaveable { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: SecurityException) {
                }
            }
            lotForPhotoPicker?.let { lot ->
                onAttachPhotos(lot.id, pendingPhotoType, uris.map { it.toString() })
            }
        }
        lotForPhotoPicker = null
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Seed Detail") }) }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (plant == null) {
                Text("Plant not found")
            } else {
                Text(plant.commonName.ifBlank { plant.botanicalName }, style = MaterialTheme.typography.headlineSmall)
                Text("Botanical: ${plant.botanicalName}")
                Text("Variety: ${plant.variety}")
                Text("Type: ${plant.plantType}")
                Text("Light: ${plant.lightRequirement}")
                Text("Environment: ${plant.indoorOutdoor}")
                Text("Description: ${plant.description}")
                Text("Medicinal uses: ${plant.medicinalUses}")
                Text("Culinary uses: ${plant.culinaryUses}")
                Text("Growing instructions: ${plant.growingInstructions}")
                Text("Notes: ${plant.notes}")
                Button(onClick = { showLotDialog = true }) { Text("Add Packet Lot") }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(lots, key = { it.packetLot.id }) { lotWithPhotos ->
                        val lot = lotWithPhotos.packetLot
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Lot ${lot.lotCode}", fontWeight = FontWeight.Bold)
                                Text("Quantity: ${lot.quantity}")
                                Text("Notes: ${lot.notes}")

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = { editingLot = lot }) { Text("Edit") }
                                    TextButton(onClick = { onDeleteLot(lot) }) { Text("Delete") }
                                    TextButton(onClick = {
                                        lotForPhotoPicker = lot
                                        showPhotoTypeDialog = true
                                    }) { Text("Attach Photos") }
                                }

                                if (lotWithPhotos.photos.isEmpty()) {
                                    Text("No photos")
                                } else {
                                    PhotoCarousel(
                                        lotCode = lot.lotCode,
                                        photos = lotWithPhotos.photos,
                                        onDeletePhoto = onDeletePhoto
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Button(onClick = onBack) { Text("Back") }
        }
    }

    if (showPhotoTypeDialog) {
        PhotoTypeDialog(
            onDismiss = {
                showPhotoTypeDialog = false
                lotForPhotoPicker = null
            },
            onSelectType = { type ->
                pendingPhotoType = type
                showPhotoTypeDialog = false
                launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        )
    }

    if (showLotDialog) {
        PacketLotDialog(
            title = "Add Packet Lot",
            onDismiss = { showLotDialog = false },
            onSave = { lotCode, quantity, notes ->
                onCreateLot(lotCode, quantity, notes)
                showLotDialog = false
            }
        )
    }

    editingLot?.let { lot ->
        PacketLotDialog(
            title = "Edit Packet Lot",
            initialLotCode = lot.lotCode,
            initialQuantity = lot.quantity.toString(),
            initialNotes = lot.notes,
            onDismiss = { editingLot = null },
            onSave = { lotCode, quantity, notes ->
                onUpdateLot(lot.copy(lotCode = lotCode, quantity = quantity, notes = notes))
                editingLot = null
            }
        )
    }
}

@Composable
private fun PhotoCarousel(
    lotCode: String,
    photos: List<Photo>,
    onDeletePhoto: (Photo) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { photos.size })
    val currentPhoto = photos[pagerState.currentPage]

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth()) { page ->
            val photo = photos[page]
            AsyncImage(
                model = photo.uri,
                contentDescription = "Photo for lot $lotCode",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                contentScale = ContentScale.Crop
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("${photoTypeDisplay(currentPhoto.type)} • ${pagerState.currentPage + 1}/${photos.size}")
            TextButton(onClick = { onDeletePhoto(currentPhoto) }) {
                Text("Delete Photo")
            }
        }
    }
}

@Composable
private fun PhotoTypeDialog(
    onDismiss: () -> Unit,
    onSelectType: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose photo type") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PhotoType.entries.forEach { type ->
                    TextButton(onClick = { onSelectType(type.dbValue) }, modifier = Modifier.fillMaxWidth()) {
                        Text(type.displayName)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val backupManager = remember { BackupManager(context, db) }
    val scope = rememberCoroutineScope()
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = backupManager.exportToZip(uri)
            Toast.makeText(context, result.warningMessage ?: "Export completed", Toast.LENGTH_LONG).show()
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingRestoreUri = uri
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(onClick = { exportLauncher.launch("seed_catalog_backup.zip") }, modifier = Modifier.fillMaxWidth()) {
                Text("Export backup")
            }
            Button(onClick = { restoreLauncher.launch(arrayOf("application/zip")) }, modifier = Modifier.fillMaxWidth()) {
                Text("Restore backup")
            }
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Back")
            }
        }
    }

    if (pendingRestoreUri != null) {
        AlertDialog(
            onDismissRequest = { pendingRestoreUri = null },
            title = { Text("Restore backup?") },
            text = { Text("Import replaces your current database. This action is destructive and cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    val uri = pendingRestoreUri
                    pendingRestoreUri = null
                    if (uri != null) {
                        scope.launch {
                            val result = backupManager.restoreFromZip(uri)
                            Toast.makeText(context, result.warningMessage ?: "Restore completed", Toast.LENGTH_LONG).show()
                        }
                    }
                }) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreUri = null }) { Text("Cancel") }
            }
        )
    }
}

private fun photoTypeDisplay(dbValue: String): String =
    PhotoType.entries.firstOrNull { it.dbValue == dbValue }?.displayName ?: dbValue

@Composable
private fun PlantDialog(
    title: String,
    initialBotanicalName: String = "",
    initialCommonName: String = "",
    initialVariety: String = "",
    initialPlantType: String = "",
    initialLightRequirement: String = "",
    initialIndoorOutdoor: String = "",
    initialDescription: String = "",
    initialMedicinalUses: String = "",
    initialCulinaryUses: String = "",
    initialGrowingInstructions: String = "",
    initialNotes: String = "",
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String, String, String, String, String, String) -> Unit
) {
    var botanicalName by rememberSaveable { mutableStateOf(initialBotanicalName) }
    var commonName by rememberSaveable { mutableStateOf(initialCommonName) }
    var variety by rememberSaveable { mutableStateOf(initialVariety) }
    var plantType by rememberSaveable { mutableStateOf(initialPlantType) }
    var lightRequirement by rememberSaveable { mutableStateOf(initialLightRequirement) }
    var indoorOutdoor by rememberSaveable { mutableStateOf(initialIndoorOutdoor) }
    var description by rememberSaveable { mutableStateOf(initialDescription) }
    var medicinalUses by rememberSaveable { mutableStateOf(initialMedicinalUses) }
    var culinaryUses by rememberSaveable { mutableStateOf(initialCulinaryUses) }
    var growingInstructions by rememberSaveable { mutableStateOf(initialGrowingInstructions) }
    var notes by rememberSaveable { mutableStateOf(initialNotes) }

    var frontCapture by remember { mutableStateOf<Bitmap?>(null) }
    var backCapture by remember { mutableStateOf<Bitmap?>(null) }
    var closeUpCapture by remember { mutableStateOf<Bitmap?>(null) }
    var ocrResult by remember { mutableStateOf<SeedPacketOcrResult?>(null) }
    var isScanning by remember { mutableStateOf(false) }
    var rawTextExpanded by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val frontCaptureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) {
        frontCapture = it
    }
    val backCaptureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) {
        backCapture = it
    }
    val closeUpCaptureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) {
        closeUpCapture = it
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Text("Scan seed packet", style = MaterialTheme.typography.titleMedium) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { frontCaptureLauncher.launch(null) }) { Text("Capture front") }
                        Button(onClick = { backCaptureLauncher.launch(null) }) { Text("Capture back") }
                        Button(onClick = { closeUpCaptureLauncher.launch(null) }) { Text("Capture close-up") }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ScanCapturePreview(label = "Front", capture = frontCapture)
                        ScanCapturePreview(label = "Back", capture = backCapture)
                        ScanCapturePreview(label = "Close-up", capture = closeUpCapture)
                    }
                }
                item {
                    Button(
                        onClick = {
                            val captures = listOfNotNull(
                                frontCapture?.let { "Front" to it },
                                backCapture?.let { "Back" to it },
                                closeUpCapture?.let { "Close-up" to it }
                            )
                            scope.launch {
                                isScanning = true
                                val result = runSeedPacketOcr(captures)
                                ocrResult = result
                                if (result.botanicalName.isNotBlank()) botanicalName = result.botanicalName
                                if (result.variety.isNotBlank()) variety = result.variety
                                if (result.brand.isNotBlank()) {
                                    notes = listOf(notes, "Brand: ${result.brand}")
                                        .filter { it.isNotBlank() }
                                        .joinToString("\n")
                                }
                                isScanning = false
                            }
                        },
                        enabled = !isScanning && listOf(frontCapture, backCapture, closeUpCapture).any { it != null },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isScanning) "Scanning…" else "Run OCR and prefill")
                    }
                }
                if (isScanning) {
                    item { CircularProgressIndicator() }
                }
                ocrResult?.let { result ->
                    item { Text("Likely botanical name: ${result.botanicalName.ifBlank { "Not found" }}") }
                    item { Text("Likely variety: ${result.variety.ifBlank { "Not found" }}") }
                    item { Text("Likely brand: ${result.brand.ifBlank { "Not found" }}") }
                    item {
                        TextButton(onClick = { rawTextExpanded = !rawTextExpanded }) {
                            Text(if (rawTextExpanded) "Hide raw OCR text" else "Show raw OCR text")
                        }
                    }
                    if (rawTextExpanded) {
                        item { Text(result.mergedText.ifBlank { "No text recognized." }) }
                    }
                }
                item { OutlinedTextField(value = botanicalName, onValueChange = { botanicalName = it }, label = { Text("Botanical name") }) }
                item { OutlinedTextField(value = commonName, onValueChange = { commonName = it }, label = { Text("Common name") }) }
                item { OutlinedTextField(value = variety, onValueChange = { variety = it }, label = { Text("Variety") }) }
                item { OutlinedTextField(value = plantType, onValueChange = { plantType = it }, label = { Text("Plant type") }) }
                item { OutlinedTextField(value = lightRequirement, onValueChange = { lightRequirement = it }, label = { Text("Light") }) }
                item { OutlinedTextField(value = indoorOutdoor, onValueChange = { indoorOutdoor = it }, label = { Text("Indoor / outdoor") }) }
                item { OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }) }
                item { OutlinedTextField(value = medicinalUses, onValueChange = { medicinalUses = it }, label = { Text("Medicinal uses") }) }
                item { OutlinedTextField(value = culinaryUses, onValueChange = { culinaryUses = it }, label = { Text("Culinary uses") }) }
                item { OutlinedTextField(value = growingInstructions, onValueChange = { growingInstructions = it }, label = { Text("Growing instructions") }) }
                item { OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        botanicalName,
                        commonName,
                        variety,
                        plantType,
                        lightRequirement,
                        indoorOutdoor,
                        description,
                        medicinalUses,
                        culinaryUses,
                        growingInstructions,
                        notes
                    )
                },
                enabled = commonName.isNotBlank() || botanicalName.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun PacketLotDialog(
    title: String,
    initialLotCode: String = "",
    initialQuantity: String = "",
    initialNotes: String = "",
    onDismiss: () -> Unit,
    onSave: (String, Int, String) -> Unit
) {
    var lotCode by rememberSaveable { mutableStateOf(initialLotCode) }
    var quantityText by rememberSaveable { mutableStateOf(initialQuantity) }
    var notes by rememberSaveable { mutableStateOf(initialNotes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = lotCode, onValueChange = { lotCode = it }, label = { Text("Lot code") })
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it.filter { c -> c.isDigit() } },
                    label = { Text("Quantity") }
                )
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") })
            }
        },
        confirmButton = {
            val quantity = quantityText.toIntOrNull() ?: 0
            TextButton(onClick = { onSave(lotCode, quantity, notes) }, enabled = lotCode.isNotBlank()) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ScanCapturePreview(label: String, capture: Bitmap?) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        if (capture == null) {
            Text("Not captured", style = MaterialTheme.typography.bodySmall)
        } else {
            Image(
                bitmap = capture.asImageBitmap(),
                contentDescription = "$label capture",
                modifier = Modifier.height(64.dp)
            )
        }
    }
}

private data class SeedPacketOcrResult(
    val mergedText: String,
    val botanicalName: String,
    val variety: String,
    val brand: String
)

private suspend fun runSeedPacketOcr(captures: List<Pair<String, Bitmap>>): SeedPacketOcrResult {
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    return try {
        val sections = captures.mapNotNull { (label, bitmap) ->
            val image = InputImage.fromBitmap(bitmap, 0)
            val text = recognizer.process(image).await().text.trim()
            if (text.isBlank()) null else "$label:\n$text"
        }
        val mergedText = sections.joinToString("\n\n")
        val lines = mergedText
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.endsWith(":") }
            .toList()
        SeedPacketOcrResult(
            mergedText = mergedText,
            botanicalName = extractLikelyBotanicalName(lines),
            variety = extractLikelyVariety(lines),
            brand = extractLikelyBrand(lines)
        )
    } finally {
        recognizer.close()
    }
}

private fun extractLikelyBotanicalName(lines: List<String>): String {
    val botanicalRegex = Regex("\\b([A-Z][a-z]{2,})\\s+([a-z]{3,})(?:\\s+(?:subsp\\.?|var\\.?|x)\\s+[a-z-]+)?")
    return lines
        .asSequence()
        .filterNot { it.contains("seed", ignoreCase = true) || it.contains("brand", ignoreCase = true) }
        .mapNotNull { line -> botanicalRegex.find(line)?.value }
        .firstOrNull()
        .orEmpty()
}

private fun extractLikelyVariety(lines: List<String>): String {
    val indicators = listOf("variety", "cultivar", "cv.", "type")
    val indicatorLine = lines.firstOrNull { line -> indicators.any { marker -> line.contains(marker, ignoreCase = true) } }
    if (indicatorLine != null) return indicatorLine
    val quoted = Regex("['\"][^'\"]{3,}['\"]")
    return lines.mapNotNull { line -> quoted.find(line)?.value?.trim('\'', '"') }.firstOrNull().orEmpty()
}

private fun extractLikelyBrand(lines: List<String>): String {
    val indicators = listOf("seed", "seeds", "company", "co.", "brand", "farm")
    return lines.firstOrNull { line -> indicators.any { marker -> line.contains(marker, ignoreCase = true) } }.orEmpty()
}
