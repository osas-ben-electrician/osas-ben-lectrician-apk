package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DeviceStatus
import com.example.data.model.RegisteredDevice
import com.example.i18n.LocalAppStrings
import com.example.ui.FeedUiState
import com.example.ui.components.DetailKeyValueRow
import com.example.ui.theme.DemoAmber
import com.example.ui.theme.DemoAmberContainer
import com.example.ui.theme.EmergencyRedBright
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
fun PhoneTrackerScreen(
    devicesState: FeedUiState<List<RegisteredDevice>>,
    hasLocationPermission: Boolean,
    isBusy: Boolean,
    onBackToMap: () -> Unit,
    onLocationPermissionGranted: () -> Unit,
    onRegisterOrUpdateDevice: (String) -> Unit,
    onSetOffline: (RegisteredDevice) -> Unit,
    onDeleteDevice: (String) -> Unit
) {
    BackHandler { onBackToMap() }

    val strings = LocalAppStrings.current
    var deviceName by remember { mutableStateOf("${Build.MANUFACTURER} ${Build.MODEL}".trim()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms.values.any { it }) {
            onLocationPermissionGranted()
            onRegisterOrUpdateDevice(deviceName)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = BorderStroke(1.dp, NavyBorder),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = SafetyYellow, modifier = Modifier.size(28.dp))
                        Text(strings.phoneTrackerTitle, style = MaterialTheme.typography.headlineSmall, color = WhiteText)
                    }

                    Text(strings.phoneAuthorizationNotice, style = MaterialTheme.typography.bodyMedium, color = WhiteText)

                    Surface(
                        color = DemoAmberContainer,
                        border = BorderStroke(1.dp, DemoAmber.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = SafetyYellow, modifier = Modifier.size(18.dp))
                            Text(strings.phoneOfflineNotice, style = MaterialTheme.typography.bodyMedium, color = SlateTextSecondary)
                        }
                    }

                    OutlinedTextField(
                        value = deviceName,
                        onValueChange = { deviceName = it.take(80) },
                        label = { Text(strings.deviceNameLabel) },
                        singleLine = true,
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
                            .testTag("device_name_input")
                    )

                    Button(
                        onClick = {
                            if (!hasLocationPermission) {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            } else {
                                onRegisterOrUpdateDevice(deviceName)
                            }
                        },
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = SafetyYellow, contentColor = NavyDeepest),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("register_update_device_btn")
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = NavyDeepest)
                        } else {
                            Icon(Icons.Default.GpsFixed, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("${strings.registerDeviceButton} / ${strings.updateDeviceGpsButton}", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }

        when (devicesState) {
            is FeedUiState.Loading -> {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = SafetyYellow)
                    }
                }
            }
            is FeedUiState.Error -> {
                item {
                    Text(devicesState.message, color = EmergencyRedBright)
                }
            }
            is FeedUiState.Success -> {
                val devices = devicesState.data
                if (devices.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = NavySurfaceElevated),
                            border = BorderStroke(1.dp, NavyBorder),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(strings.noDevicesYet, color = SlateTextSecondary, modifier = Modifier.padding(16.dp))
                        }
                    }
                } else {
                    items(devices, key = { it.id }) { dev ->
                        val isActive = dev.statusEnum() == DeviceStatus.ACTIVE
                        Card(
                            colors = CardDefaults.cardColors(containerColor = NavySurface),
                            border = BorderStroke(1.dp, if (isActive) OnlineEmerald else NavyBorder),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(dev.deviceName, style = MaterialTheme.typography.titleMedium, color = WhiteText, fontWeight = FontWeight.Bold)
                                        Text(dev.deviceModel, style = MaterialTheme.typography.labelSmall, color = SlateTextSecondary)
                                    }
                                    Surface(
                                        color = if (isActive) OnlineEmerald else DemoAmberContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = if (isActive) strings.deviceActive else strings.deviceOfflineLastKnown,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isActive) NavyDeepest else SafetyYellow,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                DetailKeyValueRow(
                                    strings.coordinatesLabel,
                                    "${"%.5f".format(dev.latitude)}, ${"%.5f".format(dev.longitude)}"
                                )
                                DetailKeyValueRow(strings.lastUpdateLabel, dev.lastLocationUpdateText)
                                DetailKeyValueRow(strings.countryLabel, dev.country)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { onRegisterOrUpdateDevice(dev.deviceName) },
                                        border = BorderStroke(1.dp, SafetyYellow),
                                        modifier = Modifier.weight(1f).minimumInteractiveComponentSize()
                                    ) {
                                        Text(strings.updateDeviceGpsButton, color = SafetyYellow, fontSize = 12.sp)
                                    }
                                    if (isActive) {
                                        OutlinedButton(
                                            onClick = { onSetOffline(dev) },
                                            border = BorderStroke(1.dp, DemoAmber),
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) {
                                            Icon(Icons.Default.PowerSettingsNew, contentDescription = null, tint = DemoAmber, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(strings.markOfflineButton, color = DemoAmber, fontSize = 12.sp)
                                        }
                                    }
                                    IconButton(
                                        onClick = { onDeleteDevice(dev.id) },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remove device", tint = EmergencyRedBright)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
