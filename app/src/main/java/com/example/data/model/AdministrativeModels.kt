package com.example.data.model

data class NigeriaLga(
    val name: String,
    val lat: Double,
    val lon: Double
)

data class NigeriaState(
    val name: String,
    val lgas: List<NigeriaLga>
)

data class NigeriaDataset(
    val statesCount: Int,
    val lgasCount: Int,
    val states: List<NigeriaState>
)

data class SpainMunicipality(
    val code: String,
    val name: String,
    val lat: Double,
    val lon: Double
)

data class SpainProvince(
    val code: String,
    val name: String,
    val municipalities: List<SpainMunicipality>
)

data class SpainCommunity(
    val code: String,
    val name: String,
    val subtype: String,
    val provinces: List<SpainProvince>
)

data class SpainDataset(
    val communitiesCount: Int,
    val provincesCount: Int,
    val municipalitiesCount: Int,
    val communities: List<SpainCommunity>
)

data class LocationSnapshot(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float? = null,
    val detectedCountry: SupportedCountry? = null,
    val detectedLocality: String? = null,
    val timestampMillis: Long = System.currentTimeMillis()
)

data class PhotoMetadataResult(
    val uriString: String,
    val displayName: String,
    val dateTaken: String,
    val timeTaken: String,
    val latitude: Double?,
    val longitude: Double?,
    val city: String,
    val country: String
) {
    val hasGps: Boolean get() = latitude != null && longitude != null
}
