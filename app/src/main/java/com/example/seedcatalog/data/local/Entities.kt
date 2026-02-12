package com.example.seedcatalog.data.local

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "plants",
    indices = [Index("plantType"), Index("lightRequirement"), Index("indoorOutdoor")]
)
data class Plant(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val botanicalName: String,
    val commonName: String,
    val variety: String,
    val plantType: String,
    val lightRequirement: String,
    val indoorOutdoor: String,
    val description: String,
    val medicinalUses: String,
    val culinaryUses: String,
    val growingInstructions: String,
    val notes: String
)

@Fts4(contentEntity = Plant::class)
@Entity(tableName = "plants_fts")
data class PlantFts(
    val botanicalName: String,
    val commonName: String,
    val variety: String,
    val plantType: String,
    val description: String,
    val medicinalUses: String,
    val culinaryUses: String,
    val growingInstructions: String,
    val notes: String
)

@Entity(
    tableName = "packet_lots",
    foreignKeys = [
        ForeignKey(
            entity = Plant::class,
            parentColumns = ["id"],
            childColumns = ["plantId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("plantId")]
)
data class PacketLot(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val plantId: Int,
    val lotCode: String,
    val quantity: Int,
    val notes: String
)

@Entity(
    tableName = "photos",
    foreignKeys = [
        ForeignKey(
            entity = PacketLot::class,
            parentColumns = ["id"],
            childColumns = ["packetLotId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("packetLotId")]
)
data class Photo(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val packetLotId: Int,
    val uri: String,
    val type: String = PhotoType.FRONT.dbValue
)

@Entity(
    tableName = "notes",
    indices = [Index("plantId"), Index("packetLotId")]
)
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val plantId: Int? = null,
    val packetLotId: Int? = null,
    val content: String,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "source_attributions",
    foreignKeys = [
        ForeignKey(
            entity = Plant::class,
            parentColumns = ["id"],
            childColumns = ["plantId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("plantId")]
)
data class SourceAttribution(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val plantId: Int,
    val fieldName: String,
    val sourceName: String,
    val sourceUrl: String,
    val retrievedAtEpochMs: Long,
    val confidence: Double
)

@Entity(tableName = "gbif_name_match_cache")
data class GbifNameMatchCache(
    @PrimaryKey val queryName: String,
    val responseJson: String,
    val retrievedAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "gbif_species_details_cache")
data class GbifSpeciesDetailsCache(
    @PrimaryKey val usageKey: Int,
    val responseJson: String,
    val vernacularJson: String,
    val retrievedAtEpochMs: Long = System.currentTimeMillis()
)
