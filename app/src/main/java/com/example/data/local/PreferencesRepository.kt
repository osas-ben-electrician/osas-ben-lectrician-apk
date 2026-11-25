package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.SupportedCountry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "osas_ben_prefs")

data class LocalAppPreferences(
    val language: String = "en",
    val testModeEnabled: Boolean = false,
    val demoModeActive: Boolean = false,
    val firstName: String = "",
    val email: String = "",
    val country: String = SupportedCountry.NIGERIA.wireName,
    val region: String = "",
    val province: String = "",
    val localArea: String = "",
    val nigeriaEmergencyLabel: String = "Nigeria National Emergency",
    val nigeriaEmergencyNumber: String = "112",
    val pendingEmailLinkEmail: String = "",
    val pendingEmailLinkFirstName: String = "",
    val pendingEmailLinkCountry: String = SupportedCountry.NIGERIA.wireName,
    val pendingEmailLinkRegion: String = "",
    val pendingEmailLinkProvince: String = "",
    val pendingEmailLinkLocalArea: String = "",
    val pendingEmailLinkSentAtMillis: Long = 0L
)

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val TEST_MODE = booleanPreferencesKey("test_mode")
        val DEMO_ACTIVE = booleanPreferencesKey("demo_active")
        val FIRST_NAME = stringPreferencesKey("first_name")
        val EMAIL = stringPreferencesKey("email")
        val COUNTRY = stringPreferencesKey("country")
        val REGION = stringPreferencesKey("region")
        val PROVINCE = stringPreferencesKey("province")
        val LOCAL_AREA = stringPreferencesKey("local_area")
        val NG_EMERGENCY_LABEL = stringPreferencesKey("ng_emergency_label")
        val NG_EMERGENCY_NUMBER = stringPreferencesKey("ng_emergency_number")
        val PENDING_EMAIL = stringPreferencesKey("pending_email_link_email")
        val PENDING_FIRST_NAME = stringPreferencesKey("pending_email_link_first_name")
        val PENDING_COUNTRY = stringPreferencesKey("pending_email_link_country")
        val PENDING_REGION = stringPreferencesKey("pending_email_link_region")
        val PENDING_PROVINCE = stringPreferencesKey("pending_email_link_province")
        val PENDING_LOCAL_AREA = stringPreferencesKey("pending_email_link_local_area")
        val PENDING_SENT_AT = longPreferencesKey("pending_email_link_sent_at")
    }

    val preferencesFlow: Flow<LocalAppPreferences> = context.dataStore.data.map { prefs ->
        LocalAppPreferences(
            language = prefs[Keys.LANGUAGE] ?: "en",
            testModeEnabled = prefs[Keys.TEST_MODE] ?: false,
            demoModeActive = prefs[Keys.DEMO_ACTIVE] ?: false,
            firstName = prefs[Keys.FIRST_NAME] ?: "",
            email = prefs[Keys.EMAIL] ?: "",
            country = prefs[Keys.COUNTRY] ?: SupportedCountry.NIGERIA.wireName,
            region = prefs[Keys.REGION] ?: "",
            province = prefs[Keys.PROVINCE] ?: "",
            localArea = prefs[Keys.LOCAL_AREA] ?: "",
            nigeriaEmergencyLabel = prefs[Keys.NG_EMERGENCY_LABEL] ?: "Nigeria National Emergency",
            nigeriaEmergencyNumber = prefs[Keys.NG_EMERGENCY_NUMBER] ?: "112",
            pendingEmailLinkEmail = prefs[Keys.PENDING_EMAIL] ?: "",
            pendingEmailLinkFirstName = prefs[Keys.PENDING_FIRST_NAME] ?: "",
            pendingEmailLinkCountry = prefs[Keys.PENDING_COUNTRY] ?: SupportedCountry.NIGERIA.wireName,
            pendingEmailLinkRegion = prefs[Keys.PENDING_REGION] ?: "",
            pendingEmailLinkProvince = prefs[Keys.PENDING_PROVINCE] ?: "",
            pendingEmailLinkLocalArea = prefs[Keys.PENDING_LOCAL_AREA] ?: "",
            pendingEmailLinkSentAtMillis = prefs[Keys.PENDING_SENT_AT] ?: 0L
        )
    }

    suspend fun setLanguage(language: String) {
        context.dataStore.edit { it[Keys.LANGUAGE] = if (language == "es") "es" else "en" }
    }

    suspend fun setTestMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.TEST_MODE] = enabled }
    }

    suspend fun saveRegistrationDraftOrDemo(
        demoActive: Boolean,
        firstName: String,
        email: String,
        country: String,
        region: String,
        province: String?,
        localArea: String,
        language: String
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DEMO_ACTIVE] = demoActive
            prefs[Keys.FIRST_NAME] = firstName.trim()
            prefs[Keys.EMAIL] = email.trim()
            prefs[Keys.COUNTRY] = country
            prefs[Keys.REGION] = region
            prefs[Keys.PROVINCE] = province ?: ""
            prefs[Keys.LOCAL_AREA] = localArea
            prefs[Keys.LANGUAGE] = language
        }
    }

    suspend fun savePendingEmailLinkDraft(
        firstName: String,
        email: String,
        country: String,
        region: String,
        province: String?,
        localArea: String
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PENDING_FIRST_NAME] = firstName.trim()
            prefs[Keys.PENDING_EMAIL] = email.trim()
            prefs[Keys.PENDING_COUNTRY] = country
            prefs[Keys.PENDING_REGION] = region
            prefs[Keys.PENDING_PROVINCE] = province ?: ""
            prefs[Keys.PENDING_LOCAL_AREA] = localArea
            prefs[Keys.PENDING_SENT_AT] = System.currentTimeMillis()
        }
    }

    suspend fun clearPendingEmailLinkDraft() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.PENDING_EMAIL)
            prefs.remove(Keys.PENDING_FIRST_NAME)
            prefs.remove(Keys.PENDING_COUNTRY)
            prefs.remove(Keys.PENDING_REGION)
            prefs.remove(Keys.PENDING_PROVINCE)
            prefs.remove(Keys.PENDING_LOCAL_AREA)
            prefs.remove(Keys.PENDING_SENT_AT)
        }
    }

    suspend fun clearSessionMode() {
        context.dataStore.edit { prefs ->
            prefs[Keys.DEMO_ACTIVE] = false
        }
    }

    suspend fun saveNigeriaEmergencyContact(label: String, number: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.NG_EMERGENCY_LABEL] = label.trim().ifBlank { "Nigeria National Emergency" }
            prefs[Keys.NG_EMERGENCY_NUMBER] = number.trim().ifBlank { "112" }
        }
    }
}
