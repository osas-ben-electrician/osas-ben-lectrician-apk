package com.example

import android.Manifest
import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.SupportedCountry
import com.example.ui.components.hasRuntimeLocationPermission
import com.example.ui.components.initialViewportForCountry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MapViewWrapperTest {

    @Test
    fun `initialViewportForCountry sets Spain and Nigeria coordinates and zoom`() {
        val spain = initialViewportForCountry(SupportedCountry.SPAIN)
        assertEquals(SupportedCountry.SPAIN, spain.country)
        assertEquals(40.4168, spain.center.latitude, 0.001)
        assertEquals(-3.7038, spain.center.longitude, 0.001)
        assertEquals(6.2, spain.initialZoom, 0.01)

        val nigeria = initialViewportForCountry(SupportedCountry.NIGERIA)
        assertEquals(SupportedCountry.NIGERIA, nigeria.country)
        assertEquals(9.0820, nigeria.center.latitude, 0.001)
        assertEquals(8.6753, nigeria.center.longitude, 0.001)
        assertEquals(6.2, nigeria.initialZoom, 0.01)
    }

    @Test
    fun `hasRuntimeLocationPermission reflects fine and coarse permission grants`() {
        val app = ApplicationProvider.getApplicationContext<Context>() as Application
        val shadowApp = shadowOf(app)

        assertFalse(hasRuntimeLocationPermission(app))

        shadowApp.grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)
        assertTrue(hasRuntimeLocationPermission(app))

        shadowApp.denyPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)
        assertFalse(hasRuntimeLocationPermission(app))

        shadowApp.grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        assertTrue(hasRuntimeLocationPermission(app))
    }
}
