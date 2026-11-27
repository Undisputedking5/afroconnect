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
import com.example.africonnect.data.Repo
import com.example.africonnect.navigation.NavGraph
import com.example.africonnect.ui.theme.AfriconnectTheme
import com.example.africonnect.util.Prefs
import com.example.africonnect.viewmodel.AppViewModel
import com.google.firebase.database.FirebaseDatabase
import org.osmdroid.config.Configuration

private var emulatorConfigured = false

class MainActivity : ComponentActivity() {

    private val app: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.init(this)
        if (BuildConfig.USE_FIREBASE_EMULATOR && !emulatorConfigured) {
            emulatorConfigured = true
            com.google.firebase.appcheck.FirebaseAppCheck.getInstance()
                .installAppCheckProviderFactory(com.example.africonnect.util.EmulatorAppCheckFactory())
            com.google.firebase.auth.FirebaseAuth.getInstance().useEmulator("10.0.2.2", 9099)
            FirebaseDatabase.getInstance().setLogLevel(com.google.firebase.database.Logger.Level.DEBUG)
            FirebaseDatabase.getInstance().useEmulator("10.0.2.2", 9000)
            com.google.firebase.storage.FirebaseStorage.getInstance().useEmulator("10.0.2.2", 9199)
        } else if (!BuildConfig.USE_FIREBASE_EMULATOR) {
            runCatching { FirebaseDatabase.getInstance().setPersistenceEnabled(true) }
        }
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
                val view = androidx.compose.ui.platform.LocalView.current
                androidx.compose.runtime.DisposableEffect(dark) {
                    androidx.core.view.WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
                    onDispose { }
                }
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    NavGraph(rememberNavController(), app)
                }
            }
        }
    }

    // Presence: online while the app is in the foreground.
    override fun onStart() {
        super.onStart()
        if (Repo.auth.currentUser != null) Repo.setupPresence()
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations && Repo.auth.currentUser != null) Repo.goOffline()
    }
}
