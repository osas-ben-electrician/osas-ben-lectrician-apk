package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PersonPinCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.LocalAppPreferences
import com.example.data.model.EmergencyReport
import com.example.data.model.EmergencyType
import com.example.data.model.LocationSnapshot
import com.example.data.model.ReportStatus
import com.example.data.model.SupportedCountry
import com.example.data.model.UserProfile
import com.example.i18n.LocalAppStrings
import com.example.ui.ClockCalendarState
import com.example.ui.FeedUiState
import com.example.ui.MapAreaFilter
import com.example.ui.components.EmergencyDetailBottomSheet
import com.example.ui.components.EmergencyTypeBadge
import com.example.ui.components.MapViewWrapper
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.EmergencyRedBright
import com.example.ui.theme.KidnappingOrange
import com.example.ui.theme.NavyBackground
import com.example.ui.theme.NavyBorder
import com.example.ui.theme.NavyDeepest
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceElevated
import com.example.ui.theme.OnlineEmerald
import com.example.ui.theme.SafetyYellow
import com.example.ui.theme.SlateTextSecondary
import com.example.ui.theme.WhiteText

@Composable
fun HomeMapScreen(
    profile: UserProfile,
    country: SupportedCountry,
    preferences: LocalAppPreferences,
    clock: ClockCalendarState,
    reportsState: FeedUiState<List<EmergencyReport>>,
    areaFilter: MapAreaFilter,
    typeFilter: EmergencyType?,
    userGps: LocationSnapshot?,
    hasLocationPermission: Boolean,
    onAreaFilterChanged: (MapAreaFilter) -> Unit,
    onTypeFilterChanged: (EmergencyType?) -> Unit,
    onLocationPermissionGranted: () -> Unit,
    onRefreshGps: () -> Unit,
    onNavigateToSos: (EmergencyType) -> Unit,
    onUpdateLiveGps: (EmergencyReport) -> Unit,
    onCancelFalseAlarm: (EmergencyReport) -> Unit,
    onMarkResolved: (EmergencyReport) -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    var selectedReport by remember { mutableStateOf<EmergencyReport?>(null) }
    var focusOverride by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms.values.any { it }) {
            onLocationPermissionGranted()
            onRefreshGps()
        }
    }

    val rawReports = (reportsState as? FeedUiState.Success)?.data.orEmpty()
    val filteredReports = remember(rawReports, areaFilter, typeFilter, profile, country) {
        rawReports.filter { rep ->
            val countryOk = rep.countryEnum() == country
            val areaOk = when (areaFilter) {
                MapAreaFilter.COUNTRY -> true
                MapAreaFilter.REGION -> rep.region.equals(profile.region, ignoreCase = true)
                MapAreaFilter.LOCAL_AREA -> rep.localArea.equals(profile.localArea, ignoreCase = true)
            }
            val typeOk = typeFilter == null || rep.typeEnum() == typeFilter
            countryOk && areaOk && typeOk
        }
    }

    val effectiveFocus = focusOverride ?: userGps?.let { it.latitude to it.longitude }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyBackground)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Live Clock, Calendar & Assigned Area Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = BorderStroke(1.dp, NavyBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${profile.firstName} • ${profile.localArea}",
                            style = MaterialTheme.typography.titleMedium,
                            color = WhiteText,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = listOfNotNull(profile.province, profile.region, country.wireName).joinToString(" • "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = SafetyYellow
                        )
                        Text(
                            text = if (userGps != null) {
                                "${strings.gpsActiveLabel}: ${"%.4f".format(userGps.latitude)}, ${"%.4f".format(userGps.longitude)}"
                            } else {
                                strings.gpsUnavailableLabel
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (userGps != null) OnlineEmerald else SlateTextSecondary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = clock.liveTime,
                            style = MaterialTheme.typography.headlineSmall,
                            color = SafetyYellow,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "${clock.dayOfWeek}, ${clock.dayOfMonth} ${clock.monthName} ${clock.year}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SlateTextSecondary
                        )
                    }
                }
            }
        }

        // 2. Main Interactive OpenStreetMap Dashboard
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = BorderStroke(1.dp, NavyBorder),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${strings.mapTitle} (${country.wireName})",
                            style = MaterialTheme.typography.titleMedium,
                            color = WhiteText,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (!hasLocationPermission) {
                                OutlinedButton(
                                    onClick = {
                                        permissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    },
                                    border = BorderStroke(1.dp, SafetyYellow),
                                    modifier = Modifier.testTag("map_enable_gps_btn")
                                ) {
                                    Icon(Icons.Default.GpsFixed, contentDescription = null, tint = SafetyYellow, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(strings.grantLocationBtn, color = SafetyYellow, fontSize = 12.sp)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        onRefreshGps()
                                        focusOverride = userGps?.let { it.latitude to it.longitude }
                                    },
                                    border = BorderStroke(1.dp, SafetyYellow),
                                    modifier = Modifier.testTag("map_center_me_btn")
                                ) {
                                    Icon(Icons.Default.MyLocation, contentDescription = null, tint = SafetyYellow, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(strings.centerOnMe, color = SafetyYellow, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    MapViewWrapper(
                        country = country,
                        reports = filteredReports,
                        userGps = userGps,
                        focusPoint = effectiveFocus,
                        focusZoom = if (effectiveFocus != null) 11.5 else country.defaultZoom,
                        enableLocationOverlay = true,
                        showPermissionOverlay = !hasLocationPermission,
                        onLocationPermissionChanged = { granted ->
                            if (granted) {
                                onLocationPermissionGranted()
                                onRefreshGps()
                            }
                        },
                        onMarkerSelected = { report -> selectedReport = report },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(330.dp)
                    )

                    // Filter chips bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MapAreaFilter.entries.forEach { f ->
                            val label = when (f) {
                                MapAreaFilter.COUNTRY -> strings.filterAllCountry
                                MapAreaFilter.REGION -> strings.filterMyRegion
                                MapAreaFilter.LOCAL_AREA -> strings.filterMyLocalArea
                            }
                            FilterChip(
                                selected = areaFilter == f,
                                onClick = { onAreaFilterChanged(f) },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SafetyYellow,
                                    selectedLabelColor = NavyDeepest,
                                    containerColor = NavySurfaceElevated,
                                    labelColor = WhiteText
                                )
                            )
                        }
                        FilterChip(
                            selected = typeFilter == null,
                            onClick = { onTypeFilterChanged(null) },
                            label = { Text(strings.filterAllTypes) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmergencyRed,
                                selectedLabelColor = WhiteText,
                                containerColor = NavySurfaceElevated,
                                labelColor = WhiteText
                            )
                        )
                        EmergencyType.entries.forEach { t ->
                            FilterChip(
                                selected = typeFilter == t,
                                onClick = { onTypeFilterChanged(if (typeFilter == t) null else t) },
                                label = {
                                    Text(
                                        when (t) {
                                            EmergencyType.RESCUE -> strings.rescueTitle
                                            EmergencyType.ROBBERY -> strings.robberyTitle
                                            EmergencyType.KIDNAPPING -> strings.kidnappingTitle
                                        }
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmergencyRed,
                                    selectedLabelColor = WhiteText,
                                    containerColor = NavySurfaceElevated,
                                    labelColor = WhiteText
                                )
                            )
                        }
                    }
                }
            }
        }

        // 3. Quick SOS Launch Bar (Red Emergency Buttons)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickSosButton(
                    label = "RESCUE",
                    icon = Icons.Default.PersonPinCircle,
                    color = EmergencyRed,
                    onClick = { onNavigateToSos(EmergencyType.RESCUE) },
                    testTag = "quick_sos_rescue",
                    modifier = Modifier.weight(1f)
                )
                QuickSosButton(
                    label = "ROBBERY",
                    icon = Icons.Default.VisibilityOff,
                    color = EmergencyRedBright,
                    onClick = { onNavigateToSos(EmergencyType.ROBBERY) },
                    testTag = "quick_sos_robbery",
                    modifier = Modifier.weight(1f)
                )
                QuickSosButton(
                    label = "KIDNAP",
                    icon = Icons.Default.DirectionsCar,
                    color = KidnappingOrange,
                    onClick = { onNavigateToSos(EmergencyType.KIDNAPPING) },
                    testTag = "quick_sos_kidnapping",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 4. Country Emergency Calling Card (Spain 112 / Nigeria Configurable)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NavySurfaceElevated),
                border = BorderStroke(1.dp, SafetyYellow.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val dialNumber = if (country == SupportedCountry.SPAIN) "112" else preferences.nigeriaEmergencyNumber
                    val dialLabel = if (country == SupportedCountry.SPAIN) {
                        strings.call112Spain
                    } else {
                        "${preferences.nigeriaEmergencyLabel}: $dialNumber"
                    }
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$dialNumber"))
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed, contentColor = WhiteText),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("emergency_dial_button")
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(dialLabel, fontWeight = FontWeight.ExtraBold)
                    }
                    Text(
                        text = strings.emergencyCallDisclaimer,
                        style = MaterialTheme.typography.labelSmall,
                        color = SlateTextSecondary
                    )
                }
            }
        }

        // 5. Country-Filtered Emergency Feed List
        when (reportsState) {
            is FeedUiState.Loading -> {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = SafetyYellow)
                    }
                }
            }
            is FeedUiState.Error -> {
                item {
                    Surface(
                        color = EmergencyRedBright.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, EmergencyRedBright)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(reportsState.message, color = WhiteText, modifier = Modifier.weight(1f))
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = SafetyYellow)
                        }
                    }
                }
            }
            is FeedUiState.Success -> {
                if (filteredReports.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = NavySurface),
                            border = BorderStroke(1.dp, NavyBorder),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(strings.noReportsTitle, style = MaterialTheme.typography.titleMedium, color = WhiteText)
                                Text(strings.noReportsSubtitle, style = MaterialTheme.typography.bodyMedium, color = SlateTextSecondary)
                            }
                        }
                    }
                } else {
                    items(filteredReports, key = { it.id }) { report ->
                        EmergencyFeedItemCard(
                            report = report,
                            onClick = {
                                focusOverride = report.latitude to report.longitude
                                selectedReport = report
                            }
                        )
                    }
                }
            }
        }
    }

    selectedReport?.let { current ->
        val fresh = filteredReports.firstOrNull { it.id == current.id } ?: current
        EmergencyDetailBottomSheet(
            report = fresh,
            currentUserId = profile.userId,
            onDismiss = { selectedReport = null },
            onUpdateLiveGps = onUpdateLiveGps,
            onCancelFalseAlarm = {
                onCancelFalseAlarm(it)
                selectedReport = null
            },
            onMarkResolved = {
                onMarkResolved(it)
                selectedReport = null
            }
        )
    }
}

@Composable
private fun QuickSosButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = WhiteText),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .height(58.dp)
            .testTag(testTag)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
        }
    }
}

@Composable
fun EmergencyFeedItemCard(
    report: EmergencyReport,
    onClick: () -> Unit
) {
    val strings = LocalAppStrings.current
    Card(
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        border = BorderStroke(1.dp, if (report.statusEnum() == ReportStatus.ACTIVE) EmergencyRed.copy(alpha = 0.7f) else NavyBorder),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("feed_report_${report.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                EmergencyTypeBadge(type = report.typeEnum(), isTest = report.testAlert)
                Text(
                    text = "${report.reportedDate} • ${report.reportedTime}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SlateTextSecondary
                )
            }
            Text(
                text = listOfNotNull(report.localArea, report.province, report.region, report.country).joinToString(", "),
                style = MaterialTheme.typography.titleMedium,
                color = WhiteText,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "GPS: ${"%.5f".format(report.latitude)}, ${"%.5f".format(report.longitude)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SafetyYellow
                )
                Text(
                    text = when (report.statusEnum()) {
                        ReportStatus.ACTIVE -> strings.statusActive
                        ReportStatus.RESOLVED -> strings.statusResolved
                        ReportStatus.CANCELLED_FALSE_ALARM -> strings.statusCancelled
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (report.statusEnum() == ReportStatus.ACTIVE) EmergencyRedBright else SlateTextSecondary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
