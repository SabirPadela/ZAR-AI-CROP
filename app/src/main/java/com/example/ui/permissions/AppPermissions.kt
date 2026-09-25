package com.example.ui.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

/**
 * Types of hardware/system permissions supported with tailored agricultural context.
 */
enum class AppPermissionType(
    val permission: String,
    val title: String,
    val rationaleMessage: String,
    val permanentlyDeniedMessage: String,
    val icon: ImageVector
) {
    CAMERA(
        permission = Manifest.permission.CAMERA,
        title = "Camera Access Required",
        rationaleMessage = "Zaria AI needs camera access to capture high-resolution photos of damaged crop leaves, pests, and symptoms for Gemini 3.1 Pro analysis.",
        permanentlyDeniedMessage = "Camera permission was permanently denied. Please enable Camera in Application Settings to take crop photos.",
        icon = Icons.Default.CameraAlt
    ),
    MICROPHONE(
        permission = Manifest.permission.RECORD_AUDIO,
        title = "Microphone Access Required",
        rationaleMessage = "Zaria AI uses the microphone to record your spoken crop questions and field notes in Urdu or English, transcribing them with Gemini 3.5 Transcribe.",
        permanentlyDeniedMessage = "Microphone permission was permanently denied. Please enable Microphone in Application Settings to record voice.",
        icon = Icons.Default.Mic
    )
}

/**
 * Utility to navigate user to application details in Android OS Settings.
 */
fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

/**
 * Encapsulates the UI state and invocation actions for a runtime permission request.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Stable
class PermissionHandler(
    val permissionType: AppPermissionType,
    val permissionState: PermissionState,
    private val onPermissionGrantedAction: () -> Unit = {},
    private val context: Context
) {
    var showRationaleDialog by mutableStateOf(false)
        private set

    val isGranted: Boolean
        get() = permissionState.status.isGranted

    val shouldShowRationale: Boolean
        get() = permissionState.status.shouldShowRationale

    /**
     * Executes the requested action if permission is already granted;
     * otherwise triggers accompanist permission request or displays rationale dialog.
     */
    fun checkAndRequest(onGranted: () -> Unit = onPermissionGrantedAction) {
        if (isGranted) {
            onGranted()
        } else if (shouldShowRationale) {
            showRationaleDialog = true
        } else {
            // First time request or permanently denied
            permissionState.launchPermissionRequest()
        }
    }

    fun dismissRationale() {
        showRationaleDialog = false
    }

    fun openSettings() {
        openAppSettings(context)
    }

    fun onRationaleConfirmed() {
        showRationaleDialog = false
        if (shouldShowRationale) {
            permissionState.launchPermissionRequest()
        } else {
            openSettings()
        }
    }
}

/**
 * Composable utility hook that produces a [PermissionHandler] for Camera or Microphone.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun rememberPermissionHandler(
    permissionType: AppPermissionType,
    onPermissionGranted: () -> Unit = {}
): PermissionHandler {
    val context = LocalContext.current
    val permissionState = rememberPermissionState(
        permission = permissionType.permission,
        onPermissionResult = { isGranted ->
            if (isGranted) {
                onPermissionGranted()
            }
        }
    )

    return remember(permissionType, permissionState, context) {
        PermissionHandler(
            permissionType = permissionType,
            permissionState = permissionState,
            onPermissionGrantedAction = onPermissionGranted,
            context = context
        )
    }
}

/**
 * Standard Dialog displaying rationale or settings navigation based on [PermissionHandler].
 */
@Composable
fun PermissionRationaleDialog(
    handler: PermissionHandler,
    onDismiss: () -> Unit = { handler.dismissRationale() }
) {
    if (!handler.showRationaleDialog) return

    val type = handler.permissionType
    val isPermanentlyDenied = !handler.shouldShowRationale && !handler.isGranted

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = type.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = type.title,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column {
                Text(
                    text = type.rationaleMessage,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isPermanentlyDenied) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = type.permanentlyDeniedMessage,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error,
                        lineHeight = 16.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    handler.onRationaleConfirmed()
                }
            ) {
                Text(if (handler.shouldShowRationale) "Grant Permission" else "Open Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Composable container that handles permission verification around protected content.
 */
@Composable
fun WithPermission(
    handler: PermissionHandler,
    content: @Composable (requestPermission: () -> Unit) -> Unit
) {
    content {
        handler.checkAndRequest()
    }

    if (handler.showRationaleDialog) {
        PermissionRationaleDialog(handler = handler)
    }
}

/**
 * Multiple permissions handler for screens requiring both Camera and Microphone.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun rememberAppMultiplePermissionsHandler(
    permissions: List<AppPermissionType> = listOf(AppPermissionType.CAMERA, AppPermissionType.MICROPHONE),
    onAllGranted: () -> Unit = {}
): MultiplePermissionsHandler {
    val context = LocalContext.current
    val accompanistState = rememberMultiplePermissionsState(
        permissions = permissions.map { it.permission },
        onPermissionsResult = { results ->
            if (results.values.all { it }) {
                onAllGranted()
            }
        }
    )

    return remember(permissions, accompanistState, context) {
        MultiplePermissionsHandler(
            permissionTypes = permissions,
            multiplePermissionsState = accompanistState,
            context = context
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Stable
class MultiplePermissionsHandler(
    val permissionTypes: List<AppPermissionType>,
    val multiplePermissionsState: MultiplePermissionsState,
    private val context: Context
) {
    var showDialog by mutableStateOf(false)
        private set

    val allPermissionsGranted: Boolean
        get() = multiplePermissionsState.allPermissionsGranted

    val shouldShowRationale: Boolean
        get() = multiplePermissionsState.shouldShowRationale

    fun checkAndRequest(onGranted: () -> Unit = {}) {
        if (allPermissionsGranted) {
            onGranted()
        } else if (shouldShowRationale) {
            showDialog = true
        } else {
            multiplePermissionsState.launchMultiplePermissionRequest()
        }
    }

    fun dismissDialog() {
        showDialog = false
    }

    fun onConfirmed() {
        showDialog = false
        if (shouldShowRationale) {
            multiplePermissionsState.launchMultiplePermissionRequest()
        } else {
            openAppSettings(context)
        }
    }
}

// -------------------------------------------------------------
// Compatibility aliases for existing screens
// -------------------------------------------------------------

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraPermissionRationaleDialog(
    permissionState: PermissionState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val shouldShowRationale = permissionState.status.shouldShowRationale

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Camera Permission Required",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column {
                Text(
                    text = "Zaria AI needs camera access to capture high-resolution photos of damaged crop leaves, pests, and symptoms for Gemini 3.1 Pro analysis.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                if (!shouldShowRationale) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Permission was permanently denied. Please enable Camera in Application Settings to take photos.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    if (shouldShowRationale) {
                        permissionState.launchPermissionRequest()
                    } else {
                        openAppSettings(context)
                    }
                }
            ) {
                Text(if (shouldShowRationale) "Grant Permission" else "Open Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AudioPermissionRationaleDialog(
    permissionState: PermissionState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val shouldShowRationale = permissionState.status.shouldShowRationale

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
        },
        title = {
            Text(
                text = "Microphone Permission Required",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column {
                Text(
                    text = "Zaria AI uses the microphone to record your spoken crop questions and field notes in Urdu or English, transcribing them with Gemini 3.5 Transcribe.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                if (!shouldShowRationale) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Permission was permanently denied. Please enable Microphone in Application Settings to record voice.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    if (shouldShowRationale) {
                        permissionState.launchPermissionRequest()
                    } else {
                        openAppSettings(context)
                    }
                }
            ) {
                Text(if (shouldShowRationale) "Grant Permission" else "Open Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
