package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PersonPinCircle
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.auth.GoogleAuthHelper
import com.example.data.model.EmergencyType
import com.example.data.model.ReportStatus
import com.example.i18n.LocalAppStrings
import com.example.i18n.stringsForLanguage
import com.example.ui.FeedUiState
import com.example.ui.OsasViewModel
import com.example.ui.components.OsasTopBanner
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.EmergencyHistoryScreen
import com.example.ui.screens.HomeMapScreen
import com.example.ui.screens.PhoneTrackerScreen
import com.example.ui.screens.PhotoTrackerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SosActionScreen
import com.example.ui.screens.WelcomeRegistrationScreen
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.NavyBackground
import com.example.ui.theme.NavyBorder
import com.example.ui.theme.NavyDeepest
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceElevated
import com.example.ui.theme.OsasBenTrackerTheme
import com.example.ui.theme.SafetyYellow
import com.example.ui.theme.SlateTextSecondary
import com.example.ui.theme.WhiteText
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

const val ROUTE_HOME_MAP = "home_map"
const val ROUTE_RESCUE_SOS = "rescue_sos"
const val ROUTE_ROBBERY_SOS = "robbery_sos"
const val ROUTE_KIDNAPPING_SOS = "kidnapping_sos"
const val ROUTE_PHOTO_TRACKER = "photo_tracker"
const val ROUTE_PHONE_TRACKER = "phone_tracker"
const val ROUTE_HISTORY = "emergency_history"
const val ROUTE_SETTINGS = "settings"
const val ROUTE_ABOUT = "about_osas_ben_tracker"

data class NavDestinationItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val isEmergency: Boolean = false
)

class MainActivity : ComponentActivity() {
    private val _incomingEmailLink = MutableStateFlow<String?>(null)
    private val incomingEmailLink: StateFlow<String?> = _incomingEmailLink.asStateFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _incomingEmailLink.value = intent?.data?.toString()
        enableEdgeToEdge()
        setContent {
            OsasBenTrackerTheme {
                val link by incomingEmailLink.collectAsStateWithLifecycle()
                OsasBenTrackerRoot(
                    incomingEmailLink = link,
                    onConsumeIncomingEmailLink = { _incomingEmailLink.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.data?.toString()?.let { _incomingEmailLink.value = it }
    }
}

@Composable
fun OsasBenTrackerRoot(
    incomingEmailLink: String? = null,
    onConsumeIncomingEmailLink: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    LaunchedEffect(Unit) {
        if (currentUser == null) {
            GoogleAuthHelper.attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = { currentUser = Firebase.auth.currentUser },
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    val sessionKey = currentUser?.uid ?: "demo_session"
    key(sessionKey) {
        val viewModel: OsasViewModel = viewModel(
            key = sessionKey,
            factory = OsasViewModel.factory(
                context = context,
                authenticatedUid = currentUser?.uid,
                authenticatedEmail = currentUser?.email
            )
        )

        val preferences by viewModel.preferences.collectAsStateWithLifecycle()
        val strings = remember(preferences.language) { stringsForLanguage(preferences.language) }

        CompositionLocalProvider(LocalAppStrings provides strings) {
            val activeProfile by viewModel.activeProfile.collectAsStateWithLifecycle()
            val onlineProfileState by viewModel.onlineProfileState.collectAsStateWithLifecycle()
            val nigeriaDataset by viewModel.nigeriaDataset.collectAsStateWithLifecycle()
            val spainDataset by viewModel.spainDataset.collectAsStateWithLifecycle()
            val hasLocationPermission by viewModel.hasLocationPermission.collectAsStateWithLifecycle()
            val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
            val bannerMessage by viewModel.statusBannerMessage.collectAsStateWithLifecycle()

            LaunchedEffect(currentUser?.uid, onlineProfileState, preferences.pendingEmailLinkLocalArea) {
                if (
                    currentUser != null &&
                    onlineProfileState is FeedUiState.Success &&
                    (onlineProfileState as FeedUiState.Success).data == null &&
                    preferences.pendingEmailLinkFirstName.isNotBlank() &&
                    preferences.pendingEmailLinkLocalArea.isNotBlank()
                ) {
                    viewModel.autoCompletePendingEmailLinkRegistration()
                }
            }

            val snackbarHostState = remember { SnackbarHostState() }
            LaunchedEffect(bannerMessage) {
                bannerMessage?.let { msg ->
                    snackbarHostState.showSnackbar(msg)
                    viewModel.clearBanner()
                }
            }

            if (viewModel.isOnlineAuth && onlineProfileState is FeedUiState.Loading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(NavyBackground),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = SafetyYellow)
                }
            } else if (activeProfile == null) {
                Scaffold(
                    containerColor = NavyBackground,
                    contentWindowInsets = WindowInsets.safeDrawing,
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        WelcomeRegistrationScreen(
                            isAuthenticatedOnline = currentUser != null,
                            authenticatedEmail = currentUser?.email,
                            authenticatedDisplayName = currentUser?.displayName,
                            preferences = preferences,
                            incomingEmailLink = incomingEmailLink,
                            currentLanguage = preferences.language,
                            nigeriaDataset = nigeriaDataset,
                            spainDataset = spainDataset,
                            hasLocationPermission = hasLocationPermission,
                            isBusy = isBusy,
                            onLanguageChange = viewModel::setLanguage,
                            onLocationPermissionGranted = { viewModel.onLocationPermissionResult(true) },
                            onDetectCountryGps = { viewModel.detectCountryFromGps() },
                            onConsumeIncomingEmailLink = onConsumeIncomingEmailLink,
                            onEmailLinkDraftSaved = { firstName, email, country, region, province, localArea ->
                                viewModel.savePendingEmailLinkDraft(
                                    firstName = firstName,
                                    email = email,
                                    country = country,
                                    region = region,
                                    province = province,
                                    localArea = localArea
                                )
                            },
                            onClearEmailLinkDraft = viewModel::clearPendingEmailLinkDraft,
                            onFirebaseAuthCompleted = { currentUser = Firebase.auth.currentUser },
                            onSaveProfile = { firstName, email, country, region, province, localArea, demoMode ->
                                viewModel.saveRegistrationProfile(
                                    firstName = firstName,
                                    email = email,
                                    country = country,
                                    region = region,
                                    province = province,
                                    localArea = localArea,
                                    demoMode = demoMode
                                )
                            }
                        )
                    }
                }
            } else {
                MainAuthenticatedShell(
                    viewModel = viewModel,
                    snackbarHostState = snackbarHostState,
                    onSignOutOrReset = {
                        if (currentUser != null) {
                            GoogleAuthHelper.signOut(credentialManager, scope) {
                                viewModel.resetDemoSession()
                                currentUser = null
                            }
                        } else {
                            viewModel.resetDemoSession()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun MainAuthenticatedShell(
    viewModel: OsasViewModel,
    snackbarHostState: SnackbarHostState,
    onSignOutOrReset: () -> Unit
) {
    val strings = LocalAppStrings.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    val profile = viewModel.activeProfile.collectAsStateWithLifecycle().value ?: return
    val country by viewModel.activeCountry.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val clock by viewModel.clockCalendar.collectAsStateWithLifecycle()
    val reportsState by viewModel.emergencyReportsState.collectAsStateWithLifecycle()
    val devicesState by viewModel.registeredDevicesState.collectAsStateWithLifecycle()
    val areaFilter by viewModel.mapAreaFilter.collectAsStateWithLifecycle()
    val typeFilter by viewModel.mapTypeFilter.collectAsStateWithLifecycle()
    val currentGps by viewModel.currentGps.collectAsStateWithLifecycle()
    val hasLocationPermission by viewModel.hasLocationPermission.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val lastPhoto by viewModel.lastInspectedPhoto.collectAsStateWithLifecycle()
    val photoHistory by viewModel.photoHistory.collectAsStateWithLifecycle()
    val activityHistory by viewModel.activityHistory.collectAsStateWithLifecycle()
    val nigeriaDataset by viewModel.nigeriaDataset.collectAsStateWithLifecycle()
    val spainDataset by viewModel.spainDataset.collectAsStateWithLifecycle()
    val diagnosticResults by viewModel.diagnosticResults.collectAsStateWithLifecycle()

    var currentRoute by remember { mutableStateOf(ROUTE_HOME_MAP) }

    val allNavItems = remember(strings) {
        listOf(
            NavDestinationItem(ROUTE_HOME_MAP, strings.navHomeMap, Icons.Default.Map),
            NavDestinationItem(ROUTE_RESCUE_SOS, strings.navRescueSos, Icons.Default.PersonPinCircle, isEmergency = true),
            NavDestinationItem(ROUTE_ROBBERY_SOS, strings.navRobberySos, Icons.Default.VisibilityOff, isEmergency = true),
            NavDestinationItem(ROUTE_KIDNAPPING_SOS, strings.navKidnappingSos, Icons.Default.DirectionsCar, isEmergency = true),
            NavDestinationItem(ROUTE_PHOTO_TRACKER, strings.navPhotoTracker, Icons.Default.PhotoCamera),
            NavDestinationItem(ROUTE_PHONE_TRACKER, strings.navPhoneTracker, Icons.Default.PhoneAndroid),
            NavDestinationItem(ROUTE_HISTORY, strings.navHistory, Icons.Default.History),
            NavDestinationItem(ROUTE_SETTINGS, strings.navSettings, Icons.Default.Settings),
            NavDestinationItem(ROUTE_ABOUT, strings.navAbout, Icons.Default.Info)
        )
    }

    val bottomBarItems = remember(allNavItems) {
        listOf(allNavItems[0], allNavItems[1], allNavItems[4], allNavItems[5], allNavItems[7])
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyBackground)
    ) {
        val isWideScreen = maxWidth >= 840.dp

        val mainContent: @Composable (Boolean) -> Unit = { showMenuButton ->
            Scaffold(
                containerColor = NavyBackground,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    OsasTopBanner(
                        clock = clock,
                        country = country,
                        isOnlineAuth = viewModel.isOnlineAuth,
                        isTestMode = preferences.testModeEnabled,
                        showMenuButton = showMenuButton,
                        onMenuClick = { scope.launch { drawerState.open() } },
                        modifier = Modifier.statusBarsPadding()
                    )
                },
                bottomBar = {
                    if (!isWideScreen) {
                        NavigationBar(
                            containerColor = NavyDeepest,
                            contentColor = WhiteText,
                            modifier = Modifier.navigationBarsPadding()
                        ) {
                            bottomBarItems.forEach { item ->
                                val selected = currentRoute == item.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentRoute = item.route },
                                    icon = {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.label
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = item.label.substringBefore(" /").take(10),
                                            maxLines = 1
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = if (item.isEmergency) WhiteText else NavyDeepest,
                                        selectedTextColor = if (item.isEmergency) EmergencyRed else SafetyYellow,
                                        indicatorColor = if (item.isEmergency) EmergencyRed else SafetyYellow,
                                        unselectedIconColor = SlateTextSecondary,
                                        unselectedTextColor = SlateTextSecondary
                                    ),
                                    modifier = Modifier.testTag("bottom_nav_${item.route}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    val activeReportsList = (reportsState as? FeedUiState.Success)?.data.orEmpty()
                    when (currentRoute) {
                        ROUTE_HOME_MAP -> HomeMapScreen(
                            profile = profile,
                            country = country,
                            preferences = preferences,
                            clock = clock,
                            reportsState = reportsState,
                            areaFilter = areaFilter,
                            typeFilter = typeFilter,
                            userGps = currentGps,
                            hasLocationPermission = hasLocationPermission,
                            onAreaFilterChanged = viewModel::setMapAreaFilter,
                            onTypeFilterChanged = viewModel::setMapTypeFilter,
                            onLocationPermissionGranted = { viewModel.onLocationPermissionResult(true) },
                            onRefreshGps = viewModel::refreshGpsLocation,
                            onNavigateToSos = { type ->
                                currentRoute = when (type) {
                                    EmergencyType.RESCUE -> ROUTE_RESCUE_SOS
                                    EmergencyType.ROBBERY -> ROUTE_ROBBERY_SOS
                                    EmergencyType.KIDNAPPING -> ROUTE_KIDNAPPING_SOS
                                }
                            },
                            onUpdateLiveGps = viewModel::updateActiveReportGps,
                            onCancelFalseAlarm = { rep ->
                                viewModel.updateReportStatus(
                                    rep,
                                    ReportStatus.CANCELLED_FALSE_ALARM,
                                    "False alarm closed by reporter"
                                )
                            },
                            onMarkResolved = { rep ->
                                viewModel.updateReportStatus(
                                    rep,
                                    ReportStatus.RESOLVED,
                                    "Marked resolved"
                                )
                            }
                        )
                        ROUTE_RESCUE_SOS -> SosActionScreen(
                            emergencyType = EmergencyType.RESCUE,
                            profile = profile,
                            country = country,
                            userGps = currentGps,
                            hasLocationPermission = hasLocationPermission,
                            isTestMode = preferences.testModeEnabled,
                            isOnlineAuth = viewModel.isOnlineAuth,
                            isBusy = isBusy,
                            activeReports = activeReportsList,
                            onBackToMap = { currentRoute = ROUTE_HOME_MAP },
                            onLocationPermissionGranted = { viewModel.onLocationPermissionResult(true) },
                            onTriggerSos = { discreet, note, done ->
                                viewModel.submitSosReport(EmergencyType.RESCUE, discreet, note, onComplete = done)
                            },
                            onUpdateLiveGps = viewModel::updateActiveReportGps,
                            onCancelFalseAlarm = { rep ->
                                viewModel.updateReportStatus(rep, ReportStatus.CANCELLED_FALSE_ALARM, "False alarm closed")
                            },
                            onMarkResolved = { rep ->
                                viewModel.updateReportStatus(rep, ReportStatus.RESOLVED, "Resolved")
                            }
                        )
                        ROUTE_ROBBERY_SOS -> SosActionScreen(
                            emergencyType = EmergencyType.ROBBERY,
                            profile = profile,
                            country = country,
                            userGps = currentGps,
                            hasLocationPermission = hasLocationPermission,
                            isTestMode = preferences.testModeEnabled,
                            isOnlineAuth = viewModel.isOnlineAuth,
                            isBusy = isBusy,
                            activeReports = activeReportsList,
                            onBackToMap = { currentRoute = ROUTE_HOME_MAP },
                            onLocationPermissionGranted = { viewModel.onLocationPermissionResult(true) },
                            onTriggerSos = { discreet, note, done ->
                                viewModel.submitSosReport(EmergencyType.ROBBERY, discreet, note, onComplete = done)
                            },
                            onUpdateLiveGps = viewModel::updateActiveReportGps,
                            onCancelFalseAlarm = { rep ->
                                viewModel.updateReportStatus(rep, ReportStatus.CANCELLED_FALSE_ALARM, "False alarm closed")
                            },
                            onMarkResolved = { rep ->
                                viewModel.updateReportStatus(rep, ReportStatus.RESOLVED, "Resolved")
                            }
                        )
                        ROUTE_KIDNAPPING_SOS -> SosActionScreen(
                            emergencyType = EmergencyType.KIDNAPPING,
                            profile = profile,
                            country = country,
                            userGps = currentGps,
                            hasLocationPermission = hasLocationPermission,
                            isTestMode = preferences.testModeEnabled,
                            isOnlineAuth = viewModel.isOnlineAuth,
                            isBusy = isBusy,
                            activeReports = activeReportsList,
                            onBackToMap = { currentRoute = ROUTE_HOME_MAP },
                            onLocationPermissionGranted = { viewModel.onLocationPermissionResult(true) },
                            onTriggerSos = { discreet, note, done ->
                                viewModel.submitSosReport(EmergencyType.KIDNAPPING, discreet, note, onComplete = done)
                            },
                            onUpdateLiveGps = viewModel::updateActiveReportGps,
                            onCancelFalseAlarm = { rep ->
                                viewModel.updateReportStatus(rep, ReportStatus.CANCELLED_FALSE_ALARM, "False alarm closed")
                            },
                            onMarkResolved = { rep ->
                                viewModel.updateReportStatus(rep, ReportStatus.RESOLVED, "Resolved")
                            }
                        )
                        ROUTE_PHOTO_TRACKER -> PhotoTrackerScreen(
                            country = country,
                            lastPhoto = lastPhoto,
                            photoHistory = photoHistory,
                            onBackToMap = { currentRoute = ROUTE_HOME_MAP },
                            onPhotoPicked = viewModel::inspectPhoto
                        )
                        ROUTE_PHONE_TRACKER -> PhoneTrackerScreen(
                            devicesState = devicesState,
                            hasLocationPermission = hasLocationPermission,
                            isBusy = isBusy,
                            onBackToMap = { currentRoute = ROUTE_HOME_MAP },
                            onLocationPermissionGranted = { viewModel.onLocationPermissionResult(true) },
                            onRegisterOrUpdateDevice = { name -> viewModel.registerOrUpdateCurrentPhone(name) },
                            onSetOffline = viewModel::setDeviceOffline,
                            onDeleteDevice = viewModel::deleteRegisteredDevice
                        )
                        ROUTE_HISTORY -> EmergencyHistoryScreen(
                            profile = profile,
                            reportsState = reportsState,
                            localActivity = activityHistory,
                            onBackToMap = { currentRoute = ROUTE_HOME_MAP },
                            onUpdateLiveGps = viewModel::updateActiveReportGps,
                            onCancelFalseAlarm = { rep ->
                                viewModel.updateReportStatus(rep, ReportStatus.CANCELLED_FALSE_ALARM, "False alarm closed")
                            },
                            onMarkResolved = { rep ->
                                viewModel.updateReportStatus(rep, ReportStatus.RESOLVED, "Resolved")
                            }
                        )
                        ROUTE_SETTINGS -> SettingsScreen(
                            profile = profile,
                            preferences = preferences,
                            nigeriaDataset = nigeriaDataset,
                            spainDataset = spainDataset,
                            diagnosticResults = diagnosticResults,
                            onBackToMap = { currentRoute = ROUTE_HOME_MAP },
                            onLanguageChange = viewModel::setLanguage,
                            onUpdateArea = viewModel::updateProfileArea,
                            onSaveNigeriaContact = viewModel::saveNigeriaEmergencyContact,
                            onToggleTestMode = viewModel::setTestMode,
                            onRunDiagnostics = viewModel::runTestModeDiagnostics,
                            onTriggerTestAlert = { t ->
                                viewModel.submitSosReport(t, discreet = false, note = "TEST MODE Verification", forceTestAlert = true)
                            },
                            onSignOutOrReset = onSignOutOrReset
                        )
                        ROUTE_ABOUT -> AboutScreen(
                            onBackToMap = { currentRoute = ROUTE_HOME_MAP }
                        )
                    }
                }
            }
        }

        if (isWideScreen) {
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    containerColor = NavyDeepest,
                    contentColor = WhiteText,
                    modifier = Modifier
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 8.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.osas_ben_emblem),
                        contentDescription = strings.appName,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(Modifier.height(10.dp))
                    allNavItems.forEach { item ->
                        NavigationRailItem(
                            selected = currentRoute == item.route,
                            onClick = { currentRoute = item.route },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label, maxLines = 1) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = if (item.isEmergency) WhiteText else NavyDeepest,
                                selectedTextColor = if (item.isEmergency) EmergencyRed else SafetyYellow,
                                indicatorColor = if (item.isEmergency) EmergencyRed else SafetyYellow,
                                unselectedIconColor = SlateTextSecondary,
                                unselectedTextColor = SlateTextSecondary
                            ),
                            modifier = Modifier.testTag("rail_nav_${item.route}")
                        )
                    }
                }
                Box(modifier = Modifier.weight(1f)) {
                    mainContent(false)
                }
            }
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        drawerContainerColor = NavySurface,
                        drawerContentColor = WhiteText
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.osas_ben_emblem),
                                    contentDescription = strings.appName,
                                    modifier = Modifier.size(42.dp)
                                )
                                Column {
                                    Text(strings.appName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = WhiteText)
                                    Text(strings.tagline, style = MaterialTheme.typography.labelSmall, color = SafetyYellow)
                                }
                            }
                            HorizontalDivider(color = NavyBorder, modifier = Modifier.padding(vertical = 6.dp))
                            allNavItems.forEach { item ->
                                NavigationDrawerItem(
                                    selected = currentRoute == item.route,
                                    onClick = {
                                        currentRoute = item.route
                                        scope.launch { drawerState.close() }
                                    },
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    label = { Text(item.label, fontWeight = FontWeight.SemiBold) },
                                    colors = NavigationDrawerItemDefaults.colors(
                                        selectedContainerColor = if (item.isEmergency) EmergencyRed else SafetyYellow,
                                        selectedIconColor = if (item.isEmergency) WhiteText else NavyDeepest,
                                        selectedTextColor = if (item.isEmergency) WhiteText else NavyDeepest,
                                        unselectedContainerColor = NavySurfaceElevated.copy(alpha = 0.45f),
                                        unselectedIconColor = SlateTextSecondary,
                                        unselectedTextColor = WhiteText
                                    ),
                                    modifier = Modifier.testTag("drawer_nav_${item.route}")
                                )
                            }
                        }
                    }
                }
            ) {
                mainContent(true)
            }
        }
    }
}
