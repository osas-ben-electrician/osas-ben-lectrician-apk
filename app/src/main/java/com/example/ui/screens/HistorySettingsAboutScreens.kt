package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.ActivityHistoryEntity
import com.example.data.local.LocalAppPreferences
import com.example.data.model.EmergencyReport
import com.example.data.model.EmergencyType
import com.example.data.model.NigeriaDataset
import com.example.data.model.SpainDataset
import com.example.data.model.SupportedCountry
import com.example.data.model.UserProfile
import com.example.i18n.LocalAppStrings
import com.example.ui.DiagnosticCheckResult
import com.example.ui.FeedUiState
import com.example.ui.components.CountryAndAreaPicker
import com.example.ui.components.EmergencyDetailBottomSheet
import com.example.ui.theme.EmergencyRed
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
fun EmergencyHistoryScreen(
    profile: UserProfile,
    reportsState: FeedUiState<List<EmergencyReport>>,
    localActivity: List<ActivityHistoryEntity>,
    onBackToMap: () -> Unit,
    onUpdateLiveGps: (EmergencyReport) -> Unit,
    onCancelFalseAlarm: (EmergencyReport) -> Unit,
    onMarkResolved: (EmergencyReport) -> Unit
) {
    BackHandler { onBackToMap() }

    val strings = LocalAppStrings.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedReport by remember { mutableStateOf<EmergencyReport?>(null) }
    val reports = (reportsState as? FeedUiState.Success)?.data.orEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyBackground)
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = NavySurface,
            contentColor = SafetyYellow
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text(strings.sharedFeedTab) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text(strings.localActivityTab) }
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (selectedTab == 0) {
                if (reports.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = NavySurface),
                            border = BorderStroke(1.dp, NavyBorder),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(strings.noReportsTitle, color = SlateTextSecondary, modifier = Modifier.padding(16.dp))
                        }
                    }
                } else {
                    items(reports, key = { it.id }) { rep ->
                        EmergencyFeedItemCard(
                            report = rep,
                            onClick = { selectedReport = rep }
                        )
                    }
                }
            } else {
                if (localActivity.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = NavySurface),
                            border = BorderStroke(1.dp, NavyBorder),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(strings.noActivityYet, color = SlateTextSecondary, modifier = Modifier.padding(16.dp))
                        }
                    }
                } else {
                    items(localActivity, key = { it.id }) { act ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = NavySurface),
                            border = BorderStroke(1.dp, if (act.testEntry) SafetyYellow.copy(alpha = 0.6f) else NavyBorder),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(act.title, style = MaterialTheme.typography.titleMedium, color = WhiteText)
                                Text(act.detail, style = MaterialTheme.typography.bodyMedium, color = SlateTextSecondary)
                                Text(act.country, style = MaterialTheme.typography.labelSmall, color = SafetyYellow)
                            }
                        }
                    }
                }
            }
        }
    }

    selectedReport?.let { current ->
        EmergencyDetailBottomSheet(
            report = current,
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
fun SettingsScreen(
    profile: UserProfile,
    preferences: LocalAppPreferences,
    nigeriaDataset: NigeriaDataset?,
    spainDataset: SpainDataset?,
    diagnosticResults: List<DiagnosticCheckResult>,
    onBackToMap: () -> Unit,
    onLanguageChange: (String) -> Unit,
    onUpdateArea: (SupportedCountry, String, String?, String) -> Unit,
    onSaveNigeriaContact: (String, String) -> Unit,
    onToggleTestMode: (Boolean) -> Unit,
    onRunDiagnostics: () -> Unit,
    onTriggerTestAlert: (EmergencyType) -> Unit,
    onSignOutOrReset: () -> Unit
) {
    BackHandler { onBackToMap() }

    val strings = LocalAppStrings.current
    var editCountry by remember(profile.country) { mutableStateOf(SupportedCountry.fromWire(profile.country)) }
    var editRegion by remember(profile.region) { mutableStateOf(profile.region) }
    var editProvince by remember(profile.province) { mutableStateOf(profile.province ?: "") }
    var editLocalArea by remember(profile.localArea) { mutableStateOf(profile.localArea) }

    var ngLabel by remember(preferences.nigeriaEmergencyLabel) { mutableStateOf(preferences.nigeriaEmergencyLabel) }
    var ngNumber by remember(preferences.nigeriaEmergencyNumber) { mutableStateOf(preferences.nigeriaEmergencyNumber) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Language Switcher
        Card(
            colors = CardDefaults.cardColors(containerColor = NavySurface),
            border = BorderStroke(1.dp, NavyBorder),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(strings.languageSection, style = MaterialTheme.typography.titleMedium, color = SafetyYellow)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("en" to strings.englishLabel, "es" to strings.spanishLabel).forEach { (code, label) ->
                        val active = preferences.language == code
                        Button(
                            onClick = { onLanguageChange(code) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (active) SafetyYellow else NavySurfaceElevated,
                                contentColor = if (active) NavyDeepest else WhiteText
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("settings_lang_$code")
                        ) {
                            Text(label, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 2. Country & Local Area Reassignment
        Card(
            colors = CardDefaults.cardColors(containerColor = NavySurface),
            border = BorderStroke(1.dp, NavyBorder),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(strings.profileLocationSection, style = MaterialTheme.typography.titleMedium, color = SafetyYellow)
                CountryAndAreaPicker(
                    country = editCountry,
                    region = editRegion,
                    province = editProvince,
                    localArea = editLocalArea,
                    nigeriaDataset = nigeriaDataset,
                    spainDataset = spainDataset,
                    onCountryChanged = {
                        if (it != editCountry) {
                            editCountry = it
                            editRegion = ""
                            editProvince = ""
                            editLocalArea = ""
                        }
                    },
                    onRegionChanged = {
                        editRegion = it
                        editProvince = ""
                        editLocalArea = ""
                    },
                    onProvinceChanged = {
                        editProvince = it
                        editLocalArea = ""
                    },
                    onLocalAreaChanged = { editLocalArea = it }
                )
                Button(
                    onClick = {
                        if (editRegion.isNotBlank() && editLocalArea.isNotBlank()) {
                            onUpdateArea(editCountry, editRegion, editProvince.ifBlank { null }, editLocalArea)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SafetyYellow, contentColor = NavyDeepest),
                    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize()
                ) {
                    Text(strings.saveProfileChanges, fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        // 3. Configurable Nigeria Emergency Contact
        Card(
            colors = CardDefaults.cardColors(containerColor = NavySurface),
            border = BorderStroke(1.dp, NavyBorder),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(strings.emergencyCallSection, style = MaterialTheme.typography.titleMedium, color = SafetyYellow)
                Text("Spain: 112 • Nigeria: Configurable local emergency number", style = MaterialTheme.typography.bodyMedium, color = SlateTextSecondary)
                OutlinedTextField(
                    value = ngLabel,
                    onValueChange = { ngLabel = it },
                    label = { Text(strings.nigeriaContactNameLabel) },
                    singleLine = true,
                    colors = settingsTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = ngNumber,
                    onValueChange = { ngNumber = it },
                    label = { Text(strings.nigeriaContactNumberLabel) },
                    singleLine = true,
                    colors = settingsTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = { onSaveNigeriaContact(ngLabel, ngNumber) },
                    border = BorderStroke(1.dp, SafetyYellow),
                    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize()
                ) {
                    Text(strings.saveContactButton, color = SafetyYellow, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 4. Dedicated TEST MODE Section
        Card(
            colors = CardDefaults.cardColors(containerColor = NavySurfaceElevated),
            border = BorderStroke(1.5.dp, SafetyYellow),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.BugReport, contentDescription = null, tint = SafetyYellow)
                        Text(strings.testModeTitle, style = MaterialTheme.typography.titleMedium, color = SafetyYellow, fontWeight = FontWeight.Bold)
                    }
                    Switch(
                        checked = preferences.testModeEnabled,
                        onCheckedChange = onToggleTestMode,
                        colors = SwitchDefaults.colors(checkedThumbColor = NavyDeepest, checkedTrackColor = SafetyYellow),
                        modifier = Modifier.testTag("settings_test_mode_switch")
                    )
                }
                Text(strings.testModeDesc, style = MaterialTheme.typography.bodyMedium, color = SlateTextSecondary)

                Button(
                    onClick = onRunDiagnostics,
                    colors = ButtonDefaults.buttonColors(containerColor = SafetyYellow, contentColor = NavyDeepest),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("run_diagnostics_btn")
                ) {
                    Text(strings.runDiagnosticsButton, fontWeight = FontWeight.ExtraBold)
                }

                // Quick [TEST] SOS Triggers
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    EmergencyType.entries.forEach { t ->
                        OutlinedButton(
                            onClick = { onTriggerTestAlert(t) },
                            border = BorderStroke(1.dp, EmergencyRedBright),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("test_trigger_${t.name.lowercase()}")
                        ) {
                            Text("[TEST]\n${t.name.take(6)}", color = WhiteText, fontSize = 11.sp, textAlign = TextAlign.Center)
                        }
                    }
                }

                diagnosticResults.forEach { res ->
                    Surface(
                        color = NavySurface,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (res.passed) OnlineEmerald else EmergencyRedBright),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (res.passed) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (res.passed) OnlineEmerald else EmergencyRedBright,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(res.label, style = MaterialTheme.typography.labelLarge, color = WhiteText)
                                Text(res.detail, style = MaterialTheme.typography.labelSmall, color = SlateTextSecondary)
                            }
                        }
                    }
                }
            }
        }

        // 5. Local Storage Deletion Notice + Sign Out
        Surface(
            color = NavySurface,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, NavyBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.Security, contentDescription = null, tint = SafetyYellow)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(strings.deleteNoticeTitle, style = MaterialTheme.typography.titleMedium, color = WhiteText)
                    Text(strings.localStorageNotice, style = MaterialTheme.typography.bodyMedium, color = SlateTextSecondary)
                }
            }
        }

        Button(
            onClick = onSignOutOrReset,
            colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed, contentColor = WhiteText),
            modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .testTag("settings_signout_btn")
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(strings.switchAccountSignOut, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AboutScreen(
    onBackToMap: () -> Unit
) {
    BackHandler { onBackToMap() }

    val strings = LocalAppStrings.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyBackground)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.osas_ben_logo),
            contentDescription = strings.appName,
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clip(RoundedCornerShape(18.dp)),
            contentScale = ContentScale.Fit
        )
        Text(strings.appName, style = MaterialTheme.typography.displaySmall, color = WhiteText, textAlign = TextAlign.Center)
        Text(strings.tagline, style = MaterialTheme.typography.labelLarge, color = SafetyYellow, textAlign = TextAlign.Center)
        Text(strings.versionLabel, style = MaterialTheme.typography.bodyMedium, color = SlateTextSecondary, textAlign = TextAlign.Center)

        Card(
            colors = CardDefaults.cardColors(containerColor = NavySurface),
            border = BorderStroke(1.dp, NavyBorder),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(strings.aboutDesc, style = MaterialTheme.typography.bodyLarge, color = WhiteText)
                Text(strings.privacySummaryTitle, style = MaterialTheme.typography.titleMedium, color = SafetyYellow)
                Text(strings.privacySummaryBody, style = MaterialTheme.typography.bodyMedium, color = SlateTextSecondary)
                Text(strings.osmAttribution, style = MaterialTheme.typography.labelSmall, color = SlateTextSecondary)
            }
        }
    }
}

@Composable
private fun settingsTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = SafetyYellow,
    unfocusedBorderColor = NavyBorder,
    focusedLabelColor = SafetyYellow,
    unfocusedLabelColor = SlateTextSecondary,
    focusedTextColor = WhiteText,
    unfocusedTextColor = WhiteText
)
