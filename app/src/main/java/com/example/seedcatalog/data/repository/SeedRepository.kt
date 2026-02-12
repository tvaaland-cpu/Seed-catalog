package com.example.seedcatalog.data.repository

import com.example.seedcatalog.data.local.PacketLot
import com.example.seedcatalog.data.local.PacketLotWithPhotos
import com.example.seedcatalog.data.local.Photo
import com.example.seedcatalog.data.local.Plant
import com.example.seedcatalog.data.local.PlantFilterOptions
import kotlinx.coroutines.flow.Flow

interface SeedRepository {
    fun observePlants(
        query: String,
        plantType: String,
        lightRequirement: String,
        indoorOutdoor: String
    ): Flow<List<Plant>>

    fun observePlantFilterOptions(): Flow<PlantFilterOptions>
    fun observePlant(id: Int): Flow<Plant?>
    fun observePacketLotsWithPhotos(plantId: Int): Flow<List<PacketLotWithPhotos>>

    suspend fun createPlant(
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
    )

    suspend fun updatePlant(plant: Plant)
    suspend fun deletePlant(plant: Plant)

    suspend fun createPacketLot(plantId: Int, lotCode: String, quantity: Int, notes: String)
    suspend fun updatePacketLot(packetLot: PacketLot)
    suspend fun deletePacketLot(packetLot: PacketLot)

    suspend fun addPhoto(packetLotId: Int, uri: String, type: String)
    suspend fun deletePhoto(photo: Photo)
}
