package com.example.easydrive

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignUpActivity : AppCompatActivity() {
    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    
    private lateinit var etFirstName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var tvFirstNameError: TextView
    private lateinit var tvLastNameError: TextView
    private lateinit var tvEmailError: TextView
    private lateinit var tvPasswordError: TextView
    private lateinit var tvConfirmPasswordError: TextView
    private lateinit var tvGenderError: TextView
    private lateinit var tvUserTypeError: TextView
    private lateinit var rgGender: RadioGroup
    private lateinit var rgUserType: RadioGroup
    private lateinit var btnSignUp: Button
    private lateinit var ivTogglePassword: ImageView
    private lateinit var ivToggleConfirmPassword: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        initializeViews()
        setupPasswordToggles()
        setupRealTimeValidation()
        setupClickListeners()
        styleLoginText()
    }
    
    private fun initializeViews() {
        etFirstName = findViewById(R.id.etFirstName)
        etLastName = findViewById(R.id.etLastName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        tvFirstNameError = findViewById(R.id.tvFirstNameError)
        tvLastNameError = findViewById(R.id.tvLastNameError)
        tvEmailError = findViewById(R.id.tvEmailError)
        tvPasswordError = findViewById(R.id.tvPasswordError)
        tvConfirmPasswordError = findViewById(R.id.tvConfirmPasswordError)
        tvGenderError = findViewById(R.id.tvGenderError)
        tvUserTypeError = findViewById(R.id.tvUserTypeError)
        rgGender = findViewById(R.id.rgGender)
        rgUserType = findViewById(R.id.rgUserType)
        btnSignUp = findViewById(R.id.btnSignUp)
        ivTogglePassword = findViewById(R.id.ivTogglePassword)
        ivToggleConfirmPassword = findViewById(R.id.ivToggleConfirmPassword)
    }
    
    private fun setupPasswordToggles() {
        ivTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                etPassword.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                ivTogglePassword.setImageResource(R.drawable.ic_eye_open)
            } else {
                etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                ivTogglePassword.setImageResource(R.drawable.ic_eye_closed)
            }
            etPassword.setSelection(etPassword.text.length)
        }

        ivToggleConfirmPassword.setOnClickListener {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
            if (isConfirmPasswordVisible) {
                etConfirmPassword.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                ivToggleConfirmPassword.setImageResource(R.drawable.ic_eye_open)
            } else {
                etConfirmPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                ivToggleConfirmPassword.setImageResource(R.drawable.ic_eye_closed)
            }
            etConfirmPassword.setSelection(etConfirmPassword.text.length)
        }
    }
    
    private fun setupRealTimeValidation() {
        etFirstName.addTextChangedListener(createTextWatcher { if (tvFirstNameError.visibility == View.VISIBLE) validateFirstName() })
        etLastName.addTextChangedListener(createTextWatcher { if (tvLastNameError.visibility == View.VISIBLE) validateLastName() })
        etEmail.addTextChangedListener(createTextWatcher { if (tvEmailError.visibility == View.VISIBLE) validateEmail() })
        etPassword.addTextChangedListener(createTextWatcher { if (tvPasswordError.visibility == View.VISIBLE) validatePassword() })
        etConfirmPassword.addTextChangedListener(createTextWatcher { if (tvConfirmPasswordError.visibility == View.VISIBLE) validateConfirmPassword() })
    }
    
    private fun createTextWatcher(afterTextChanged: () -> Unit): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { afterTextChanged() }
        }
    }
    
    private fun setupClickListeners() {
        btnSignUp.setOnClickListener {
            if (validateForm()) {
                performSignUp()
            }
        }

        findViewById<TextView>(R.id.tvLogin).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
    
    private fun validateFirstName(): Boolean {
        val firstName = etFirstName.text.toString().trim()
        return when {
            firstName.isEmpty() -> {
                tvFirstNameError.text = "First name is required"
                tvFirstNameError.visibility = View.VISIBLE
                false
            }
            firstName.length < 2 -> {
                tvFirstNameError.text = "First name must be at least 2 characters"
                tvFirstNameError.visibility = View.VISIBLE
                false
            }
            !firstName.matches(Regex("^[a-zA-Z\\s]+$")) -> {
                tvFirstNameError.text = "First name can only contain letters"
                tvFirstNameError.visibility = View.VISIBLE
                false
            }
            else -> {
                tvFirstNameError.visibility = View.GONE
                true
            }
        }
    }
    
    private fun validateLastName(): Boolean {
        val lastName = etLastName.text.toString().trim()
        return when {
            lastName.isEmpty() -> {
                tvLastNameError.text = "Last name is required"
                tvLastNameError.visibility = View.VISIBLE
                false
            }
            lastName.length < 2 -> {
                tvLastNameError.text = "Last name must be at least 2 characters"
                tvLastNameError.visibility = View.VISIBLE
                false
            }
            !lastName.matches(Regex("^[a-zA-Z\\s]+$")) -> {
                tvLastNameError.text = "Last name can only contain letters"
                tvLastNameError.visibility = View.VISIBLE
                false
            }
            else -> {
                tvLastNameError.visibility = View.GONE
                true
            }
        }
    }
    
    private fun validateEmail(): Boolean {
        val email = etEmail.text.toString().trim()
        return when {
            email.isEmpty() -> {
                tvEmailError.text = "Email is required"
                tvEmailError.visibility = View.VISIBLE
                false
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                tvEmailError.text = "Please enter a valid email address"
                tvEmailError.visibility = View.VISIBLE
                false
            }
            else -> {
                tvEmailError.visibility = View.GONE
                true
            }
        }
    }
    
    private fun validatePassword(): Boolean {
        val password = etPassword.text.toString()
        return when {
            password.isEmpty() -> {
                tvPasswordError.text = "Password is required"
                tvPasswordError.visibility = View.VISIBLE
                false
            }
            password.length < 6 -> {
                tvPasswordError.text = "Password must be at least 6 characters"
                tvPasswordError.visibility = View.VISIBLE
                false
            }
            password.length > 20 -> {
                tvPasswordError.text = "Password must not exceed 20 characters"
                tvPasswordError.visibility = View.VISIBLE
                false
            }
            !password.matches(Regex(".*[A-Z].*")) -> {
                tvPasswordError.text = "Password must contain at least one uppercase letter"
                tvPasswordError.visibility = View.VISIBLE
                false
            }
            !password.matches(Regex(".*[a-z].*")) -> {
                tvPasswordError.text = "Password must contain at least one lowercase letter"
                tvPasswordError.visibility = View.VISIBLE
                false
            }
            !password.matches(Regex(".*[0-9].*")) -> {
                tvPasswordError.text = "Password must contain at least one number"
                tvPasswordError.visibility = View.VISIBLE
                false
            }
            else -> {
                tvPasswordError.visibility = View.GONE
                true
            }
        }
    }
    
    private fun validateConfirmPassword(): Boolean {
        val password = etPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()
        return when {
            confirmPassword.isEmpty() -> {
                tvConfirmPasswordError.text = "Please confirm your password"
                tvConfirmPasswordError.visibility = View.VISIBLE
                false
            }
            password != confirmPassword -> {
                tvConfirmPasswordError.text = "Passwords do not match"
                tvConfirmPasswordError.visibility = View.VISIBLE
                false
            }
            else -> {
                tvConfirmPasswordError.visibility = View.GONE
                true
            }
        }
    }
    
    private fun validateGender(): Boolean {
        return if (rgGender.checkedRadioButtonId == -1) {
            tvGenderError.text = "Please select your gender"
            tvGenderError.visibility = View.VISIBLE
            false
        } else {
            tvGenderError.visibility = View.GONE
            true
        }
    }
    
    private fun validateUserType(): Boolean {
        return if (rgUserType.checkedRadioButtonId == -1) {
            tvUserTypeError.text = "Please select user type"
            tvUserTypeError.visibility = View.VISIBLE
            false
        } else {
            tvUserTypeError.visibility = View.GONE
            true
        }
    }
    
    private fun validateForm(): Boolean {
        val isFirstNameValid = validateFirstName()
        val isLastNameValid = validateLastName()
        val isEmailValid = validateEmail()
        val isPasswordValid = validatePassword()
        val isConfirmPasswordValid = validateConfirmPassword()
        val isGenderValid = validateGender()
        val isUserTypeValid = validateUserType()
        
        // Focus on first error field
        when {
            !isFirstNameValid -> etFirstName.requestFocus()
            !isLastNameValid -> etLastName.requestFocus()
            !isEmailValid -> etEmail.requestFocus()
            !isPasswordValid -> etPassword.requestFocus()
            !isConfirmPasswordValid -> etConfirmPassword.requestFocus()
        }
        
        return isFirstNameValid && isLastNameValid && isEmailValid && 
               isPasswordValid && isConfirmPasswordValid && isGenderValid && isUserTypeValid
    }
    
    private fun performSignUp() {
        val firstName = etFirstName.text.toString().trim()
        val lastName = etLastName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()
        val selectedGenderId = rgGender.checkedRadioButtonId
        val selectedUserTypeId = rgUserType.checkedRadioButtonId
        
        btnSignUp.isEnabled = false
        btnSignUp.text = "Creating account..."
        
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    saveUserData(firstName, lastName, email, "", selectedGenderId, selectedUserTypeId, false)
                } else {
                    // Firebase failed - use local storage fallback
                    saveUserData(firstName, lastName, email, password, selectedGenderId, selectedUserTypeId, true)
                }
            }
            .addOnFailureListener {
                btnSignUp.isEnabled = true
                btnSignUp.text = "Sign up"
            }
    }
    
    private fun saveUserData(firstName: String, lastName: String, email: String, password: String, 
                            selectedGenderId: Int, selectedUserTypeId: Int, useLocalOnly: Boolean) {
        val genderText = when (selectedGenderId) {
            R.id.rbMale -> "Male"
            R.id.rbFemale -> "Female"
            else -> "Other"
        }
        val userTypeText = when (selectedUserTypeId) {
            R.id.rbRenter -> "Renter"
            R.id.rbAgency -> "Agency"
            else -> "Renter"
        }
        
        if (!useLocalOnly) {
            val userId = auth.currentUser?.uid
            if (userId != null) {
                val userData = hashMapOf(
                    "firstName" to firstName,
                    "lastName" to lastName,
                    "email" to email,
                    "gender" to genderText,
                    "userType" to userTypeText,
                    "phone" to "",
                    "profileImageUrl" to ""
                )
                
                firestore.collection("users").document(userId)
                    .set(userData)
                    .addOnSuccessListener {
                        saveToLocalStorage(firstName, lastName, email, password, genderText, userTypeText, false)
                        navigateToHome(userTypeText)
                    }
                    .addOnFailureListener {
                        saveToLocalStorage(firstName, lastName, email, password, genderText, userTypeText, true)
                        navigateToHome(userTypeText)
                    }
            }
        } else {
            saveToLocalStorage(firstName, lastName, email, password, genderText, userTypeText, true)
            navigateToHome(userTypeText)
        }
    }
    
    private fun saveToLocalStorage(firstName: String, lastName: String, email: String, 
                                   password: String, gender: String, userType: String, useLocalStorage: Boolean) {
        val sharedPreferences = getSharedPreferences("EasyDrivePrefs", MODE_PRIVATE)
        sharedPreferences.edit().apply {
            putBoolean("isLoggedIn", true)
            putBoolean("useLocalStorage", useLocalStorage)
            putString("firstName", firstName)
            putString("lastName", lastName)
            putString("email", email)
            putString("password", password)
            putString("gender", gender)
            putString("userType", userType)
            putString("phone", "")
            apply()
        }
    }
    
    private fun navigateToHome(userType: String) {
        Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
        
        val intent = if (userType == "Agency") {
            Intent(this, AgencyDashboardActivity::class.java)
        } else {
            Intent(this, MainActivity::class.java)
        }
        
        startActivity(intent)
        finish()
    }
    
    private fun styleLoginText() {
        val tvLogin = findViewById<TextView>(R.id.tvLogin)
        val loginText = tvLogin.text.toString()
        val spannableLogin = android.text.SpannableString(loginText)
        val loginStart = loginText.indexOf("Login")
        
        if (loginStart >= 0) {
            spannableLogin.setSpan(
                android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                loginStart,
                loginStart + 5,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannableLogin.setSpan(
                android.text.style.ForegroundColorSpan(0xFF212121.toInt()),
                loginStart,
                loginStart + 5,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            tvLogin.text = spannableLogin
        }
    }
}
