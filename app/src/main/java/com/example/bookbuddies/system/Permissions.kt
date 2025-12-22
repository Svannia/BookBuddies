package com.example.bookbuddies.system

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.core.content.ContextCompat

/**
 * Checks for a specific permission.
 * If the permission is granted, follows up with action that needed permission.
 * If not, asks the user for permission.
 *
 * @param context to check for permission
 * @param permission permission name to be checked
 * @param launcher a permission launcher that is launched if permission needs to be requested
 * @param alreadyGranted block that runs if permission is granted
 */
fun checkPermission(
    context: Context,
    permission: String,
    launcher: ManagedActivityResultLauncher<String, Boolean>,
    alreadyGranted: () -> Unit
) {
    // if the permission is not granted, launch the permission request
    if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
        launcher.launch(permission)
        // if the permission is granted, run the block that needed the permission
    } else {
        alreadyGranted()
    }
}

/**
 * Fetches the permission name for media access, depending on system version.
 *
 * @return media access permission name
 */
fun imagePermissionVersion(): String {
    return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        // For older Android versions, use READ_EXTERNAL_STORAGE permission
        "android.permission.READ_EXTERNAL_STORAGE"
    } else {
        "android.permission.READ_MEDIA_IMAGES"
    }
}

const val cameraPermission = Manifest.permission.CAMERA
enum class CameraPermissionStatus {
    CHECKING,
    GRANTED,
    DENIED,
}