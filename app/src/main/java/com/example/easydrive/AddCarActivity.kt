package com.example.easydrive

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class AddCarActivity : AppCompatActivity() {

    private lateinit var imagesContainer: LinearLayout
    private lateinit var btnSelectImage: LinearLayout
    private lateinit var etCarBrand: EditText
    private lateinit var etCarModel: EditText
    private lateinit var etYear: EditText
    private lateinit var spinnerFuelType: Spinner
    private lateinit var spinnerTransmission: Spinner
    private lateinit var etSeatingCapacity: EditText
    private lateinit var etPricePerDay: EditText
    private lateinit var etLocation: EditText
    private lateinit var etDescription: EditText
    private lateinit var btnAddCar: Button
    private lateinit var ivBack: ImageView

    private val selectedImageUris = mutableListOf<Uri>()
    private val maxImages = 5
    private val storage = FirebaseStorage.getInstance()

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        val remaining = maxImages - selectedImageUris.size
        val toAdd = uris.take(remaining)
        toAdd.forEach { uri ->
            try {
                contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) { /* ignore */ }
            selectedImageUris.add(uri)
        }
        if (uris.size > remaining) {
            Toast.makeText(this, "Max $maxImages images allowed", Toast.LENGTH_SHORT).show()
        }
        refreshImageStrip()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_car)

        initializeViews()
        setupSpinners()
        setupClickListeners()
    }

    private fun initializeViews() {
        imagesContainer = findViewById(R.id.imagesContainer)
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
        btnAddCar = findViewById(R.id.btnAddCar)
        ivBack = findViewById(R.id.ivBack)
    }

    private fun setupSpinners() {
        val fuelTypes = arrayOf("Select Fuel Type", "Petrol", "Diesel", "Electric", "Hybrid")
        spinnerFuelType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, fuelTypes)
            .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        val transmissions = arrayOf("Select Transmission", "Manual", "Automatic")
        spinnerTransmission.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, transmissions)
            .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
    }

    private fun setupClickListeners() {
        ivBack.setOnClickListener { finish() }
        btnSelectImage.setOnClickListener { openImagePicker() }
        btnAddCar.setOnClickListener { if (validateInputs()) addCar() }
    }

    private fun openImagePicker() {
        if (selectedImageUris.size >= maxImages) {
            Toast.makeText(this, "Max $maxImages images already selected", Toast.LENGTH_SHORT).show()
            return
        }
        imagePickerLauncher.launch("image/*")
    }

    private fun refreshImageStrip() {
        // Remove all views except the add button (first child)
        while (imagesContainer.childCount > 1) {
            imagesContainer.removeViewAt(1)
        }

        val size = resources.getDimensionPixelSize(android.R.dimen.app_icon_size) * 2 // ~100dp
        val margin = resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width) / 4

        selectedImageUris.forEachIndexed { index, uri ->
            val wrapper = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(size, size).also {
                    it.marginEnd = margin
                }
                orientation = LinearLayout.VERTICAL
            }

            val iv = com.google.android.material.imageview.ShapeableImageView(this).apply {
                layoutParams = ViewGroup.LayoutParams(size, size)
                scaleType = ImageView.ScaleType.CENTER_CROP
                shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                    .setAllCornerSizes(24f)
                    .build()
            }
            Glide.with(this).load(uri).centerCrop().into(iv)

            // Remove button overlay
            val removeBtn = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(28, 28).also {
                    it.topMargin = -size
                    it.marginStart = size - 32
                }
                setImageResource(R.drawable.ic_check)
                setColorFilter(android.graphics.Color.RED)
                setOnClickListener {
                    selectedImageUris.removeAt(index)
                    refreshImageStrip()
                }
            }

            wrapper.addView(iv)
            imagesContainer.addView(wrapper)

            // Tap to remove
            iv.setOnLongClickListener {
                selectedImageUris.removeAt(index)
                refreshImageStrip()
                true
            }
        }

        // Hide add button if max reached
        btnSelectImage.visibility = if (selectedImageUris.size >= maxImages)
            android.view.View.GONE else android.view.View.VISIBLE
    }

    private fun validateInputs(): Boolean {
        val brand = etCarBrand.text.toString().trim()
        val model = etCarModel.text.toString().trim()
        val year = etYear.text.toString().trim()
        val seatingCapacity = etSeatingCapacity.text.toString().trim()
        val pricePerDay = etPricePerDay.text.toString().trim()
        val location = etLocation.text.toString().trim()

        return when {
            brand.isEmpty() -> { etCarBrand.error = "Required"; etCarBrand.requestFocus(); false }
            model.isEmpty() -> { etCarModel.error = "Required"; etCarModel.requestFocus(); false }
            year.isEmpty() || year.toIntOrNull() == null -> { etYear.error = "Enter valid year"; etYear.requestFocus(); false }
            spinnerFuelType.selectedItemPosition == 0 -> { Toast.makeText(this, "Select fuel type", Toast.LENGTH_SHORT).show(); false }
            spinnerTransmission.selectedItemPosition == 0 -> { Toast.makeText(this, "Select transmission", Toast.LENGTH_SHORT).show(); false }
            seatingCapacity.isEmpty() -> { etSeatingCapacity.error = "Required"; etSeatingCapacity.requestFocus(); false }
            pricePerDay.isEmpty() || pricePerDay.toDoubleOrNull() == null -> { etPricePerDay.error = "Enter valid price"; etPricePerDay.requestFocus(); false }
            location.isEmpty() -> { etLocation.error = "Required"; etLocation.requestFocus(); false }
            else -> true
        }
    }

    private fun addCar() {
        btnAddCar.isEnabled = false
        btnAddCar.text = "Adding car..."

        if (selectedImageUris.isEmpty()) {
            saveCarWithImages(emptyList())
            return
        }

        // Upload all images
        val uploadedUrls = mutableListOf<String>()
        var completed = 0

        selectedImageUris.forEach { uri ->
            val fileName = "car_images/${UUID.randomUUID()}.jpg"
            storage.reference.child(fileName).putFile(uri)
                .continueWithTask { task ->
                    if (!task.isSuccessful) task.exception?.let { throw it }
                    storage.reference.child(fileName).downloadUrl
                }
                .addOnSuccessListener { downloadUri ->
                    uploadedUrls.add(downloadUri.toString())
                    completed++
                    if (completed == selectedImageUris.size) saveCarWithImages(uploadedUrls)
                }
                .addOnFailureListener {
                    // fallback to local URI
                    uploadedUrls.add(uri.toString())
                    completed++
                    if (completed == selectedImageUris.size) saveCarWithImages(uploadedUrls)
                }
        }
    }

    private fun saveCarWithImages(imageUrls: List<String>) {
        val newCar = Car(
            id = CarManager.generateCarId(),
            brand = etCarBrand.text.toString().trim(),
            model = etCarModel.text.toString().trim(),
            year = etYear.text.toString().trim(),
            fuelType = spinnerFuelType.selectedItem.toString(),
            transmission = spinnerTransmission.selectedItem.toString(),
            seatingCapacity = etSeatingCapacity.text.toString().trim().toInt(),
            pricePerDay = etPricePerDay.text.toString().trim().toDouble(),
            location = etLocation.text.toString().trim(),
            description = etDescription.text.toString().trim(),
            imageUri = imageUrls.firstOrNull() ?: "",
            imageUris = imageUrls
        )

        CarManager.saveCar(this, newCar,
            onSuccess = {
                runOnUiThread {
                    Toast.makeText(this, "Car added successfully!", Toast.LENGTH_LONG).show()
                    finish()
                }
            },
            onFailure = { e ->
                runOnUiThread {
                    btnAddCar.isEnabled = true
                    btnAddCar.text = "Add Car"
                    Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}
