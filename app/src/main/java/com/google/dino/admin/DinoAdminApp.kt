package com.google.dino.admin

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.content.ContextCompat

class DinoAdminApp : Application() {
    
    companion object {
        const val CHANNEL_ID = "dino_admin_channel"
        const val CHANNEL_NAME = "Dino Admin Service"
        const val CHANNEL_DESCRIPTION = "Background service for device management"
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initializeFirebase()
        startBackgroundService()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun initializeFirebase() {
        // Firebase initialization code
    }
    
    private fun startBackgroundService() {
        // Start CommandService if needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceIntent = Intent(this, CommandService::class.java)
            ContextCompat.startForegroundService(this, serviceIntent)
        }
    }
}
