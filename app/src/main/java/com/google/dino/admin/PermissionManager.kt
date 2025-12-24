package com.google.dino.admin

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionManager(private val context: Context) {

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
        
        // Permission groups based on features
        val SMS_PERMISSIONS = arrayOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS
        )

        val CONTACT_PERMISSIONS = arrayOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS
        )

        val STORAGE_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }

        val PHONE_PERMISSIONS = arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.CALL_PHONE
        )

        val LOCATION_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val AUDIO_PERMISSIONS = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.MODIFY_AUDIO_SETTINGS
        )

        val CAMERA_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA
        )

        val NOTIFICATION_PERMISSION = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            emptyArray()
        }

        // All permissions combined
        val ALL_PERMISSIONS = SMS_PERMISSIONS +
                CONTACT_PERMISSIONS +
                STORAGE_PERMISSIONS +
                PHONE_PERMISSIONS +
                AUDIO_PERMISSIONS +
                CAMERA_PERMISSIONS +
                NOTIFICATION_PERMISSION
    }

    data class PermissionResult(
        val granted: List<String>,
        val denied: List<String>,
        val permanentlyDenied: List<String>
    )

    fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == 
               PackageManager.PERMISSION_GRANTED
    }

    fun checkPermissions(permissions: Array<String>): PermissionResult {
        val granted = mutableListOf<String>()
        val denied = mutableListOf<String>()
        val permanentlyDenied = mutableListOf<String>()

        for (permission in permissions) {
            when {
                checkPermission(permission) -> granted.add(permission)
                ActivityCompat.shouldShowRequestPermissionRationale(
                    context as Activity, permission
                ) -> denied.add(permission)
                else -> permanentlyDenied.add(permission)
            }
        }

        return PermissionResult(granted, denied, permanentlyDenied)
    }

    fun requestPermissions(activity: Activity, permissions: Array<String>) {
        ActivityCompat.requestPermissions(
            activity,
            permissions,
            PERMISSION_REQUEST_CODE
        )
    }

    fun getPermissionDescription(permission: String): String {
        return when (permission) {
            Manifest.permission.READ_SMS -> "Read SMS messages"
            Manifest.permission.SEND_SMS -> "Send SMS messages"
            Manifest.permission.READ_CONTACTS -> "Access contacts"
            Manifest.permission.READ_EXTERNAL_STORAGE -> "Access files"
            Manifest.permission.READ_MEDIA_IMAGES -> "Access photos"
            Manifest.permission.RECORD_AUDIO -> "Record audio"
            Manifest.permission.CAMERA -> "Use camera"
            Manifest.permission.READ_CALL_LOG -> "Read call history"
            Manifest.permission.READ_PHONE_STATE -> "Read phone status"
            Manifest.permission.ACCESS_FINE_LOCATION -> "Access precise location"
            Manifest.permission.POST_NOTIFICATIONS -> "Show notifications"
            else -> "Unknown permission"
        }
    }

    fun getFeatureForPermission(permission: String): String {
        return when (permission) {
            in SMS_PERMISSIONS -> "SMS Features"
            in CONTACT_PERMISSIONS -> "Contact Management"
            in STORAGE_PERMISSIONS -> "File Access"
            in PHONE_PERMISSIONS -> "Phone Features"
            in LOCATION_PERMISSIONS -> "Location Services"
            in AUDIO_PERMISSIONS -> "Audio Recording"
            in CAMERA_PERMISSIONS -> "Camera Access"
            in NOTIFICATION_PERMISSION -> "Notifications"
            else -> "Other Features"
        }
    }

    fun areAllPermissionsGranted(permissions: Array<String>): Boolean {
        return permissions.all { checkPermission(it) }
    }

    fun getMissingPermissions(permissions: Array<String>): List<String> {
        return permissions.filter { !checkPermission(it) }
    }

    fun getRequiredPermissionsForFeature(feature: String): Array<String> {
        return when (feature.toLowerCase()) {
            "sms" -> SMS_PERMISSIONS
            "contacts" -> CONTACT_PERMISSIONS
            "storage", "gallery", "files" -> STORAGE_PERMISSIONS
            "calls", "phone" -> PHONE_PERMISSIONS
            "location" -> LOCATION_PERMISSIONS
            "audio", "record" -> AUDIO_PERMISSIONS
            "camera", "photo" -> CAMERA_PERMISSIONS
            "notifications" -> NOTIFICATION_PERMISSION
            else -> emptyArray()
        }
    }
}
