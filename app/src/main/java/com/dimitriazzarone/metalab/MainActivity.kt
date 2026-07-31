package com.dimitriazzarone.metalab

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.dimitriazzarone.metalab.ui.MetaLabApp
import com.dimitriazzarone.metalab.ui.PermissionStatus
import com.dimitriazzarone.metalab.ui.theme.MetaLabTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MetaLabTheme {
                val microphoneLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { granted ->
                    PermissionResultBus.onMicrophoneResult(granted)
                }
                val cameraLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { granted ->
                    PermissionResultBus.onCameraResult(granted)
                }
                val bothLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { results ->
                    PermissionResultBus.onBothResult(
                        microphoneGranted = results[Manifest.permission.RECORD_AUDIO] == true,
                        cameraGranted = results[Manifest.permission.CAMERA] == true
                    )
                }

                MetaLabApp(
                    activity = this,
                    requestMicrophone = {
                        microphoneLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    requestCamera = {
                        cameraLauncher.launch(Manifest.permission.CAMERA)
                    },
                    requestBoth = {
                        bothLauncher.launch(
                            arrayOf(
                                Manifest.permission.RECORD_AUDIO,
                                Manifest.permission.CAMERA
                            )
                        )
                    },
                    openAppSettings = { openApplicationSettings(this) }
                )
            }
        }
    }
}

internal object PermissionResultBus {
    var onMicrophoneResult: (Boolean) -> Unit = {}
    var onCameraResult: (Boolean) -> Unit = {}
    var onBothResult: (Boolean, Boolean) -> Unit = { _, _ -> }
}

internal fun resolvePermissionStatus(
    activity: Activity,
    permission: String,
    requestedBefore: Boolean
): PermissionStatus {
    if (ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED) {
        return PermissionStatus.GRANTED
    }
    if (!requestedBefore) return PermissionStatus.NOT_REQUESTED
    return if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
        PermissionStatus.DENIED
    } else {
        PermissionStatus.PERMANENTLY_DENIED
    }
}

private fun openApplicationSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
