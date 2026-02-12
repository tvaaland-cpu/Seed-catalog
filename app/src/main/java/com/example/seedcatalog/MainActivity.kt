package com.example.seedcatalog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import com.example.seedcatalog.data.local.Plant
import com.example.seedcatalog.data.local.PlantFilterOptions
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
                onAttachPhoto = { lotId, uri -> seedViewModel.addPhoto(lotId, uri) }
            )
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
    onSeedClick: (Int) -> Unit,
    onSavePlant: (String, String, String, String, String, String, String, String, String, String, String) -> Unit,
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
    onAttachPhoto: (Int, String) -> Unit
) {
    var showLotDialog by rememberSaveable { mutableStateOf(false) }
    var editingLot by remember { mutableStateOf<PacketLot?>(null) }
    var lotForPhotoPicker by remember { mutableStateOf<PacketLot?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            lotForPhotoPicker?.let { onAttachPhoto(it.id, uri.toString()) }
            lotForPhotoPicker = null
        }
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
                                        launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    }) { Text("Attach Photo") }
                                }

                                if (lotWithPhotos.photos.isEmpty()) {
                                    Text("No photos")
                                } else {
                                    lotWithPhotos.photos.forEach { photo ->
                                        AsyncImage(
                                            model = photo.uri,
                                            contentDescription = "Photo for lot ${lot.lotCode}",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp),
                                            contentScale = ContentScale.Crop
                                        )
                                        Text(
                                            text = photo.uri,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Button(onClick = onBack) { Text("Back") }
        }
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
