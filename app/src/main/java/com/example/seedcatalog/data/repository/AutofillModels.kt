package com.example.seedcatalog.data.repository

data class TaxonomySummary(
    val kingdom: String,
    val phylum: String,
    val order: String,
    val family: String,
    val genus: String
)

data class SpeciesMatchCandidate(
    val usageKey: Int,
    val scientificName: String,
    val confidence: Double,
    val status: String,
    val rank: String,
    val taxonomy: TaxonomySummary
)

data class AutofillResult(
    val acceptedScientificName: String,
    val taxonomy: TaxonomySummary,
    val vernacularNames: List<String>,
    val confidence: Double,
    val sourceUrl: String,
    val retrievedAtEpochMs: Long,
    val attributions: Map<String, FieldAttribution>
)

data class FieldAttribution(
    val sourceName: String,
    val sourceUrl: String,
    val retrievedAtEpochMs: Long,
    val confidence: Double
)
