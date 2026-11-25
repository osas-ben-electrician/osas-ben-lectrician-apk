package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class SupportedCountry(
    val wireName: String,
    val defaultLat: Double,
    val defaultLon: Double,
    val defaultZoom: Double
) {
    SPAIN("Spain", 40.4168, -3.7038, 6.2),
    NIGERIA("Nigeria", 9.0820, 8.6753, 6.2);

    companion object {
        fun fromWire(raw: String?): SupportedCountry =
            entries.firstOrNull { it.wireName.equals(raw, ignoreCase = true) } ?: NIGERIA
    }
}

enum class EmergencyType {
    RESCUE,
    ROBBERY,
    KIDNAPPING;

    companion object {
        fun fromWire(raw: String?): EmergencyType =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: RESCUE
    }
}

enum class ReportStatus {
    ACTIVE,
    RESOLVED,
    CANCELLED_FALSE_ALARM;

    companion object {
        fun fromWire(raw: String?): ReportStatus =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: ACTIVE
    }
}

enum class DeviceStatus {
    ACTIVE,
    OFFLINE_LAST_KNOWN;

    companion object {
        fun fromWire(raw: String?): DeviceStatus =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: OFFLINE_LAST_KNOWN
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val firstName: String = "",
    val email: String = "",
    val country: String = SupportedCountry.NIGERIA.wireName,
    val region: String = "",
    val province: String? = null,
    val localArea: String = "",
    val language: String = "en",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class EmergencyReport(
    @DocumentId val id: String = "",
    val userId: String = "",
    val emergencyType: String = EmergencyType.RESCUE.name,
    val status: String = ReportStatus.ACTIVE.name,
    val country: String = SupportedCountry.NIGERIA.wireName,
    val region: String = "",
    val province: String? = null,
    val localArea: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val reportedTime: String = "",
    val reportedDate: String = "",
    val testAlert: Boolean = false,
    val discreetMode: Boolean = false,
    val note: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
    val localDemoOnly: Boolean = false
) {
    fun typeEnum(): EmergencyType = EmergencyType.fromWire(emergencyType)
    fun statusEnum(): ReportStatus = ReportStatus.fromWire(status)
    fun countryEnum(): SupportedCountry = SupportedCountry.fromWire(country)
}

@Keep
@IgnoreExtraProperties
data class RegisteredDevice(
    @DocumentId val id: String = "",
    val userId: String = "",
    val deviceName: String = "",
    val deviceModel: String = "",
    val country: String = SupportedCountry.NIGERIA.wireName,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val status: String = DeviceStatus.ACTIVE.name,
    val lastLocationUpdateText: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun statusEnum(): DeviceStatus = DeviceStatus.fromWire(status)
}
