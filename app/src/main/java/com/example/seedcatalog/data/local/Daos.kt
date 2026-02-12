package com.example.seedcatalog.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantDao {
    @Query(
        """
        SELECT plants.*
        FROM plants
        WHERE (:plantType = '' OR plants.plantType = :plantType)
          AND (:lightRequirement = '' OR plants.lightRequirement = :lightRequirement)
          AND (:indoorOutdoor = '' OR plants.indoorOutdoor = :indoorOutdoor)
        ORDER BY plants.commonName
        """
    )
    fun observePlantsFiltered(
        plantType: String,
        lightRequirement: String,
        indoorOutdoor: String
    ): Flow<List<Plant>>

    @Query(
        """
        SELECT plants.*
        FROM plants
        INNER JOIN plants_fts ON plants.id = plants_fts.rowid
        WHERE plants_fts MATCH :ftsQuery
          AND (:plantType = '' OR plants.plantType = :plantType)
          AND (:lightRequirement = '' OR plants.lightRequirement = :lightRequirement)
          AND (:indoorOutdoor = '' OR plants.indoorOutdoor = :indoorOutdoor)
        ORDER BY plants.commonName
        """
    )
    fun observePlantsByFts(
        ftsQuery: String,
        plantType: String,
        lightRequirement: String,
        indoorOutdoor: String
    ): Flow<List<Plant>>

    @Query("SELECT * FROM plants WHERE id = :id")
    fun observePlant(id: Int): Flow<Plant?>

    @Query("SELECT DISTINCT plantType FROM plants WHERE plantType != '' ORDER BY plantType")
    fun observePlantTypes(): Flow<List<String>>

    @Query("SELECT DISTINCT lightRequirement FROM plants WHERE lightRequirement != '' ORDER BY lightRequirement")
    fun observeLightRequirements(): Flow<List<String>>

    @Query("SELECT DISTINCT indoorOutdoor FROM plants WHERE indoorOutdoor != '' ORDER BY indoorOutdoor")
    fun observeIndoorOutdoorOptions(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plant: Plant): Long

    @Update
    suspend fun update(plant: Plant)

    @Delete
    suspend fun delete(plant: Plant)
}

@Dao
interface PacketLotDao {
    @Transaction
    @Query("SELECT * FROM packet_lots WHERE plantId = :plantId ORDER BY id DESC")
    fun observeLotsWithPhotos(plantId: Int): Flow<List<PacketLotWithPhotos>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(packetLot: PacketLot): Long

    @Update
    suspend fun update(packetLot: PacketLot)

    @Delete
    suspend fun delete(packetLot: PacketLot)
}

@Dao
interface PhotoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(photo: Photo): Long

    @Query("DELETE FROM photos WHERE id = :id")
    suspend fun deleteById(id: Int)
}

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: Note): Long

    @Query("SELECT * FROM notes WHERE plantId = :plantId ORDER BY createdAtEpochMs DESC")
    fun observeForPlant(plantId: Int): Flow<List<Note>>
}

@Dao
interface SourceAttributionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sourceAttribution: SourceAttribution): Long

    @Query("SELECT * FROM source_attributions WHERE plantId = :plantId")
    fun observeForPlant(plantId: Int): Flow<List<SourceAttribution>>
}
