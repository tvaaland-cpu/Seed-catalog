package com.example.seedcatalog.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Plant::class,
        PlantFts::class,
        PacketLot::class,
        Photo::class,
        Note::class,
        SourceAttribution::class,
        GbifNameMatchCache::class,
        GbifSpeciesDetailsCache::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun plantDao(): PlantDao
    abstract fun packetLotDao(): PacketLotDao
    abstract fun photoDao(): PhotoDao
    abstract fun noteDao(): NoteDao
    abstract fun sourceAttributionDao(): SourceAttributionDao
    abstract fun gbifNameMatchCacheDao(): GbifNameMatchCacheDao
    abstract fun gbifSpeciesDetailsCacheDao(): GbifSpeciesDetailsCacheDao

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
                database.execSQL("ALTER TABLE photos ADD COLUMN type TEXT NOT NULL DEFAULT 'front'")

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



        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS source_attributions_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        plantId INTEGER NOT NULL,
                        fieldName TEXT NOT NULL,
                        sourceName TEXT NOT NULL,
                        sourceUrl TEXT NOT NULL,
                        retrievedAtEpochMs INTEGER NOT NULL,
                        confidence REAL NOT NULL,
                        FOREIGN KEY(plantId) REFERENCES plants(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    INSERT INTO source_attributions_new(id, plantId, fieldName, sourceName, sourceUrl, retrievedAtEpochMs, confidence)
                    SELECT id, plantId, '', sourceName, url, 0, 0.0 FROM source_attributions
                    """.trimIndent()
                )
                database.execSQL("DROP TABLE source_attributions")
                database.execSQL("ALTER TABLE source_attributions_new RENAME TO source_attributions")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_source_attributions_plantId ON source_attributions(plantId)")

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS gbif_name_match_cache (
                        queryName TEXT NOT NULL PRIMARY KEY,
                        responseJson TEXT NOT NULL,
                        retrievedAtEpochMs INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS gbif_species_details_cache (
                        usageKey INTEGER NOT NULL PRIMARY KEY,
                        responseJson TEXT NOT NULL,
                        vernacularJson TEXT NOT NULL,
                        retrievedAtEpochMs INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "seed_catalog.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
