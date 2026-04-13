package com.example.easydrive

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

object CarManager {
    private const val PREFS_NAME = "EasyDriveCarPrefs"
    private const val KEY_CARS = "cars"
    private const val TAG = "CarManager"
    
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(Car::class.java, CarDeserializer())
        .create()
    
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveCar(context: Context, car: Car, onSuccess: (() -> Unit)? = null, onFailure: ((Exception) -> Unit)? = null) {
        // If using local URI, take persistent permission
        if (car.imageUri.isNotEmpty() && !car.imageUri.startsWith("http")) {
            try {
                val uri = Uri.parse(car.imageUri)
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to take persistent URI permission", e)
            }
        }
        
        // Save to Firestore
        val carData = hashMapOf(
            "id" to car.id,
            "brand" to car.brand,
            "model" to car.model,
            "year" to car.year,
            "fuelType" to car.fuelType,
            "transmission" to car.transmission,
            "seatingCapacity" to car.seatingCapacity,
            "pricePerDay" to car.pricePerDay,
            "location" to car.location,
            "description" to car.description,
            "imageUri" to car.imageUri,
            "imageUris" to car.imageUris,
            "agencyId" to (auth.currentUser?.uid ?: ""),
            "timestamp" to System.currentTimeMillis()
        )
        
        firestore.collection("cars").document(car.id)
            .set(carData)
            .addOnSuccessListener {
                Log.d(TAG, "Car saved to Firestore: ${car.id}")
                // Also save to local storage as backup
                saveToLocalStorage(context, car)
                onSuccess?.invoke()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to save car to Firestore", e)
                // Fallback to local storage
                saveToLocalStorage(context, car)
                onSuccess?.invoke() // Still call success since we saved locally
            }
    }
    
    private fun saveToLocalStorage(context: Context, car: Car) {
        val cars = getAllCarsFromLocal(context).toMutableList()
        cars.removeAll { it.id == car.id } // Remove if exists
        cars.add(car)
        saveCarsToLocal(context, cars)
    }

    fun updateCar(context: Context, updatedCar: Car, onSuccess: (() -> Unit)? = null, onFailure: ((Exception) -> Unit)? = null) {
        // If using local URI, take persistent permission
        if (updatedCar.imageUri.isNotEmpty() && !updatedCar.imageUri.startsWith("http")) {
            try {
                val uri = Uri.parse(updatedCar.imageUri)
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to take persistent URI permission", e)
            }
        }
        
        val carData = hashMapOf(
            "id" to updatedCar.id,
            "brand" to updatedCar.brand,
            "model" to updatedCar.model,
            "year" to updatedCar.year,
            "fuelType" to updatedCar.fuelType,
            "transmission" to updatedCar.transmission,
            "seatingCapacity" to updatedCar.seatingCapacity,
            "pricePerDay" to updatedCar.pricePerDay,
            "location" to updatedCar.location,
            "description" to updatedCar.description,
            "imageUri" to updatedCar.imageUri,
            "agencyId" to (auth.currentUser?.uid ?: ""),
            "timestamp" to System.currentTimeMillis()
        )
        
        firestore.collection("cars").document(updatedCar.id)
            .set(carData)
            .addOnSuccessListener {
                Log.d(TAG, "Car updated in Firestore: ${updatedCar.id}")
                updateInLocalStorage(context, updatedCar)
                onSuccess?.invoke()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to update car in Firestore", e)
                updateInLocalStorage(context, updatedCar)
                onSuccess?.invoke() // Still call success since we saved locally
            }
    }
    
    private fun updateInLocalStorage(context: Context, updatedCar: Car) {
        val cars = getAllCarsFromLocal(context).toMutableList()
        val index = cars.indexOfFirst { it.id == updatedCar.id }
        if (index != -1) {
            cars[index] = updatedCar
            saveCarsToLocal(context, cars)
        }
    }

    fun deleteCar(context: Context, carId: String, onSuccess: (() -> Unit)? = null, onFailure: ((Exception) -> Unit)? = null) {
        firestore.collection("cars").document(carId)
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "Car deleted from Firestore: $carId")
                deleteFromLocalStorage(context, carId)
                onSuccess?.invoke()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to delete car from Firestore", e)
                deleteFromLocalStorage(context, carId)
                onFailure?.invoke(e)
            }
    }
    
    private fun deleteFromLocalStorage(context: Context, carId: String) {
        val cars = getAllCarsFromLocal(context).toMutableList()
        cars.removeAll { it.id == carId }
        saveCarsToLocal(context, cars)
    }

    fun getAllCars(context: Context, onSuccess: ((List<Car>) -> Unit)? = null, onFailure: ((Exception) -> Unit)? = null): List<Car> {
        // Fetch ALL cars from Firestore (for renters to browse)
        firestore.collection("cars")
            .get()
            .addOnSuccessListener { documents ->
                val cars = parseCars(documents)
                Log.d(TAG, "Fetched ${cars.size} cars from Firestore")
                saveCarsToLocal(context, cars)
                onSuccess?.invoke(cars)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to fetch cars from Firestore", e)
                onFailure?.invoke(e)
            }
        return getAllCarsFromLocal(context)
    }

    fun getCarsForAgency(context: Context, onSuccess: (List<Car>) -> Unit, onFailure: (Exception) -> Unit) {
        val agencyId = auth.currentUser?.uid ?: return
        firestore.collection("cars")
            .whereEqualTo("agencyId", agencyId)
            .get()
            .addOnSuccessListener { documents ->
                val cars = parseCars(documents)
                Log.d(TAG, "Fetched ${cars.size} cars for agency $agencyId")
                onSuccess(cars)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to fetch agency cars", e)
                // Fallback: filter local cars by agencyId
                val local = getAllCarsFromLocal(context).filter { it.agencyId == agencyId }
                onSuccess(local)
            }
    }

    private fun parseCars(documents: com.google.firebase.firestore.QuerySnapshot): List<Car> {
        val cars = mutableListOf<Car>()
        for (document in documents) {
            try {
                cars.add(Car(
                    id = document.getString("id") ?: "",
                    brand = document.getString("brand") ?: "",
                    model = document.getString("model") ?: "",
                    year = document.getString("year") ?: "",
                    fuelType = document.getString("fuelType") ?: "",
                    transmission = document.getString("transmission") ?: "",
                    seatingCapacity = document.getLong("seatingCapacity")?.toInt() ?: 0,
                    pricePerDay = document.getDouble("pricePerDay") ?: 0.0,
                    location = document.getString("location") ?: "",
                    description = document.getString("description") ?: "",
                    imageUri = document.getString("imageUri") ?: "",
                    imageUris = (document.get("imageUris") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    agencyId = document.getString("agencyId") ?: ""
                ))
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing car document", e)
            }
        }
        return cars
    }
    
    private fun getAllCarsFromLocal(context: Context): List<Car> {
        val prefs = getPrefs(context)
        val carsJson = prefs.getString(KEY_CARS, null) ?: return emptyList()
        
        return try {
            val type = object : TypeToken<List<Car>>() {}.type
            gson.fromJson(carsJson, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing local cars", e)
            emptyList()
        }
    }

    fun getTotalCarsCount(context: Context): Int {
        return getAllCarsFromLocal(context).size
    }

    private fun saveCarsToLocal(context: Context, cars: List<Car>) {
        val prefs = getPrefs(context)
        val carsJson = gson.toJson(cars)
        prefs.edit().putString(KEY_CARS, carsJson).apply()
    }

    fun generateCarId(): String {
        return System.currentTimeMillis().toString()
    }
    
    // Custom deserializer to handle missing imageUri field
    private class CarDeserializer : JsonDeserializer<Car> {
        override fun deserialize(
            json: JsonElement,
            typeOfT: Type,
            context: JsonDeserializationContext
        ): Car {
            val jsonObject = json.asJsonObject
            
            return Car(
                id = jsonObject.get("id")?.asString ?: "",
                brand = jsonObject.get("brand")?.asString ?: "",
                model = jsonObject.get("model")?.asString ?: "",
                year = jsonObject.get("year")?.asString ?: "",
                fuelType = jsonObject.get("fuelType")?.asString ?: "",
                transmission = jsonObject.get("transmission")?.asString ?: "",
                seatingCapacity = jsonObject.get("seatingCapacity")?.asInt ?: 0,
                pricePerDay = jsonObject.get("pricePerDay")?.asDouble ?: 0.0,
                location = jsonObject.get("location")?.asString ?: "",
                description = jsonObject.get("description")?.asString ?: "",
                imageUri = jsonObject.get("imageUri")?.asString ?: ""
            )
        }
    }
}
