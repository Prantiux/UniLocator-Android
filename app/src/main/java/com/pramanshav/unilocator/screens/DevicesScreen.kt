package com.pramanshav.unilocator.screens

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.DevicesOther
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.pramanshav.unilocator.data.Device
import com.pramanshav.unilocator.ui.components.AddDeviceBottomSheet
import com.pramanshav.unilocator.utils.DeviceRegistrationManager
import com.pramanshav.unilocator.utils.DeviceIdGenerator
import com.pramanshav.unilocator.viewmodel.ConnectionResult
import com.pramanshav.unilocator.viewmodel.DevicesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen() {
    val viewModel: DevicesViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    var showAddDeviceSheet by remember { mutableStateOf(false) }
    
    // User devices from registration system
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val deviceRegistrationManager = remember { DeviceRegistrationManager.getInstance() }
    var userDevices by remember { mutableStateOf<List<DeviceRegistrationManager.UserDevice>>(emptyList()) }
    var isLoadingUserDevices by remember { mutableStateOf(true) }
    var currentDeviceId by remember { mutableStateOf<String?>(null) }
    
    // Load user devices on first composition
    LaunchedEffect(Unit) {
        try {
            currentDeviceId = DeviceIdGenerator.getInstance().getPrimaryDeviceId(context)
            userDevices = deviceRegistrationManager.getUserDevices(context)
        } catch (e: Exception) {
            // Handle error silently for now
        } finally {
            isLoadingUserDevices = false
        }
    }
    
    LaunchedEffect(uiState.connectionResult) {
        if (uiState.connectionResult is ConnectionResult.Success) {
            showAddDeviceSheet = false
            viewModel.clearConnectionResult()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "Devices",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
            },
            actions = {
                IconButton(
                    onClick = { showAddDeviceSheet = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Device",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            )
        )

        // Content
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // My Devices Section
            item {
                MyDevicesSection(
                    userDevices = userDevices,
                    currentDeviceId = currentDeviceId,
                    isLoading = isLoadingUserDevices,
                    deviceRegistrationManager = deviceRegistrationManager,
                    context = context,
                    coroutineScope = coroutineScope,
                    onDevicesUpdated = { 
                        // Refresh user devices list
                        coroutineScope.launch {
                            try {
                                userDevices = deviceRegistrationManager.getUserDevices(context)
                            } catch (e: Exception) {
                                // Handle error silently
                            }
                        }
                    }
                )
            }
            
            // Connected Devices Section
            if (uiState.devices.isNotEmpty()) {
                item {
                    ConnectedDevicesSection(
                        devices = uiState.devices
                    )
                }
            }
            
            // Add Device Section
            item {
                AddDeviceCard(
                    onAddDevice = { showAddDeviceSheet = true }
                )
            }
        }
        
        // Error handling
        uiState.error?.let { error ->
            LaunchedEffect(error) {
                // Show error snackbar here if needed
                viewModel.clearError()
            }
        }
    }
    
    // Add Device Bottom Sheet
    if (showAddDeviceSheet) {
        ModalBottomSheet(
            onDismissRequest = { 
                showAddDeviceSheet = false
                viewModel.clearConnectionResult()
            },
            dragHandle = null
        ) {
            AddDeviceBottomSheet(
                onDismiss = { 
                    showAddDeviceSheet = false
                    viewModel.clearConnectionResult()
                },
                onConnectByQR = { qrData -> viewModel.connectByQR(qrData) },
                onConnectByCode = { code -> viewModel.connectByCode(code) },
                onGenerateCode = { viewModel.generateUserCode() },
                userDeviceCode = uiState.userDeviceCode,
                isGeneratingCode = uiState.isGeneratingCode,
                isConnecting = uiState.isConnecting,
                connectionResult = uiState.connectionResult
            )
        }
    }
}

@Composable
private fun NoDevicesContent(
    onAddDevice: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Large icon
        Surface(
            modifier = Modifier.size(120.dp),
            shape = RoundedCornerShape(60.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DevicesOther,
                    contentDescription = "No devices",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(60.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "No devices connected",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Connect your devices to track their location and keep them safe",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onAddDevice,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Device",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Add Your First Device",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceCard(device: Device) {
    // Format last seen date
    val lastSeenText = remember(device.lastSeen) {
        device.lastSeen?.let { date ->
            val now = java.util.Date()
            val diffInMillis = now.time - date.time
            val diffInMinutes = diffInMillis / (1000 * 60)
            
            when {
                diffInMinutes < 1 -> "Just now"
                diffInMinutes < 60 -> "${diffInMinutes.toInt()} minutes ago"
                diffInMinutes < 1440 -> "${(diffInMinutes / 60).toInt()} hours ago"
                else -> "${(diffInMinutes / 1440).toInt()} days ago"
            }
        } ?: "Unknown"
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = { /* TODO: Navigate to device details */ }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Device Icon
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Smartphone,
                        contentDescription = device.type,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Device Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = device.name.ifEmpty { "Unknown Device" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Status Indicator
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (device.isOnline) 
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        else 
                            MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = if (device.isOnline) "Online" else "Offline",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (device.isOnline) 
                                MaterialTheme.colorScheme.primary
                            else 
                                MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = device.type.ifEmpty { "Unknown Type" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Last seen",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = lastSeenText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Battery Level (if available)
                    device.batteryLevel?.let { battery ->
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "$battery%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MyDevicesSection(
    userDevices: List<DeviceRegistrationManager.UserDevice>,
    currentDeviceId: String?,
    isLoading: Boolean,
    deviceRegistrationManager: DeviceRegistrationManager,
    context: android.content.Context,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    onDevicesUpdated: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "My Devices",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (userDevices.isEmpty() && !isLoading) {
                Text(
                    text = "Device registration in progress...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // Sort devices: current device first, then others
                val sortedDevices = userDevices.sortedByDescending { it.isCurrentDevice }
                
                sortedDevices.forEachIndexed { index, device ->
                    UserDeviceCard(
                        device = device,
                        onRename = { newName ->
                            // Handle device rename
                            coroutineScope.launch {
                                try {
                                    val success = deviceRegistrationManager.updateDeviceName(device.deviceId, newName)
                                    if (success) {
                                        // Refresh devices list
                                        onDevicesUpdated()
                                    }
                                } catch (e: Exception) {
                                    // Handle error silently for now
                                }
                            }
                        },
                        onViewDetails = { 
                            // Handle view device details - will implement next
                        }
                    )
                    
                    if (index < sortedDevices.size - 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun UserDeviceCard(
    device: DeviceRegistrationManager.UserDevice,
    onRename: (String) -> Unit,
    onViewDetails: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (device.isCurrentDevice) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        } else {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
            )
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Device Icon
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(20.dp),
                color = if (device.isCurrentDevice) {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                } else {
                    MaterialTheme.colorScheme.primary
                }
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Smartphone,
                        contentDescription = "Device",
                        tint = if (device.isCurrentDevice) {
                            MaterialTheme.colorScheme.surface
                        } else {
                            MaterialTheme.colorScheme.onPrimary
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Device Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = device.deviceName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = if (device.isCurrentDevice) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (device.isCurrentDevice) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = "This Device",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                
                if (device.deviceModel.isNotEmpty()) {
                    Text(
                        text = device.deviceModel,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (device.isCurrentDevice) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // Menu Button
            Box {
                IconButton(
                    onClick = { showMenu = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Device options"
                    )
                }
                
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = {
                            showMenu = false
                            showRenameDialog = true
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null)
                        }
                    )
                    
                    if (!device.isCurrentDevice) {
                        DropdownMenuItem(
                            text = { Text("View Details") },
                            onClick = {
                                showMenu = false
                                onViewDetails()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Info, contentDescription = null)
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Rename Dialog
    if (showRenameDialog) {
        var newName by remember { mutableStateOf(device.deviceName) }
        
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Device") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Device Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newName.isNotBlank()) {
                            onRename(newName)
                            showRenameDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRenameDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ConnectedDevicesSection(
    devices: List<Device>
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Connected Devices",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            devices.forEach { device ->
                DeviceCard(device = device)
                if (device != devices.last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun AddDeviceCard(
    onAddDevice: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onAddDevice,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primary
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Device",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = "Add New Device",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// Keep existing composables below this point
