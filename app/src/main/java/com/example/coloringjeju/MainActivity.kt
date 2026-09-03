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
import com.example.coloringjeju.ui.showcase.DesignSystemShowcaseScreen
import com.example.coloringjeju.ui.theme.ColoringJejuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ColoringJejuTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DesignSystemShowcaseScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun DesignSystemShowcasePreview() {
    ColoringJejuTheme {
        DesignSystemShowcaseScreen()
    }
}
