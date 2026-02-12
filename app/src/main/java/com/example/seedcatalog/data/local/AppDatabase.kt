package com.example.seedcatalog.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Plant::class, PlantFts::class, PacketLot::class, Photo::class, Note::class, SourceAttribution::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun plantDao(): PlantDao
    abstract fun packetLotDao(): PacketLotDao
    abstract fun photoDao(): PhotoDao
    abstract fun noteDao(): NoteDao
    abstract fun sourceAttributionDao(): SourceAttributionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE plants ADD COLUMN botanicalName TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE plants ADD COLUMN commonName TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE plants ADD COLUMN variety TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE plants ADD COLUMN plantType TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE plants ADD COLUMN lightRequirement TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE plants ADD COLUMN indoorOutdoor TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE plants ADD COLUMN description TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE plants ADD COLUMN medicinalUses TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE plants ADD COLUMN culinaryUses TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE plants ADD COLUMN growingInstructions TEXT NOT NULL DEFAULT ''")

                database.execSQL("UPDATE plants SET commonName = name")
                database.execSQL("UPDATE plants SET plantType = type")

                database.execSQL("CREATE INDEX IF NOT EXISTS index_plants_plantType ON plants(plantType)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_plants_lightRequirement ON plants(lightRequirement)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_plants_indoorOutdoor ON plants(indoorOutdoor)")

                database.execSQL(
                    """
                    CREATE VIRTUAL TABLE IF NOT EXISTS plants_fts USING fts4(
                        botanicalName,
                        commonName,
                        variety,
                        plantType,
                        description,
                        medicinalUses,
                        culinaryUses,
                        growingInstructions,
                        notes,
                        content=`plants`
                    )
                    """.trimIndent()
                )
                database.execSQL("INSERT INTO plants_fts(plants_fts) VALUES('rebuild')")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "seed_catalog.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
