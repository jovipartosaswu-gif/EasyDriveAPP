package com.example.easydrive

data class Car(
    val id: String,
    val brand: String,
    val model: String,
    val year: String,
    val fuelType: String,
    val transmission: String,
    val seatingCapacity: Int,
    val pricePerDay: Double,
    val location: String,
    val description: String,
    val imageUri: String = "",       // first/primary image (kept for backward compat)
    val imageUris: List<String> = emptyList(), // all images
    val agencyId: String = ""
)
