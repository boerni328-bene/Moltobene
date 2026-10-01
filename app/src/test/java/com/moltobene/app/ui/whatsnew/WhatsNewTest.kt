package com.moltobene.app.ui.whatsnew

import com.moltobene.app.BuildConfig
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsNewTest {

    @Test
    fun nachUpdateVonAlterVersionErscheintDieUebersicht() {
        assertTrue(WhatsNew.shouldShow(lastSeenVersionCode = null, isFreshInstall = false, versionCode = 11))
        assertTrue(WhatsNew.shouldShow(lastSeenVersionCode = 10, isFreshInstall = false, versionCode = 11))
    }

    @Test
    fun nachNeuinstallationOderBereitsGesehenNicht() {
        assertFalse(WhatsNew.shouldShow(lastSeenVersionCode = null, isFreshInstall = true, versionCode = 11))
        assertFalse(WhatsNew.shouldShow(lastSeenVersionCode = 11, isFreshInstall = false, versionCode = 11))
        assertFalse(WhatsNew.shouldShow(lastSeenVersionCode = 12, isFreshInstall = false, versionCode = 11))
    }

    @Test
    fun textePassenZuEinerErschienenenVersion() {
        assertTrue(
            "WhatsNew.VERSION_CODE darf nicht größer sein als der versionCode der App",
            WhatsNew.VERSION_CODE <= BuildConfig.VERSION_CODE,
        )
        assertTrue(Regex("""\d+\.\d+\.\d+""").matches(WhatsNew.VERSION_NAME))
    }
}
