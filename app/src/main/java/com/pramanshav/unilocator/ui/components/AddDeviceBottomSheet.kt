package com.pramanshav.unilocator.ui.components

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.pramanshav.unilocator.data.UserDeviceCode
import com.pramanshav.unilocator.utils.QRCodeGenerator
import com.pramanshav.unilocator.viewmodel.ConnectionResult
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDeviceBottomSheet(
    onDismiss: () -> Unit,
    onConnectByQR: (String) -> Unit,
    onConnectByCode: (String) -> Unit,
    onGenerateCode: () -> Unit,
    userDeviceCode: UserDeviceCode?,
    isGeneratingCode: Boolean,
    isConnecting: Boolean,
    connectionResult: ConnectionResult?
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var manualCode by remember { mutableStateOf("") }
    var showMyCodeDialog by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    
    // QR Code Scanner using custom activity
    val qrScannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scannedData = result.data?.getStringExtra("SCAN_RESULT")
            if (scannedData != null) {
                onConnectByQR(scannedData)
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Single handle bar
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(4.dp)
                .align(Alignment.CenterHorizontally)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(2.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            ) {}
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Add Device",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Close"
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Connect Device") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("My Code") }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        when (selectedTab) {
            0 -> {
                // Connect Device Tab
                ConnectDeviceContent(
                    manualCode = manualCode,
                    onManualCodeChange = { manualCode = it },
                    onScanQR = {
                        val intent = Intent(context, QRScannerActivity::class.java)
                        qrScannerLauncher.launch(intent)
                    },
                    onConnectByCode = { onConnectByCode(manualCode) },
                    isConnecting = isConnecting,
                    connectionResult = connectionResult
                )
            }
            1 -> {
                // My Code Tab
                MyCodeContent(
                    userDeviceCode = userDeviceCode,
                    isGeneratingCode = isGeneratingCode,
                    onGenerateCode = onGenerateCode,
                    onShowQRCode = { showMyCodeDialog = true }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
    }
    
    // QR Code Dialog
    if (showMyCodeDialog && userDeviceCode != null) {
        Dialog(onDismissRequest = { showMyCodeDialog = false }) {
            QRCodeDialog(
                userDeviceCode = userDeviceCode,
                onDismiss = { showMyCodeDialog = false },
                onCopyCode = {
                    clipboardManager.setText(AnnotatedString(userDeviceCode.deviceCode))
                }
            )
        }
    }
}

@Composable
private fun ConnectDeviceContent(
    manualCode: String,
    onManualCodeChange: (String) -> Unit,
    onScanQR: () -> Unit,
    onConnectByCode: () -> Unit,
    isConnecting: Boolean,
    connectionResult: ConnectionResult?
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Scan QR Code Button
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            onClick = onScanQR,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "Scan QR Code",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Scan QR Code",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Scan someone's QR code to connect",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        // Divider with OR
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(
                text = "  OR  ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(modifier = Modifier.weight(1f))
        }
        
        // Manual Code Entry
        Text(
            text = "Enter Code Manually",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        // Split Code Input Boxes
        CodeInputBoxes(
            code = manualCode,
            onCodeChange = onManualCodeChange,
            isError = connectionResult is ConnectionResult.Error
        )
        
        if (connectionResult is ConnectionResult.Error) {
            Text(
                text = connectionResult.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        
        if (connectionResult is ConnectionResult.Success) {
            Text(
                text = "Device connected successfully!",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        Button(
            onClick = onConnectByCode,
            modifier = Modifier.fillMaxWidth(),
            enabled = manualCode.length == 9 && !isConnecting, // XXXX-XXXX format = 9 characters
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isConnecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(if (isConnecting) "Connecting..." else "Connect Device")
        }
    }
}

@Composable
private fun MyCodeContent(
    userDeviceCode: UserDeviceCode?,
    isGeneratingCode: Boolean,
    onGenerateCode: () -> Unit,
    onShowQRCode: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Share Your Code",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        Text(
            text = "Generate a code that others can use to connect to your device",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        if (userDeviceCode != null) {
            // Show existing code
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Your Device Code",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = userDeviceCode.deviceCode,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onShowQRCode,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = "Show QR",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Show QR")
                        }
                        
                        Button(
                            onClick = onGenerateCode,
                            modifier = Modifier.weight(1f),
                            enabled = !isGeneratingCode
                        ) {
                            if (isGeneratingCode) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("New Code")
                            }
                        }
                    }
                }
            }
        } else {
            // No code yet, show generate button
            Button(
                onClick = onGenerateCode,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isGeneratingCode,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isGeneratingCode) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generating...")
                } else {
                    Text("Generate My Code")
                }
            }
        }
    }
}

@Composable
private fun QRCodeDialog(
    userDeviceCode: UserDeviceCode,
    onDismiss: () -> Unit,
    onCopyCode: () -> Unit
) {
    val qrBitmap = remember(userDeviceCode.qrCodeData) {
        QRCodeGenerator.generateQRCodeBitmap(userDeviceCode.qrCodeData)
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "My QR Code",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (qrBitmap != null) {
                Card(
                    modifier = Modifier.size(200.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier.size(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Code: ${userDeviceCode.deviceCode}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCopyCode,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy Code")
                }
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
private fun CodeInputBoxes(
    code: String,
    onCodeChange: (String) -> Unit,
    isError: Boolean
) {
    var focusedBoxIndex by remember { mutableIntStateOf(0) }
    
    // Split the code into individual characters
    val codeChars = remember(code) {
        val chars = code.replace("-", "").take(8).toCharArray()
        Array(8) { index ->
            if (index < chars.size) chars[index].toString() else ""
        }
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        // First 4 boxes
        repeat(4) { index ->
            CodeInputBox(
                value = codeChars[index],
                onValueChange = { newValue ->
                    updateCodeAtIndex(code, onCodeChange, index, newValue)
                    if (newValue.isNotEmpty() && index < 3) {
                        focusedBoxIndex = index + 1
                    }
                },
                isFocused = focusedBoxIndex == index,
                isError = isError,
                modifier = Modifier.weight(1f)
            )
        }
        
        // Hyphen separator
        Text(
            text = "-",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        
        // Last 4 boxes
        repeat(4) { index ->
            val boxIndex = index + 4
            CodeInputBox(
                value = codeChars[boxIndex],
                onValueChange = { newValue ->
                    updateCodeAtIndex(code, onCodeChange, boxIndex, newValue)
                    if (newValue.isNotEmpty() && boxIndex < 7) {
                        focusedBoxIndex = boxIndex + 1
                    }
                },
                isFocused = focusedBoxIndex == boxIndex,
                isError = isError,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CodeInputBox(
    value: String,
    onValueChange: (String) -> Unit,
    isFocused: Boolean,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    val borderColor = when {
        isError -> MaterialTheme.colorScheme.error
        value.isNotEmpty() -> MaterialTheme.colorScheme.primary
        isFocused -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    }
    
    val backgroundColor = when {
        isError -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
        value.isNotEmpty() -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        else -> MaterialTheme.colorScheme.surface
    }
    
    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            // Only allow single alphanumeric character and convert to uppercase
            if (newValue.length <= 1 && newValue.all { it.isLetterOrDigit() }) {
                onValueChange(newValue.uppercase())
            }
        },
        modifier = modifier
            .size(48.dp)
            .padding(2.dp),
        textStyle = MaterialTheme.typography.headlineSmall.copy(
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        ),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            autoCorrect = false
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = borderColor,
            unfocusedBorderColor = borderColor,
            errorBorderColor = MaterialTheme.colorScheme.error,
            focusedContainerColor = backgroundColor,
            unfocusedContainerColor = backgroundColor,
            errorContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
        ),
        isError = isError
    )
}

private fun updateCodeAtIndex(
    currentCode: String,
    onCodeChange: (String) -> Unit,
    index: Int,
    newValue: String
) {
    val cleanCode = currentCode.replace("-", "").padEnd(8, ' ')
    val codeArray = cleanCode.toCharArray()
    
    if (index < codeArray.size) {
        codeArray[index] = if (newValue.isNotEmpty()) newValue[0] else ' '
    }
    
    val firstPart = String(codeArray.sliceArray(0..3)).trim()
    val secondPart = String(codeArray.sliceArray(4..7)).trim()
    
    val formattedCode = if (firstPart.isNotEmpty() && secondPart.isNotEmpty()) {
        "$firstPart-$secondPart"
    } else if (firstPart.isNotEmpty()) {
        firstPart
    } else {
        ""
    }
    
    onCodeChange(formattedCode)
}
