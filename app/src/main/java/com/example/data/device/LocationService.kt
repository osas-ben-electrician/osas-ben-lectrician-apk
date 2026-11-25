package com.example.data.device

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.core.content.ContextCompat
import com.example.data.model.LocationSnapshot
import com.example.data.model.SupportedCountry
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class LocationService(private val context: Context) {
    private val fusedClient by lazy {
        LocationServices.getFusedLocationProviderClient(context.applicationContext)
    }

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    suspend fun getBestCurrentLocation(): LocationSnapshot? {
        if (!hasLocationPermission()) return null
        return try {
            val tokenSource = CancellationTokenSource()
            val current: Location? = fusedClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                tokenSource.token
            ).await()
            val loc = current ?: fusedClient.lastLocation.await()
            loc?.let { toSnapshot(it) }
        } catch (_: Exception) {
            try {
                fusedClient.lastLocation.await()?.let { toSnapshot(it) }
            } catch (_: Exception) {
                null
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun observeLocationUpdates(intervalMs: Long = 8000L): Flow<LocationSnapshot> = callbackFlow {
        if (!hasLocationPermission()) {
            close()
            return@callbackFlow
        }
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(4000L)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    trySend(
                        LocationSnapshot(
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            accuracyMeters = if (loc.hasAccuracy()) loc.accuracy else null,
                            detectedCountry = inferCountryByCoordinates(loc.latitude, loc.longitude),
                            timestampMillis = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
        try {
            fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (_: Exception) {
            close()
        }
        awaitClose {
            fusedClient.removeLocationUpdates(callback)
        }
    }

    private suspend fun toSnapshot(location: Location): LocationSnapshot {
        val geocoded = reverseGeocode(location.latitude, location.longitude)
        val countryFromGeo = when (geocoded?.countryCode?.uppercase(Locale.ROOT)) {
            "ES" -> SupportedCountry.SPAIN
            "NG" -> SupportedCountry.NIGERIA
            else -> inferCountryByCoordinates(location.latitude, location.longitude)
        }
        val locality = geocoded?.locality ?: geocoded?.subAdminArea ?: geocoded?.adminArea
        return LocationSnapshot(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracyMeters = if (location.hasAccuracy()) location.accuracy else null,
            detectedCountry = countryFromGeo,
            detectedLocality = locality,
            timestampMillis = System.currentTimeMillis()
        )
    }

    suspend fun reverseGeocode(lat: Double, lon: Double): Address? = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) return@withContext null
        try {
            val geocoder = Geocoder(context.applicationContext, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocation(lat, lon, 1) { addresses ->
                        if (cont.isActive) cont.resume(addresses.firstOrNull())
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(lat, lon, 1)?.firstOrNull()
            }
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        fun inferCountryByCoordinates(lat: Double, lon: Double): SupportedCountry? {
            // Nigeria approximate bounding box
            if (lat in 4.0..14.1 && lon in 2.6..14.8) {
                return SupportedCountry.NIGERIA
            }
            // Mainland Spain + Balearic Islands + Ceuta/Melilla
            if (lat in 35.1..43.9 && lon in -9.5..4.5) {
                return SupportedCountry.SPAIN
            }
            // Canary Islands (Spain)
            if (lat in 27.5..29.5 && lon in -18.5..-13.2) {
                return SupportedCountry.SPAIN
            }
            return null
        }
    }
}
