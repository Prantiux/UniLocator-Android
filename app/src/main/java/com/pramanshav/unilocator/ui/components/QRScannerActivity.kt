package com.pramanshav.unilocator.ui.components

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.journeyapps.barcodescanner.CaptureManager
import com.journeyapps.barcodescanner.CompoundBarcodeView
import com.pramanshav.unilocator.ui.theme.UniLocatorTheme

class QRScannerActivity : ComponentActivity() {
    private var captureManager: CaptureManager? = null
    private var barcodeView: CompoundBarcodeView? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            UniLocatorTheme {
                QRScannerContent(
                    onBackPressed = { finish() },
                    onQRCodeScanned = { result ->
                        val intent = Intent().apply {
                            putExtra("SCAN_RESULT", result)
                        }
                        setResult(Activity.RESULT_OK, intent)
                        finish()
                    },
                    onCaptureManagerCreated = { manager, view ->
                        captureManager = manager
                        barcodeView = view
                    },
                    onFlashToggle = { isOn ->
                        try {
                            if (isOn) {
                                barcodeView?.setTorchOn()
                            } else {
                                barcodeView?.setTorchOff()
                            }
                        } catch (e: Exception) {
                            // Handle flash not available
                        }
                    }
                )
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        captureManager?.onResume()
    }
    
    override fun onPause() {
        super.onPause()
        captureManager?.onPause()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        captureManager?.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRScannerContent(
    onBackPressed: () -> Unit,
    onQRCodeScanned: (String) -> Unit,
    onCaptureManagerCreated: (CaptureManager, CompoundBarcodeView) -> Unit = { _, _ -> },
    onFlashToggle: (Boolean) -> Unit = {}
) {
    var isFlashOn by remember { mutableStateOf(false) }
    var hasPermission by remember { mutableStateOf(false) }
    var scannedResult by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }
    
    // Check permission on first composition
    LaunchedEffect(Unit) {
        hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        if (hasPermission) {
            // Camera Preview
            AndroidView(
                factory = { context ->
                    CompoundBarcodeView(context).apply {
                        val captureManager = CaptureManager(context as Activity, this)
                        captureManager.initializeFromIntent(context.intent, null)
                        captureManager.decode()
                        
                        this.setStatusText("")
                        this.decodeContinuous { result ->
                            scannedResult = result.text
                            onQRCodeScanned(result.text)
                        }
                        
                        // Pass capture manager and view to parent
                        onCaptureManagerCreated(captureManager, this)
                        
                        // Start preview
                        this.resume()
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            
            // Dark overlay with scan area cutout
            Canvas(modifier = Modifier.fillMaxSize()) {
                val scanAreaSize = size.width * 0.7f
                val scanAreaLeft = (size.width - scanAreaSize) / 2
                val scanAreaTop = (size.height - scanAreaSize) / 2
                
                // Dark overlay
                drawRect(
                    color = Color.Black.copy(alpha = 0.6f),
                    size = size
                )
                
                // Clear scan area (transparent)
                drawRect(
                    color = Color.Transparent,
                    topLeft = androidx.compose.ui.geometry.Offset(scanAreaLeft, scanAreaTop),
                    size = androidx.compose.ui.geometry.Size(scanAreaSize, scanAreaSize)
                )
                
                // Scan area border
                drawRect(
                    color = Color(0xFF037d3a),
                    topLeft = androidx.compose.ui.geometry.Offset(scanAreaLeft, scanAreaTop),
                    size = androidx.compose.ui.geometry.Size(scanAreaSize, scanAreaSize),
                    style = Stroke(width = 4.dp.toPx())
                )
                
                // Corner indicators
                val cornerSize = 40.dp.toPx()
                val cornerStroke = 6.dp.toPx()
                
                // Top-left corner
                drawLine(
                    color = Color(0xFF037d3a),
                    start = androidx.compose.ui.geometry.Offset(scanAreaLeft, scanAreaTop),
                    end = androidx.compose.ui.geometry.Offset(scanAreaLeft + cornerSize, scanAreaTop),
                    strokeWidth = cornerStroke
                )
                drawLine(
                    color = Color(0xFF037d3a),
                    start = androidx.compose.ui.geometry.Offset(scanAreaLeft, scanAreaTop),
                    end = androidx.compose.ui.geometry.Offset(scanAreaLeft, scanAreaTop + cornerSize),
                    strokeWidth = cornerStroke
                )
                
                // Top-right corner
                drawLine(
                    color = Color(0xFF037d3a),
                    start = androidx.compose.ui.geometry.Offset(scanAreaLeft + scanAreaSize, scanAreaTop),
                    end = androidx.compose.ui.geometry.Offset(scanAreaLeft + scanAreaSize - cornerSize, scanAreaTop),
                    strokeWidth = cornerStroke
                )
                drawLine(
                    color = Color(0xFF037d3a),
                    start = androidx.compose.ui.geometry.Offset(scanAreaLeft + scanAreaSize, scanAreaTop),
                    end = androidx.compose.ui.geometry.Offset(scanAreaLeft + scanAreaSize, scanAreaTop + cornerSize),
                    strokeWidth = cornerStroke
                )
                
                // Bottom-left corner
                drawLine(
                    color = Color(0xFF037d3a),
                    start = androidx.compose.ui.geometry.Offset(scanAreaLeft, scanAreaTop + scanAreaSize),
                    end = androidx.compose.ui.geometry.Offset(scanAreaLeft + cornerSize, scanAreaTop + scanAreaSize),
                    strokeWidth = cornerStroke
                )
                drawLine(
                    color = Color(0xFF037d3a),
                    start = androidx.compose.ui.geometry.Offset(scanAreaLeft, scanAreaTop + scanAreaSize),
                    end = androidx.compose.ui.geometry.Offset(scanAreaLeft, scanAreaTop + scanAreaSize - cornerSize),
                    strokeWidth = cornerStroke
                )
                
                // Bottom-right corner
                drawLine(
                    color = Color(0xFF037d3a),
                    start = androidx.compose.ui.geometry.Offset(scanAreaLeft + scanAreaSize, scanAreaTop + scanAreaSize),
                    end = androidx.compose.ui.geometry.Offset(scanAreaLeft + scanAreaSize - cornerSize, scanAreaTop + scanAreaSize),
                    strokeWidth = cornerStroke
                )
                drawLine(
                    color = Color(0xFF037d3a),
                    start = androidx.compose.ui.geometry.Offset(scanAreaLeft + scanAreaSize, scanAreaTop + scanAreaSize),
                    end = androidx.compose.ui.geometry.Offset(scanAreaLeft + scanAreaSize, scanAreaTop + scanAreaSize - cornerSize),
                    strokeWidth = cornerStroke
                )
            }
        } else {
            // Permission denied state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Camera Permission Required",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Please grant camera permission to scan QR codes",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF037d3a)
                    )
                ) {
                    Text("Grant Permission")
                }
            }
        }
        
        // Top bar
        TopAppBar(
            title = {
                Text(
                    text = "Scan QR Code",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackPressed) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            },
            actions = {
                if (hasPermission) {
                    IconButton(
                        onClick = { 
                            isFlashOn = !isFlashOn
                            onFlashToggle(isFlashOn)
                        }
                    ) {
                        Icon(
                            imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = if (isFlashOn) "Turn off flash" else "Turn on flash",
                            tint = Color.White
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )
        
        // Instructions (only show when permission is granted)
        if (hasPermission) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(32.dp)
                    .background(
                        Color.Black.copy(alpha = 0.8f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Position QR code within the frame",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "The QR code will be scanned automatically",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
