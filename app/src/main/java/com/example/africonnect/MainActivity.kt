package com.example.africonnect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.africonnect.navigation.NavGraph
import com.example.africonnect.ui.theme.AfriconnectTheme
import com.example.africonnect.util.Prefs
import com.example.africonnect.viewmodel.AppViewModel
import com.google.firebase.database.FirebaseDatabase
import org.osmdroid.config.Configuration

class MainActivity : ComponentActivity() {

    private val app: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.init(this)
        runCatching { FirebaseDatabase.getInstance().setPersistenceEnabled(true) }
        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = cacheDir
            osmdroidTileCache = cacheDir.resolve("tiles")
        }
        enableEdgeToEdge()
        setContent {
            val theme by Prefs.theme.collectAsState()
            val dark = when (theme) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }
            AfriconnectTheme(darkTheme = dark) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    NavGraph(rememberNavController(), app)
                }
            }
        }
    }
}
