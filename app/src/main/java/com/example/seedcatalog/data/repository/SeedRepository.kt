package com.example.seedcatalog.data.repository

import com.example.seedcatalog.data.local.PacketLot
import com.example.seedcatalog.data.local.PacketLotWithPhotos
import com.example.seedcatalog.data.local.Photo
import com.example.seedcatalog.data.local.Plant
import kotlinx.coroutines.flow.Flow

interface SeedRepository {
    fun observePlants(): Flow<List<Plant>>
    fun observePlant(id: Int): Flow<Plant?>
    fun observePacketLotsWithPhotos(plantId: Int): Flow<List<PacketLotWithPhotos>>

    suspend fun createPlant(name: String, type: String, notes: String)
    suspend fun updatePlant(plant: Plant)
    suspend fun deletePlant(plant: Plant)

    suspend fun createPacketLot(plantId: Int, lotCode: String, quantity: Int, notes: String)
    suspend fun updatePacketLot(packetLot: PacketLot)
    suspend fun deletePacketLot(packetLot: PacketLot)

    suspend fun addPhoto(packetLotId: Int, uri: String, type: String)
    suspend fun deletePhoto(photo: Photo)
}
