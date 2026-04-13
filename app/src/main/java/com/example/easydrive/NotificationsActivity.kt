package com.example.easydrive

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationsActivity : AppCompatActivity() {

    private lateinit var ivBack: ImageView
    private lateinit var container: LinearLayout
    private lateinit var tvEmptyState: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        ivBack = findViewById(R.id.ivBack)
        container = findViewById(R.id.notificationsContainer)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        progressBar = findViewById(R.id.progressBar)

        ivBack.setOnClickListener { finish() }
        loadNotifications()
    }

    private fun loadNotifications() {
        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: return
        val userEmail = auth.currentUser?.email ?: ""

        progressBar.visibility = View.VISIBLE

        // Get userType from Firestore to be accurate
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                val userType = doc.getString("userType") ?: "Renter"
                if (userType == "Agency") {
                    loadAgencyNotifications(userId)
                } else {
                    loadRenterNotifications(userEmail)
                }
            }
            .addOnFailureListener {
                // Fallback to SharedPreferences
                val prefs = getSharedPreferences("EasyDrivePrefs", android.content.Context.MODE_PRIVATE)
                val userType = prefs.getString("userType", "Renter") ?: "Renter"
                if (userType == "Agency") loadAgencyNotifications(userId)
                else loadRenterNotifications(userEmail)
            }
    }

    private fun loadAgencyNotifications(userId: String) {
        NotificationManager.getNotificationsForAgency(userId,
            onSuccess = { notifications ->
                progressBar.visibility = View.GONE
                if (notifications.isEmpty()) {
                    tvEmptyState.visibility = View.VISIBLE
                } else {
                    tvEmptyState.visibility = View.GONE
                    notifications.forEach { addNotificationView(it) }
                    NotificationManager.markAllAsRead(userId)
                }
            },
            onFailure = {
                progressBar.visibility = View.GONE
                tvEmptyState.text = "Failed to load notifications."
                tvEmptyState.visibility = View.VISIBLE
            }
        )
    }

    private fun loadRenterNotifications(userEmail: String) {
        NotificationManager.getNotificationsForRenter(userEmail,
            onSuccess = { notifications ->
                progressBar.visibility = View.GONE
                if (notifications.isEmpty()) {
                    tvEmptyState.visibility = View.VISIBLE
                } else {
                    tvEmptyState.visibility = View.GONE
                    notifications.forEach { addNotificationView(it) }
                    NotificationManager.markRenterNotificationsAsRead(userEmail)
                }
            },
            onFailure = {
                progressBar.visibility = View.GONE
                tvEmptyState.text = "Failed to load notifications."
                tvEmptyState.visibility = View.VISIBLE
            }
        )
    }

    private fun addNotificationView(notification: AgencyNotification) {
        val view = layoutInflater.inflate(R.layout.item_notification, container, false)

        view.findViewById<TextView>(R.id.tvTitle).text = notification.title
        view.findViewById<TextView>(R.id.tvMessage).text = notification.message
        view.findViewById<TextView>(R.id.tvTime).text = formatTime(notification.timestamp)

        // Show unread dot for unread notifications
        if (!notification.isRead) {
            view.findViewById<View>(R.id.unreadDot).visibility = View.VISIBLE
            view.setBackgroundResource(R.drawable.bg_notification_unread)
            // Make text darker for readability on yellow background
            view.findViewById<TextView>(R.id.tvTitle).setTextColor(0xFF212121.toInt())
            view.findViewById<TextView>(R.id.tvMessage).setTextColor(0xFF424242.toInt())
            view.findViewById<TextView>(R.id.tvTime).setTextColor(0xFF616161.toInt())
        }

        container.addView(view)
    }

    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
