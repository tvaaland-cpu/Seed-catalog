package com.example.seedcatalog.data.repository

import com.example.seedcatalog.data.local.PacketLot
import com.example.seedcatalog.data.local.PacketLotDao
import com.example.seedcatalog.data.local.PacketLotWithPhotos
import com.example.seedcatalog.data.local.Photo
import com.example.seedcatalog.data.local.PhotoDao
import com.example.seedcatalog.data.local.Plant
import com.example.seedcatalog.data.local.PlantDao
import kotlinx.coroutines.flow.Flow

class OfflineSeedRepository(
    private val plantDao: PlantDao,
    private val packetLotDao: PacketLotDao,
    private val photoDao: PhotoDao
) : SeedRepository {

    override fun observePlants(): Flow<List<Plant>> = plantDao.observePlants()

    override fun observePlant(id: Int): Flow<Plant?> = plantDao.observePlant(id)

    override fun observePacketLotsWithPhotos(plantId: Int): Flow<List<PacketLotWithPhotos>> =
        packetLotDao.observeLotsWithPhotos(plantId)

    override suspend fun createPlant(name: String, type: String, notes: String) {
        plantDao.insert(Plant(name = name, type = type, notes = notes))
    }

    override suspend fun updatePlant(plant: Plant) {
        plantDao.update(plant)
    }

    override suspend fun deletePlant(plant: Plant) {
        plantDao.delete(plant)
    }

    override suspend fun createPacketLot(plantId: Int, lotCode: String, quantity: Int, notes: String) {
        packetLotDao.insert(
            PacketLot(
                plantId = plantId,
                lotCode = lotCode,
                quantity = quantity,
                notes = notes
            )
        )
    }

    override suspend fun updatePacketLot(packetLot: PacketLot) {
        packetLotDao.update(packetLot)
    }

    override suspend fun deletePacketLot(packetLot: PacketLot) {
        packetLotDao.delete(packetLot)
    }

    override suspend fun addPhoto(packetLotId: Int, uri: String) {
        photoDao.insert(Photo(packetLotId = packetLotId, uri = uri))
    }

    override suspend fun deletePhoto(photo: Photo) {
        photoDao.deleteById(photo.id)
    }
}
