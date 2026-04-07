package com.example.tsumobilkabeta

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import com.example.tsumobilkabeta.ui.theme.TSUMapTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableIntStateOf
import com.yandex.mapkit.MapKitFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MapKitFactory.setApiKey("4bef7f08-48f0-4a69-b484-4f79582744c8")

        try {
            MapKitFactory.initialize(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        enableEdgeToEdge()
        setContent {
            TSUMapTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MapScreen(modifier = Modifier.fillMaxSize())
                }
            }
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
fun Greeting(){
}


