package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PersonPinCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.model.EmergencyReport
import com.example.data.model.EmergencyType
import com.example.data.model.NigeriaDataset
import com.example.data.model.ReportStatus
import com.example.data.model.SpainDataset
import com.example.data.model.SupportedCountry
import com.example.i18n.LocalAppStrings
import com.example.ui.ClockCalendarState
import com.example.ui.theme.DemoAmber
import com.example.ui.theme.DemoAmberContainer
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.EmergencyRedBright
import com.example.ui.theme.EmergencyRedContainer
import com.example.ui.theme.KidnappingOrange
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
fun OsasTopBanner(
    clock: ClockCalendarState,
    country: SupportedCountry,
    isOnlineAuth: Boolean,
    isTestMode: Boolean,
    showMenuButton: Boolean,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(NavyDeepest)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (showMenuButton) {
                IconButton(
                    onClick = onMenuClick,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("nav_menu_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Open navigation menu",
                        tint = WhiteText
                    )
                }
            }
            Image(
                painter = painterResource(id = R.drawable.osas_ben_emblem),
                contentDescription = strings.appName,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Fit
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = strings.appName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = WhiteText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    CountryBadge(country = country)
                }
                Text(
                    text = strings.tagline,
                    style = MaterialTheme.typography.labelSmall,
                    color = SafetyYellow,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = SafetyYellow,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = clock.liveTime,
                        style = MaterialTheme.typography.labelMedium,
                        color = WhiteText,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = clock.fullDateFormatted,
                    style = MaterialTheme.typography.labelSmall,
                    color = SlateTextSecondary
                )
            }
        }

        // Mode strip: clearly differentiates Online Verified vs Local DEMO vs TEST MODE
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    when {
                        isTestMode -> SafetyYellow
                        !isOnlineAuth -> DemoAmberContainer
                        else -> OnlineEmeraldContainer
                    }
                )
                .padding(horizontal = 14.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = when {
                    isTestMode -> strings.testModeBanner
                    !isOnlineAuth -> strings.localDemoMode
                    else -> "${strings.onlineVerifiedMode} • ${country.wireName.uppercase()}"
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (isTestMode) NavyDeepest else WhiteText,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = clock.timezoneId,
                style = MaterialTheme.typography.labelSmall,
                color = if (isTestMode) NavyDeepest else SlateTextSecondary,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
fun CountryBadge(country: SupportedCountry) {
    Surface(
        color = NavySurfaceElevated,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, SafetyYellow.copy(alpha = 0.65f))
    ) {
        Text(
            text = if (country == SupportedCountry.SPAIN) "ES" else "NG",
            style = MaterialTheme.typography.labelSmall,
            color = SafetyYellow,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun EmergencyTypeBadge(type: EmergencyType, isTest: Boolean) {
    val strings = LocalAppStrings.current
    val (bg, label, icon) = when (type) {
        EmergencyType.RESCUE -> Triple(EmergencyRed, strings.rescueTitle, Icons.Default.PersonPinCircle)
        EmergencyType.ROBBERY -> Triple(EmergencyRedBright, strings.robberyTitle, Icons.Default.VisibilityOff)
        EmergencyType.KIDNAPPING -> Triple(KidnappingOrange, strings.kidnappingTitle, Icons.Default.DirectionsCar)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(color = bg, shape = RoundedCornerShape(8.dp)) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = WhiteText, modifier = Modifier.size(15.dp))
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = WhiteText,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        if (isTest) {
            Surface(color = SafetyYellow, shape = RoundedCornerShape(8.dp)) {
                Text(
                    text = "TEST",
                    style = MaterialTheme.typography.labelSmall,
                    color = NavyDeepest,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyDetailBottomSheet(
    report: EmergencyReport,
    currentUserId: String?,
    onDismiss: () -> Unit,
    onUpdateLiveGps: (EmergencyReport) -> Unit,
    onCancelFalseAlarm: (EmergencyReport) -> Unit,
    onMarkResolved: (EmergencyReport) -> Unit
) {
    val strings = LocalAppStrings.current
    val isCreator = currentUserId != null && currentUserId == report.userId
    val isActive = report.statusEnum() == ReportStatus.ACTIVE

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = NavySurface,
        contentColor = WhiteText
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                EmergencyTypeBadge(type = report.typeEnum(), isTest = report.testAlert)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = strings.closeLabel, tint = SlateTextSecondary)
                }
            }

            if (report.localDemoOnly) {
                Surface(
                    color = DemoAmberContainer,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, DemoAmber)
                ) {
                    Text(
                        text = strings.localDemoMode,
                        style = MaterialTheme.typography.labelSmall,
                        color = SafetyYellow,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            DetailKeyValueRow(strings.timeReportedLabel, report.reportedTime)
            DetailKeyValueRow(strings.dateReportedLabel, report.reportedDate)
            DetailKeyValueRow(
                strings.areaLabel,
                listOfNotNull(report.localArea, report.province, report.region, report.country).joinToString(" • ")
            )
            DetailKeyValueRow(
                strings.coordinatesLabel,
                "${"%.5f".format(report.latitude)}, ${"%.5f".format(report.longitude)}"
            )
            DetailKeyValueRow(
                strings.statusLabel,
                when (report.statusEnum()) {
                    ReportStatus.ACTIVE -> strings.statusActive
                    ReportStatus.RESOLVED -> strings.statusResolved
                    ReportStatus.CANCELLED_FALSE_ALARM -> strings.statusCancelled
                }
            )
            if (!report.note.isNullOrBlank()) {
                DetailKeyValueRow("Note", report.note)
            }

            if (isCreator && isActive) {
                HorizontalDivider(color = NavyBorder, modifier = Modifier.padding(vertical = 4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onUpdateLiveGps(report) },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("sheet_update_gps_btn"),
                        border = BorderStroke(1.dp, SafetyYellow)
                    ) {
                        Icon(Icons.Default.GpsFixed, contentDescription = null, tint = SafetyYellow, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(strings.updateLiveLocation, color = SafetyYellow, fontSize = 12.sp)
                    }
                    Button(
                        onClick = { onMarkResolved(report) },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("sheet_resolve_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = OnlineEmerald, contentColor = NavyDeepest)
                    ) {
                        Text(strings.markResolved, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
                Button(
                    onClick = { onCancelFalseAlarm(report) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("sheet_cancel_false_alarm_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed, contentColor = WhiteText)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.cancelFalseAlarm, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun DetailKeyValueRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = SlateTextSecondary)
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = WhiteText,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun SearchableDropdownField(
    label: String,
    selectedValue: String,
    options: List<String>,
    enabled: Boolean = true,
    onSelected: (String) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    var showDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) NavySurfaceElevated else NavySurface)
            .border(1.dp, if (selectedValue.isNotBlank()) SafetyYellow.copy(alpha = 0.65f) else NavyBorder, RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) { showDialog = true }
            .minimumInteractiveComponentSize()
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = SafetyYellow
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = selectedValue.ifBlank { strings.searchPlaceholder },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (selectedValue.isNotBlank()) WhiteText else SlateTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = label,
                tint = SafetyYellow
            )
        }
    }

    if (showDialog) {
        var query by remember { mutableStateOf("") }
        val filtered = remember(options, query) {
            if (query.isBlank()) options.take(250)
            else options.filter { it.contains(query.trim(), ignoreCase = true) }.take(250)
        }
        Dialog(onDismissRequest = { showDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, NavyBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, style = MaterialTheme.typography.titleMedium, color = WhiteText)
                        IconButton(onClick = { showDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = strings.closeLabel, tint = SlateTextSecondary)
                        }
                    }
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text(strings.searchPlaceholder, color = SlateTextSecondary) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SafetyYellow) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SafetyYellow,
                            unfocusedBorderColor = NavyBorder,
                            focusedTextColor = WhiteText,
                            unfocusedTextColor = WhiteText
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                    Text(
                        text = "${options.size} total • showing ${filtered.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SlateTextSecondary
                    )
                    Spacer(Modifier.height(6.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                        items(filtered, key = { it }) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelected(item)
                                        showDialog = false
                                    }
                                    .minimumInteractiveComponentSize()
                                    .padding(horizontal = 8.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (item == selectedValue) SafetyYellow else WhiteText
                                )
                            }
                            HorizontalDivider(color = NavyBorder.copy(alpha = 0.4f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CountryAndAreaPicker(
    country: SupportedCountry,
    region: String,
    province: String,
    localArea: String,
    nigeriaDataset: NigeriaDataset?,
    spainDataset: SpainDataset?,
    onCountryChanged: (SupportedCountry) -> Unit,
    onRegionChanged: (String) -> Unit,
    onProvinceChanged: (String) -> Unit,
    onLocalAreaChanged: (String) -> Unit,
    onAutoDetectClick: (() -> Unit)? = null
) {
    val strings = LocalAppStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(strings.countryLabel, style = MaterialTheme.typography.labelLarge, color = SafetyYellow)
            if (onAutoDetectClick != null) {
                OutlinedButton(
                    onClick = onAutoDetectClick,
                    border = BorderStroke(1.dp, SafetyYellow),
                    modifier = Modifier.testTag("auto_detect_country_btn")
                ) {
                    Icon(Icons.Default.GpsFixed, contentDescription = null, tint = SafetyYellow, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(strings.detectCountryGps, color = SafetyYellow, fontSize = 12.sp)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SupportedCountry.entries.forEach { option ->
                val selected = option == country
                Button(
                    onClick = { onCountryChanged(option) },
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("select_country_${option.wireName.lowercase()}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) SafetyYellow else NavySurfaceElevated,
                        contentColor = if (selected) NavyDeepest else WhiteText
                    ),
                    border = if (!selected) BorderStroke(1.dp, NavyBorder) else null
                ) {
                    Text(
                        text = if (option == SupportedCountry.SPAIN) strings.spainName else strings.nigeriaName,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        when (country) {
            SupportedCountry.NIGERIA -> {
                val states = nigeriaDataset?.states.orEmpty()
                val stateNames = states.map { it.name }
                val lgaNames = states.firstOrNull { it.name == region }?.lgas?.map { it.name }.orEmpty()

                SearchableDropdownField(
                    label = strings.stateLabel,
                    selectedValue = region,
                    options = stateNames,
                    enabled = stateNames.isNotEmpty(),
                    onSelected = onRegionChanged,
                    testTag = "ng_state_picker"
                )
                SearchableDropdownField(
                    label = strings.lgaLabel,
                    selectedValue = localArea,
                    options = lgaNames,
                    enabled = lgaNames.isNotEmpty(),
                    onSelected = onLocalAreaChanged,
                    testTag = "ng_lga_picker"
                )
            }
            SupportedCountry.SPAIN -> {
                val communities = spainDataset?.communities.orEmpty()
                val comNames = communities.map { it.name }
                val provinces = communities.firstOrNull { it.name == region }?.provinces.orEmpty()
                val provNames = provinces.map { it.name }
                val munNames = provinces.firstOrNull { it.name == province }?.municipalities?.map { it.name }.orEmpty()

                SearchableDropdownField(
                    label = strings.communityLabel,
                    selectedValue = region,
                    options = comNames,
                    enabled = comNames.isNotEmpty(),
                    onSelected = onRegionChanged,
                    testTag = "es_community_picker"
                )
                SearchableDropdownField(
                    label = strings.provinceLabel,
                    selectedValue = province,
                    options = provNames,
                    enabled = provNames.isNotEmpty(),
                    onSelected = onProvinceChanged,
                    testTag = "es_province_picker"
                )
                SearchableDropdownField(
                    label = strings.municipalityLabel,
                    selectedValue = localArea,
                    options = munNames,
                    enabled = munNames.isNotEmpty(),
                    onSelected = onLocalAreaChanged,
                    testTag = "es_municipality_picker"
                )
            }
        }
    }
}
