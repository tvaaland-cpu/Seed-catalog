package com.example.seedcatalog.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.seedcatalog.data.local.PacketLot
import com.example.seedcatalog.data.local.PacketLotWithPhotos
import com.example.seedcatalog.data.local.Plant
import com.example.seedcatalog.data.local.PlantFilterOptions
import com.example.seedcatalog.data.repository.SeedRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class SeedViewModel(private val repository: SeedRepository) : ViewModel() {
    private val searchQuery = MutableStateFlow("")
    private val selectedPlantType = MutableStateFlow("")
    private val selectedLightRequirement = MutableStateFlow("")
    private val selectedIndoorOutdoor = MutableStateFlow("")

    val plants: StateFlow<List<Plant>> = combine(
        searchQuery.debounce(200).distinctUntilChanged(),
        selectedPlantType,
        selectedLightRequirement,
        selectedIndoorOutdoor
    ) { query, plantType, lightRequirement, indoorOutdoor ->
        SearchParams(query, plantType, lightRequirement, indoorOutdoor)
    }
        .flatMapLatest { params ->
            repository.observePlants(
                query = params.query,
                plantType = params.plantType,
                lightRequirement = params.lightRequirement,
                indoorOutdoor = params.indoorOutdoor
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val filterOptions: StateFlow<PlantFilterOptions> = repository.observePlantFilterOptions().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PlantFilterOptions(emptyList(), emptyList(), emptyList())
    )

    val uiSearchQuery: StateFlow<String> = searchQuery
    val uiPlantType: StateFlow<String> = selectedPlantType
    val uiLightRequirement: StateFlow<String> = selectedLightRequirement
    val uiIndoorOutdoor: StateFlow<String> = selectedIndoorOutdoor

    fun updateSearchQuery(value: String) {
        searchQuery.value = value
    }

    fun updatePlantTypeFilter(value: String) {
        selectedPlantType.value = value
    }

    fun updateLightRequirementFilter(value: String) {
        selectedLightRequirement.value = value
    }

    fun updateIndoorOutdoorFilter(value: String) {
        selectedIndoorOutdoor.value = value
    }

    fun plant(plantId: Int): Flow<Plant?> = repository.observePlant(plantId)

    fun lotsWithPhotos(plantId: Int): Flow<List<PacketLotWithPhotos>> =
        repository.observePacketLotsWithPhotos(plantId)

    fun createPlant(
        botanicalName: String,
        commonName: String,
        variety: String,
        plantType: String,
        lightRequirement: String,
        indoorOutdoor: String,
        description: String,
        medicinalUses: String,
        culinaryUses: String,
        growingInstructions: String,
        notes: String
    ) = viewModelScope.launch {
        repository.createPlant(
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
    }

    fun updatePlant(plant: Plant) = viewModelScope.launch {
        repository.updatePlant(plant)
    }

    fun deletePlant(plant: Plant) = viewModelScope.launch {
        repository.deletePlant(plant)
    }

    fun createPacketLot(plantId: Int, lotCode: String, quantity: Int, notes: String) = viewModelScope.launch {
        repository.createPacketLot(plantId, lotCode, quantity, notes)
    }

    fun updatePacketLot(packetLot: PacketLot) = viewModelScope.launch {
        repository.updatePacketLot(packetLot)
    }

    fun deletePacketLot(packetLot: PacketLot) = viewModelScope.launch {
        repository.deletePacketLot(packetLot)
    }

    fun addPhoto(packetLotId: Int, uri: String) = viewModelScope.launch {
        repository.addPhoto(packetLotId, uri)
    }

    private data class SearchParams(
        val query: String,
        val plantType: String,
        val lightRequirement: String,
        val indoorOutdoor: String
    )
}

class SeedViewModelFactory(private val repository: SeedRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SeedViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SeedViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
