package com.example.tsumobilkabeta

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.tsumobilkabeta.ui.theme.TSUMapTheme
import com.yandex.mapkit.MapKitFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        MapKitFactory.setApiKey("4bef7f08-48f0-4a69-b484-4f79582744c8")

        try {
            MapKitFactory.initialize(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        enableEdgeToEdge()

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        controller.hide(WindowInsetsCompat.Type.systemBars())

        val themePrefs = getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

        setContent {
            val systemDark = isSystemInDarkTheme()
            var isDarkTheme by rememberSaveable {
                mutableStateOf(
                    themePrefs.getBoolean(
                        "is_dark_theme",
                        systemDark
                    )
                )
            }

            TSUMapTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    MapScreen(
                        modifier = Modifier.fillMaxSize(),
                        isDarkTheme = isDarkTheme,
                        onThemeToggle = {
                            val newDark = !isDarkTheme
                            isDarkTheme = newDark
                            themePrefs.edit().putBoolean("is_dark_theme", newDark).apply()
                        }
                    )
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            WindowInsetsControllerCompat(window, window.decorView).hide(
                WindowInsetsCompat.Type.systemBars()
            )
        }
    }

    override fun onStart() {
        super.onStart()
        try {
            MapKitFactory.getInstance().onStart()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStop() {
        try {
            MapKitFactory.getInstance().onStop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        super.onStop()
    }
}

@PreviewScreenSizes
@Composable
fun Greeting() {
}
