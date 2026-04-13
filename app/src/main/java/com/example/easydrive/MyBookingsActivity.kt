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

class MyBookingsActivity : AppCompatActivity() {

    private lateinit var ivBack: ImageView
    private lateinit var bookingsContainer: LinearLayout
    private lateinit var tvEmptyState: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_bookings)

        ivBack = findViewById(R.id.ivBack)
        bookingsContainer = findViewById(R.id.bookingsContainer)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        progressBar = findViewById(R.id.progressBar)

        ivBack.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        loadMyBookings()
    }

    private fun loadMyBookings() {
        val prefs = getSharedPreferences("EasyDrivePrefs", MODE_PRIVATE)
        val email = FirebaseAuth.getInstance().currentUser?.email
            ?: prefs.getString("email", "") ?: ""

        if (email.isEmpty()) {
            tvEmptyState.text = "Please log in to view your bookings."
            tvEmptyState.visibility = View.VISIBLE
            return
        }

        progressBar.visibility = View.VISIBLE
        tvEmptyState.visibility = View.GONE
        bookingsContainer.removeAllViews()

        BookingManager.getBookingsForRenter(email,
            onSuccess = { bookings ->
                progressBar.visibility = View.GONE
                val unique = bookings.distinctBy { it.id }
                if (unique.isEmpty()) {
                    tvEmptyState.text = "You haven't made any bookings yet."
                    tvEmptyState.visibility = View.VISIBLE
                } else {
                    unique.forEach { addBookingView(it) }
                }
            },
            onFailure = {
                progressBar.visibility = View.GONE
                tvEmptyState.text = "Failed to load bookings. Please try again."
                tvEmptyState.visibility = View.VISIBLE
            }
        )
    }

    private fun addBookingView(booking: Booking) {
        val view = layoutInflater.inflate(R.layout.item_my_booking, bookingsContainer, false)

        val ivCarImage = view.findViewById<ImageView>(R.id.ivCarImage)
        val tvCarName = view.findViewById<TextView>(R.id.tvCarName)
        val tvCarLocation = view.findViewById<TextView>(R.id.tvCarLocation)
        val tvStatus = view.findViewById<TextView>(R.id.tvStatus)
        val tvPickupDate = view.findViewById<TextView>(R.id.tvPickupDate)
        val tvReturnDate = view.findViewById<TextView>(R.id.tvReturnDate)
        val tvNumDays = view.findViewById<TextView>(R.id.tvNumDays)
        val tvTotalPrice = view.findViewById<TextView>(R.id.tvTotalPrice)

        tvCarName.text = booking.carName
        tvCarLocation.text = "📍 ${booking.carLocation}"
        tvPickupDate.text = booking.pickupDate
        tvReturnDate.text = booking.returnDate
        tvNumDays.text = "${booking.numDays} day${if (booking.numDays > 1) "s" else ""}"
        tvTotalPrice.text = "₱${booking.totalPrice}"

        tvStatus.text = booking.status
        tvStatus.setTextColor(when (booking.status) {
            "Confirmed"  -> 0xFF2E7D32.toInt()
            "Completed"  -> 0xFF1565C0.toInt()
            "Cancelled"  -> 0xFFB00020.toInt()
            else         -> 0xFFE65100.toInt()
        })
        tvStatus.setBackgroundColor(when (booking.status) {
            "Confirmed"  -> 0xFFE8F5E9.toInt()
            "Completed"  -> 0xFFE3F2FD.toInt()
            "Cancelled"  -> 0xFFFFEBEE.toInt()
            else         -> 0xFFFFF3E0.toInt()
        })

        if (booking.carImageUri.isNotEmpty()) {
            try {
                val load: Any = if (booking.carImageUri.startsWith("http")) booking.carImageUri
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
}
