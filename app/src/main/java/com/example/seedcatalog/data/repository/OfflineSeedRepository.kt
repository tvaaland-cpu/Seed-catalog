package com.example.seedcatalog.data.repository

import com.example.seedcatalog.data.local.PacketLot
import com.example.seedcatalog.data.local.PacketLotDao
import com.example.seedcatalog.data.local.PacketLotWithPhotos
import com.example.seedcatalog.data.local.Photo
import com.example.seedcatalog.data.local.PhotoDao
import com.example.seedcatalog.data.local.Plant
import com.example.seedcatalog.data.local.PlantDao
import com.example.seedcatalog.data.local.PlantFilterOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class OfflineSeedRepository(
    private val plantDao: PlantDao,
    private val packetLotDao: PacketLotDao,
    private val photoDao: PhotoDao
) : SeedRepository {

    override fun observePlants(
        query: String,
        plantType: String,
        lightRequirement: String,
        indoorOutdoor: String
    ): Flow<List<Plant>> {
        val normalizedQuery = buildFtsQuery(query)
        return if (normalizedQuery.isBlank()) {
            plantDao.observePlantsFiltered(plantType, lightRequirement, indoorOutdoor)
        } else {
            plantDao.observePlantsByFts(
                ftsQuery = normalizedQuery,
                plantType = plantType,
                lightRequirement = lightRequirement,
                indoorOutdoor = indoorOutdoor
            )
        }
    }

    override fun observePlantFilterOptions(): Flow<PlantFilterOptions> =
        combine(
            plantDao.observePlantTypes(),
            plantDao.observeLightRequirements(),
            plantDao.observeIndoorOutdoorOptions()
        ) { plantTypes, lightRequirements, indoorOutdoorOptions ->
            PlantFilterOptions(
                plantTypes = plantTypes,
                lightRequirements = lightRequirements,
                indoorOutdoorOptions = indoorOutdoorOptions
            )
        }

    override fun observePlant(id: Int): Flow<Plant?> = plantDao.observePlant(id)

    override fun observePacketLotsWithPhotos(plantId: Int): Flow<List<PacketLotWithPhotos>> =
        packetLotDao.observeLotsWithPhotos(plantId)

    private fun buildFtsQuery(rawQuery: String): String =
        rawQuery
            .trim()
            .split(Regex("\\s+"))
            .map { it.replace(Regex("[^\\p{L}\\p{N}]"), "") }
            .filter { it.isNotBlank() }
            .joinToString(" AND ") { "${it}*" }

    override suspend fun createPlant(
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
    ) {
        plantDao.insert(
            Plant(
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

    override suspend fun addPhoto(packetLotId: Int, uri: String, type: String) {
        photoDao.insert(Photo(packetLotId = packetLotId, uri = uri, type = type))
    }

    override suspend fun deletePhoto(photo: Photo) {
        photoDao.deleteById(photo.id)
    }
}
