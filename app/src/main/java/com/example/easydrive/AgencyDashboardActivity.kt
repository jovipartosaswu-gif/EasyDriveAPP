package com.example.easydrive

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AgencyDashboardActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agency_dashboard)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        loadAgencyData()

        // Notification icon click
        findViewById<ImageView>(R.id.ivNotification)?.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }

        // Bottom nav
        findViewById<android.view.View>(R.id.agencyNavHome)?.setOnClickListener {
            // already on home, do nothing
        }
        findViewById<android.view.View>(R.id.agencyNavMessage)?.setOnClickListener {
            startActivity(Intent(this, MessagesActivity::class.java))
        }
        findViewById<android.view.View>(R.id.agencyNavProfile)?.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        // Add New Car button click
        findViewById<LinearLayout>(R.id.btnAddCar)?.setOnClickListener {
            startActivity(Intent(this, AddCarActivity::class.java))
        }

        // Manage Cars button click
        findViewById<LinearLayout>(R.id.btnManageCars)?.setOnClickListener {
            startActivity(Intent(this, ManageCarsActivity::class.java))
        }

        // View Bookings button click
        findViewById<LinearLayout>(R.id.btnViewBookings)?.setOnClickListener {
            startActivity(Intent(this, ViewBookingsActivity::class.java))
        }

        // Reports button click
        findViewById<LinearLayout>(R.id.btnReports)?.setOnClickListener {
            Toast.makeText(this, "Reports & Analytics - Coming Soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadAgencyData() {
        val sharedPreferences = getSharedPreferences("EasyDrivePrefs", MODE_PRIVATE)
        val useLocalStorage = sharedPreferences.getBoolean("useLocalStorage", false)

        if (useLocalStorage) {
            val firstName = sharedPreferences.getString("firstName", "Agency") ?: "Agency"
            val lastName = sharedPreferences.getString("lastName", "Owner") ?: "Owner"
            findViewById<TextView>(R.id.tvWelcome)?.text = "Welcome, $firstName $lastName"
        } else {
            val userId = auth.currentUser?.uid
            if (userId != null) {
                firestore.collection("users").document(userId)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document != null && document.exists()) {
                            val firstName = document.getString("firstName") ?: "Agency"
                            val lastName = document.getString("lastName") ?: "Owner"
                            findViewById<TextView>(R.id.tvWelcome)?.text = "Welcome, $firstName $lastName"
                        }
                    }
            }
        }

        // Load total cars count
        updateCarStats()
    }

    private fun updateCarStats() {
        val agencyId = auth.currentUser?.uid ?: return

        CarManager.getCarsForAgency(this,
            onSuccess = { cars ->
                findViewById<TextView>(R.id.tvTotalCars)?.text = cars.size.toString()
            },
            onFailure = {
                findViewById<TextView>(R.id.tvTotalCars)?.text = "0"
            }
        )

        BookingManager.getBookingsForAgency(agencyId,
            onSuccess = { bookings ->
                val active = bookings.count { it.status == "Pending" || it.status == "Confirmed" }
                findViewById<TextView>(R.id.tvActiveRentals)?.text = active.toString()
            },
            onFailure = {
                findViewById<TextView>(R.id.tvActiveRentals)?.text = "0"
            }
        )

        // Update notification badge
        NotificationManager.getUnreadCount(agencyId) { count ->
            runOnUiThread {
                val badge = findViewById<TextView>(R.id.tvNotifBadge)
                if (count > 0) {
                    badge?.text = if (count > 9) "9+" else count.toString()
                    badge?.setBackgroundResource(R.drawable.bg_notif_badge)
                    badge?.setTextColor(android.graphics.Color.WHITE)
                    badge?.visibility = android.view.View.VISIBLE
                } else {
                    badge?.visibility = android.view.View.GONE
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh car stats when returning to this activity
        updateCarStats()
    }
}
