package com.example.data.device

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.exifinterface.media.ExifInterface
import com.example.data.model.PhotoMetadataResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PhotoExifReader(
    private val context: Context,
    private val locationService: LocationService
) {
    /**
     * Reads EXIF metadata strictly read-only from the ContentResolver input stream.
     * Never modifies the original photograph and never invents GPS coordinates.
     */
    suspend fun inspectPhoto(uri: Uri): PhotoMetadataResult = withContext(Dispatchers.IO) {
        val displayName = resolveDisplayName(uri)
        var rawDateTime: String? = null
        var latLong: DoubleArray? = null

        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                rawDateTime = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    ?: exif.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED)
                    ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
                latLong = exif.latLong
            }
        } catch (_: Exception) {
            // Keep graceful fallback without modifying file
        }

        val (dateTaken, timeTaken) = parseExifDateTime(rawDateTime)
        val lat = latLong?.getOrNull(0)
        val lon = latLong?.getOrNull(1)

        var city = "Not available"
        var country = "Not available"
        if (lat != null && lon != null) {
            val address = locationService.reverseGeocode(lat, lon)
            if (address != null) {
                city = address.locality ?: address.subAdminArea ?: address.adminArea ?: "Available via GPS"
                country = address.countryName ?: (LocationService.inferCountryByCoordinates(lat, lon)?.wireName ?: "Available via GPS")
            } else {
                val inferred = LocationService.inferCountryByCoordinates(lat, lon)
                if (inferred != null) {
                    country = inferred.wireName
                }
                city = "GPS coordinates present"
            }
        }

        PhotoMetadataResult(
            uriString = uri.toString(),
            displayName = displayName,
            dateTaken = dateTaken,
            timeTaken = timeTaken,
            latitude = lat,
            longitude = lon,
            city = city,
            country = country
        )
    }

    private fun resolveDisplayName(uri: Uri): String {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) cursor.getString(idx) else null
                    } else null
                } ?: uri.lastPathSegment ?: "Selected Photo"
        } catch (_: Exception) {
            uri.lastPathSegment ?: "Selected Photo"
        }
    }

    companion object {
        fun parseExifDateTime(raw: String?): Pair<String, String> {
            if (raw.isNullOrBlank()) return "Not recorded in EXIF" to "Not recorded in EXIF"
            // Standard EXIF format: "YYYY:MM:DD HH:MM:SS"
            val parts = raw.trim().split(" ")
            val datePart = parts.getOrNull(0)?.replace(":", "-") ?: "Not recorded in EXIF"
            val timePart = parts.getOrNull(1) ?: "Not recorded in EXIF"
            return datePart to timePart
        }
    }
}
