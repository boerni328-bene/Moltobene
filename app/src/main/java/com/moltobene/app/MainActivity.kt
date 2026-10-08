package com.moltobene.app

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moltobene.app.data.share.IncomingImages
import com.moltobene.app.data.share.IncomingRecipeFile
import com.moltobene.app.data.share.IncomingText
import com.moltobene.app.ui.edit.SharedContent
import com.moltobene.app.ui.navigation.MoltobeneNavHost
import com.moltobene.app.ui.theme.MoltobeneTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Bilder (#46), Text oder Rezeptdateien (#54) aus „Teilen mit…“ bzw. „Öffnen mit…“ –
        // nur beim ersten Start, nicht noch einmal nach dem Drehen.
        val shared = if (savedInstanceState == null) sharedContent() else null
        val preferences = (application as MoltobeneApplication).container.preferences
        setContent {
            // Die Darstellung wird schon beim Start der App gelesen; bis dahin bleibt die Fensterfarbe stehen.
            val appearance by preferences.appearance.collectAsStateWithLifecycle()
            val current = appearance
            if (current != null) {
                val dark = current.mode.isDark(isSystemInDarkTheme())
                // Symbole der Systemleisten passend zu Hell oder Dunkel der App, nicht nur des Handys.
                DisposableEffect(dark) {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                        navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { dark },
                    )
                    onDispose {}
                }
                MoltobeneTheme(darkTheme = dark, palette = current.palette) {
                    val background = MaterialTheme.colorScheme.background.toArgb()
                    // Fensterfarbe wie der Hintergrund der Farbwelt, z. B. hinter der Tastatur beim Öffnen.
                    SideEffect { window.setBackgroundDrawable(ColorDrawable(background)) }
                    MoltobeneNavHost(shared = shared)
                }
            }
        }
    }

    private fun sharedContent(): SharedContent? {
        val ownAuthority = "$packageName.fileprovider"
        IncomingRecipeFile.from(intent, ownAuthority)?.let { return SharedContent.RecipeFile(it) }
        val photos = IncomingImages.from(intent, ownAuthority)
        if (photos.isNotEmpty()) return SharedContent.Photos(photos)
        return IncomingText.from(intent)?.let { SharedContent.Text(it) }
    }

    private companion object {
        // Schleier hinter der Navigationsleiste wie in enableEdgeToEdge() von AndroidX (dort nicht öffentlich).
        val LIGHT_SCRIM = Color.argb(0xE6, 0xFF, 0xFF, 0xFF)
        val DARK_SCRIM = Color.argb(0x80, 0x1B, 0x1B, 0x1B)
    }
}
