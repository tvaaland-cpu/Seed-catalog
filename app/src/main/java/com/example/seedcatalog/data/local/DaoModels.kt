package com.example.seedcatalog.data.local

import androidx.room.Embedded
import androidx.room.Relation

data class PacketLotWithPhotos(
    @Embedded val packetLot: PacketLot,
    @Relation(
        parentColumn = "id",
        entityColumn = "packetLotId"
    )
    val photos: List<Photo>
)

data class PlantFilterOptions(
    val plantTypes: List<String>,
    val lightRequirements: List<String>,
    val indoorOutdoorOptions: List<String>
)
