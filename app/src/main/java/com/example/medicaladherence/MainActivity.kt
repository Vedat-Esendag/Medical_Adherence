package com.example.medicaladherence

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.medicaladherence.ui.nav.MedicalAdherenceNavGraph
import com.example.medicaladherence.ui.theme.MedicalAdherenceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MedicalAdherenceApp()
        }
    }
}

@Composable
fun MedicalAdherenceApp() {
    val navController = rememberNavController()
    var fontScale by remember { mutableFloatStateOf(1.0f) }

    MedicalAdherenceTheme(fontScale = fontScale) {
        MedicalAdherenceNavGraph(
            navController = navController,
            onFontScaleChanged = { newScale -> fontScale = newScale },
            modifier = Modifier.fillMaxSize()
        )
    }
}