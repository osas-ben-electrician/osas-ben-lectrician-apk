package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.device.LocationService
import com.example.data.device.PhotoExifReader
import com.example.data.local.AdministrativeLocationRepository
import com.example.data.model.SupportedCountry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LocationDatasetsTest {

    @Test
    fun nigeriaDatasetHas37StatesAnd774LgasWithCoordinates() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dataset = context.assets.open("nigeria_locations.json").use {
            AdministrativeLocationRepository.parseNigeriaJson(it)
        }
        assertEquals(37, dataset.statesCount)
        assertEquals(37, dataset.states.size)
        assertEquals(774, dataset.lgasCount)
        val totalLgas = dataset.states.sumOf { it.lgas.size }
        assertEquals(774, totalLgas)
        assertTrue(
            dataset.states.any {
                it.name.contains("Federal Capital Territory", ignoreCase = true) ||
                    it.name.contains("FCT", ignoreCase = true) ||
                    it.name.equals("Abuja", ignoreCase = true)
            }
        )
        assertTrue(
            dataset.states.all { st ->
                st.lgas.all { it.name.isNotBlank() && it.lat in 4.0..14.5 && it.lon in 2.5..15.0 }
            }
        )
    }

    @Test
    fun spainDatasetHas19Communities52ProvincesAnd8132Municipalities() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dataset = context.assets.open("spain_locations.json").use {
            AdministrativeLocationRepository.parseSpainJson(it)
        }
        assertEquals(19, dataset.communitiesCount)
        assertEquals(19, dataset.communities.size)
        assertEquals(52, dataset.provincesCount)
        assertEquals(52, dataset.communities.sumOf { it.provinces.size })
        assertEquals(8132, dataset.municipalitiesCount)
        assertEquals(8132, dataset.communities.sumOf { c -> c.provinces.sumOf { it.municipalities.size } })
        assertTrue(dataset.communities.any { it.name.equals("Ceuta", ignoreCase = true) })
        assertTrue(dataset.communities.any { it.name.equals("Melilla", ignoreCase = true) })
    }

    @Test
    fun countryCoordinateInferenceSeparatesSpainAndNigeria() {
        assertEquals(SupportedCountry.SPAIN, LocationService.inferCountryByCoordinates(40.4168, -3.7038))
        assertEquals(SupportedCountry.SPAIN, LocationService.inferCountryByCoordinates(28.4636, -16.2518))
        assertEquals(SupportedCountry.NIGERIA, LocationService.inferCountryByCoordinates(9.0765, 7.3986))
        assertEquals(SupportedCountry.NIGERIA, LocationService.inferCountryByCoordinates(6.5244, 3.3792))
        assertEquals(null, LocationService.inferCountryByCoordinates(48.8566, 2.3522))
    }

    @Test
    fun photoExifDateTimeParserHandlesValidAndMissingMetadata() {
        val (d1, t1) = PhotoExifReader.parseExifDateTime("2026:11:25 14:32:10")
        assertEquals("2026-11-25", d1)
        assertEquals("14:32:10", t1)

        val (d2, t2) = PhotoExifReader.parseExifDateTime(null)
        assertEquals("Not recorded in EXIF", d2)
        assertEquals("Not recorded in EXIF", t2)
    }
}
