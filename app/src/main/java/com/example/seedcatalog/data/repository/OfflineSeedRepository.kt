package com.example.seedcatalog.data.repository

import com.example.seedcatalog.data.local.GbifNameMatchCache
import com.example.seedcatalog.data.local.GbifNameMatchCacheDao
import com.example.seedcatalog.data.local.GbifSpeciesDetailsCache
import com.example.seedcatalog.data.local.GbifSpeciesDetailsCacheDao
import com.example.seedcatalog.data.local.PacketLot
import com.example.seedcatalog.data.local.PacketLotDao
import com.example.seedcatalog.data.local.PacketLotWithPhotos
import com.example.seedcatalog.data.local.Photo
import com.example.seedcatalog.data.local.PhotoDao
import com.example.seedcatalog.data.local.Plant
import com.example.seedcatalog.data.local.PlantDao
import com.example.seedcatalog.data.local.PlantFilterOptions
import com.example.seedcatalog.data.local.SourceAttribution
import com.example.seedcatalog.data.local.SourceAttributionDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

class OfflineSeedRepository(
    private val plantDao: PlantDao,
    private val packetLotDao: PacketLotDao,
    private val photoDao: PhotoDao,
    private val sourceAttributionDao: SourceAttributionDao,
    private val gbifNameMatchCacheDao: GbifNameMatchCacheDao,
    private val gbifSpeciesDetailsCacheDao: GbifSpeciesDetailsCacheDao
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
    ): Long {
        return plantDao.insert(
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

    override suspend fun fetchSpeciesMatchCandidates(extractedName: String): List<SpeciesMatchCandidate> {
        if (extractedName.isBlank()) return emptyList()
        val normalizedName = extractedName.trim()
        val cached = gbifNameMatchCacheDao.getByQueryName(normalizedName)
        val payload = cached?.responseJson ?: fetchMatchJson(normalizedName)?.also { json ->
            gbifNameMatchCacheDao.insert(
                GbifNameMatchCache(queryName = normalizedName, responseJson = json)
            )
        } ?: return emptyList()

        val match = JSONObject(payload)
        if (match.optInt("usageKey", 0) == 0) return emptyList()
        return listOf(
            SpeciesMatchCandidate(
                usageKey = match.getInt("usageKey"),
                scientificName = match.optString("scientificName", normalizedName),
                confidence = match.optDouble("confidence", 0.0),
                status = match.optString("status", ""),
                rank = match.optString("rank", ""),
                taxonomy = TaxonomySummary(
                    kingdom = match.optString("kingdom", ""),
                    phylum = match.optString("phylum", ""),
                    order = match.optString("order", ""),
                    family = match.optString("family", ""),
                    genus = match.optString("genus", "")
                )
            )
        )
    }

    override suspend fun applySpeciesSelection(candidate: SpeciesMatchCandidate): AutofillResult? {
        val cached = gbifSpeciesDetailsCacheDao.getByUsageKey(candidate.usageKey)
        val detailsJson: String
        val vernacularJson: String
        val now = System.currentTimeMillis()
        if (cached != null) {
            detailsJson = cached.responseJson
            vernacularJson = cached.vernacularJson
        } else {
            detailsJson = fetchSpeciesJson(candidate.usageKey) ?: return null
            vernacularJson = fetchVernacularJson(candidate.usageKey) ?: "{\"results\": []}"
            gbifSpeciesDetailsCacheDao.insert(
                GbifSpeciesDetailsCache(
                    usageKey = candidate.usageKey,
                    responseJson = detailsJson,
                    vernacularJson = vernacularJson,
                    retrievedAtEpochMs = now
                )
            )
        }

        val details = JSONObject(detailsJson)
        val acceptedName = details.optString("scientificName", candidate.scientificName)
        val taxonomy = TaxonomySummary(
            kingdom = details.optString("kingdom", candidate.taxonomy.kingdom),
            phylum = details.optString("phylum", candidate.taxonomy.phylum),
            order = details.optString("order", candidate.taxonomy.order),
            family = details.optString("family", candidate.taxonomy.family),
            genus = details.optString("genus", candidate.taxonomy.genus)
        )
        val vernacularNames = parseVernacularNames(vernacularJson)
        val sourceUrl = "https://api.gbif.org/v1/species/${candidate.usageKey}"
        val retrievedAt = cached?.retrievedAtEpochMs ?: now
        val attribution = FieldAttribution(
            sourceName = "GBIF Species API",
            sourceUrl = sourceUrl,
            retrievedAtEpochMs = retrievedAt,
            confidence = candidate.confidence
        )

        return AutofillResult(
            acceptedScientificName = acceptedName,
            taxonomy = taxonomy,
            vernacularNames = vernacularNames,
            confidence = candidate.confidence,
            sourceUrl = sourceUrl,
            retrievedAtEpochMs = retrievedAt,
            attributions = mapOf(
                "botanicalName" to attribution,
                "commonName" to attribution,
                "description" to attribution,
                "notes" to attribution
            )
        )
    }

    override suspend fun saveAutofillAttributions(plantId: Int, attributions: Map<String, FieldAttribution>) {
        sourceAttributionDao.deleteForPlant(plantId)
        val rows = attributions.map { (fieldName, attr) ->
            SourceAttribution(
                plantId = plantId,
                fieldName = fieldName,
                sourceName = attr.sourceName,
                sourceUrl = attr.sourceUrl,
                retrievedAtEpochMs = attr.retrievedAtEpochMs,
                confidence = attr.confidence
            )
        }
        sourceAttributionDao.insertAll(rows)
    }

    private fun fetchMatchJson(name: String): String? {
        val encoded = URLEncoder.encode(name, Charsets.UTF_8.name())
        return fetchUrl("https://api.gbif.org/v1/species/match?name=$encoded")
    }

    private fun fetchSpeciesJson(usageKey: Int): String? = fetchUrl("https://api.gbif.org/v1/species/$usageKey")

    private fun fetchVernacularJson(usageKey: Int): String? = fetchUrl("https://api.gbif.org/v1/species/$usageKey/vernacularNames")

    private fun fetchUrl(urlString: String): String? {
        val connection = URL(urlString).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 4_000
            connection.readTimeout = 4_000
            connection.setRequestProperty("Accept", "application/json")
            if (connection.responseCode !in 200..299) return null
            connection.inputStream.bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun parseVernacularNames(payload: String): List<String> {
        val results = JSONObject(payload).optJSONArray("results") ?: JSONArray()
        val names = linkedSetOf<String>()
        for (i in 0 until results.length()) {
            val row = results.optJSONObject(i) ?: continue
            val name = row.optString("vernacularName", "").trim()
            if (name.isNotBlank()) names += name
        }
        return names.toList()
    }
}
