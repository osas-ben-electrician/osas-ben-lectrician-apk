package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.auth.EmailLinkAuthHelper
import com.google.firebase.FirebaseApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class EmailLinkAuthHelperTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
    }

    @Test
    fun `buildActionCodeSettings configures in-app handling and package name`() {
        val settings = EmailLinkAuthHelper.buildActionCodeSettings(context)
        assertTrue(settings.canHandleCodeInApp())
        assertEquals(context.packageName, settings.androidPackageName)
        assertTrue(settings.url.contains("firebaseapp.com/finishSignUp"))
    }

    @Test
    fun `isIncomingEmailLink rejects blank and non-auth links`() {
        assertFalse(EmailLinkAuthHelper.isIncomingEmailLink(null))
        assertFalse(EmailLinkAuthHelper.isIncomingEmailLink(""))
        assertFalse(EmailLinkAuthHelper.isIncomingEmailLink("https://example.com/hello"))
    }
}
