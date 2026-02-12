package com.example.seedcatalog.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "plants")
data class Plant(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String,
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
    val uri: String
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
    val sourceName: String,
    val url: String
)
