package com.example.easydrive

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth

class ViewBookingsActivity : AppCompatActivity() {

    private lateinit var ivBack: ImageView
    private lateinit var bookingsContainer: LinearLayout
    private lateinit var tvEmptyState: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_bookings)

        ivBack = findViewById(R.id.ivBack)
        bookingsContainer = findViewById(R.id.bookingsContainer)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        progressBar = findViewById(R.id.progressBar)

        ivBack.setOnClickListener { finish() }
    }

    private fun loadBookings() {
        val agencyId = FirebaseAuth.getInstance().currentUser?.uid
            ?: getSharedPreferences("EasyDrivePrefs", MODE_PRIVATE).getString("email", "") ?: ""

        android.util.Log.d("ViewBookings", "Loading bookings for agencyId: $agencyId")

        progressBar.visibility = View.VISIBLE
        tvEmptyState.visibility = View.GONE
        bookingsContainer.removeAllViews()

        BookingManager.getBookingsForAgency(
            agencyId,
            onSuccess = { bookings ->
                android.util.Log.d("ViewBookings", "Fetched ${bookings.size} bookings")
                progressBar.visibility = View.GONE
                val unique = bookings.distinctBy { it.id }
                if (unique.isEmpty()) {
                    tvEmptyState.visibility = View.VISIBLE
                } else {
                    unique.forEach { addBookingView(it) }
                }
            },
            onFailure = { e ->
                android.util.Log.e("ViewBookings", "Failed: ${e.message}")
                progressBar.visibility = View.GONE
                tvEmptyState.text = "Failed to load bookings. Please try again."
                tvEmptyState.visibility = View.VISIBLE
            }
        )
    }

    private fun addBookingView(booking: Booking) {
        val view = layoutInflater.inflate(R.layout.item_booking, bookingsContainer, false)

        val ivCarImage = view.findViewById<ImageView>(R.id.ivCarImage)
        val tvCarName = view.findViewById<TextView>(R.id.tvCarName)
        val tvCarLocation = view.findViewById<TextView>(R.id.tvCarLocation)
        val tvStatus = view.findViewById<TextView>(R.id.tvStatus)
        val tvRenterName = view.findViewById<TextView>(R.id.tvRenterName)
        val tvRenterPhone = view.findViewById<TextView>(R.id.tvRenterPhone)
        val tvPickupDate = view.findViewById<TextView>(R.id.tvPickupDate)
        val tvReturnDate = view.findViewById<TextView>(R.id.tvReturnDate)
        val tvTotalPrice = view.findViewById<TextView>(R.id.tvTotalPrice)
        val btnConfirm = view.findViewById<android.widget.Button>(R.id.btnConfirm)
        val btnCancel = view.findViewById<android.widget.Button>(R.id.btnCancel)

        tvCarName.text = booking.carName
        tvCarLocation.text = "📍 ${booking.carLocation}"
        tvRenterName.text = booking.renterName
        tvRenterPhone.text = booking.renterPhone
        tvPickupDate.text = booking.pickupDate
        tvReturnDate.text = booking.returnDate
        tvTotalPrice.text = "₱${booking.totalPrice}"

        // Status badge color
        updateStatusBadge(tvStatus, booking.status)

        // Show/hide action buttons based on status
        when (booking.status) {
            "Pending" -> {
                btnConfirm.visibility = android.view.View.VISIBLE
                btnCancel.visibility = android.view.View.VISIBLE
            }
            "Confirmed" -> {
                btnConfirm.visibility = android.view.View.GONE
                btnCancel.visibility = android.view.View.VISIBLE
                btnCancel.text = "Mark Completed"
                btnCancel.setBackgroundColor(0xFF1565C0.toInt())
            }
            else -> {
                btnConfirm.visibility = android.view.View.GONE
                btnCancel.visibility = android.view.View.GONE
            }
        }

        btnConfirm.setOnClickListener {
            updateStatus(booking.id, "Confirmed", tvStatus, btnConfirm, btnCancel,
                booking.renterEmail, booking.carName)
        }

        btnCancel.setOnClickListener {
            val newStatus = if (booking.status == "Confirmed") "Completed" else "Cancelled"
            val msg = if (booking.status == "Confirmed") "Mark this booking as Completed?" else "Cancel this booking?"
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Confirm Action")
                .setMessage(msg)
                .setPositiveButton("Yes") { _, _ ->
                    updateStatus(booking.id, newStatus, tvStatus, btnConfirm, btnCancel,
                        booking.renterEmail, booking.carName)
                }
                .setNegativeButton("No", null)
                .show()
        }

        // Load car image
        if (booking.carImageUri.isNotEmpty()) {
            try {
                val load = if (booking.carImageUri.startsWith("http")) booking.carImageUri
                           else Uri.parse(booking.carImageUri)
                Glide.with(this).load(load)
                    .placeholder(R.drawable.ic_menu).error(R.drawable.ic_menu)
                    .centerCrop().into(ivCarImage)
            } catch (e: Exception) {
                ivCarImage.setImageResource(R.drawable.ic_menu)
            }
        }

        bookingsContainer.addView(view)
    }

    private fun updateStatus(
        bookingId: String,
        newStatus: String,
        tvStatus: TextView,
        btnConfirm: android.widget.Button,
        btnCancel: android.widget.Button,
        renterEmail: String,
        carName: String
    ) {
        BookingManager.updateBookingStatus(bookingId, newStatus,
            onSuccess = {
                // Notify the renter
                NotificationManager.sendStatusUpdateNotification(
                    renterEmail = renterEmail,
                    carName = carName,
                    newStatus = newStatus,
                    bookingId = bookingId
                )
                updateStatusBadge(tvStatus, newStatus)
                when (newStatus) {
                    "Confirmed" -> {
                        btnConfirm.visibility = android.view.View.GONE
                        btnCancel.visibility = android.view.View.VISIBLE
                        btnCancel.text = "Mark Completed"
                        btnCancel.setBackgroundColor(0xFF1565C0.toInt())
                    }
                    else -> {
                        btnConfirm.visibility = android.view.View.GONE
                        btnCancel.visibility = android.view.View.GONE
                    }
                }
                android.widget.Toast.makeText(this, "Status updated to $newStatus", android.widget.Toast.LENGTH_SHORT).show()
            },
            onFailure = {
                android.widget.Toast.makeText(this, "Failed to update status", android.widget.Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun updateStatusBadge(tvStatus: TextView, status: String) {
        tvStatus.text = status
        tvStatus.setTextColor(when (status) {
            "Confirmed"  -> 0xFF2E7D32.toInt()
            "Completed"  -> 0xFF1565C0.toInt()
            "Cancelled"  -> 0xFFB00020.toInt()
            else         -> 0xFFE65100.toInt()
        })
        tvStatus.setBackgroundColor(when (status) {
            "Confirmed"  -> 0xFFE8F5E9.toInt()
            "Completed"  -> 0xFFE3F2FD.toInt()
            "Cancelled"  -> 0xFFFFEBEE.toInt()
            else         -> 0xFFFFF3E0.toInt()
        })
    }

    override fun onResume() {
        super.onResume()
        loadBookings()
    }
}
