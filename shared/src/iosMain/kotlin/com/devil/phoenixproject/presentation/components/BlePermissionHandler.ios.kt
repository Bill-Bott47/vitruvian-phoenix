package com.devil.phoenixproject.presentation.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * State holder for BLE permission status on iOS.
 * Note: iOS requests BLE permission automatically when CoreBluetooth is first used.
 * This component provides UI guidance before the app attempts to use BLE.
 */
sealed class BlePermissionState {
    data object Granted : BlePermissionState()
    data object NotGranted : BlePermissionState()
    data object Denied : BlePermissionState()
}

/**
 * Composable that wraps content and provides BLE permission guidance for iOS.
 * On iOS, CoreBluetooth automatically requests permission when first used,
 * so this component shows informational screens and allows the app to proceed.
 * The actual permission dialog will appear when BLE scanning starts.
 *
 * @param content The composable content to show when ready to proceed
 */
@Composable
fun RequireBlePermissions(
    content: @Composable () -> Unit
) {
    // On iOS, we show guidance initially, then proceed
    // The actual permission will be requested by CoreBluetooth when scanning starts
    var permissionState by remember {
        mutableStateOf(BlePermissionState.NotGranted)
    }

    when (permissionState) {
        is BlePermissionState.Granted -> {
            content()
        }
        is BlePermissionState.NotGranted -> {
            // Wrap permission screens in a basic theme
            PermissionScreenTheme {
                BlePermissionRequestScreen(
                    onRequestPermission = {
                        // On iOS, permission is requested automatically when BLE is used
                        // So we just proceed - the system will show the dialog
                        permissionState = BlePermissionState.Granted
                    }
                )
            }
        }
        is BlePermissionState.Denied -> {
            PermissionScreenTheme {
                BlePermissionDeniedScreen(
                    onRetry = {
                        // Allow user to try again - permission will be requested when BLE is used
                        permissionState = BlePermissionState.Granted
                    }
                )
            }
        }
    }
}

/**
 * Simple theme wrapper for permission screens.
 */
@Composable
private fun PermissionScreenTheme(content: @Composable () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val colorScheme = if (isDark) darkColorScheme() else lightColorScheme()
    MaterialTheme(colorScheme = colorScheme, content = content)
}

/**
 * Screen shown when BLE permissions need to be requested.
 */
@Composable
private fun BlePermissionRequestScreen(
    onRequestPermission: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Bluetooth,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Bluetooth Permission Required",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Vitruvian Phoenix needs Bluetooth permission to scan for and connect to your Vitruvian Trainer machine. When you tap Continue, iOS will ask for permission.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onRequestPermission,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "Continue",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Screen shown when BLE permissions have been denied.
 */
@Composable
private fun BlePermissionDeniedScreen(
    onRetry: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Permission Denied",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Bluetooth permission is required to connect to your Vitruvian Trainer. Please enable Bluetooth permission in Settings > Vitruvian Phoenix > Bluetooth, then return to the app.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "Try Again",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "If Bluetooth is disabled in Settings, the app cannot connect to your Vitruvian Trainer.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

