package com.example.easydrive

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide

class MainActivity : AppCompatActivity() {

    private lateinit var carsContainer: LinearLayout
    private lateinit var tvEmptyState: TextView
    private lateinit var etSearch: EditText
    private var allCars: List<Car> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                scrim = Color.TRANSPARENT,
                darkScrim = Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                scrim = Color.TRANSPARENT,
                darkScrim = Color.TRANSPARENT
            )
        )

        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.headerSection)) { view, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.setPadding(view.paddingLeft, statusBarHeight + 16, view.paddingRight, view.paddingBottom)
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.bottomNavigation)) { view, insets ->
            val navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, navBarHeight + 8)
            insets
        }

        initializeViews()
        setupClickListeners()
        loadCars()
    }

    private fun initializeViews() {
        carsContainer = findViewById(R.id.carsContainer)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        etSearch = findViewById(R.id.etSearch)
    }

    private fun setupClickListeners() {
        findViewById<ImageView>(R.id.ivNotification).setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }

        // Search functionality
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterCars(s.toString().trim())
            }
        })

        // Filter button
        findViewById<ImageView>(R.id.ivFilter).setOnClickListener {
            showFilterDialog()
        }

        // Bottom navigation
        findViewById<android.view.View>(R.id.navHome).setOnClickListener {
            etSearch.setText("")
            displayCars(allCars)
        }
        findViewById<android.view.View>(R.id.navSearch).setOnClickListener {
            startActivity(Intent(this, MessagesActivity::class.java))
        }
        findViewById<android.view.View>(R.id.navHistory).setOnClickListener {
            startActivity(Intent(this, MyBookingsActivity::class.java))
        }
        findViewById<android.view.View>(R.id.navProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    private fun filterCars(query: String) {
        if (query.isEmpty()) {
            displayCars(allCars)
            return
        }
        val filtered = allCars.filter { car ->
            car.brand.contains(query, ignoreCase = true) ||
            car.model.contains(query, ignoreCase = true) ||
            car.location.contains(query, ignoreCase = true) ||
            car.fuelType.contains(query, ignoreCase = true) ||
            car.transmission.contains(query, ignoreCase = true)
        }
        displayCars(filtered)
        if (filtered.isEmpty()) {
            tvEmptyState.text = "No cars found for \"$query\""
            tvEmptyState.visibility = View.VISIBLE
            carsContainer.visibility = View.GONE
        }
    }

    private fun showFilterDialog() {
        val options = arrayOf("All", "Petrol", "Diesel", "Electric", "Automatic", "Manual")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Filter Cars")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> displayCars(allCars)
                    else -> {
                        val filter = options[which]
                        val filtered = allCars.filter { car ->
                            car.fuelType.equals(filter, ignoreCase = true) ||
                            car.transmission.equals(filter, ignoreCase = true)
                        }
                        displayCars(filtered)
                        if (filtered.isEmpty()) {
                            tvEmptyState.text = "No $filter cars available"
                            tvEmptyState.visibility = View.VISIBLE
                            carsContainer.visibility = View.GONE
                        }
                    }
                }
            }
            .show()
    }

    private fun loadCars() {
        CarManager.getAllCars(this,
            onSuccess = { cars ->
                allCars = cars
                filterCars(etSearch.text.toString().trim())
            },
            onFailure = {
                allCars = CarManager.getAllCars(this)
                displayCars(allCars)
            }
        )
        allCars = CarManager.getAllCars(this)
        displayCars(allCars)
    }

    private fun displayCars(cars: List<Car>) {
        carsContainer.removeAllViews()
        if (cars.isEmpty()) {
            if (tvEmptyState.text.isEmpty()) tvEmptyState.text = "No cars available yet.\nCheck back later!"
            tvEmptyState.visibility = View.VISIBLE
            carsContainer.visibility = View.GONE
        } else {
            tvEmptyState.visibility = View.GONE
            carsContainer.visibility = View.VISIBLE
            for (car in cars) addCarView(car)
        }
    }

    private fun addCarView(car: Car) {
        val carView = layoutInflater.inflate(R.layout.item_car_renter, carsContainer, false)

        val ivCarImage = carView.findViewById<ImageView>(R.id.ivCarImage)
        val tvCarName = carView.findViewById<TextView>(R.id.tvCarName)
        val tvCarDetails = carView.findViewById<TextView>(R.id.tvCarDetails)
        val tvCarPrice = carView.findViewById<TextView>(R.id.tvCarPrice)
        val tvCarLocation = carView.findViewById<TextView>(R.id.tvCarLocation)
        val btnViewDetails = carView.findViewById<Button>(R.id.btnViewDetails)

        if (car.imageUri.isNotEmpty()) {
            try {
                val load: Any = if (car.imageUri.startsWith("http")) car.imageUri else Uri.parse(car.imageUri)
                Glide.with(this).load(load)
                    .placeholder(R.drawable.ic_menu).error(R.drawable.ic_menu)
                    .centerCrop().into(ivCarImage)
            } catch (e: Exception) {
                ivCarImage.setImageResource(R.drawable.ic_menu)
            }
        } else {
            ivCarImage.setImageResource(R.drawable.ic_menu)
        }

        tvCarName.text = "${car.brand} ${car.model}"
        tvCarDetails.text = "${car.year} • ${car.fuelType} • ${car.transmission} • ${car.seatingCapacity} Seats"
        tvCarPrice.text = "₱${car.pricePerDay}/day"
        tvCarLocation.text = car.location

        btnViewDetails.setOnClickListener { openCarDetails(car) }
        carsContainer.addView(carView)
    }

    private fun openCarDetails(car: Car) {
        val intent = Intent(this, CarDetailsActivity::class.java).apply {
            putExtra("carId", car.id)
            putExtra("brand", car.brand)
            putExtra("model", car.model)
            putExtra("year", car.year)
            putExtra("fuelType", car.fuelType)
            putExtra("transmission", car.transmission)
            putExtra("seatingCapacity", car.seatingCapacity)
            putExtra("pricePerDay", car.pricePerDay)
            putExtra("location", car.location)
            putExtra("description", car.description)
            putExtra("imageUri", car.imageUri)
            putExtra("agencyId", car.agencyId)
            putStringArrayListExtra("imageUris", ArrayList(car.imageUris.ifEmpty { listOf(car.imageUri) }))
        }
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        loadCars()
        // Show unread notification badge for renter
        val email = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email ?: ""
        if (email.isNotEmpty()) {
            NotificationManager.getUnreadCountForRenter(email) { count ->
                runOnUiThread {
                    val badge = findViewById<android.widget.TextView>(R.id.tvNotifBadge)
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
    }
}
