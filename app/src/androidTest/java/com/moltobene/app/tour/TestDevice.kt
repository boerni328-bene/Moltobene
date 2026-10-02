package com.moltobene.app.tour

import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry

/** Befehle an das Test-Handy (Emulator), ausgeführt mit den Rechten der Android-Shell. */
object TestDevice {

    /**
     * Ordner der Bildschirmfotos. Er liegt bewusst außerhalb der App: Nach dem Testlauf wird die App
     * deinstalliert, die Fotos bleiben und werden vom Testlauf auf GitHub abgeholt (siehe build.yml).
     */
    const val SCREENSHOT_DIR = "/data/local/tmp/moltobene-bildschirmfotos"

    /** Führt [command] aus und wartet, bis er fertig ist (ohne Shell-Zeichen wie | oder >). */
    fun shell(command: String) {
        val output = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        ParcelFileDescriptor.AutoCloseInputStream(output).use { it.readBytes() }
    }

    /** Fotografiert den ganzen Bildschirm, wie Nutzer ihn sehen – mit Dialogen und Statusleiste. */
    fun screenshot(variant: DisplayVariant, name: String) {
        val dir = "$SCREENSHOT_DIR/${variant.folder}"
        shell("mkdir -p $dir")
        shell("screencap -p $dir/$name.png")
    }
}
