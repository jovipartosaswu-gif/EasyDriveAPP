package com.example.easydrive

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException

class EditProfileActivity : AppCompatActivity() {
    private lateinit var etFirstName: EditText
    private lateinit var etLastName: EditText   
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var btnSaveChange: Button
    private lateinit var ivProfilePic: ImageView
    private var selectedImageBitmap: Bitmap? = null
    private var uploadedImageUrl: String = ""
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private val okHttpClient = OkHttpClient()
    
    companion object {
        private const val IMGBB_API_KEY = "34ce02bd62df32a7055799d8f246f47a"
        private const val IMGBB_UPLOAD_URL = "https://api.imgbb.com/1/upload"
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                try {
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                    val resizedBitmap = resizeBitmap(bitmap, 800, 800)
                    selectedImageBitmap = resizedBitmap
                    ivProfilePic.setImageBitmap(resizedBitmap)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Initialize views
        etFirstName = findViewById(R.id.etFirstName)
        etLastName = findViewById(R.id.etLastName)
        etEmail = findViewById(R.id.etEmail)
        etPhone = findViewById(R.id.etPhone)
        btnSaveChange = findViewById(R.id.btnSaveChange)
        ivProfilePic = findViewById(R.id.ivProfilePic)

        // Back button
        findViewById<ImageView>(R.id.ivBack).setOnClickListener {
            finish()
        }

        // Load current user data
        loadUserData()

        // Save changes
        btnSaveChange.setOnClickListener {
            saveChanges()
        }

        // Edit profile picture
        findViewById<ImageView>(R.id.ivEditPhoto).setOnClickListener {
            openImagePicker()
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun loadUserData() {
        val sharedPreferences = getSharedPreferences("EasyDrivePrefs", MODE_PRIVATE)
        val useLocalStorage = sharedPreferences.getBoolean("useLocalStorage", false)
        
        if (useLocalStorage) {
            loadFromLocalStorage()
        } else {
            loadFromFirebase()
        }
    }
    
    private fun loadFromLocalStorage() {
        val sharedPreferences = getSharedPreferences("EasyDrivePrefs", MODE_PRIVATE)
        val firstName = sharedPreferences.getString("firstName", "") ?: ""
        val lastName = sharedPreferences.getString("lastName", "") ?: ""
        val email = sharedPreferences.getString("email", "") ?: ""
        val phone = sharedPreferences.getString("phone", "") ?: ""
        val profileImageUrl = sharedPreferences.getString("profileImageUrl", "") ?: ""
        
        etFirstName.setText(firstName)
        etLastName.setText(lastName)
        etEmail.setText(email)
        etPhone.setText(phone)
        uploadedImageUrl = profileImageUrl
        
        findViewById<TextView>(R.id.tvUserName).text = "$firstName $lastName"
        
        if (profileImageUrl.isNotEmpty()) {
            Glide.with(this)
                .load(profileImageUrl)
                .placeholder(R.drawable.ic_profile_placeholder)
                .into(ivProfilePic)
        }
    }
    
    private fun loadFromFirebase() {
        val sharedPreferences = getSharedPreferences("EasyDrivePrefs", MODE_PRIVATE)
        val useLocalStorage = sharedPreferences.getBoolean("useLocalStorage", false)
        
        if (useLocalStorage) {
            // User is using local storage, load from there instead
            loadFromLocalStorage()
            return
        }
        
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val firstName = document.getString("firstName") ?: ""
                    val lastName = document.getString("lastName") ?: ""
                    val email = document.getString("email") ?: ""
                    val phone = document.getString("phone") ?: ""
                    val profileImageUrl = document.getString("profileImageUrl") ?: ""
                    
                    etFirstName.setText(firstName)
                    etLastName.setText(lastName)
                    etEmail.setText(email)
                    etPhone.setText(phone)
                    uploadedImageUrl = profileImageUrl
                    
                    findViewById<TextView>(R.id.tvUserName).text = "$firstName $lastName"
                    
                    if (profileImageUrl.isNotEmpty()) {
                        Glide.with(this)
                            .load(profileImageUrl)
                            .placeholder(R.drawable.ic_profile_placeholder)
                            .into(ivProfilePic)
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveChanges() {
        val firstName = etFirstName.text.toString().trim()
        val lastName = etLastName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val phone = etPhone.text.toString().trim()

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val sharedPreferences = getSharedPreferences("EasyDrivePrefs", MODE_PRIVATE)
        val useLocalStorage = sharedPreferences.getBoolean("useLocalStorage", false)
        
        if (useLocalStorage) {
            // Save to local storage only
            btnSaveChange.isEnabled = false
            
            if (selectedImageBitmap != null) {
                Toast.makeText(this, "Uploading image...", Toast.LENGTH_SHORT).show()
                uploadImageToImgBB(selectedImageBitmap!!) { imageUrl ->
                    if (imageUrl != null) {
                        uploadedImageUrl = imageUrl
                        saveToLocalStorage(firstName, lastName, email, phone, uploadedImageUrl)
                        Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show()
                        btnSaveChange.isEnabled = true
                    }
                }
            } else {
                saveToLocalStorage(firstName, lastName, email, phone, uploadedImageUrl)
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            return
        }

        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        btnSaveChange.isEnabled = false

        // Check if we need to upload a new image
        if (selectedImageBitmap != null) {
            Toast.makeText(this, "Uploading image...", Toast.LENGTH_SHORT).show()
            uploadImageToImgBB(selectedImageBitmap!!) { imageUrl ->
                if (imageUrl != null) {
                    uploadedImageUrl = imageUrl
                    saveProfileToFirestore(userId, firstName, lastName, email, phone, imageUrl)
                } else {
                    Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show()
                    btnSaveChange.isEnabled = true
                }
            }
        } else {
            saveProfileToFirestore(userId, firstName, lastName, email, phone, uploadedImageUrl)
        }
    }
    
    private fun uploadImageToImgBB(bitmap: Bitmap, callback: (String?) -> Unit) {
        Thread {
            try {
                // Convert bitmap to base64
                val byteArrayOutputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
                val imageBytes = byteArrayOutputStream.toByteArray()
                val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
                
                // Build request
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("key", IMGBB_API_KEY)
                    .addFormDataPart("image", base64Image)
                    .build()
                
                val request = Request.Builder()
                    .url(IMGBB_UPLOAD_URL)
                    .post(requestBody)
                    .build()
                
                // Execute request
                okHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        val jsonObject = JSONObject(responseBody ?: "")
                        val imageUrl = jsonObject.getJSONObject("data").getString("url")
                        runOnUiThread { callback(imageUrl) }
                    } else {
                        runOnUiThread { callback(null) }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread { callback(null) }
            }
        }.start()
    }
    
    private fun saveProfileToFirestore(userId: String, firstName: String, lastName: String, email: String, phone: String, profileImageUrl: String) {
        val userData = hashMapOf(
            "firstName" to firstName,
            "lastName" to lastName,
            "email" to email,
            "phone" to phone,
            "profileImageUrl" to profileImageUrl
        )

        firestore.collection("users").document(userId)
            .update(userData as Map<String, Any>)
            .addOnSuccessListener {
                saveToLocalStorage(firstName, lastName, email, phone, profileImageUrl)
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error updating profile: ${e.message}", Toast.LENGTH_SHORT).show()
                btnSaveChange.isEnabled = true
            }
    }
    
    private fun saveToLocalStorage(firstName: String, lastName: String, email: String, phone: String, profileImageUrl: String) {
        val sharedPreferences = getSharedPreferences("EasyDrivePrefs", MODE_PRIVATE)
        sharedPreferences.edit().apply {
            putString("firstName", firstName)
            putString("lastName", lastName)
            putString("email", email)
            putString("phone", phone)
            putString("profileImageUrl", profileImageUrl)
            apply()
        }
    }
    
    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        val ratioBitmap = width.toFloat() / height.toFloat()
        val ratioMax = maxWidth.toFloat() / maxHeight.toFloat()
        
        var finalWidth = maxWidth
        var finalHeight = maxHeight
        
        if (ratioMax > ratioBitmap) {
            finalWidth = (maxHeight.toFloat() * ratioBitmap).toInt()
        } else {
            finalHeight = (maxWidth.toFloat() / ratioBitmap).toInt()
        }
        
        return Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true)
    }
}
