package com.example.seedcatalog.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.seedcatalog.data.local.PacketLot
import com.example.seedcatalog.data.local.PacketLotWithPhotos
import com.example.seedcatalog.data.local.Photo
import com.example.seedcatalog.data.local.Plant
import com.example.seedcatalog.data.repository.SeedRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SeedViewModel(private val repository: SeedRepository) : ViewModel() {
    val plants: StateFlow<List<Plant>> = repository.observePlants().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun plant(plantId: Int): Flow<Plant?> = repository.observePlant(plantId)

    fun lotsWithPhotos(plantId: Int): Flow<List<PacketLotWithPhotos>> =
        repository.observePacketLotsWithPhotos(plantId)

    fun createPlant(name: String, type: String, notes: String) = viewModelScope.launch {
        repository.createPlant(name, type, notes)
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

    fun addPhoto(packetLotId: Int, uri: String, type: String) = viewModelScope.launch {
        repository.addPhoto(packetLotId, uri, type)
    }

    fun deletePhoto(photo: Photo) = viewModelScope.launch {
        repository.deletePhoto(photo)
    }
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
