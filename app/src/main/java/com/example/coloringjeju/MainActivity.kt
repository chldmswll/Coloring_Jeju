package com.example.coloringjeju

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.coloringjeju.presentation.MainTabsScreen
import com.example.coloringjeju.ui.theme.ColoringJejuTheme
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
        enableEdgeToEdge()
        setContent {
            ColoringJejuTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainTabsScreen(modifier = Modifier.padding(innerPadding))
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
