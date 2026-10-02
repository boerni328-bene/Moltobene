package com.moltobene.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.moltobene.app.data.share.IncomingImages
import com.moltobene.app.data.share.IncomingText
import com.moltobene.app.ui.edit.SharedContent
import com.moltobene.app.ui.navigation.MoltobeneNavHost
import com.moltobene.app.ui.theme.MoltobeneTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Bilder (#46) oder Text aus „Teilen mit…“ – nur beim ersten Start, nicht noch einmal nach dem Drehen.
        val shared = if (savedInstanceState == null) sharedContent() else null
        setContent {
            MoltobeneTheme {
                MoltobeneNavHost(shared = shared)
            }
        }
    }

    private fun sharedContent(): SharedContent? {
        val photos = IncomingImages.from(intent, ownAuthority = "$packageName.fileprovider")
        if (photos.isNotEmpty()) return SharedContent.Photos(photos)
        return IncomingText.from(intent)?.let { SharedContent.Text(it) }
    }
}
