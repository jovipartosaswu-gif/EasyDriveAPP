package com.example.easydrive

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class BookingActivity : AppCompatActivity() {

    private lateinit var ivBack: ImageView
    private lateinit var ivCarImage: ImageView
    private lateinit var tvCarName: TextView
    private lateinit var tvCarLocation: TextView
    private lateinit var tvCarPrice: TextView
    private lateinit var btnPickupDate: LinearLayout
    private lateinit var btnReturnDate: LinearLayout
    private lateinit var tvPickupDate: TextView
    private lateinit var tvReturnDate: TextView
    private lateinit var etFullName: EditText
    private lateinit var etPhone: EditText
    private lateinit var etEmail: EditText
    private lateinit var etAddress: EditText
    private lateinit var etLicense: EditText
    private lateinit var tvFullNameError: TextView
    private lateinit var tvPhoneError: TextView
    private lateinit var tvEmailError: TextView
    private lateinit var tvAddressError: TextView
    private lateinit var tvLicenseError: TextView
    private lateinit var tvPricePerDay: TextView
    private lateinit var tvNumDays: TextView
    private lateinit var tvTotalPrice: TextView
    private lateinit var btnConfirmBooking: Button

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private var pickupCalendar: Calendar? = null
    private var returnCalendar: Calendar? = null
    private var pricePerDay: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking)

        initViews()
        loadCarData()
        prefillUserData()
        setupClickListeners()
    }

    private fun initViews() {
        ivBack = findViewById(R.id.ivBack)
        ivCarImage = findViewById(R.id.ivCarImage)
        tvCarName = findViewById(R.id.tvCarName)
        tvCarLocation = findViewById(R.id.tvCarLocation)
        tvCarPrice = findViewById(R.id.tvCarPrice)
        btnPickupDate = findViewById(R.id.btnPickupDate)
        btnReturnDate = findViewById(R.id.btnReturnDate)
        tvPickupDate = findViewById(R.id.tvPickupDate)
        tvReturnDate = findViewById(R.id.tvReturnDate)
        etFullName = findViewById(R.id.etFullName)
        etPhone = findViewById(R.id.etPhone)
        etEmail = findViewById(R.id.etEmail)
        etAddress = findViewById(R.id.etAddress)
        etLicense = findViewById(R.id.etLicense)
        tvFullNameError = findViewById(R.id.tvFullNameError)
        tvPhoneError = findViewById(R.id.tvPhoneError)
        tvEmailError = findViewById(R.id.tvEmailError)
        tvAddressError = findViewById(R.id.tvAddressError)
        tvLicenseError = findViewById(R.id.tvLicenseError)
        tvPricePerDay = findViewById(R.id.tvPricePerDay)
        tvNumDays = findViewById(R.id.tvNumDays)
        tvTotalPrice = findViewById(R.id.tvTotalPrice)
        btnConfirmBooking = findViewById(R.id.btnConfirmBooking)
    }

    private fun loadCarData() {
        val brand = intent.getStringExtra("brand") ?: ""
        val model = intent.getStringExtra("model") ?: ""
        val location = intent.getStringExtra("location") ?: ""
        val imageUri = intent.getStringExtra("imageUri") ?: ""
        pricePerDay = intent.getDoubleExtra("pricePerDay", 0.0)

        tvCarName.text = "$brand $model"
        tvCarLocation.text = "📍 $location"
        tvCarPrice.text = "₱$pricePerDay/day"
        tvPricePerDay.text = "₱$pricePerDay"

        if (imageUri.isNotEmpty()) {
            try {
                val load = if (imageUri.startsWith("http")) imageUri else Uri.parse(imageUri)
                Glide.with(this).load(load)
                    .placeholder(R.drawable.ic_menu)
                    .error(R.drawable.ic_menu)
                    .centerCrop()
                    .into(ivCarImage)
            } catch (e: Exception) {
                ivCarImage.setImageResource(R.drawable.ic_menu)
            }
        }
    }

    private fun prefillUserData() {
        val prefs = getSharedPreferences("EasyDrivePrefs", MODE_PRIVATE)
        val firstName = prefs.getString("firstName", "") ?: ""
        val lastName = prefs.getString("lastName", "") ?: ""
        val email = prefs.getString("email", "") ?: ""
        val phone = prefs.getString("phone", "") ?: ""

        if (firstName.isNotEmpty() || lastName.isNotEmpty()) {
            etFullName.setText("$firstName $lastName".trim())
        }
        if (email.isNotEmpty()) etEmail.setText(email)
        if (phone.isNotEmpty()) etPhone.setText(phone)
    }

    private fun setupClickListeners() {
        ivBack.setOnClickListener { finish() }

        btnPickupDate.setOnClickListener { showDatePicker(isPickup = true) }
        btnReturnDate.setOnClickListener { showDatePicker(isPickup = false) }

        btnConfirmBooking.setOnClickListener {
            if (validateForm()) confirmBooking()
        }
    }

    private fun showDatePicker(isPickup: Boolean) {
        val cal = Calendar.getInstance()
        // Return date picker should start from pickup date if set
        if (!isPickup && pickupCalendar != null) {
            cal.time = pickupCalendar!!.time
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }

        DatePickerDialog(this, { _, year, month, day ->
            val selected = Calendar.getInstance()
            selected.set(year, month, day)

            if (isPickup) {
                pickupCalendar = selected
                tvPickupDate.text = dateFormat.format(selected.time)
                tvPickupDate.setTextColor(getColor(android.R.color.black))
                // Reset return date if it's before pickup
                if (returnCalendar != null && returnCalendar!!.before(selected)) {
                    returnCalendar = null
                    tvReturnDate.text = "Select date"
                    tvReturnDate.setTextColor(0xFF9E9E9E.toInt())
                }
            } else {
                returnCalendar = selected
                tvReturnDate.text = dateFormat.format(selected.time)
                tvReturnDate.setTextColor(getColor(android.R.color.black))
            }
            updatePriceSummary()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).also { dialog ->
            // Minimum date: today for pickup, day after pickup for return
            if (isPickup) {
                dialog.datePicker.minDate = System.currentTimeMillis()
            } else {
                val minDate = pickupCalendar?.let {
                    val next = it.clone() as Calendar
                    next.add(Calendar.DAY_OF_MONTH, 1)
                    next.timeInMillis
                } ?: System.currentTimeMillis()
                dialog.datePicker.minDate = minDate
            }
            dialog.show()
        }
    }

    private fun updatePriceSummary() {
        if (pickupCalendar != null && returnCalendar != null) {
            val diff = returnCalendar!!.timeInMillis - pickupCalendar!!.timeInMillis
            val days = TimeUnit.MILLISECONDS.toDays(diff).toInt()
            val total = days * pricePerDay
            tvNumDays.text = "$days day${if (days > 1) "s" else ""}"
            tvTotalPrice.text = "₱$total"
        } else {
            tvNumDays.text = "—"
            tvTotalPrice.text = "—"
        }
    }

    private fun validateForm(): Boolean {
        var valid = true

        // Dates
        if (pickupCalendar == null) {
            Toast.makeText(this, "Please select a pick-up date", Toast.LENGTH_SHORT).show()
            valid = false
        } else if (returnCalendar == null) {
            Toast.makeText(this, "Please select a return date", Toast.LENGTH_SHORT).show()
            valid = false
        }

        // Full name
        val name = etFullName.text.toString().trim()
        if (name.isEmpty()) {
            tvFullNameError.text = "Full name is required"
            tvFullNameError.visibility = View.VISIBLE
            valid = false
        } else {
            tvFullNameError.visibility = View.GONE
        }

        // Phone
        val phone = etPhone.text.toString().trim()
        if (phone.isEmpty()) {
            tvPhoneError.text = "Phone number is required"
            tvPhoneError.visibility = View.VISIBLE
            valid = false
        } else if (phone.length < 10) {
            tvPhoneError.text = "Enter a valid phone number"
            tvPhoneError.visibility = View.VISIBLE
            valid = false
        } else {
            tvPhoneError.visibility = View.GONE
        }

        // Email
        val email = etEmail.text.toString().trim()
        if (email.isEmpty()) {
            tvEmailError.text = "Email is required"
            tvEmailError.visibility = View.VISIBLE
            valid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tvEmailError.text = "Enter a valid email address"
            tvEmailError.visibility = View.VISIBLE
            valid = false
        } else {
            tvEmailError.visibility = View.GONE
        }

        // Address
        val address = etAddress.text.toString().trim()
        if (address.isEmpty()) {
            tvAddressError.text = "Address is required"
            tvAddressError.visibility = View.VISIBLE
            valid = false
        } else {
            tvAddressError.visibility = View.GONE
        }

        // License
        val license = etLicense.text.toString().trim()
        if (license.isEmpty()) {
            tvLicenseError.text = "Driver's license number is required"
            tvLicenseError.visibility = View.VISIBLE
            valid = false
        } else {
            tvLicenseError.visibility = View.GONE
        }

        return valid
    }

    private fun confirmBooking() {
        val brand = intent.getStringExtra("brand") ?: ""
        val model = intent.getStringExtra("model") ?: ""
        val carId = intent.getStringExtra("carId") ?: ""
        val agencyId = intent.getStringExtra("agencyId") ?: ""
        val imageUri = intent.getStringExtra("imageUri") ?: ""
        val location = intent.getStringExtra("location") ?: ""

        val diff = returnCalendar!!.timeInMillis - pickupCalendar!!.timeInMillis
        val days = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diff).toInt()
        val total = days * pricePerDay

        val booking = Booking(
            id = BookingManager.generateBookingId(),
            carId = carId,
            carName = "$brand $model",
            carLocation = location,
            carImageUri = imageUri,
            pricePerDay = pricePerDay,
            totalPrice = total,
            numDays = days,
            pickupDate = tvPickupDate.text.toString(),
            returnDate = tvReturnDate.text.toString(),
            renterName = etFullName.text.toString().trim(),
            renterPhone = etPhone.text.toString().trim(),
            renterEmail = etEmail.text.toString().trim(),
            renterAddress = etAddress.text.toString().trim(),
            licenseNumber = etLicense.text.toString().trim(),
            agencyId = agencyId,
            status = "Pending",
            timestamp = System.currentTimeMillis()
        )

        btnConfirmBooking.isEnabled = false
        btnConfirmBooking.text = "Confirming..."

        BookingManager.saveBooking(booking,
            onSuccess = {
                // Send notification to agency
                NotificationManager.sendBookingNotification(
                    agencyId = agencyId,
                    renterName = booking.renterName,
                    carName = booking.carName,
                    pickupDate = booking.pickupDate,
                    returnDate = booking.returnDate,
                    bookingId = booking.id
                )
                // Create chat thread between renter and agency
                val renterId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
                val prefs = getSharedPreferences("EasyDrivePrefs", MODE_PRIVATE)
                val renterName = "${prefs.getString("firstName","")} ${prefs.getString("lastName","")}".trim()
                val renterEmail = booking.renterEmail
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users").document(agencyId).get()
                    .addOnSuccessListener { doc ->
                        val agencyName = "${doc.getString("firstName") ?: ""} ${doc.getString("lastName") ?: ""}".trim()
                        ChatManager.createOrGetChat(
                            renterId = renterId,
                            renterName = renterName.ifEmpty { booking.renterName },
                            renterEmail = renterEmail,
                            agencyId = agencyId,
                            agencyName = agencyName.ifEmpty { "Agency" },
                            carName = booking.carName,
                            onSuccess = {},
                            onFailure = {}
                        )
                    }
                runOnUiThread {
                    android.widget.Toast.makeText(
                        this,
                        "Booking confirmed!\n${booking.carName}\n${booking.pickupDate} → ${booking.returnDate}\nTotal: ₱$total",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                    finish()
                }
            },
            onFailure = { e ->
                runOnUiThread {
                    btnConfirmBooking.isEnabled = true
                    btnConfirmBooking.text = "Confirm Booking"
                    android.widget.Toast.makeText(this, "Failed to save booking: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}
