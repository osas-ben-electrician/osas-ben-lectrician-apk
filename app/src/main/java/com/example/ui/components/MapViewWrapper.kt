package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.EmergencyReport
import com.example.data.model.LocationSnapshot
import com.example.data.model.SupportedCountry
import com.example.i18n.LocalAppStrings
import com.example.ui.theme.NavyDeepest
import com.example.ui.theme.NavySurface
import com.example.ui.theme.SafetyYellow
import com.example.ui.theme.WhiteText
import java.io.File
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

data class CountryMapViewport(
    val country: SupportedCountry,
    val center: GeoPoint,
    val initialZoom: Double,
    val minZoom: Double = 4.5,
    val maxZoom: Double = 19.0
)

fun initialViewportForCountry(country: SupportedCountry): CountryMapViewport =
    when (country) {
        SupportedCountry.SPAIN -> CountryMapViewport(
            country = SupportedCountry.SPAIN,
            center = GeoPoint(country.defaultLat, country.defaultLon),
            initialZoom = country.defaultZoom, // 6.2 — frames Spain cleanly
            minZoom = 4.5,
            maxZoom = 19.0
        )
        SupportedCountry.NIGERIA -> CountryMapViewport(
            country = SupportedCountry.NIGERIA,
            center = GeoPoint(country.defaultLat, country.defaultLon),
            initialZoom = country.defaultZoom, // 6.2 — frames all 36 states & FCT
            minZoom = 4.5,
            maxZoom = 19.0
        )
    }

fun hasRuntimeLocationPermission(context: Context): Boolean {
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

@Composable
fun MapViewWrapper(
    country: SupportedCountry,
    reports: List<EmergencyReport> = emptyList(),
    userGps: LocationSnapshot? = null,
    focusPoint: Pair<Double, Double>? = null,
    focusZoom: Double = 11.5,
    enableLocationOverlay: Boolean = true,
    showPermissionOverlay: Boolean = true,
    requestPermissionOnLaunch: Boolean = false,
    onLocationPermissionChanged: (Boolean) -> Unit = {},
    onMarkerSelected: (EmergencyReport) -> Unit = {},
    photoMarker: Pair<Double, Double>? = null,
    photoMarkerLabel: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val strings = LocalAppStrings.current

    var locationPermissionGranted by remember {
        mutableStateOf(hasRuntimeLocationPermission(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            hasRuntimeLocationPermission(context)
        locationPermissionGranted = granted
        onLocationPermissionChanged(granted)
    }

    LaunchedEffect(requestPermissionOnLaunch) {
        if (requestPermissionOnLaunch && !locationPermissionGranted) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Configure osmdroid using app-private directories (no broad storage permission required)
    remember(context) {
        val cfg = Configuration.getInstance()
        cfg.osmdroidBasePath = context.filesDir
        cfg.osmdroidTileCache = File(context.cacheDir, "osmdroid")
        cfg.userAgentValue = context.packageName
        true
    }

    val initialViewport = remember(country) { initialViewportForCountry(country) }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            isTilesScaledToDpi = true
            minZoomLevel = initialViewport.minZoom
            maxZoomLevel = initialViewport.maxZoom
            controller.setZoom(initialViewport.initialZoom)
            controller.setCenter(initialViewport.center)
        }
    }

    val myLocationOverlay = remember(mapView) {
        MyLocationNewOverlay(GpsMyLocationProvider(context.applicationContext), mapView).apply {
            setDrawAccuracyEnabled(true)
        }
    }

    // Recenter to Spain or Nigeria default viewport when country changes and no manual focus override is set
    LaunchedEffect(country, focusPoint == null) {
        if (focusPoint == null) {
            val viewport = initialViewportForCountry(country)
            mapView.minZoomLevel = viewport.minZoom
            mapView.maxZoomLevel = viewport.maxZoom
            mapView.controller.setZoom(viewport.initialZoom)
            mapView.controller.animateTo(viewport.center)
        }
    }

    // Synchronize osmdroid's MyLocationNewOverlay with runtime permission state
    LaunchedEffect(locationPermissionGranted, enableLocationOverlay) {
        if (enableLocationOverlay && locationPermissionGranted) {
            myLocationOverlay.enableMyLocation()
        } else {
            myLocationOverlay.disableMyLocation()
        }
    }

    DisposableEffect(lifecycleOwner, mapView, myLocationOverlay) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    val refreshedPermission = hasRuntimeLocationPermission(context)
                    if (refreshedPermission != locationPermissionGranted) {
                        locationPermissionGranted = refreshedPermission
                        onLocationPermissionChanged(refreshedPermission)
                    }
                    if (enableLocationOverlay && refreshedPermission) {
                        myLocationOverlay.enableMyLocation()
                    }
                    mapView.onResume()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    myLocationOverlay.disableMyLocation()
                    mapView.onPause()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            myLocationOverlay.disableMyLocation()
            mapView.onDetach()
        }
    }

    Box(modifier = modifier.testTag("osm_map_container")) {
        AndroidView(
            factory = { mapView },
            update = { view ->
                view.overlays.clear()

                if (focusPoint != null) {
                    view.controller.setZoom(focusZoom)
                    view.controller.animateTo(GeoPoint(focusPoint.first, focusPoint.second))
                }

                if (enableLocationOverlay && locationPermissionGranted) {
                    view.overlays.add(myLocationOverlay)
                }

                // Explicit live user GPS marker when FusedLocation snapshot is available
                if (userGps != null) {
                    val userMarker = Marker(view).apply {
                        position = GeoPoint(userGps.latitude, userGps.longitude)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        title = strings.gpsActiveLabel
                        icon = createUserGpsDrawable(context)
                    }
                    view.overlays.add(userMarker)
                }

                // Optional Photo Location pin (read-only EXIF map)
                if (photoMarker != null) {
                    val pMarker = Marker(view).apply {
                        position = GeoPoint(photoMarker.first, photoMarker.second)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = photoMarkerLabel ?: strings.photoTrackerTitle
                        icon = createPhotoPinDrawable(context)
                    }
                    view.overlays.add(pMarker)
                }

                // Country-isolated Emergency Report markers
                reports.forEach { report ->
                    val marker = Marker(view).apply {
                        position = GeoPoint(report.latitude, report.longitude)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = buildString {
                            if (report.testAlert) append("[TEST] ")
                            append(report.typeEnum().name)
                            append(" • ")
                            append(report.localArea)
                        }
                        icon = createEmergencyMarkerDrawable(
                            context = context,
                            type = report.typeEnum(),
                            status = report.statusEnum(),
                            isTest = report.testAlert
                        )
                        setOnMarkerClickListener { _, _ ->
                            onMarkerSelected(report)
                            true
                        }
                    }
                    view.overlays.add(marker)
                }

                view.invalidate()
            },
            modifier = Modifier.fillMaxSize()
        )

        // Runtime location permission prompt overlay when location access has not yet been granted
        if (showPermissionOverlay && !locationPermissionGranted) {
            Surface(
                color = NavySurface.copy(alpha = 0.94f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, SafetyYellow.copy(alpha = 0.8f)),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .testTag("map_wrapper_permission_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = strings.grantLocationBtn,
                        tint = SafetyYellow,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = strings.gpsUnavailableLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = WhiteText
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    TextButton(
                        onClick = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = SafetyYellow,
                            contentColor = NavyDeepest
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("map_wrapper_grant_location_btn")
                    ) {
                        Text(
                            text = strings.grantLocationBtn,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        // Required OpenStreetMap attribution overlay
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
                .background(Color(0xCC06101F), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = strings.osmAttribution,
                color = Color(0xFFF8FAFC),
                fontSize = 10.sp
            )
        }
    }
}
