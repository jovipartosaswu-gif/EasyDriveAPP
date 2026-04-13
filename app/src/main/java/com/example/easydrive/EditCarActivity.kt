package com.example.easydrive

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class EditCarActivity : AppCompatActivity() {

    private lateinit var ivCarImage: ImageView
    private lateinit var btnSelectImage: Button
    private lateinit var etCarBrand: EditText
    private lateinit var etCarModel: EditText
    private lateinit var etYear: EditText
    private lateinit var spinnerFuelType: Spinner
    private lateinit var spinnerTransmission: Spinner
    private lateinit var etSeatingCapacity: EditText
    private lateinit var etPricePerDay: EditText
    private lateinit var etLocation: EditText
    private lateinit var etDescription: EditText
    private lateinit var btnUpdateCar: Button
    private lateinit var ivBack: ImageView

    private var carId: String? = null
    private var selectedImageUri: Uri? = null
    private var existingImageUri: String = ""
    private val storage = FirebaseStorage.getInstance()

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                // Take persistent URI permission
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    android.util.Log.e("EditCar", "Failed to take persistent permission", e)
                }
                
                selectedImageUri = uri
                Glide.with(this)
                    .load(uri)
                    .centerCrop()
                    .into(ivCarImage)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_car)

        initializeViews()
        setupSpinners()
        loadCarData()
        setupClickListeners()
    }

    private fun initializeViews() {
        ivCarImage = findViewById(R.id.ivCarImage)
        btnSelectImage = findViewById(R.id.btnSelectImage)
        etCarBrand = findViewById(R.id.etCarBrand)
        etCarModel = findViewById(R.id.etCarModel)
        etYear = findViewById(R.id.etYear)
        spinnerFuelType = findViewById(R.id.spinnerFuelType)
        spinnerTransmission = findViewById(R.id.spinnerTransmission)
        etSeatingCapacity = findViewById(R.id.etSeatingCapacity)
        etPricePerDay = findViewById(R.id.etPricePerDay)
        etLocation = findViewById(R.id.etLocation)
        etDescription = findViewById(R.id.etDescription)
        btnUpdateCar = findViewById(R.id.btnUpdateCar)
        ivBack = findViewById(R.id.ivBack)
    }

    private fun setupSpinners() {
        // Fuel Type Spinner
        val fuelTypes = arrayOf("Select Fuel Type", "Petrol", "Diesel", "Electric", "Hybrid")
        val fuelAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, fuelTypes)
        fuelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFuelType.adapter = fuelAdapter

        // Transmission Spinner
        val transmissions = arrayOf("Select Transmission", "Manual", "Automatic")
        val transmissionAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, transmissions)
        transmissionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTransmission.adapter = transmissionAdapter
    }

    private fun loadCarData() {
        carId = intent.getStringExtra("carId")
        val brand = intent.getStringExtra("brand")
        val model = intent.getStringExtra("model")
        val year = intent.getStringExtra("year")
        val fuelType = intent.getStringExtra("fuelType")
        val transmission = intent.getStringExtra("transmission")
        val seatingCapacity = intent.getIntExtra("seatingCapacity", 0)
        val pricePerDay = intent.getDoubleExtra("pricePerDay", 0.0)
        val location = intent.getStringExtra("location")
        val description = intent.getStringExtra("description")
        existingImageUri = intent.getStringExtra("imageUri") ?: ""

        etCarBrand.setText(brand)
        etCarModel.setText(model)
        etYear.setText(year)
        etSeatingCapacity.setText(seatingCapacity.toString())
        etPricePerDay.setText(pricePerDay.toString())
        etLocation.setText(location)
        etDescription.setText(description)

        // Load existing image from Firebase Storage URL or local URI
        if (existingImageUri.isNotEmpty()) {
            try {
                if (existingImageUri.startsWith("http")) {
                    // Firebase Storage URL
                    Glide.with(this)
                        .load(existingImageUri)
                        .placeholder(R.drawable.ic_menu)
                        .error(R.drawable.ic_menu)
                        .centerCrop()
                        .into(ivCarImage)
                } else {
                    // Local URI
                    Glide.with(this)
                        .load(Uri.parse(existingImageUri))
                        .placeholder(R.drawable.ic_menu)
                        .error(R.drawable.ic_menu)
                        .centerCrop()
                        .into(ivCarImage)
                }
            } catch (e: Exception) {
                ivCarImage.setImageResource(R.drawable.ic_menu)
            }
        } else {
            ivCarImage.setImageResource(R.drawable.ic_menu)
        }

        // Set spinner selections
        val fuelTypes = arrayOf("Select Fuel Type", "Petrol", "Diesel", "Electric", "Hybrid")
        val fuelIndex = fuelTypes.indexOf(fuelType)
        if (fuelIndex >= 0) spinnerFuelType.setSelection(fuelIndex)

        val transmissions = arrayOf("Select Transmission", "Manual", "Automatic")
        val transmissionIndex = transmissions.indexOf(transmission)
        if (transmissionIndex >= 0) spinnerTransmission.setSelection(transmissionIndex)
    }

    private fun setupClickListeners() {
        ivBack.setOnClickListener {
            finish()
        }

        btnSelectImage.setOnClickListener {
            openImagePicker()
        }

        btnUpdateCar.setOnClickListener {
            if (validateInputs()) {
                updateCar()
            }
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        imagePickerLauncher.launch(intent)
    }

    private fun validateInputs(): Boolean {
        val brand = etCarBrand.text.toString().trim()
        val model = etCarModel.text.toString().trim()
        val year = etYear.text.toString().trim()
        val fuelType = spinnerFuelType.selectedItemPosition
        val transmission = spinnerTransmission.selectedItemPosition
        val seatingCapacity = etSeatingCapacity.text.toString().trim()
        val pricePerDay = etPricePerDay.text.toString().trim()
        val location = etLocation.text.toString().trim()

        when {
            brand.isEmpty() -> {
                etCarBrand.error = "Please enter car brand"
                etCarBrand.requestFocus()
                return false
            }
            model.isEmpty() -> {
                etCarModel.error = "Please enter car model"
                etCarModel.requestFocus()
                return false
            }
            year.isEmpty() -> {
                etYear.error = "Please enter year"
                etYear.requestFocus()
                return false
            }
            year.toIntOrNull() == null || year.toInt() < 1900 || year.toInt() > 2030 -> {
                etYear.error = "Please enter valid year"
                etYear.requestFocus()
                return false
            }
            fuelType == 0 -> {
                Toast.makeText(this, "Please select fuel type", Toast.LENGTH_SHORT).show()
                return false
            }
            transmission == 0 -> {
                Toast.makeText(this, "Please select transmission type", Toast.LENGTH_SHORT).show()
                return false
            }
            seatingCapacity.isEmpty() -> {
                etSeatingCapacity.error = "Please enter seating capacity"
                etSeatingCapacity.requestFocus()
                return false
            }
            seatingCapacity.toIntOrNull() == null || seatingCapacity.toInt() < 2 || seatingCapacity.toInt() > 50 -> {
                etSeatingCapacity.error = "Please enter valid seating capacity (2-50)"
                etSeatingCapacity.requestFocus()
                return false
            }
            pricePerDay.isEmpty() -> {
                etPricePerDay.error = "Please enter price per day"
                etPricePerDay.requestFocus()
                return false
            }
            pricePerDay.toDoubleOrNull() == null || pricePerDay.toDouble() <= 0 -> {
                etPricePerDay.error = "Please enter valid price"
                etPricePerDay.requestFocus()
                return false
            }
            location.isEmpty() -> {
                etLocation.error = "Please enter location"
                etLocation.requestFocus()
                return false
            }
        }

        return true
    }

    private fun updateCar() {
        val brand = etCarBrand.text.toString().trim()
        val model = etCarModel.text.toString().trim()
        val year = etYear.text.toString().trim()
        val fuelType = spinnerFuelType.selectedItem.toString()
        val transmission = spinnerTransmission.selectedItem.toString()
        val seatingCapacity = etSeatingCapacity.text.toString().trim().toInt()
        val pricePerDay = etPricePerDay.text.toString().trim().toDouble()
        val location = etLocation.text.toString().trim()
        val description = etDescription.text.toString().trim()

        // Show loading state
        btnUpdateCar.isEnabled = false
        btnUpdateCar.text = "Updating..."

        // If a new image was selected, try to upload it
        if (selectedImageUri != null) {
            uploadImageToFirebase(selectedImageUri!!) { imageUrl ->
                if (imageUrl != null) {
                    // Successfully uploaded to Firebase
                    saveUpdatedCar(brand, model, year, fuelType, transmission, seatingCapacity, pricePerDay, location, description, imageUrl)
                } else {
                    // Firebase upload failed, use local URI as fallback
                    val localUri = selectedImageUri.toString()
                    saveUpdatedCar(brand, model, year, fuelType, transmission, seatingCapacity, pricePerDay, location, description, localUri)
                }
            }
        } else {
            // Use existing image
            saveUpdatedCar(brand, model, year, fuelType, transmission, seatingCapacity, pricePerDay, location, description, existingImageUri)
        }
    }

    private fun saveUpdatedCar(brand: String, model: String, year: String, fuelType: String, 
                               transmission: String, seatingCapacity: Int, pricePerDay: Double, 
                               location: String, description: String, imageUri: String) {
        val updatedCar = Car(
            id = carId ?: "",
            brand = brand,
            model = model,
            year = year,
            fuelType = fuelType,
            transmission = transmission,
            seatingCapacity = seatingCapacity,
            pricePerDay = pricePerDay,
            location = location,
            description = description,
            imageUri = imageUri
        )

        CarManager.updateCar(this, updatedCar,
            onSuccess = {
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Car updated successfully!\n$brand $model - ₱$pricePerDay/day",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            },
            onFailure = { e ->
                runOnUiThread {
                    btnUpdateCar.isEnabled = true
                    btnUpdateCar.text = "Update Car"
                    Toast.makeText(this, "Failed to update car: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun uploadImageToFirebase(imageUri: Uri, callback: (String?) -> Unit) {
        try {
            val fileName = "car_images/${UUID.randomUUID()}.jpg"
            val storageRef = storage.reference.child(fileName)

            storageRef.putFile(imageUri)
                .addOnSuccessListener { taskSnapshot ->
                    // Get download URL
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        callback(uri.toString())
                    }.addOnFailureListener {
                        // Failed to get download URL, return null
                        callback(null)
                    }
                }
                .addOnFailureListener {
                    // Upload failed, return null
                    callback(null)
                }
        } catch (e: Exception) {
            // Any exception, return null
            callback(null)
        }
    }
}
