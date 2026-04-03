package com.example.tsumobilkabeta.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.tsumobilkabeta.R

private val LightColorScheme = lightColorScheme(
    primary = LightBackGround,
    secondary = LightSurface,
    background = LightBackGround,
    surface = LightSurface,
    onBackground = LgihtOnBackground,
    onSurface = LgihtOnBackground
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkSurface,
    secondary = DarkSurface,
    background = DarkBackGround,
    surface = DarkSurface,
    onBackground = DarkOnBackground,
    onSurface = DarkOnBackground,
    onPrimary = DarkOnBackground
)

data class MapResources(
    val mapImageResource: Int
)

val LocalMapResources = staticCompositionLocalOf {
    MapResources(
        mapImageResource = R.drawable.usermap_dark
    )
}
@Composable
fun TSUMapTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val mapResources = if (darkTheme) {
        R.drawable.usermap_dark
    } else {
        R.drawable.usermap_light
    }


    MaterialTheme(
        colorScheme = colorScheme
    ) {
        CompositionLocalProvider (
            LocalMapResources provides MapResources(mapResources)
        ) {
            content()
        }
    }
}