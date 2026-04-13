package com.example.easydrive

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.material.imageview.ShapeableImageView

class CarDetailsActivity : AppCompatActivity() {

    private lateinit var viewPagerImages: ViewPager2
    private lateinit var dotsContainer: LinearLayout
    private lateinit var ivBack: ImageView
    private lateinit var tvCarName: TextView
    private lateinit var tvCarPrice: TextView
    private lateinit var tvCarLocation: TextView
    private lateinit var tvYear: TextView
    private lateinit var tvFuelType: TextView
    private lateinit var tvTransmission: TextView
    private lateinit var tvSeats: TextView
    private lateinit var tvDescription: TextView
    private lateinit var btnBookNow: Button
    private var imageUris: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_car_details)
        initializeViews()
        loadCarDetails()
        setupClickListeners()
    }

    private fun initializeViews() {
        viewPagerImages = findViewById(R.id.viewPagerImages)
        dotsContainer = findViewById(R.id.dotsContainer)
        ivBack = findViewById(R.id.ivBack)
        tvCarName = findViewById(R.id.tvCarName)
        tvCarPrice = findViewById(R.id.tvCarPrice)
        tvCarLocation = findViewById(R.id.tvCarLocation)
        tvYear = findViewById(R.id.tvYear)
        tvFuelType = findViewById(R.id.tvFuelType)
        tvTransmission = findViewById(R.id.tvTransmission)
        tvSeats = findViewById(R.id.tvSeats)
        tvDescription = findViewById(R.id.tvDescription)
        btnBookNow = findViewById(R.id.btnBookNow)
    }

    private fun loadCarDetails() {
        val brand = intent.getStringExtra("brand") ?: ""
        val model = intent.getStringExtra("model") ?: ""
        val year = intent.getStringExtra("year") ?: ""
        val fuelType = intent.getStringExtra("fuelType") ?: ""
        val transmission = intent.getStringExtra("transmission") ?: ""
        val seatingCapacity = intent.getIntExtra("seatingCapacity", 0)
        val pricePerDay = intent.getDoubleExtra("pricePerDay", 0.0)
        val location = intent.getStringExtra("location") ?: ""
        val description = intent.getStringExtra("description") ?: ""
        val imageUri = intent.getStringExtra("imageUri") ?: ""
        val imageUrisExtra = intent.getStringArrayListExtra("imageUris")

        imageUris = when {
            !imageUrisExtra.isNullOrEmpty() -> imageUrisExtra.toList()
            imageUri.isNotEmpty() -> listOf(imageUri)
            else -> listOf("")
        }

        tvCarName.text = "$brand $model"
        tvCarPrice.text = "₱$pricePerDay/day"
        tvCarLocation.text = "📍 $location"
        tvYear.text = year
        tvFuelType.text = fuelType
        tvTransmission.text = transmission
        tvSeats.text = seatingCapacity.toString()
        tvDescription.text = if (description.isNotEmpty()) description else "No description available"

        setupImagePager()
    }

    private fun setupImagePager() {
        viewPagerImages.adapter = ImagePagerAdapter(imageUris)
        setupDots(imageUris.size, 0)

        viewPagerImages.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                setupDots(imageUris.size, position)
            }
        })
    }

    private fun setupDots(count: Int, selected: Int) {
        dotsContainer.removeAllViews()
        if (count <= 1) return

        val margin = (4 * resources.displayMetrics.density).toInt()
        for (i in 0 until count) {
            val dot = View(this)
            val size = if (i == selected)
                (8 * resources.displayMetrics.density).toInt()
            else
                (6 * resources.displayMetrics.density).toInt()

            val params = LinearLayout.LayoutParams(size, size)
            params.setMargins(margin, 0, margin, 0)
            dot.layoutParams = params
            dot.background = if (i == selected)
                resources.getDrawable(R.drawable.dot_active, null)
            else
                resources.getDrawable(R.drawable.dot_inactive, null)
            dotsContainer.addView(dot)
        }
    }

    private fun setupClickListeners() {
        ivBack.setOnClickListener { finish() }

        btnBookNow.setOnClickListener {
            val bookingIntent = Intent(this, BookingActivity::class.java).apply {
                putExtra("brand", intent.getStringExtra("brand"))
                putExtra("model", intent.getStringExtra("model"))
                putExtra("year", intent.getStringExtra("year"))
                putExtra("fuelType", intent.getStringExtra("fuelType"))
                putExtra("transmission", intent.getStringExtra("transmission"))
                putExtra("seatingCapacity", intent.getIntExtra("seatingCapacity", 0))
                putExtra("pricePerDay", intent.getDoubleExtra("pricePerDay", 0.0))
                putExtra("location", intent.getStringExtra("location"))
                putExtra("description", intent.getStringExtra("description"))
                putExtra("imageUri", intent.getStringExtra("imageUri"))
                putExtra("carId", intent.getStringExtra("carId"))
                putExtra("agencyId", intent.getStringExtra("agencyId"))
            }
            startActivity(bookingIntent)
        }
    }

    // ViewPager2 Adapter
    inner class ImagePagerAdapter(private val uris: List<String>) :
        RecyclerView.Adapter<ImagePagerAdapter.ImageViewHolder>() {

        inner class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val imageView: ShapeableImageView = view.findViewById(R.id.ivImage)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_car_image, parent, false)
            return ImageViewHolder(view)
        }

        override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
            val uri = uris[position]
            if (uri.isEmpty()) {
                holder.imageView.setImageResource(R.drawable.ic_menu)
                return
            }
            try {
                val load: Any = if (uri.startsWith("http")) uri else Uri.parse(uri)
                Glide.with(holder.imageView.context)
                    .load(load)
                    .placeholder(R.drawable.ic_menu)
                    .error(R.drawable.ic_menu)
                    .centerCrop()
                    .into(holder.imageView)
            } catch (e: Exception) {
                holder.imageView.setImageResource(R.drawable.ic_menu)
            }
        }

        override fun getItemCount() = uris.size
    }
}
