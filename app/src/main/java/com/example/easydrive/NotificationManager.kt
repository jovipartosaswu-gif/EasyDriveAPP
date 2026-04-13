package com.example.easydrive

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore

object NotificationManager {
    private const val TAG = "NotificationManager"
    private val firestore = FirebaseFirestore.getInstance()

    // --- Agency notifications (new booking request) ---

    fun sendBookingNotification(
        agencyId: String,
        renterName: String,
        carName: String,
        pickupDate: String,
        returnDate: String,
        bookingId: String
    ) {
        val notification = hashMapOf(
            "agencyId" to agencyId,
            "title" to "New Booking Request",
            "message" to "$renterName booked $carName from $pickupDate to $returnDate",
            "bookingId" to bookingId,
            "isRead" to false,
            "timestamp" to System.currentTimeMillis()
        )
        firestore.collection("notifications").add(notification)
            .addOnSuccessListener { Log.d(TAG, "Agency notification sent") }
            .addOnFailureListener { e -> Log.e(TAG, "Failed", e) }
    }

    fun getNotificationsForAgency(
        agencyId: String,
        onSuccess: (List<AgencyNotification>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        firestore.collection("notifications")
            .whereEqualTo("agencyId", agencyId)
            .get()
            .addOnSuccessListener { docs ->
                val list = docs.map { doc ->
                    AgencyNotification(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        message = doc.getString("message") ?: "",
                        bookingId = doc.getString("bookingId") ?: "",
                        isRead = doc.getBoolean("isRead") ?: false,
                        timestamp = doc.getLong("timestamp") ?: 0L
                    )
                }.sortedByDescending { it.timestamp }
                onSuccess(list)
            }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun markAllAsRead(agencyId: String) {
        firestore.collection("notifications")
            .whereEqualTo("agencyId", agencyId)
            .whereEqualTo("isRead", false)
            .get()
            .addOnSuccessListener { docs ->
                val batch = firestore.batch()
                docs.forEach { batch.update(it.reference, "isRead", true) }
                batch.commit()
            }
    }

    fun getUnreadCount(agencyId: String, onResult: (Int) -> Unit) {
        firestore.collection("notifications")
            .whereEqualTo("agencyId", agencyId)
            .whereEqualTo("isRead", false)
            .get()
            .addOnSuccessListener { docs -> onResult(docs.size()) }
            .addOnFailureListener { onResult(0) }
    }

    // --- Renter notifications (booking status updates) ---

    fun sendStatusUpdateNotification(
        renterEmail: String,
        carName: String,
        newStatus: String,
        bookingId: String
    ) {
        val title = when (newStatus) {
            "Confirmed" -> "Booking Confirmed! 🎉"
            "Cancelled" -> "Booking Cancelled"
            "Completed" -> "Booking Completed"
            else -> "Booking Update"
        }
        val message = when (newStatus) {
            "Confirmed" -> "Your booking for $carName has been confirmed by the agency."
            "Cancelled" -> "Your booking for $carName has been cancelled by the agency."
            "Completed" -> "Your rental of $carName is now marked as completed."
            else -> "Your booking for $carName has been updated to $newStatus."
        }
        val notification = hashMapOf(
            "renterEmail" to renterEmail,
            "title" to title,
            "message" to message,
            "bookingId" to bookingId,
            "isRead" to false,
            "timestamp" to System.currentTimeMillis()
        )
        firestore.collection("renter_notifications").add(notification)
            .addOnSuccessListener { Log.d(TAG, "Renter notification sent") }
            .addOnFailureListener { e -> Log.e(TAG, "Failed", e) }
    }

    fun getNotificationsForRenter(
        renterEmail: String,
        onSuccess: (List<AgencyNotification>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        firestore.collection("renter_notifications")
            .whereEqualTo("renterEmail", renterEmail)
            .get()
            .addOnSuccessListener { docs ->
                val list = docs.map { doc ->
                    AgencyNotification(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        message = doc.getString("message") ?: "",
                        bookingId = doc.getString("bookingId") ?: "",
                        isRead = doc.getBoolean("isRead") ?: false,
                        timestamp = doc.getLong("timestamp") ?: 0L
                    )
                }.sortedByDescending { it.timestamp }
                onSuccess(list)
            }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun getUnreadCountForRenter(renterEmail: String, onResult: (Int) -> Unit) {
        firestore.collection("renter_notifications")
            .whereEqualTo("renterEmail", renterEmail)
            .whereEqualTo("isRead", false)
            .get()
            .addOnSuccessListener { docs -> onResult(docs.size()) }
            .addOnFailureListener { onResult(0) }
    }

    fun markRenterNotificationsAsRead(renterEmail: String) {
        firestore.collection("renter_notifications")
            .whereEqualTo("renterEmail", renterEmail)
            .whereEqualTo("isRead", false)
            .get()
            .addOnSuccessListener { docs ->
                val batch = firestore.batch()
                docs.forEach { batch.update(it.reference, "isRead", true) }
                batch.commit()
            }
    }
}

data class AgencyNotification(
    val id: String,
    val title: String,
    val message: String,
    val bookingId: String,
    val isRead: Boolean,
    val timestamp: Long
)
