package com.example.ui

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.device.LocationService
import com.example.data.device.PhotoExifReader
import com.example.data.local.ActivityHistoryEntity
import com.example.data.local.AdministrativeLocationRepository
import com.example.data.local.DemoDeviceEntity
import com.example.data.local.DemoEmergencyEntity
import com.example.data.local.LocalAppPreferences
import com.example.data.local.OsasDao
import com.example.data.local.OsasLocalDatabase
import com.example.data.local.PhotoHistoryEntity
import com.example.data.local.PreferencesRepository
import com.example.data.model.DeviceStatus
import com.example.data.model.EmergencyReport
import com.example.data.model.EmergencyType
import com.example.data.model.LocationSnapshot
import com.example.data.model.NigeriaDataset
import com.example.data.model.PhotoMetadataResult
import com.example.data.model.RegisteredDevice
import com.example.data.model.ReportStatus
import com.example.data.model.SpainDataset
import com.example.data.model.SupportedCountry
import com.example.data.model.UserProfile
import com.example.data.remote.FirestoreRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface FeedUiState<out T> {
    data object Loading : FeedUiState<Nothing>
    data class Success<T>(val data: T) : FeedUiState<T>
    data class Error(val message: String) : FeedUiState<Nothing>
}

data class ClockCalendarState(
    val liveTime: String,
    val dayOfWeek: String,
    val dayOfMonth: String,
    val monthName: String,
    val year: String,
    val fullDateFormatted: String,
    val timezoneId: String
)

data class DiagnosticCheckResult(
    val label: String,
    val passed: Boolean,
    val detail: String
)

enum class MapAreaFilter {
    COUNTRY,
    REGION,
    LOCAL_AREA
}

@OptIn(ExperimentalCoroutinesApi::class)
class OsasViewModel(
    private val authenticatedUid: String?,
    private val authenticatedEmail: String?,
    private val adminRepo: AdministrativeLocationRepository,
    private val prefsRepo: PreferencesRepository,
    private val dao: OsasDao,
    private val firestoreRepo: FirestoreRepository,
    private val locationService: LocationService,
    private val photoExifReader: PhotoExifReader,
    private val deviceHardwareId: String
) : ViewModel() {

    val isOnlineAuth: Boolean = !authenticatedUid.isNullOrBlank()

    val preferences: StateFlow<LocalAppPreferences> = prefsRepo.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, LocalAppPreferences())

    private val _nigeriaDataset = MutableStateFlow<NigeriaDataset?>(null)
    val nigeriaDataset: StateFlow<NigeriaDataset?> = _nigeriaDataset.asStateFlow()

    private val _spainDataset = MutableStateFlow<SpainDataset?>(null)
    val spainDataset: StateFlow<SpainDataset?> = _spainDataset.asStateFlow()

    private val _currentGps = MutableStateFlow<LocationSnapshot?>(null)
    val currentGps: StateFlow<LocationSnapshot?> = _currentGps.asStateFlow()

    private val _hasLocationPermission = MutableStateFlow(locationService.hasLocationPermission())
    val hasLocationPermission: StateFlow<Boolean> = _hasLocationPermission.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    private val _lastInspectedPhoto = MutableStateFlow<PhotoMetadataResult?>(null)
    val lastInspectedPhoto: StateFlow<PhotoMetadataResult?> = _lastInspectedPhoto.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _diagnosticResults = MutableStateFlow<List<DiagnosticCheckResult>>(emptyList())
    val diagnosticResults: StateFlow<List<DiagnosticCheckResult>> = _diagnosticResults.asStateFlow()

    private val _mapAreaFilter = MutableStateFlow(MapAreaFilter.COUNTRY)
    val mapAreaFilter: StateFlow<MapAreaFilter> = _mapAreaFilter.asStateFlow()

    private val _mapTypeFilter = MutableStateFlow<EmergencyType?>(null)
    val mapTypeFilter: StateFlow<EmergencyType?> = _mapTypeFilter.asStateFlow()

    val clockCalendar: StateFlow<ClockCalendarState> = combine(
        flow {
            while (true) {
                emit(System.currentTimeMillis())
                delay(1000L)
            }
        },
        preferences.map { it.language }
    ) { nowMillis, lang ->
        buildClockState(nowMillis, lang)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        buildClockState(System.currentTimeMillis(), "en")
    )

    val onlineProfileState: StateFlow<FeedUiState<UserProfile?>> =
        if (!authenticatedUid.isNullOrBlank()) {
            firestoreRepo.observeUserProfile(authenticatedUid)
                .map<UserProfile?, FeedUiState<UserProfile?>> { FeedUiState.Success(it) }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    emit(FeedUiState.Error((fsErr ?: error).localizedMessage ?: "Failed to load profile"))
                }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), FeedUiState.Loading)
        } else {
            MutableStateFlow(FeedUiState.Success(null))
        }

    val activeProfile: StateFlow<UserProfile?> = combine(
        onlineProfileState,
        preferences
    ) { onlineState, prefs ->
        if (isOnlineAuth) {
            (onlineState as? FeedUiState.Success)?.data
        } else if (prefs.demoModeActive && prefs.firstName.isNotBlank()) {
            UserProfile(
                id = "demo_local_user",
                userId = "demo_local_user",
                firstName = prefs.firstName,
                email = prefs.email,
                country = prefs.country,
                region = prefs.region,
                province = prefs.province.ifBlank { null },
                localArea = prefs.localArea,
                language = prefs.language
            )
        } else {
            null
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val activeCountry: StateFlow<SupportedCountry> = combine(
        activeProfile,
        preferences
    ) { profile, prefs ->
        SupportedCountry.fromWire(profile?.country ?: prefs.country)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SupportedCountry.NIGERIA)

    val emergencyReportsState: StateFlow<FeedUiState<List<EmergencyReport>>> =
        combine(activeProfile, activeCountry) { profile, country ->
            profile to country
        }.flatMapLatest { (profile, country) ->
            when {
                isOnlineAuth && profile != null -> {
                    firestoreRepo.observeCountryEmergencyReports(country)
                        .map<List<EmergencyReport>, FeedUiState<List<EmergencyReport>>> { list ->
                            FeedUiState.Success(list.filter { it.countryEnum() == country })
                        }
                        .catch { error ->
                            val fsErr = generateSequence(error) { it.cause }
                                .filterIsInstance<FirebaseFirestoreException>()
                                .firstOrNull()
                            if (fsErr == null && error is CancellationException) throw error
                            emit(
                                FeedUiState.Error(
                                    (fsErr ?: error).localizedMessage ?: "Unable to stream emergency feed"
                                )
                            )
                        }
                }
                !isOnlineAuth && profile != null -> {
                    dao.observeDemoReportsByCountry(country.wireName)
                        .map<List<DemoEmergencyEntity>, FeedUiState<List<EmergencyReport>>> { entities ->
                            FeedUiState.Success(entities.map { it.toDomain() })
                        }
                }
                else -> flowOf(FeedUiState.Success(emptyList()))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), FeedUiState.Loading)

    val registeredDevicesState: StateFlow<FeedUiState<List<RegisteredDevice>>> =
        activeProfile.flatMapLatest { profile ->
            when {
                isOnlineAuth && !authenticatedUid.isNullOrBlank() && profile != null -> {
                    firestoreRepo.observeUserDevices(authenticatedUid)
                        .map<List<RegisteredDevice>, FeedUiState<List<RegisteredDevice>>> {
                            FeedUiState.Success(it)
                        }
                        .catch { error ->
                            val fsErr = generateSequence(error) { it.cause }
                                .filterIsInstance<FirebaseFirestoreException>()
                                .firstOrNull()
                            if (fsErr == null && error is CancellationException) throw error
                            emit(
                                FeedUiState.Error(
                                    (fsErr ?: error).localizedMessage ?: "Failed to observe devices"
                                )
                            )
                        }
                }
                !isOnlineAuth && profile != null -> {
                    dao.observeDemoDevices()
                        .map<List<DemoDeviceEntity>, FeedUiState<List<RegisteredDevice>>> { list ->
                            FeedUiState.Success(list.map { it.toDomain() })
                        }
                }
                else -> flowOf(FeedUiState.Success(emptyList()))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), FeedUiState.Loading)

    val photoHistory: StateFlow<List<PhotoHistoryEntity>> = dao.observePhotoHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val activityHistory: StateFlow<List<ActivityHistoryEntity>> = dao.observeActivityHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    init {
        viewModelScope.launch {
            _nigeriaDataset.value = adminRepo.getNigeriaDataset()
            _spainDataset.value = adminRepo.getSpainDataset()
        }
        refreshGpsLocation()
        startContinuousLocationIfPermitted()
    }

    fun onLocationPermissionResult(granted: Boolean) {
        _hasLocationPermission.value = granted || locationService.hasLocationPermission()
        if (_hasLocationPermission.value) {
            refreshGpsLocation()
            startContinuousLocationIfPermitted()
        }
    }

    fun refreshGpsLocation() {
        val permitted = locationService.hasLocationPermission()
        _hasLocationPermission.value = permitted
        if (!permitted) return
        viewModelScope.launch {
            val snap = locationService.getBestCurrentLocation()
            if (snap != null) {
                _currentGps.value = snap
            }
        }
    }

    private var locationJobStarted = false
    private fun startContinuousLocationIfPermitted() {
        if (locationJobStarted || !locationService.hasLocationPermission()) return
        locationJobStarted = true
        viewModelScope.launch {
            locationService.observeLocationUpdates(intervalMs = 12000L).collect { snap ->
                _currentGps.value = snap
            }
        }
    }

    fun clearBanner() {
        _statusBannerMessage.value = null
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch {
            prefsRepo.setLanguage(lang)
            val profile = activeProfile.value
            if (isOnlineAuth && profile != null) {
                firestoreRepo.saveUserProfile(profile.copy(language = lang))
            }
        }
    }

    fun setTestMode(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setTestMode(enabled)
            dao.insertActivity(
                ActivityHistoryEntity(
                    title = if (enabled) "TEST MODE Enabled" else "TEST MODE Disabled",
                    detail = if (enabled) "Alerts triggered now are marked [TEST]." else "Live operational mode restored.",
                    country = activeCountry.value.wireName,
                    testEntry = enabled
                )
            )
        }
    }

    fun setMapAreaFilter(filter: MapAreaFilter) {
        _mapAreaFilter.value = filter
    }

    fun setMapTypeFilter(type: EmergencyType?) {
        _mapTypeFilter.value = type
    }

    suspend fun detectCountryFromGps(): SupportedCountry? {
        val snap = locationService.getBestCurrentLocation()
        if (snap != null) {
            _currentGps.value = snap
            return snap.detectedCountry
        }
        return null
    }

    fun saveRegistrationProfile(
        firstName: String,
        email: String,
        country: SupportedCountry,
        region: String,
        province: String?,
        localArea: String,
        demoMode: Boolean
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val lang = preferences.value.language
            if (demoMode || !isOnlineAuth) {
                prefsRepo.saveRegistrationDraftOrDemo(
                    demoActive = true,
                    firstName = firstName,
                    email = email,
                    country = country.wireName,
                    region = region,
                    province = province,
                    localArea = localArea,
                    language = lang
                )
                dao.insertActivity(
                    ActivityHistoryEntity(
                        title = "Local DEMO Profile Saved",
                        detail = "${country.wireName} • $region • $localArea (Device-only)",
                        country = country.wireName,
                        testEntry = false
                    )
                )
                _statusBannerMessage.value = "Local DEMO profile active ($localArea, ${country.wireName})."
            } else {
                val uid = authenticatedUid ?: return@launch
                val resolvedEmail = authenticatedEmail?.takeIf { it.isNotBlank() } ?: email.trim()
                val profile = UserProfile(
                    userId = uid,
                    firstName = firstName.trim(),
                    email = resolvedEmail,
                    country = country.wireName,
                    region = region,
                    province = province?.takeIf { it.isNotBlank() },
                    localArea = localArea,
                    language = lang
                )
                val res = firestoreRepo.saveUserProfile(profile)
                res.onSuccess {
                    prefsRepo.saveRegistrationDraftOrDemo(
                        demoActive = false,
                        firstName = profile.firstName,
                        email = profile.email,
                        country = profile.country,
                        region = profile.region,
                        province = profile.province,
                        localArea = profile.localArea,
                        language = lang
                    )
                    dao.insertActivity(
                        ActivityHistoryEntity(
                            title = "Online Verified Profile Saved",
                            detail = "${country.wireName} • $region • $localArea",
                            country = country.wireName
                        )
                    )
                    _statusBannerMessage.value = "Profile saved to Cloud Firestore (${country.wireName})."
                }.onFailure { err ->
                    _statusBannerMessage.value = err.localizedMessage ?: "Failed to save profile"
                }
            }
            _isBusy.value = false
        }
    }

    fun savePendingEmailLinkDraft(
        firstName: String,
        email: String,
        country: SupportedCountry,
        region: String,
        province: String?,
        localArea: String
    ) {
        viewModelScope.launch {
            prefsRepo.savePendingEmailLinkDraft(
                firstName = firstName,
                email = email,
                country = country.wireName,
                region = region,
                province = province,
                localArea = localArea
            )
            dao.insertActivity(
                ActivityHistoryEntity(
                    title = "Email Verification Link Sent",
                    detail = "Verification link requested for ${email.trim()} (${country.wireName} • $localArea)",
                    country = country.wireName
                )
            )
        }
    }

    fun clearPendingEmailLinkDraft() {
        viewModelScope.launch {
            prefsRepo.clearPendingEmailLinkDraft()
        }
    }

    fun autoCompletePendingEmailLinkRegistration() {
        if (!isOnlineAuth) return
        val prefs = preferences.value
        val draftFirstName = prefs.pendingEmailLinkFirstName.trim()
        val draftLocalArea = prefs.pendingEmailLinkLocalArea.trim()
        val draftRegion = prefs.pendingEmailLinkRegion.trim()
        if (draftFirstName.isBlank() || draftRegion.isBlank() || draftLocalArea.isBlank()) return

        val resolvedEmail = authenticatedEmail?.takeIf { it.isNotBlank() } ?: prefs.pendingEmailLinkEmail.trim()
        val draftCountry = SupportedCountry.fromWire(prefs.pendingEmailLinkCountry)
        val draftProvince = prefs.pendingEmailLinkProvince.ifBlank { null }

        viewModelScope.launch {
            prefsRepo.clearPendingEmailLinkDraft()
            saveRegistrationProfile(
                firstName = draftFirstName,
                email = resolvedEmail,
                country = draftCountry,
                region = draftRegion,
                province = draftProvince,
                localArea = draftLocalArea,
                demoMode = false
            )
        }
    }

    fun updateProfileArea(
        country: SupportedCountry,
        region: String,
        province: String?,
        localArea: String
    ) {
        val current = activeProfile.value ?: return
        saveRegistrationProfile(
            firstName = current.firstName,
            email = current.email,
            country = country,
            region = region,
            province = province,
            localArea = localArea,
            demoMode = !isOnlineAuth
        )
    }

    fun submitSosReport(
        type: EmergencyType,
        discreet: Boolean,
        note: String?,
        forceTestAlert: Boolean = false,
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val profile = activeProfile.value
        if (profile == null) {
            onComplete(false, "Please complete registration first.")
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val isTest = forceTestAlert || preferences.value.testModeEnabled
            val country = SupportedCountry.fromWire(profile.country)

            val gps = if (locationService.hasLocationPermission()) {
                locationService.getBestCurrentLocation()?.also { _currentGps.value = it }
            } else null

            val areaFallbackCoords = if (gps == null) {
                adminRepo.findCoordinatesForArea(country, profile.region, profile.province, profile.localArea)
            } else null

            val lat = gps?.latitude ?: areaFallbackCoords?.first ?: country.defaultLat
            val lon = gps?.longitude ?: areaFallbackCoords?.second ?: country.defaultLon
            val gpsSourceLabel = if (gps != null) "GPS" else "Selected Area Centroid (GPS unavailable)"

            val now = Date()
            val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(now)
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now)

            val combinedNote = buildString {
                if (isTest) append("[TEST] ")
                if (!note.isNullOrBlank()) append(note.trim())
                if (gps == null) {
                    if (isNotEmpty()) append(" • ")
                    append("GPS unavailable at trigger; area center used")
                }
            }.take(300).ifBlank { null }

            val report = EmergencyReport(
                userId = profile.userId,
                emergencyType = type.name,
                status = ReportStatus.ACTIVE.name,
                country = country.wireName,
                region = profile.region,
                province = profile.province,
                localArea = profile.localArea,
                latitude = lat,
                longitude = lon,
                reportedTime = timeStr,
                reportedDate = dateStr,
                testAlert = isTest,
                discreetMode = discreet,
                note = combinedNote,
                localDemoOnly = !isOnlineAuth
            )

            if (isOnlineAuth) {
                val result = firestoreRepo.submitEmergencyReport(report)
                result.onSuccess { reportId ->
                    dao.insertActivity(
                        ActivityHistoryEntity(
                            title = "${if (isTest) "[TEST] " else ""}${type.name} SOS Sent",
                            detail = "${profile.localArea}, ${country.wireName} ($gpsSourceLabel) ID: $reportId",
                            country = country.wireName,
                            testEntry = isTest
                        )
                    )
                    val msg = if (isTest) {
                        "[TEST] ${type.name} alert recorded at $timeStr ($gpsSourceLabel)"
                    } else {
                        "${type.name} SOS sent to ${country.wireName} feed ($gpsSourceLabel)"
                    }
                    _statusBannerMessage.value = msg
                    onComplete(true, msg)
                }.onFailure { err ->
                    val msg = err.localizedMessage ?: "Failed to submit SOS report"
                    _statusBannerMessage.value = msg
                    onComplete(false, msg)
                }
            } else {
                val localId = "demo_${UUID.randomUUID().toString().take(12)}"
                dao.upsertDemoReport(
                    DemoEmergencyEntity(
                        id = localId,
                        userId = profile.userId,
                        emergencyType = report.emergencyType,
                        status = report.status,
                        country = report.country,
                        region = report.region,
                        province = report.province,
                        localArea = report.localArea,
                        latitude = report.latitude,
                        longitude = report.longitude,
                        reportedTime = report.reportedTime,
                        reportedDate = report.reportedDate,
                        testAlert = report.testAlert,
                        discreetMode = report.discreetMode,
                        note = report.note
                    )
                )
                dao.insertActivity(
                    ActivityHistoryEntity(
                        title = "${if (isTest) "[TEST] " else ""}[DEMO] ${type.name} SOS",
                        detail = "${profile.localArea}, ${country.wireName} ($gpsSourceLabel)",
                        country = country.wireName,
                        testEntry = isTest
                    )
                )
                val msg = if (isTest) {
                    "[TEST][DEMO] ${type.name} saved locally at $timeStr"
                } else {
                    "DEMO MODE: ${type.name} saved on this phone only ($gpsSourceLabel)"
                }
                _statusBannerMessage.value = msg
                onComplete(true, msg)
            }
            _isBusy.value = false
        }
    }

    fun updateActiveReportGps(report: EmergencyReport) {
        viewModelScope.launch {
            if (!locationService.hasLocationPermission()) {
                _statusBannerMessage.value = "Location permission is required to update live GPS."
                return@launch
            }
            val gps = locationService.getBestCurrentLocation()
            if (gps == null) {
                _statusBannerMessage.value = "GPS signal currently unavailable."
                return@launch
            }
            _currentGps.value = gps
            if (report.localDemoOnly || !isOnlineAuth) {
                dao.updateDemoReportLocation(report.id, gps.latitude, gps.longitude)
                _statusBannerMessage.value = "Updated local report GPS to ${"%.5f".format(gps.latitude)}, ${"%.5f".format(gps.longitude)}"
            } else {
                firestoreRepo.updateEmergencyLocation(report.id, gps.latitude, gps.longitude)
                    .onSuccess {
                        _statusBannerMessage.value = "Live GPS updated on Cloud Firestore."
                    }
                    .onFailure { err ->
                        _statusBannerMessage.value = err.localizedMessage ?: "Failed to update live GPS"
                    }
            }
        }
    }

    fun updateReportStatus(report: EmergencyReport, newStatus: ReportStatus, note: String?) {
        viewModelScope.launch {
            if (report.localDemoOnly || !isOnlineAuth) {
                dao.updateDemoReportStatus(report.id, newStatus.name, note)
                dao.insertActivity(
                    ActivityHistoryEntity(
                        title = "Demo Report ${newStatus.name}",
                        detail = "${report.emergencyType} in ${report.localArea}",
                        country = report.country,
                        testEntry = report.testAlert
                    )
                )
                _statusBannerMessage.value = "Alert updated to ${newStatus.name}."
            } else {
                firestoreRepo.updateEmergencyStatus(report.id, newStatus, note)
                    .onSuccess {
                        dao.insertActivity(
                            ActivityHistoryEntity(
                                title = "Report ${newStatus.name}",
                                detail = "${report.emergencyType} in ${report.localArea}",
                                country = report.country,
                                testEntry = report.testAlert
                            )
                        )
                        _statusBannerMessage.value = "Alert updated to ${newStatus.name}."
                    }
                    .onFailure { err ->
                        _statusBannerMessage.value = err.localizedMessage ?: "Failed to update alert status"
                    }
            }
        }
    }

    fun inspectPhoto(uri: Uri) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = photoExifReader.inspectPhoto(uri)
            _lastInspectedPhoto.value = result
            dao.insertPhotoHistory(
                PhotoHistoryEntity(
                    uriString = result.uriString,
                    displayName = result.displayName,
                    dateTaken = result.dateTaken,
                    timeTaken = result.timeTaken,
                    latitude = result.latitude,
                    longitude = result.longitude,
                    city = result.city,
                    country = result.country
                )
            )
            dao.insertActivity(
                ActivityHistoryEntity(
                    title = "Photo EXIF Inspected",
                    detail = "${result.displayName} • ${if (result.hasGps) "${result.city}, ${result.country}" else "LOCATION NOT AVAILABLE"}",
                    country = activeCountry.value.wireName
                )
            )
            _isBusy.value = false
        }
    }

    fun registerOrUpdateCurrentPhone(customDeviceName: String, markOffline: Boolean = false) {
        val profile = activeProfile.value ?: return
        viewModelScope.launch {
            _isBusy.value = true
            val gps = if (!markOffline && locationService.hasLocationPermission()) {
                locationService.getBestCurrentLocation()?.also { _currentGps.value = it }
            } else null

            val country = SupportedCountry.fromWire(profile.country)
            val fallbackCoords = if (gps == null) {
                adminRepo.findCoordinatesForArea(country, profile.region, profile.province, profile.localArea)
            } else null

            val lat = gps?.latitude ?: fallbackCoords?.first ?: country.defaultLat
            val lon = gps?.longitude ?: fallbackCoords?.second ?: country.defaultLon
            val nowText = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val modelName = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
            val safeName = customDeviceName.trim().ifBlank { modelName }
            val status = if (markOffline || gps == null) DeviceStatus.OFFLINE_LAST_KNOWN else DeviceStatus.ACTIVE

            val device = RegisteredDevice(
                id = deviceHardwareId,
                userId = profile.userId,
                deviceName = safeName,
                deviceModel = modelName,
                country = country.wireName,
                latitude = lat,
                longitude = lon,
                status = status.name,
                lastLocationUpdateText = nowText
            )

            if (isOnlineAuth) {
                firestoreRepo.upsertDevice(deviceHardwareId, device)
                    .onSuccess {
                        dao.insertActivity(
                            ActivityHistoryEntity(
                                title = "Phone Tracker Updated",
                                detail = "$safeName • ${status.name}",
                                country = country.wireName
                            )
                        )
                        _statusBannerMessage.value = "Device location recorded ($nowText)."
                    }
                    .onFailure { err ->
                        _statusBannerMessage.value = err.localizedMessage ?: "Failed to update device"
                    }
            } else {
                dao.upsertDemoDevice(
                    DemoDeviceEntity(
                        id = deviceHardwareId,
                        userId = profile.userId,
                        deviceName = device.deviceName,
                        deviceModel = device.deviceModel,
                        country = device.country,
                        latitude = device.latitude,
                        longitude = device.longitude,
                        status = device.status,
                        lastLocationUpdateText = device.lastLocationUpdateText
                    )
                )
                _statusBannerMessage.value = "Local device location recorded ($nowText)."
            }
            _isBusy.value = false
        }
    }

    fun setDeviceOffline(device: RegisteredDevice) {
        viewModelScope.launch {
            val nowText = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val updated = device.copy(
                status = DeviceStatus.OFFLINE_LAST_KNOWN.name,
                lastLocationUpdateText = nowText
            )
            if (isOnlineAuth) {
                firestoreRepo.upsertDevice(device.id, updated)
            } else {
                dao.upsertDemoDevice(
                    DemoDeviceEntity(
                        id = device.id,
                        userId = device.userId,
                        deviceName = updated.deviceName,
                        deviceModel = updated.deviceModel,
                        country = updated.country,
                        latitude = updated.latitude,
                        longitude = updated.longitude,
                        status = updated.status,
                        lastLocationUpdateText = updated.lastLocationUpdateText
                    )
                )
            }
            _statusBannerMessage.value = "${device.deviceName} set to OFFLINE (showing last known location)."
        }
    }

    fun deleteRegisteredDevice(deviceId: String) {
        viewModelScope.launch {
            if (isOnlineAuth) {
                firestoreRepo.deleteDevice(deviceId)
            } else {
                dao.deleteDemoDevice(deviceId)
            }
        }
    }

    fun saveNigeriaEmergencyContact(label: String, number: String) {
        viewModelScope.launch {
            prefsRepo.saveNigeriaEmergencyContact(label, number)
            _statusBannerMessage.value = "Nigeria emergency contact saved locally."
        }
    }

    fun resetDemoSession() {
        viewModelScope.launch {
            prefsRepo.clearSessionMode()
        }
    }

    fun runTestModeDiagnostics() {
        viewModelScope.launch {
            _isBusy.value = true
            val checks = mutableListOf<DiagnosticCheckResult>()

            val ng = adminRepo.getNigeriaDataset()
            checks += DiagnosticCheckResult(
                label = "Nigeria Location Dataset",
                passed = ng.statesCount == 37 && ng.lgasCount == 774,
                detail = "${ng.statesCount} states/FCT, ${ng.lgasCount} LGAs loaded"
            )

            val es = adminRepo.getSpainDataset()
            checks += DiagnosticCheckResult(
                label = "Spain INE Location Dataset",
                passed = es.communitiesCount == 19 && es.provincesCount == 52 && es.municipalitiesCount == 8132,
                detail = "${es.communitiesCount} communities/cities, ${es.provincesCount} provinces, ${es.municipalitiesCount} municipalities"
            )

            val perm = locationService.hasLocationPermission()
            val gps = if (perm) locationService.getBestCurrentLocation() else null
            checks += DiagnosticCheckResult(
                label = "GPS Permission & Fix",
                passed = perm,
                detail = if (!perm) "Location permission not granted yet" else if (gps != null) "Fix: ${"%.4f".format(gps.latitude)}, ${"%.4f".format(gps.longitude)}" else "Permission granted; waiting for GPS fix"
            )

            val country = activeCountry.value
            val feedList = (emergencyReportsState.value as? FeedUiState.Success)?.data ?: emptyList()
            val mixed = feedList.any { it.countryEnum() != country }
            checks += DiagnosticCheckResult(
                label = "Country Feed Isolation (${country.wireName})",
                passed = !mixed,
                detail = if (!mixed) "0 cross-country leaks (${feedList.size} reports in ${country.wireName})" else "Country mix detected!"
            )

            checks += DiagnosticCheckResult(
                label = "Local Room + DataStore Storage",
                passed = true,
                detail = "${photoHistory.value.size} photo records, ${activityHistory.value.size} local activity entries"
            )

            if (isOnlineAuth) {
                val ping = firestoreRepo.pingConnection()
                checks += DiagnosticCheckResult(
                    label = "Firebase Firestore Connection",
                    passed = ping.isSuccess,
                    detail = ping.getOrElse { it.localizedMessage ?: "Firestore unreachable" }
                )
            } else {
                checks += DiagnosticCheckResult(
                    label = "Firebase Firestore Connection",
                    passed = true,
                    detail = "Running in Local DEMO Mode (offline; sign in with Google to test cloud Firestore)"
                )
            }

            _diagnosticResults.value = checks
            _isBusy.value = false
        }
    }

    private fun buildClockState(millis: Long, lang: String): ClockCalendarState {
        val locale = if (lang == "es") Locale("es", "ES") else Locale.ENGLISH
        val date = Date(millis)
        val timeFmt = SimpleDateFormat("HH:mm:ss", locale)
        val dayFmt = SimpleDateFormat("EEEE", locale)
        val dayNumFmt = SimpleDateFormat("dd", locale)
        val monthFmt = SimpleDateFormat("MMMM", locale)
        val yearFmt = SimpleDateFormat("yyyy", locale)
        val fullFmt = SimpleDateFormat("EEE, dd MMM yyyy", locale)
        return ClockCalendarState(
            liveTime = timeFmt.format(date),
            dayOfWeek = dayFmt.format(date).replaceFirstChar { it.uppercase(locale) },
            dayOfMonth = dayNumFmt.format(date),
            monthName = monthFmt.format(date).replaceFirstChar { it.uppercase(locale) },
            year = yearFmt.format(date),
            fullDateFormatted = fullFmt.format(date),
            timezoneId = TimeZone.getDefault().id
        )
    }

    companion object {
        fun factory(
            context: Context,
            authenticatedUid: String?,
            authenticatedEmail: String?
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = context.applicationContext
                val locationService = LocationService(app)
                val androidId = Settings.Secure.getString(app.contentResolver, Settings.Secure.ANDROID_ID)
                    ?.replace(Regex("[^a-zA-Z0-9_-]"), "")
                    ?.takeIf { it.isNotBlank() }
                    ?: "android_device"
                return OsasViewModel(
                    authenticatedUid = authenticatedUid,
                    authenticatedEmail = authenticatedEmail,
                    adminRepo = AdministrativeLocationRepository(app),
                    prefsRepo = PreferencesRepository(app),
                    dao = OsasLocalDatabase.getInstance(app).osasDao(),
                    firestoreRepo = FirestoreRepository(app),
                    locationService = locationService,
                    photoExifReader = PhotoExifReader(app, locationService),
                    deviceHardwareId = "dev_$androidId"
                ) as T
            }
        }
    }
}
