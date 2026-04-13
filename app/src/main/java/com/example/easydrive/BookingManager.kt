package com.example.easydrive

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore

object BookingManager {
    private const val TAG = "BookingManager"
    private val firestore = FirebaseFirestore.getInstance()

    fun saveBooking(booking: Booking, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val data = hashMapOf(
            "id" to booking.id,
            "carId" to booking.carId,
            "carName" to booking.carName,
            "carLocation" to booking.carLocation,
            "carImageUri" to booking.carImageUri,
            "pricePerDay" to booking.pricePerDay,
            "totalPrice" to booking.totalPrice,
            "numDays" to booking.numDays,
            "pickupDate" to booking.pickupDate,
            "returnDate" to booking.returnDate,
            "renterName" to booking.renterName,
            "renterPhone" to booking.renterPhone,
            "renterEmail" to booking.renterEmail,
            "renterAddress" to booking.renterAddress,
            "licenseNumber" to booking.licenseNumber,
            "agencyId" to booking.agencyId,
            "status" to booking.status,
            "timestamp" to booking.timestamp
        )

        firestore.collection("bookings").document(booking.id)
            .set(data)
            .addOnSuccessListener {
                Log.d(TAG, "Booking saved: ${booking.id}")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to save booking", e)
                onFailure(e)
            }
    }

    fun getBookingsForAgency(
        agencyId: String,
        onSuccess: (List<Booking>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        Log.d(TAG, "Querying bookings for agencyId: $agencyId")
        firestore.collection("bookings")
            .whereEqualTo("agencyId", agencyId)
            .get()
            .addOnSuccessListener { documents ->
                Log.d(TAG, "Query returned ${documents.size()} documents")
                documents.forEach { Log.d(TAG, "Doc agencyId: ${it.getString("agencyId")}") }
                val bookings = documents.mapNotNull { doc ->
                    try {
                        Booking(
                            id = doc.getString("id") ?: "",
                            carId = doc.getString("carId") ?: "",
                            carName = doc.getString("carName") ?: "",
                            carLocation = doc.getString("carLocation") ?: "",
                            carImageUri = doc.getString("carImageUri") ?: "",
                            pricePerDay = doc.getDouble("pricePerDay") ?: 0.0,
                            totalPrice = doc.getDouble("totalPrice") ?: 0.0,
                            numDays = doc.getLong("numDays")?.toInt() ?: 0,
                            pickupDate = doc.getString("pickupDate") ?: "",
                            returnDate = doc.getString("returnDate") ?: "",
                            renterName = doc.getString("renterName") ?: "",
                            renterPhone = doc.getString("renterPhone") ?: "",
                            renterEmail = doc.getString("renterEmail") ?: "",
                            renterAddress = doc.getString("renterAddress") ?: "",
                            licenseNumber = doc.getString("licenseNumber") ?: "",
                            agencyId = doc.getString("agencyId") ?: "",
                            status = doc.getString("status") ?: "Pending",
                            timestamp = doc.getLong("timestamp") ?: 0L
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing booking", e)
                        null
                    }
                }.sortedByDescending { it.timestamp }
                onSuccess(bookings)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to fetch bookings", e)
                onFailure(e)
            }
    }

    fun updateBookingStatus(
        bookingId: String,
        status: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        firestore.collection("bookings").document(bookingId)
            .update("status", status)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun getBookingsForRenter(
        renterEmail: String,
        onSuccess: (List<Booking>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        firestore.collection("bookings")
            .whereEqualTo("renterEmail", renterEmail)
            .get()
            .addOnSuccessListener { documents ->
                val bookings = documents.mapNotNull { doc ->
                    try {
                        Booking(
                            id = doc.getString("id") ?: "",
                            carId = doc.getString("carId") ?: "",
                            carName = doc.getString("carName") ?: "",
                            carLocation = doc.getString("carLocation") ?: "",
                            carImageUri = doc.getString("carImageUri") ?: "",
                            pricePerDay = doc.getDouble("pricePerDay") ?: 0.0,
                            totalPrice = doc.getDouble("totalPrice") ?: 0.0,
                            numDays = doc.getLong("numDays")?.toInt() ?: 0,
                            pickupDate = doc.getString("pickupDate") ?: "",
                            returnDate = doc.getString("returnDate") ?: "",
                            renterName = doc.getString("renterName") ?: "",
                            renterPhone = doc.getString("renterPhone") ?: "",
                            renterEmail = doc.getString("renterEmail") ?: "",
                            renterAddress = doc.getString("renterAddress") ?: "",
                            licenseNumber = doc.getString("licenseNumber") ?: "",
                            agencyId = doc.getString("agencyId") ?: "",
                            status = doc.getString("status") ?: "Pending",
                            timestamp = doc.getLong("timestamp") ?: 0L
                        )
                    } catch (e: Exception) {
                        null
                    }
                }.sortedByDescending { it.timestamp }
                onSuccess(bookings)
            }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun generateBookingId(): String = "BK${System.currentTimeMillis()}"
}
