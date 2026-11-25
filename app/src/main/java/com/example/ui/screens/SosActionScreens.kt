package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.PersonPinCircle
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EmergencyReport
import com.example.data.model.EmergencyType
import com.example.data.model.LocationSnapshot
import com.example.data.model.ReportStatus
import com.example.data.model.SupportedCountry
import com.example.data.model.UserProfile
import com.example.i18n.LocalAppStrings
import com.example.ui.components.OsmEmergencyMapView
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.EmergencyRedBright
import com.example.ui.theme.EmergencyRedContainer
import com.example.ui.theme.KidnappingOrange
import com.example.ui.theme.NavyBackground
import com.example.ui.theme.NavyBorder
import com.example.ui.theme.NavyDeepest
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceElevated
import com.example.ui.theme.OnlineEmerald
import com.example.ui.theme.OnlineEmeraldContainer
import com.example.ui.theme.SafetyYellow
import com.example.ui.theme.SlateTextSecondary
import com.example.ui.theme.WhiteText

@Composable
fun SosActionScreen(
    emergencyType: EmergencyType,
    profile: UserProfile,
    country: SupportedCountry,
    userGps: LocationSnapshot?,
    hasLocationPermission: Boolean,
    isTestMode: Boolean,
    isOnlineAuth: Boolean,
    isBusy: Boolean,
    activeReports: List<EmergencyReport>,
    onBackToMap: () -> Unit,
    onLocationPermissionGranted: () -> Unit,
    onTriggerSos: (discreet: Boolean, note: String?, onDone: (Boolean, String) -> Unit) -> Unit,
    onUpdateLiveGps: (EmergencyReport) -> Unit,
    onCancelFalseAlarm: (EmergencyReport) -> Unit,
    onMarkResolved: (EmergencyReport) -> Unit
) {
    BackHandler { onBackToMap() }

    val strings = LocalAppStrings.current
    var note by remember { mutableStateOf("") }
    var discreetMode by remember { mutableStateOf(emergencyType == EmergencyType.ROBBERY) }
    var confirmationMessage by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms.values.any { it }) {
            onLocationPermissionGranted()
        }
    }

    val myActiveReport = remember(activeReports, profile.userId, emergencyType) {
        activeReports.firstOrNull {
            it.userId == profile.userId &&
                it.typeEnum() == emergencyType &&
                it.statusEnum() == ReportStatus.ACTIVE
        }
    }

    val (title, desc, icon) = when (emergencyType) {
        EmergencyType.RESCUE -> Triple(strings.rescueTitle, strings.rescueSosDesc, Icons.Default.PersonPinCircle)
        EmergencyType.ROBBERY -> Triple(strings.robberyTitle, strings.robberySafetyNotice, Icons.Default.VisibilityOff)
        EmergencyType.KIDNAPPING -> Triple(strings.kidnappingTitle, strings.kidnappingSosDesc, Icons.Default.DirectionsCar)
    }

    val buttonLabel = when (emergencyType) {
        EmergencyType.RESCUE -> strings.rescueSosButton
        EmergencyType.ROBBERY -> if (discreetMode) strings.robberyDiscreetButton else strings.robberySosButton
        EmergencyType.KIDNAPPING -> strings.kidnappingSosButton
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (emergencyType == EmergencyType.ROBBERY && discreetMode) NavySurface else EmergencyRedContainer
            ),
            border = BorderStroke(1.5.dp, if (emergencyType == EmergencyType.KIDNAPPING) KidnappingOrange else EmergencyRedBright),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = SafetyYellow,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = WhiteText,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (emergencyType == EmergencyType.ROBBERY) SafetyYellow else WhiteText,
                    textAlign = TextAlign.Center
                )

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
                        modifier = Modifier.testTag("sos_grant_location_btn")
                    ) {
                        Icon(Icons.Default.GpsFixed, contentDescription = null, tint = SafetyYellow)
                        Spacer(Modifier.width(6.dp))
                        Text(strings.grantLocationBtn, color = SafetyYellow)
                    }
                } else {
                    Text(
                        text = if (userGps != null) {
                            "${strings.gpsActiveLabel}: ${"%.5f".format(userGps.latitude)}, ${"%.5f".format(userGps.longitude)}"
                        } else {
                            "${strings.gpsUnavailableLabel} — will use ${profile.localArea} coordinates"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (userGps != null) OnlineEmerald else SafetyYellow
                    )
                }
            }
        }

        // Discreet Mode toggle for Armed Robbery (or any SOS)
        if (emergencyType == EmergencyType.ROBBERY) {
            Card(
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = BorderStroke(1.dp, NavyBorder),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.discreetModeLabel, color = WhiteText, style = MaterialTheme.typography.titleMedium)
                    Switch(
                        checked = discreetMode,
                        onCheckedChange = { discreetMode = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = NavyDeepest, checkedTrackColor = SafetyYellow),
                        modifier = Modifier.testTag("robbery_discreet_switch")
                    )
                }
            }
        }

        OutlinedTextField(
            value = note,
            onValueChange = { note = it.take(250) },
            label = { Text(strings.optionalNoteLabel) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SafetyYellow,
                unfocusedBorderColor = NavyBorder,
                focusedLabelColor = SafetyYellow,
                unfocusedLabelColor = SlateTextSecondary,
                focusedTextColor = WhiteText,
                unfocusedTextColor = WhiteText
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("sos_note_input")
        )

        // Primary Big Red SOS Button
        Button(
            onClick = {
                if (!hasLocationPermission) {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
                onTriggerSos(discreetMode, note.ifBlank { null }) { _, message ->
                    confirmationMessage = message
                }
            },
            enabled = !isBusy,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (emergencyType == EmergencyType.ROBBERY && discreetMode) NavySurfaceElevated else EmergencyRed,
                contentColor = WhiteText
            ),
            shape = RoundedCornerShape(20.dp),
            border = if (emergencyType == EmergencyType.ROBBERY && discreetMode) BorderStroke(2.dp, EmergencyRedBright) else null,
            modifier = Modifier
                .fillMaxWidth()
                .height(82.dp)
                .testTag("sos_primary_trigger_btn")
        ) {
            if (isBusy) {
                CircularProgressIndicator(color = WhiteText, modifier = Modifier.size(28.dp))
            } else {
                Icon(icon, contentDescription = null, modifier = Modifier.size(30.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    text = if (isTestMode) "[TEST] $buttonLabel" else buttonLabel,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        // Confirmation card
        if (confirmationMessage != null) {
            Surface(
                color = OnlineEmeraldContainer,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, OnlineEmerald),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = OnlineEmerald)
                        Text(strings.reportSentTitle, color = WhiteText, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = when {
                            isTestMode -> strings.reportSentTestDesc
                            !isOnlineAuth -> strings.reportSentDemoDesc
                            else -> strings.reportSentOnlineDesc
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = SlateTextSecondary
                    )
                    Text(confirmationMessage!!, style = MaterialTheme.typography.labelSmall, color = SafetyYellow)
                }
            }
        }

        // Active Report Control & False Alarm Cancellation
        if (myActiveReport != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = BorderStroke(1.5.dp, SafetyYellow),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "${if (myActiveReport.testAlert) "[TEST] " else ""}Active Alert: ${myActiveReport.reportedDate} ${myActiveReport.reportedTime}",
                        style = MaterialTheme.typography.titleMedium,
                        color = SafetyYellow,
                        fontWeight = FontWeight.Bold
                    )
                    OsmEmergencyMapView(
                        country = country,
                        reports = listOf(myActiveReport),
                        userGps = userGps,
                        focusPoint = myActiveReport.latitude to myActiveReport.longitude,
                        focusZoom = 14.0,
                        onMarkerSelected = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onUpdateLiveGps(myActiveReport) },
                            border = BorderStroke(1.dp, SafetyYellow),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("sos_update_live_gps_btn")
                        ) {
                            Text(strings.updateLiveLocation, color = SafetyYellow, fontSize = 12.sp)
                        }
                        Button(
                            onClick = { onMarkResolved(myActiveReport) },
                            colors = ButtonDefaults.buttonColors(containerColor = OnlineEmerald, contentColor = NavyDeepest),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("sos_mark_resolved_btn")
                        ) {
                            Text(strings.markResolved, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    Button(
                        onClick = { onCancelFalseAlarm(myActiveReport) },
                        colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed, contentColor = WhiteText),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("sos_cancel_false_alarm_btn")
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.cancelFalseAlarm, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
