package com.example.coloringjeju

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.coloringjeju.core.auth.AuthRepository
import com.example.coloringjeju.core.auth.AutoLoginPreferences
import com.example.coloringjeju.presentation.Auth.AuthScreen
import com.example.coloringjeju.presentation.MainTabsScreen
import com.example.coloringjeju.ui.theme.ColoringJejuTheme
import com.google.firebase.auth.FirebaseUser
import org.osmdroid.config.Configuration

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // osmdroid's OSM tile server requires a distinctive user agent — the raw applicationId
        // ("com.example.coloringjeju") is the Android Studio template default, which OSM's tile
        // servers actively blocklist as tutorial/test-app noise (HTTP 418, osm.wiki/Blocked), so
        // this identifies the app by name instead. Cache paths point at this app's own cache dir
        // so tile caching needs no storage permission.
        Configuration.getInstance().apply {
            userAgentValue = "ColoringJeju-Android/1.0"
            osmdroidBasePath = applicationContext.cacheDir
            osmdroidTileCache = applicationContext.cacheDir.resolve("osmdroid/tiles").apply { mkdirs() }
        }
        // Firebase persists a signed-in session on disk on its own; "자동 로그인" left unchecked at
        // login means that session should NOT survive a cold start, so enforce it here — once,
        // before the first composition reads AuthRepository.currentUser below.
        if (!AutoLoginPreferences.isEnabled(this)) {
            AuthRepository.signOut()
        }
        enableEdgeToEdge()
        setContent {
            ColoringJejuTheme {
                var currentUser by remember { mutableStateOf<FirebaseUser?>(AuthRepository.currentUser) }
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (currentUser == null) {
                        AuthScreen(
                            onAuthenticated = { currentUser = it },
                            modifier = Modifier.padding(innerPadding),
                        )
                    } else {
                        MainTabsScreen(
                            modifier = Modifier.padding(innerPadding),
                            onLoggedOut = { currentUser = null },
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun MainTabsPreview() {
    ColoringJejuTheme {
        MainTabsScreen()
    }
}
