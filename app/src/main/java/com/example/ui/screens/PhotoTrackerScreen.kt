package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.PhotoHistoryEntity
import com.example.data.model.PhotoMetadataResult
import com.example.data.model.SupportedCountry
import com.example.i18n.LocalAppStrings
import com.example.ui.components.DetailKeyValueRow
import com.example.ui.components.OsmEmergencyMapView
import com.example.ui.theme.EmergencyRedBright
import com.example.ui.theme.NavyBackground
import com.example.ui.theme.NavyBorder
import com.example.ui.theme.NavyDeepest
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceElevated
import com.example.ui.theme.SafetyYellow
import com.example.ui.theme.SlateTextSecondary
import com.example.ui.theme.WhiteText

@Composable
fun PhotoTrackerScreen(
    country: SupportedCountry,
    lastPhoto: PhotoMetadataResult?,
    photoHistory: List<PhotoHistoryEntity>,
    onBackToMap: () -> Unit,
    onPhotoPicked: (Uri) -> Unit
) {
    BackHandler { onBackToMap() }

    val strings = LocalAppStrings.current
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { onPhotoPicked(it) }
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
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = SafetyYellow, modifier = Modifier.size(28.dp))
                        Text(strings.photoTrackerTitle, style = MaterialTheme.typography.headlineSmall, color = WhiteText)
                    }
                    Text(strings.photoPrivacyNote, style = MaterialTheme.typography.bodyMedium, color = SlateTextSecondary)
                    Button(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SafetyYellow, contentColor = NavyDeepest),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("pick_photo_btn")
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.pickPhotoButton, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        if (lastPhoto != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = NavySurface),
                    border = BorderStroke(1.dp, SafetyYellow.copy(alpha = 0.65f)),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AsyncImage(
                            model = Uri.parse(lastPhoto.uriString),
                            contentDescription = lastPhoto.displayName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                        Text(lastPhoto.displayName, style = MaterialTheme.typography.titleMedium, color = SafetyYellow)
                        DetailKeyValueRow(strings.dateTakenLabel, lastPhoto.dateTaken)
                        DetailKeyValueRow(strings.timeTakenLabel, lastPhoto.timeTaken)

                        if (lastPhoto.hasGps) {
                            DetailKeyValueRow(strings.latitudeLabel, "%.6f".format(lastPhoto.latitude))
                            DetailKeyValueRow(strings.longitudeLabel, "%.6f".format(lastPhoto.longitude))
                            DetailKeyValueRow(strings.cityLabel, lastPhoto.city)
                            DetailKeyValueRow(strings.countryLabel, lastPhoto.country)

                            OsmEmergencyMapView(
                                country = country,
                                reports = emptyList(),
                                userGps = null,
                                focusPoint = lastPhoto.latitude!! to lastPhoto.longitude!!,
                                focusZoom = 14.5,
                                onMarkerSelected = {},
                                photoMarker = lastPhoto.latitude to lastPhoto.longitude,
                                photoMarkerLabel = lastPhoto.displayName,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(210.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        } else {
                            Surface(
                                color = EmergencyRedBright.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, EmergencyRedBright),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.LocationOff, contentDescription = null, tint = EmergencyRedBright)
                                    Column {
                                        Text(
                                            text = strings.locationNotAvailable,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = WhiteText,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "No GPS EXIF tags were stored in this photograph.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = SlateTextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = strings.photoHistoryTitle,
                style = MaterialTheme.typography.titleLarge,
                color = WhiteText,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (photoHistory.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = NavySurfaceElevated),
                    border = BorderStroke(1.dp, NavyBorder),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = strings.noPhotoHistory,
                        color = SlateTextSecondary,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(photoHistory, key = { it.id }) { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = NavySurface),
                    border = BorderStroke(1.dp, NavyBorder),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(item.displayName, style = MaterialTheme.typography.titleMedium, color = WhiteText)
                        Text("${item.dateTaken} • ${item.timeTaken}", style = MaterialTheme.typography.labelSmall, color = SlateTextSecondary)
                        Text(
                            text = if (item.latitude != null && item.longitude != null) {
                                "GPS: ${"%.5f".format(item.latitude)}, ${"%.5f".format(item.longitude)} • ${item.city}, ${item.country}"
                            } else {
                                strings.locationNotAvailable
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (item.latitude != null) SafetyYellow else EmergencyRedBright
                        )
                    }
                }
            }
        }
    }
}
