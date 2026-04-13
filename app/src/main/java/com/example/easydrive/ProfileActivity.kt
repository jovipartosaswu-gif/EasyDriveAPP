package com.example.easydrive

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.bumptech.glide.Glide

class ProfileActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        try {
            // Load and display user data
            loadUserData()

            findViewById<ImageView>(R.id.ivBack)?.setOnClickListener {
                finish()
            }

            // Edit Profile click listener
            findViewById<View>(R.id.tvEditProfile)?.setOnClickListener {
                val intent = Intent(this, EditProfileActivity::class.java)
                startActivity(intent)
            }

            // Setup menu items
            setupMenuItem(R.id.menuFavoriteCars, R.drawable.ic_favorite, "Favorite Cars")
            setupMenuItem(R.id.menuPreviousRent, R.drawable.ic_history, "My Bookings")
            setupMenuItem(R.id.menuNotification, R.drawable.ic_bell_outline, "Notification")
            setupMenuItem(R.id.menuConnected, R.drawable.ic_link, "Connected to QENT Partnerships")
            
            setupMenuItem(R.id.menuSettings, R.drawable.ic_settings, "Settings")
            setupMenuItem(R.id.menuLanguages, R.drawable.ic_language, "Languages")
            setupMenuItem(R.id.menuInviteFriends, R.drawable.ic_person_add, "Invite Friends")
            setupMenuItem(R.id.menuPrivacyPolicy, R.drawable.ic_privacy, "Privacy Policy")
            setupMenuItem(R.id.menuHelpSupport, R.drawable.ic_help, "Help & Support")
            setupMenuItem(R.id.menuLogout, R.drawable.ic_logout, "Log out")

            // My Bookings (Previous Rent)
            findViewById<View>(R.id.menuPreviousRent)?.setOnClickListener {
                startActivity(Intent(this, MyBookingsActivity::class.java))
            }

            // Notifications
            findViewById<View>(R.id.menuNotification)?.setOnClickListener {
                android.widget.Toast.makeText(this, "No new notifications", android.widget.Toast.LENGTH_SHORT).show()
            }

            // Favorite Cars
            findViewById<View>(R.id.menuFavoriteCars)?.setOnClickListener {
                android.widget.Toast.makeText(this, "Favorites coming soon", android.widget.Toast.LENGTH_SHORT).show()
            }

            // Help & Support
            findViewById<View>(R.id.menuHelpSupport)?.setOnClickListener {
                showHelpDialog()
            }

            // Privacy Policy
            findViewById<View>(R.id.menuPrivacyPolicy)?.setOnClickListener {
                android.widget.Toast.makeText(this, "Privacy Policy", android.widget.Toast.LENGTH_SHORT).show()
            }

            // Handle logout
            findViewById<View>(R.id.menuLogout)?.setOnClickListener {
                logout()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(this, "Error loading profile: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    private fun loadUserData() {
        try {
            val sharedPreferences = getSharedPreferences("EasyDrivePrefs", MODE_PRIVATE)
            val useLocalStorage = sharedPreferences.getBoolean("useLocalStorage", false)
            
            if (useLocalStorage) {
                // Load from SharedPreferences
                loadFromLocalStorage()
            } else {
                // Load from Firebase
                loadFromFirebase()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun loadFromLocalStorage() {
        val sharedPreferences = getSharedPreferences("EasyDrivePrefs", MODE_PRIVATE)
        val firstName = sharedPreferences.getString("firstName", "User") ?: "User"
        val lastName = sharedPreferences.getString("lastName", "Name") ?: "Name"
        val email = sharedPreferences.getString("email", "user@email.com") ?: "user@email.com"
        val profileImageUrl = sharedPreferences.getString("profileImageUrl", "") ?: ""
        
        findViewById<TextView>(R.id.tvUserName)?.text = "$firstName $lastName"
        findViewById<TextView>(R.id.tvUserEmail)?.text = email
        
        if (profileImageUrl.isNotEmpty()) {
            Glide.with(this)
                .load(profileImageUrl)
                .placeholder(R.drawable.ic_profile_placeholder)
                .into(findViewById(R.id.ivProfilePic))
        }
    }
    
    private fun loadFromFirebase() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }
        
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val firstName = document.getString("firstName") ?: "User"
                    val lastName = document.getString("lastName") ?: "Name"
                    val email = document.getString("email") ?: "user@email.com"
                    val profileImageUrl = document.getString("profileImageUrl") ?: ""
                    
                    findViewById<TextView>(R.id.tvUserName)?.text = "$firstName $lastName"
                    findViewById<TextView>(R.id.tvUserEmail)?.text = email
                    
                    if (profileImageUrl.isNotEmpty()) {
                        Glide.with(this)
                            .load(profileImageUrl)
                            .placeholder(R.drawable.ic_profile_placeholder)
                            .into(findViewById(R.id.ivProfilePic))
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onResume() {
        super.onResume()
        // Reload user data when returning from edit profile
        loadUserData()
    }

    private fun setupMenuItem(menuId: Int, iconRes: Int, title: String) {
        try {
            val menuItem = findViewById<View>(menuId)
            if (menuItem != null) {
                menuItem.findViewById<ImageView>(R.id.ivIcon)?.setImageResource(iconRes)
                menuItem.findViewById<TextView>(R.id.tvTitle)?.text = title
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun logout() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Log Out")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Log Out") { _, _ ->
                auth.signOut()
                val sharedPreferences = getSharedPreferences("EasyDrivePrefs", MODE_PRIVATE)
                sharedPreferences.edit().clear().apply()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showHelpDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Help & Support")
            .setMessage("For support, please contact us:\n\nEmail: support@easydrive.com\nPhone: +1 800 123 4567\n\nWe're available Mon–Fri, 9am–6pm.")
            .setPositiveButton("OK", null)
            .show()
    }
}
