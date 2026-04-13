package com.example.easydrive

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SplashActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        
        // Set the regular theme after splash
        setTheme(R.style.Theme_EasyDrive)
        setContentView(R.layout.activity_splash)

        // Apply animations
        val logo = findViewById<ImageView>(R.id.ivLogo)
        val appName = findViewById<TextView>(R.id.tvAppName)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val loadingText = findViewById<TextView>(R.id.tvLoading)

        // Scale up animation for logo
        val scaleUp = AnimationUtils.loadAnimation(this, R.anim.scale_up)
        logo.startAnimation(scaleUp)

        // Slide up animation for app name
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        appName.startAnimation(slideUp)

        // Fade in animation for progress bar and loading text
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        progressBar.startAnimation(fadeIn)
        loadingText.startAnimation(fadeIn)

        // Check login status and navigate after 2.5 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            checkLoginStatus()
        }, 2500)
    }

    private fun checkLoginStatus() {
        val sharedPreferences = getSharedPreferences("EasyDrivePrefs", MODE_PRIVATE)
        val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)
        val useLocalStorage = sharedPreferences.getBoolean("useLocalStorage", false)
        
        // Check if user is logged in with Firebase
        val currentUser = auth.currentUser

        if (currentUser != null) {
            // User logged in with Firebase - check user type from Firestore
            val userId = currentUser.uid
            firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    val userType = document.getString("userType") ?: "Renter"
                    val intent = if (userType == "Agency") {
                        Intent(this, AgencyDashboardActivity::class.java)
                    } else {
                        Intent(this, MainActivity::class.java)
                    }
                    startActivity(intent)
                    finish()
                }
                .addOnFailureListener {
                    // Default to MainActivity if can't get user type
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
        } else if (isLoggedIn && useLocalStorage) {
            // User logged in with local storage - check user type from SharedPreferences
            val userType = sharedPreferences.getString("userType", "Renter") ?: "Renter"
            val intent = if (userType == "Agency") {
                Intent(this, AgencyDashboardActivity::class.java)
            } else {
                Intent(this, MainActivity::class.java)
            }
            startActivity(intent)
            finish()
        } else {
            // Not logged in - go to welcome screen
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
        }
    }
}
