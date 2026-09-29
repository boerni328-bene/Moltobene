package com.moltobene.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.moltobene.app.ui.navigation.MoltobeneNavHost
import com.moltobene.app.ui.theme.MoltobeneTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MoltobeneTheme {
                MoltobeneNavHost()
            }
        }
    }
}
