package com.example.easydrive

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

class ManageCarsActivity : AppCompatActivity() {

    private lateinit var ivBack: ImageView
    private lateinit var carsContainer: LinearLayout
    private lateinit var tvEmptyState: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_cars)

        initializeViews()
        setupClickListeners()
        loadCars()
    }

    private fun initializeViews() {
        ivBack = findViewById(R.id.ivBack)
        carsContainer = findViewById(R.id.carsContainer)
        tvEmptyState = findViewById(R.id.tvEmptyState)
    }

    private fun setupClickListeners() {
        ivBack.setOnClickListener {
            finish()
        }
    }

    private fun loadCars() {
        try {
            CarManager.getCarsForAgency(this,
                onSuccess = { cars ->
                    displayCars(cars)
                },
                onFailure = { e ->
                    tvEmptyState.text = "Error loading cars. Please try again."
                    tvEmptyState.visibility = View.VISIBLE
                    carsContainer.visibility = View.GONE
                }
            )
        } catch (e: Exception) {
            e.printStackTrace()
            tvEmptyState.text = "Error loading cars. Please try again."
            tvEmptyState.visibility = View.VISIBLE
            carsContainer.visibility = View.GONE
        }
    }
    
    private fun displayCars(cars: List<Car>) {
        carsContainer.removeAllViews()

        if (cars.isEmpty()) {
            tvEmptyState.visibility = View.VISIBLE
            carsContainer.visibility = View.GONE
        } else {
            tvEmptyState.visibility = View.GONE
            carsContainer.visibility = View.VISIBLE

            for (car in cars) {
                addCarView(car)
            }
        }
    }

    private fun addCarView(car: Car) {
        try {
            val carView = layoutInflater.inflate(R.layout.item_manage_car, carsContainer, false)

            val ivCarImage = carView.findViewById<ImageView>(R.id.ivCarImage)
            val tvCarName = carView.findViewById<TextView>(R.id.tvCarName)
            val tvCarDetails = carView.findViewById<TextView>(R.id.tvCarDetails)
            val tvCarPrice = carView.findViewById<TextView>(R.id.tvCarPrice)
            val tvCarLocation = carView.findViewById<TextView>(R.id.tvCarLocation)
            val btnEdit = carView.findViewById<LinearLayout>(R.id.btnEdit)
            val btnDelete = carView.findViewById<LinearLayout>(R.id.btnDelete)

            // Load car image from Firebase Storage URL or local URI
            if (car.imageUri.isNotEmpty()) {
                try {
                    android.util.Log.d("ManageCars", "Loading image: ${car.imageUri}")
                    if (car.imageUri.startsWith("http")) {
                        // Firebase Storage URL
                        Glide.with(this)
                            .load(car.imageUri)
                            .placeholder(R.drawable.ic_menu)
                            .error(R.drawable.ic_menu)
                            .centerCrop()
                            .into(ivCarImage)
                    } else {
                        // Local URI
                        val uri = Uri.parse(car.imageUri)
                        android.util.Log.d("ManageCars", "Parsed URI: $uri")
                        Glide.with(this)
                            .load(uri)
                            .placeholder(R.drawable.ic_menu)
                            .error(R.drawable.ic_menu)
                            .centerCrop()
                            .into(ivCarImage)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ManageCars", "Error loading image", e)
                    ivCarImage.setImageResource(R.drawable.ic_menu)
                    ivCarImage.scaleType = ImageView.ScaleType.CENTER
                }
            } else {
                android.util.Log.d("ManageCars", "No image URI for car: ${car.brand} ${car.model}")
                ivCarImage.setImageResource(R.drawable.ic_menu)
                ivCarImage.scaleType = ImageView.ScaleType.CENTER
            }

            tvCarName.text = "${car.brand} ${car.model}"
            tvCarDetails.text = "${car.year} • ${car.fuelType} • ${car.transmission} • ${car.seatingCapacity} Seats"
            tvCarPrice.text = "₱${car.pricePerDay}/day"
            tvCarLocation.text = car.location

            btnEdit.setOnClickListener {
                editCar(car)
            }

            btnDelete.setOnClickListener {
                showDeleteConfirmation(car)
            }

            carsContainer.addView(carView)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error displaying car: ${car.brand} ${car.model}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun editCar(car: Car) {
        val intent = Intent(this, EditCarActivity::class.java)
        intent.putExtra("carId", car.id)
        intent.putExtra("brand", car.brand)
        intent.putExtra("model", car.model)
        intent.putExtra("year", car.year)
        intent.putExtra("fuelType", car.fuelType)
        intent.putExtra("transmission", car.transmission)
        intent.putExtra("seatingCapacity", car.seatingCapacity)
        intent.putExtra("pricePerDay", car.pricePerDay)
        intent.putExtra("location", car.location)
        intent.putExtra("description", car.description)
        intent.putExtra("imageUri", car.imageUri)
        startActivity(intent)
    }

    private fun showDeleteConfirmation(car: Car) {
        AlertDialog.Builder(this)
            .setTitle("Delete Car")
            .setMessage("Are you sure you want to delete ${car.brand} ${car.model}?")
            .setPositiveButton("Delete") { _, _ ->
                deleteCar(car)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteCar(car: Car) {
        CarManager.deleteCar(this, car.id)
        loadCars()
        Toast.makeText(this, "${car.brand} ${car.model} deleted successfully", Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        loadCars()
    }
}
