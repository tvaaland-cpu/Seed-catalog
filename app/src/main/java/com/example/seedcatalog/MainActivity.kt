package com.example.seedcatalog

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import com.example.seedcatalog.data.repository.OfflineSeedRepository
import com.example.seedcatalog.ui.SeedViewModel
import com.example.seedcatalog.ui.SeedViewModelFactory
import com.example.seedcatalog.ui.theme.SeedCatalogTheme

sealed class Screen(val route: String) {
    data object SeedList : Screen("seedList")
    data object SeedDetail : Screen("seedDetail/{plantId}") {
        fun createRoute(plantId: Int) = "seedDetail/$plantId"
    }
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

    NavHost(navController = navController, startDestination = Screen.SeedList.route) {
        composable(Screen.SeedList.route) {
            SeedListScreen(
                plants = plants,
                onSeedClick = { navController.navigate(Screen.SeedDetail.createRoute(it)) },
                onSavePlant = { name, type, notes -> seedViewModel.createPlant(name, type, notes) },
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeedListScreen(
    plants: List<Plant>,
    onSeedClick: (Int) -> Unit,
    onSavePlant: (String, String, String) -> Unit,
    onUpdatePlant: (Plant) -> Unit,
    onDeletePlant: (Plant) -> Unit
) {
    var editingPlant by remember { mutableStateOf<Plant?>(null) }
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text("Plants") }) }) { paddingValues ->
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
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(plants, key = { it.id }) { plant ->
                    Card(modifier = Modifier.fillMaxWidth(), onClick = { onSeedClick(plant.id) }) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(plant.name, fontWeight = FontWeight.Bold)
                            Text("Type: ${plant.type}")
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
            onSave = { name, type, notes ->
                onSavePlant(name, type, notes)
                showCreateDialog = false
            }
        )
    }

    editingPlant?.let { plant ->
        PlantDialog(
            title = "Edit Plant",
            initialName = plant.name,
            initialType = plant.type,
            initialNotes = plant.notes,
            onDismiss = { editingPlant = null },
            onSave = { name, type, notes ->
                onUpdatePlant(plant.copy(name = name, type = type, notes = notes))
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
                Text(plant.name, style = MaterialTheme.typography.headlineSmall)
                Text("Type: ${plant.type}")
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

private fun photoTypeDisplay(dbValue: String): String =
    PhotoType.entries.firstOrNull { it.dbValue == dbValue }?.displayName ?: dbValue

@Composable
private fun PlantDialog(
    title: String,
    initialName: String = "",
    initialType: String = "",
    initialNotes: String = "",
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var type by rememberSaveable { mutableStateOf(initialType) }
    var notes by rememberSaveable { mutableStateOf(initialNotes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                OutlinedTextField(value = type, onValueChange = { type = it }, label = { Text("Type") })
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") })
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, type, notes) }, enabled = name.isNotBlank() && type.isNotBlank()) {
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
