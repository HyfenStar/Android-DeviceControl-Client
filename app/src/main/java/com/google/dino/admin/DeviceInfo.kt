package com.google.dino.admin

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import java.io.File
import java.net.NetworkInterface
import java.text.SimpleDateFormat
import java.util.*

class DeviceInfo(private val context: Context) {

    @SuppressLint("HardwareIds")
    fun getAllDeviceInfo(): Map<String, Any> {
        val info = mutableMapOf<String, Any>()

        // Basic Device Info
        info["deviceId"] = getDeviceId()
        info["modelName"] = Build.MODEL
        info["androidv"] = Build.VERSION.RELEASE
        info["sdkV"] = Build.VERSION.SDK_INT.toString()
        info["brand"] = Build.BRAND
        info["manufacturer"] = Build.MANUFACTURER
        info["product"] = Build.PRODUCT
        info["hardware"] = Build.HARDWARE

        // Status Info
        info["status"] = true
        info["lastSeen"] = System.currentTimeMillis()
        info["joined"] = getCurrentDateTime()

        // Battery Info
        info["battery"] = getBatteryLevel()
        info["batteryStatus"] = getBatteryStatus()

        // Storage Info
        info["storage"] = getStorageInfo()
        info["totalStorage"] = getTotalStorage()
        info["freeStorage"] = getFreeStorage()

        // Network Info
        info["ip_address"] = getIPAddress()
        info["networkType"] = getNetworkType()
        info["wifiName"] = getWifiName()
        info["macAddress"] = getMacAddress()

        // System Info
        info["isRoot"] = isDeviceRooted()
        info["isSdCard"] = isSDCardPresent()
        info["cpuArch"] = Build.SUPPORTED_ABIS.joinToString(", ")
        info["screenResolution"] = getScreenResolution()
        info["locale"] = Locale.getDefault().toString()
        info["timezone"] = TimeZone.getDefault().id

        // Custom Details
        info["label"] = "Dino Admin Device"
        info["like"] = false
        info["custom_details"] = "nb"

        return info
    }

    @SuppressLint("HardwareIds")
    private fun getDeviceId(): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown_device"
    }

    private fun getBatteryLevel(): String {
        return try {
            val batteryIntent = context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level != -1 && scale != -1) {
                "${(level * 100 / scale.toFloat()).toInt()}%"
            } else {
                "Unknown"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun getBatteryStatus(): String {
        return try {
            val batteryIntent = context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            when (status) {
                BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
                BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
                BatteryManager.BATTERY_STATUS_FULL -> "Full"
                BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
                else -> "Unknown"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun getStorageInfo(): String {
        val total = getTotalStorage()
        val free = getFreeStorage()
        return "${total - free}GB used of ${total}GB"
    }

    private fun getTotalStorage(): Long {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            (totalBlocks * blockSize) / (1024 * 1024 * 1024) // Convert to GB
        } catch (e: Exception) {
            0L
        }
    }

    private fun getFreeStorage(): Long {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val blockSize = stat.blockSizeLong
            val availableBlocks = stat.availableBlocksLong
            (availableBlocks * blockSize) / (1024 * 1024 * 1024) // Convert to GB
        } catch (e: Exception) {
            0L
        }
    }

    private fun getIPAddress(): String {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo = wifiManager.connectionInfo
            val ip = wifiInfo.ipAddress
            "${(ip and 0xFF)}.${(ip shr 8 and 0xFF)}.${(ip shr 16 and 0xFF)}.${(ip shr 24 and 0xFF)}"
        } catch (e: Exception) {
            try {
                val interfaces = NetworkInterface.getNetworkInterfaces()
                while (interfaces.hasMoreElements()) {
                    val networkInterface = interfaces.nextElement()
                    val addresses = networkInterface.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val address = addresses.nextElement()
                        if (!address.isLoopbackAddress && address.hostAddress.indexOf(':') < 0) {
                            return address.hostAddress
                        }
                    }
                }
            } catch (ex: Exception) {
                // Ignore
            }
            "Unknown"
        }
    }

    private fun getNetworkType(): String {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo = connectivityManager.activeNetworkInfo
            when (networkInfo?.type) {
                ConnectivityManager.TYPE_WIFI -> "WiFi"
                ConnectivityManager.TYPE_MOBILE -> "Mobile"
                ConnectivityManager.TYPE_ETHERNET -> "Ethernet"
                ConnectivityManager.TYPE_VPN -> "VPN"
                else -> "Unknown"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun getWifiName(): String {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo = wifiManager.connectionInfo
            wifiInfo.ssid.removeSurrounding("\"")
        } catch (e: Exception) {
            "Unknown"
        }
    }

    @SuppressLint("HardwareIds")
    private fun getMacAddress(): String {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo = wifiManager.connectionInfo
            wifiInfo.macAddress ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun isDeviceRooted(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su"
        )

        return paths.any { File(it).exists() } || try {
            Runtime.getRuntime().exec("su")
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun isSDCardPresent(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }

    private fun getScreenResolution(): String {
        val displayMetrics = context.resources.displayMetrics
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels
        val density = displayMetrics.density
        return "${width}x${height} (${density}dpi)"
    }

    private fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy | hh:mm a", Locale.getDefault())
        return sdf.format(Date())
    }

    fun getSimpleDeviceInfo(): Map<String, String> {
        return mapOf(
            "Device ID" to getDeviceId(),
            "Model" to Build.MODEL,
            "Android Version" to Build.VERSION.RELEASE,
            "Battery" to getBatteryLevel(),
            "Storage" to getStorageInfo(),
            "IP Address" to getIPAddress(),
            "Network" to getNetworkType(),
            "Rooted" to if (isDeviceRooted()) "Yes" else "No"
        )
    }
}
