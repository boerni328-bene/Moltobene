package com.moltobene.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.moltobene.app.data.share.IncomingImages
import com.moltobene.app.ui.navigation.MoltobeneNavHost
import com.moltobene.app.ui.theme.MoltobeneTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Bilder aus „Teilen mit…“ (#46) – nur beim ersten Start, nicht noch einmal nach dem Drehen.
        val sharedPhotos = if (savedInstanceState == null) {
            IncomingImages.from(intent, ownAuthority = "$packageName.fileprovider")
        } else {
            emptyList()
        }
        setContent {
            MoltobeneTheme {
                MoltobeneNavHost(sharedPhotos = sharedPhotos)
            }
        }
    }
}
