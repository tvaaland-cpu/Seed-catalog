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
    @Query("SELECT * FROM plants ORDER BY name")
    fun observePlants(): Flow<List<Plant>>

    @Query("SELECT * FROM plants WHERE id = :id")
    fun observePlant(id: Int): Flow<Plant?>

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
