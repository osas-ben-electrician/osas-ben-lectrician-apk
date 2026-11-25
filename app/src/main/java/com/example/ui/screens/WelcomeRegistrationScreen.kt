package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.auth.EmailLinkAuthHelper
import com.example.auth.GoogleAuthHelper
import com.example.data.local.LocalAppPreferences
import com.example.data.model.NigeriaDataset
import com.example.data.model.SpainDataset
import com.example.data.model.SupportedCountry
import com.example.i18n.LocalAppStrings
import com.example.ui.components.CountryAndAreaPicker
import com.example.ui.theme.DemoAmber
import com.example.ui.theme.DemoAmberContainer
import com.example.ui.theme.EmergencyRedBright
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
import kotlinx.coroutines.launch

@Composable
fun WelcomeRegistrationScreen(
    isAuthenticatedOnline: Boolean,
    authenticatedEmail: String?,
    authenticatedDisplayName: String?,
    preferences: LocalAppPreferences,
    incomingEmailLink: String?,
    currentLanguage: String,
    nigeriaDataset: NigeriaDataset?,
    spainDataset: SpainDataset?,
    hasLocationPermission: Boolean,
    isBusy: Boolean,
    onLanguageChange: (String) -> Unit,
    onLocationPermissionGranted: () -> Unit,
    onDetectCountryGps: suspend () -> SupportedCountry?,
    onConsumeIncomingEmailLink: () -> Unit,
    onEmailLinkDraftSaved: (
        firstName: String,
        email: String,
        country: SupportedCountry,
        region: String,
        province: String?,
        localArea: String
    ) -> Unit,
    onClearEmailLinkDraft: () -> Unit,
    onFirebaseAuthCompleted: () -> Unit,
    onSaveProfile: (
        firstName: String,
        email: String,
        country: SupportedCountry,
        region: String,
        province: String?,
        localArea: String,
        demoMode: Boolean
    ) -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val initialFirstName = preferences.pendingEmailLinkFirstName.takeIf { it.isNotBlank() }
        ?: authenticatedDisplayName?.substringBefore(" ")?.takeIf { it.isNotBlank() }
        ?: ""
    val initialEmail = authenticatedEmail?.takeIf { it.isNotBlank() }
        ?: preferences.pendingEmailLinkEmail.takeIf { it.isNotBlank() }
        ?: ""

    var firstName by remember(initialFirstName) { mutableStateOf(initialFirstName) }
    var email by remember(initialEmail) { mutableStateOf(initialEmail) }
    var confirmEmail by remember(initialEmail) { mutableStateOf(initialEmail) }
    var country by remember(preferences.pendingEmailLinkCountry) {
        mutableStateOf(
            if (preferences.pendingEmailLinkEmail.isNotBlank()) {
                SupportedCountry.fromWire(preferences.pendingEmailLinkCountry)
            } else {
                SupportedCountry.NIGERIA
            }
        )
    }
    var region by remember(preferences.pendingEmailLinkRegion) {
        mutableStateOf(preferences.pendingEmailLinkRegion)
    }
    var province by remember(preferences.pendingEmailLinkProvince) {
        mutableStateOf(preferences.pendingEmailLinkProvince)
    }
    var localArea by remember(preferences.pendingEmailLinkLocalArea) {
        mutableStateOf(preferences.pendingEmailLinkLocalArea)
    }

    var validationError by remember { mutableStateOf<String?>(null) }
    var statusNotice by remember { mutableStateOf<String?>(null) }
    var pastedVerificationLink by remember { mutableStateOf("") }
    var authLoading by remember { mutableStateOf(false) }

    val hasPendingLink = preferences.pendingEmailLinkEmail.isNotBlank() && !isAuthenticatedOnline

    // Handle deep-linked Firebase email verification URL returning to the registration screen
    LaunchedEffect(incomingEmailLink) {
        val link = incomingEmailLink ?: return@LaunchedEffect
        if (!EmailLinkAuthHelper.isIncomingEmailLink(link)) {
            onConsumeIncomingEmailLink()
            return@LaunchedEffect
        }
        val targetEmail = preferences.pendingEmailLinkEmail.ifBlank { email.trim() }
        if (targetEmail.isBlank()) {
            pastedVerificationLink = link
            validationError = "Please enter the email address that requested the link and tap Verify Link."
            onConsumeIncomingEmailLink()
            return@LaunchedEffect
        }
        authLoading = true
        validationError = null
        val res = EmailLinkAuthHelper.completeSignInWithEmailLink(targetEmail, link)
        authLoading = false
        onConsumeIncomingEmailLink()
        res.onSuccess {
            onFirebaseAuthCompleted()
            if (firstName.isNotBlank() && region.isNotBlank() && localArea.isNotBlank()) {
                onClearEmailLinkDraft()
                onSaveProfile(
                    firstName,
                    targetEmail,
                    country,
                    region,
                    province.ifBlank { null },
                    localArea,
                    false
                )
            }
        }.onFailure { err ->
            pastedVerificationLink = link
            validationError = err.localizedMessage ?: "Email-link verification failed."
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms.values.any { it }
        if (granted) {
            onLocationPermissionGranted()
            scope.launch {
                val detected = onDetectCountryGps()
                if (detected != null) {
                    if (detected != country) {
                        country = detected
                        region = ""
                        province = ""
                        localArea = ""
                    }
                } else {
                    validationError = "GPS outside Spain/Nigeria or unavailable — please choose manually."
                }
            }
        }
    }

    fun validateInputs(): Boolean {
        val cleanFirst = firstName.trim()
        val cleanEmail = email.trim()
        val cleanConfirm = confirmEmail.trim()
        if (cleanFirst.isEmpty() || cleanEmail.length < 3 || !cleanEmail.contains("@") || region.isBlank() || localArea.isBlank()) {
            validationError = strings.requiredFieldsError
            return false
        }
        if (!cleanEmail.equals(cleanConfirm, ignoreCase = true)) {
            validationError = strings.emailMismatchError
            return false
        }
        validationError = null
        return true
    }

    fun sendEmailLinkVerification() {
        if (!validateInputs()) return
        val cleanFirst = firstName.trim()
        val cleanEmail = email.trim()
        authLoading = true
        validationError = null
        statusNotice = null
        scope.launch {
            val res = EmailLinkAuthHelper.sendVerificationLink(context, cleanEmail)
            authLoading = false
            res.onSuccess {
                onEmailLinkDraftSaved(
                    cleanFirst,
                    cleanEmail,
                    country,
                    region,
                    province.ifBlank { null },
                    localArea
                )
                statusNotice = "${strings.emailLinkSentTitle}: $cleanEmail"
            }.onFailure { err ->
                validationError = err.localizedMessage ?: "Failed to send verification link."
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Language quick switch at top right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Language, contentDescription = null, tint = SafetyYellow, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                TextButton(
                    onClick = { onLanguageChange("en") },
                    modifier = Modifier.testTag("welcome_lang_en")
                ) {
                    Text(
                        "EN",
                        color = if (currentLanguage == "en") SafetyYellow else SlateTextSecondary,
                        fontWeight = if (currentLanguage == "en") FontWeight.ExtraBold else FontWeight.Normal
                    )
                }
                Text("/", color = SlateTextSecondary)
                TextButton(
                    onClick = { onLanguageChange("es") },
                    modifier = Modifier.testTag("welcome_lang_es")
                ) {
                    Text(
                        "ES",
                        color = if (currentLanguage == "es") SafetyYellow else SlateTextSecondary,
                        fontWeight = if (currentLanguage == "es") FontWeight.ExtraBold else FontWeight.Normal
                    )
                }
            }

            // Brand Logo Hero
            Image(
                painter = painterResource(id = R.drawable.osas_ben_logo),
                contentDescription = strings.appName,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(175.dp)
                    .clip(RoundedCornerShape(18.dp)),
                contentScale = ContentScale.Fit
            )

            Text(
                text = strings.welcomeTitle,
                style = MaterialTheme.typography.headlineMedium,
                color = WhiteText,
                textAlign = TextAlign.Center
            )
            Text(
                text = strings.welcomeSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = SlateTextSecondary,
                textAlign = TextAlign.Center
            )

            if (isAuthenticatedOnline) {
                Surface(
                    color = OnlineEmeraldContainer,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, OnlineEmerald),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = OnlineEmerald)
                        Text(
                            text = "${strings.onlineVerifiedMode}: ${authenticatedEmail ?: ""}",
                            style = MaterialTheme.typography.labelLarge,
                            color = WhiteText
                        )
                    }
                }
            }

            // Main Registration Form Card
            Card(
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, NavyBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = { Text(strings.firstNameLabel) },
                        singleLine = true,
                        colors = registrationTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reg_first_name")
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text(strings.emailLabel) },
                        singleLine = true,
                        colors = registrationTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reg_email")
                    )

                    OutlinedTextField(
                        value = confirmEmail,
                        onValueChange = { confirmEmail = it },
                        label = { Text(strings.confirmEmailLabel) },
                        singleLine = true,
                        colors = registrationTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reg_confirm_email")
                    )

                    CountryAndAreaPicker(
                        country = country,
                        region = region,
                        province = province,
                        localArea = localArea,
                        nigeriaDataset = nigeriaDataset,
                        spainDataset = spainDataset,
                        onCountryChanged = { newCountry ->
                            if (newCountry != country) {
                                country = newCountry
                                region = ""
                                province = ""
                                localArea = ""
                            }
                        },
                        onRegionChanged = { newReg ->
                            region = newReg
                            province = ""
                            localArea = ""
                        },
                        onProvinceChanged = { newProv ->
                            province = newProv
                            localArea = ""
                        },
                        onLocalAreaChanged = { newLocal ->
                            localArea = newLocal
                        },
                        onAutoDetectClick = {
                            if (hasLocationPermission) {
                                scope.launch {
                                    val detected = onDetectCountryGps()
                                    if (detected != null && detected != country) {
                                        country = detected
                                        region = ""
                                        province = ""
                                        localArea = ""
                                    } else if (detected == null) {
                                        validationError = "GPS outside Spain/Nigeria or unavailable — select country manually."
                                    }
                                }
                            } else {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        }
                    )

                    if (validationError != null) {
                        Surface(
                            color = EmergencyRedBright.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, EmergencyRedBright),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = validationError!!,
                                color = WhiteText,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    if (statusNotice != null) {
                        Surface(
                            color = OnlineEmeraldContainer,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, OnlineEmerald),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = statusNotice!!,
                                color = WhiteText,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    // Pending Firebase Email-Link Verification Card
                    if (hasPendingLink) {
                        Surface(
                            color = NavySurfaceElevated,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.5.dp, SafetyYellow),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("email_link_sent_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.MarkEmailRead, contentDescription = null, tint = SafetyYellow)
                                    Text(
                                        text = "${strings.emailLinkSentTitle} (${preferences.pendingEmailLinkEmail})",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = SafetyYellow,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = strings.emailLinkSentDesc,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SlateTextSecondary
                                )

                                OutlinedTextField(
                                    value = pastedVerificationLink,
                                    onValueChange = { pastedVerificationLink = it },
                                    label = { Text(strings.pasteEmailLinkLabel) },
                                    singleLine = true,
                                    colors = registrationTextFieldColors(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("reg_paste_email_link_input")
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            val targetEmail = preferences.pendingEmailLinkEmail.ifBlank { email.trim() }
                                            authLoading = true
                                            validationError = null
                                            scope.launch {
                                                val res = EmailLinkAuthHelper.completeSignInWithEmailLink(
                                                    email = targetEmail,
                                                    emailLink = pastedVerificationLink
                                                )
                                                authLoading = false
                                                res.onSuccess {
                                                    onClearEmailLinkDraft()
                                                    onFirebaseAuthCompleted()
                                                    if (validateInputs()) {
                                                        onSaveProfile(
                                                            firstName,
                                                            targetEmail,
                                                            country,
                                                            region,
                                                            province.ifBlank { null },
                                                            localArea,
                                                            false
                                                        )
                                                    }
                                                }.onFailure { err ->
                                                    validationError = err.localizedMessage ?: "Verification failed."
                                                }
                                            }
                                        },
                                        enabled = !authLoading && pastedVerificationLink.isNotBlank(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = OnlineEmerald,
                                            contentColor = NavyDeepest
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .minimumInteractiveComponentSize()
                                            .testTag("reg_verify_pasted_link_btn")
                                    ) {
                                        Text(strings.verifyEmailLinkButton, fontWeight = FontWeight.ExtraBold)
                                    }
                                    OutlinedButton(
                                        onClick = { sendEmailLinkVerification() },
                                        enabled = !authLoading,
                                        border = BorderStroke(1.dp, SafetyYellow),
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("reg_resend_email_link_btn")
                                    ) {
                                        Text(strings.resendEmailLinkButton, color = SafetyYellow)
                                    }
                                }
                                TextButton(
                                    onClick = {
                                        onClearEmailLinkDraft()
                                        statusNotice = null
                                    },
                                    modifier = Modifier.testTag("reg_edit_email_link_btn")
                                ) {
                                    Text(strings.clearPendingLinkButton, color = SlateTextSecondary)
                                }
                            }
                        }
                    }

                    // Action 1: Firebase Email-Link Verification (Passwordless)
                    if (!isAuthenticatedOnline) {
                        Button(
                            onClick = { sendEmailLinkVerification() },
                            enabled = !authLoading && !isBusy,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SafetyYellow,
                                contentColor = NavyDeepest
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("reg_email_link_btn")
                        ) {
                            if (authLoading || isBusy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = NavyDeepest,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = strings.sendEmailLinkButton,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    // Action 2: Online Verified Mode (Google Sign-In via CredentialManager -> Cloud Firestore)
                    Button(
                        onClick = {
                            if (isAuthenticatedOnline) {
                                if (validateInputs()) {
                                    onSaveProfile(
                                        firstName,
                                        email,
                                        country,
                                        region,
                                        province.ifBlank { null },
                                        localArea,
                                        false
                                    )
                                }
                            } else {
                                authLoading = true
                                validationError = null
                                GoogleAuthHelper.onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = {
                                        authLoading = false
                                        onFirebaseAuthCompleted()
                                        if (validateInputs()) {
                                            onSaveProfile(
                                                firstName,
                                                email,
                                                country,
                                                region,
                                                province.ifBlank { null },
                                                localArea,
                                                false
                                            )
                                        }
                                    },
                                    onAuthError = { msg ->
                                        authLoading = false
                                        validationError = msg
                                    },
                                    scope = scope,
                                    onAuthCancelled = {
                                        authLoading = false
                                        validationError = "Google Sign-In cancelled. Tap again or use Email-Link / Local DEMO Mode."
                                    }
                                )
                            }
                        },
                        enabled = !authLoading && !isBusy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAuthenticatedOnline) SafetyYellow else NavySurfaceElevated,
                            contentColor = if (isAuthenticatedOnline) NavyDeepest else WhiteText
                        ),
                        border = if (isAuthenticatedOnline) null else BorderStroke(1.dp, SafetyYellow),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("reg_google_signin_btn")
                    ) {
                        if (authLoading || isBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = if (isAuthenticatedOnline) NavyDeepest else SafetyYellow,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (isAuthenticatedOnline) "Save Profile & Enter Online Map" else strings.signInGoogleButton,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    // Action 3: Clearly Labelled Local DEMO Mode (Device-only, never pretends to be online)
                    if (!isAuthenticatedOnline) {
                        OutlinedButton(
                            onClick = {
                                if (validateInputs()) {
                                    onSaveProfile(
                                        firstName,
                                        email,
                                        country,
                                        region,
                                        province.ifBlank { null },
                                        localArea,
                                        true
                                    )
                                }
                            },
                            enabled = !authLoading && !isBusy,
                            border = BorderStroke(1.5.dp, DemoAmber),
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("reg_demo_mode_btn")
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = DemoAmber, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = strings.continueDemoButton,
                                color = DemoAmber,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Honest Disclosures
            Surface(
                color = DemoAmberContainer,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, DemoAmber.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = SafetyYellow, modifier = Modifier.size(20.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(strings.registrationModeNote, style = MaterialTheme.typography.bodyMedium, color = WhiteText)
                        Text(strings.emailLinkNote, style = MaterialTheme.typography.labelSmall, color = SlateTextSecondary)
                    }
                }
            }

            Surface(
                color = NavySurfaceElevated,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, NavyBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = SafetyYellow, modifier = Modifier.size(20.dp))
                    Text(strings.localStorageNotice, style = MaterialTheme.typography.bodyMedium, color = SlateTextSecondary)
                }
            }
        }
    }
}

@Composable
private fun registrationTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = SafetyYellow,
    unfocusedBorderColor = NavyBorder,
    focusedLabelColor = SafetyYellow,
    unfocusedLabelColor = SlateTextSecondary,
    focusedTextColor = WhiteText,
    unfocusedTextColor = WhiteText,
    cursorColor = SafetyYellow
)
