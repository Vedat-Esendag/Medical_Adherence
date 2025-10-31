package com.example.medicaladherence.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRScannerScreen(
    onQRScanned: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    var hasScanned by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Patient QR Code") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Instructions
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "How to scan:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "1. Ask the patient to show their QR code\n" +
                               "2. Point your camera at the QR code\n" +
                               "3. Wait for automatic scan",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            // Camera preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (!hasScanned) {
                    QRCodeScanner(
                        onQRScanned = { qrData ->
                            hasScanned = true
                            onQRScanned(qrData)
                        }
                    )
                } else {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun QRCodeScanner(
    onQRScanned: (String) -> Unit
) {
    val context = LocalContext.current
    
    AndroidView(
        factory = { ctx ->
            DecoratedBarcodeView(ctx).apply {
                val formats = listOf(BarcodeFormat.QR_CODE)
                barcodeView.decoderFactory = DefaultDecoderFactory(formats)
                
                initializeFromIntent(android.content.Intent())
                
                decodeContinuous(object : BarcodeCallback {
                    override fun barcodeResult(result: BarcodeResult?) {
                        result?.text?.let { qrData ->
                            pause()
                            onQRScanned(qrData)
                        }
                    }
                    
                    override fun possibleResultPoints(resultPoints: MutableList<com.google.zxing.ResultPoint>?) {
                        // Not needed
                    }
                })
                
                resume()
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { view ->
            view.resume()
        }
    )
    
    DisposableEffect(Unit) {
        onDispose {
            // Cleanup will be handled by the view
        }
    }
}

