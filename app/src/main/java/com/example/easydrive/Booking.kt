package com.example.easydrive

data class Booking(
    val id: String = "",
    val carId: String = "",
    val carName: String = "",
    val carLocation: String = "",
    val carImageUri: String = "",
    val pricePerDay: Double = 0.0,
    val totalPrice: Double = 0.0,
    val numDays: Int = 0,
    val pickupDate: String = "",
    val returnDate: String = "",
    val renterName: String = "",
    val renterPhone: String = "",
    val renterEmail: String = "",
    val renterAddress: String = "",
    val licenseNumber: String = "",
    val agencyId: String = "",
    val status: String = "Pending", // Pending, Confirmed, Completed, Cancelled
    val timestamp: Long = 0L
)
