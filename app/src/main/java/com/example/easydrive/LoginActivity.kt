package com.example.easydrive

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {
    private var isPasswordVisible = false
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient
    
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var tvEmailError: TextView
    private lateinit var tvPasswordError: TextView
    private lateinit var btnLogin: Button
    private lateinit var btnGoogleSignIn: Button
    
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            firebaseAuthWithGoogle(account)
        } catch (e: ApiException) {
            Log.w("LoginActivity", "Google sign in failed", e)
            Toast.makeText(this, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        
        // Configure Google Sign-In
        val webClientId = "335037242068-liqttd6uj8tetki8su5icsdfjiu39i5p.apps.googleusercontent.com"
        
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        initializeViews()
        setupPasswordToggle()
        setupRealTimeValidation()
        setupClickListeners()
        styleSignUpText()
    }
    
    private fun initializeViews() {
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        tvEmailError = findViewById(R.id.tvEmailError)
        tvPasswordError = findViewById(R.id.tvPasswordError)
        btnLogin = findViewById(R.id.btnLogin)
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn)
    }
    
    private fun setupPasswordToggle() {
        val ivTogglePassword = findViewById<ImageView>(R.id.ivTogglePassword)
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
    }
    
    private fun setupRealTimeValidation() {
        // Email validation on text change
        etEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (tvEmailError.visibility == View.VISIBLE) {
                    validateEmail()
                }
            }
        })
        
        // Password validation on text change
        etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (tvPasswordError.visibility == View.VISIBLE) {
                    validatePassword()
                }
            }
        })
    }
    
    private fun setupClickListeners() {
        btnLogin.setOnClickListener {
            if (validateForm()) {
                performLogin()
            }
        }
        
        btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }

        findViewById<TextView>(R.id.tvSignUp).setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
            finish()
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
            else -> {
                tvPasswordError.visibility = View.GONE
                true
            }
        }
    }
    
    private fun validateForm(): Boolean {
        val isEmailValid = validateEmail()
        val isPasswordValid = validatePassword()
        
        // Focus on first error field
        when {
            !isEmailValid -> etEmail.requestFocus()
            !isPasswordValid -> etPassword.requestFocus()
        }
        
        return isEmailValid && isPasswordValid
    }
    
    private fun performLogin() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()
        
        // Disable button to prevent double clicks
        btnLogin.isEnabled = false
        btnLogin.text = "Logging in..."
        
        // Try Firebase login first
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    handleFirebaseLoginSuccess()
                } else {
                    // Firebase failed - try local storage fallback
                    loginWithLocalStorage(email, password)
                }
            }
            .addOnFailureListener { exception ->
                btnLogin.isEnabled = true
                btnLogin.text = "Login"
                
                // Show specific error messages
                val errorMessage = when {
                    exception.message?.contains("network", ignoreCase = true) == true -> 
                        "Network error. Please check your connection."
                    exception.message?.contains("password", ignoreCase = true) == true -> 
                        "Invalid email or password"
                    exception.message?.contains("user", ignoreCase = true) == true -> 
                        "No account found with this email"
                    else -> "Login failed. Please try again."
                }
                
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            }
    }
    
    private fun handleFirebaseLoginSuccess() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    val userType = document.getString("userType") ?: "Renter"
                    navigateToHome(userType)
                }
                .addOnFailureListener {
                    // Default to MainActivity if can't get user type
                    navigateToHome("Renter")
                }
        } else {
            navigateToHome("Renter")
        }
    }
    
    private fun loginWithLocalStorage(email: String, password: String) {
        val sharedPreferences = getSharedPreferences("EasyDrivePrefs", MODE_PRIVATE)
        val savedEmail = sharedPreferences.getString("email", "")
        val savedPassword = sharedPreferences.getString("password", "")
        
        btnLogin.isEnabled = true
        btnLogin.text = "Login"
        
        if (savedEmail == email && savedPassword == password) {
            // Login successful
            sharedPreferences.edit().apply {
                putBoolean("isLoggedIn", true)
                putBoolean("useLocalStorage", true)
                apply()
            }
            
            val userType = sharedPreferences.getString("userType", "Renter") ?: "Renter"
            navigateToHome(userType)
        } else {
            tvPasswordError.text = "Invalid email or password"
            tvPasswordError.visibility = View.VISIBLE
            Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun navigateToHome(userType: String) {
        Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show()
        
        val intent = if (userType == "Agency") {
            Intent(this, AgencyDashboardActivity::class.java)
        } else {
            Intent(this, MainActivity::class.java)
        }
        
        startActivity(intent)
        finish()
    }
    
    private fun styleSignUpText() {
        val tvSignUp = findViewById<TextView>(R.id.tvSignUp)
        val signUpText = tvSignUp.text.toString()
        val spannableSignUp = android.text.SpannableString(signUpText)
        val signUpStart = signUpText.indexOf("Sign Up")
        
        if (signUpStart >= 0) {
            spannableSignUp.setSpan(
                android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                signUpStart,
                signUpStart + 7,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannableSignUp.setSpan(
                android.text.style.ForegroundColorSpan(0xFF212121.toInt()),
                signUpStart,
                signUpStart + 7,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            tvSignUp.text = spannableSignUp
        }
    }
    
    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }
    
    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount?) {
        if (account == null) {
            Toast.makeText(this, "Google sign in failed", Toast.LENGTH_SHORT).show()
            return
        }
        
        btnGoogleSignIn.isEnabled = false
        btnGoogleSignIn.text = "Signing in..."
        
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                btnGoogleSignIn.isEnabled = true
                btnGoogleSignIn.text = "Continue with Google"
                
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        // Check if user exists in Firestore
                        firestore.collection("users").document(user.uid)
                            .get()
                            .addOnSuccessListener { document ->
                                if (document.exists()) {
                                    // User exists, navigate to appropriate screen
                                    val userType = document.getString("userType") ?: "Renter"
                                    navigateToHome(userType)
                                } else {
                                    // New user, create profile
                                    createGoogleUserProfile(user.uid, account)
                                }
                            }
                            .addOnFailureListener {
                                // If Firestore fails, create profile anyway
                                createGoogleUserProfile(user.uid, account)
                            }
                    }
                } else {
                    Toast.makeText(this, "Authentication failed: ${task.exception?.message}", 
                        Toast.LENGTH_SHORT).show()
                }
            }
    }
    
    private fun createGoogleUserProfile(userId: String, account: GoogleSignInAccount) {
        val userData = hashMapOf(
            "email" to (account.email ?: ""),
            "name" to (account.displayName ?: ""),
            "userType" to "Renter",
            "createdAt" to System.currentTimeMillis()
        )
        
        firestore.collection("users").document(userId)
            .set(userData)
            .addOnSuccessListener {
                Toast.makeText(this, "Welcome to EasyDrive!", Toast.LENGTH_SHORT).show()
                navigateToHome("Renter")
            }
            .addOnFailureListener { e ->
                Log.w("LoginActivity", "Error creating user profile", e)
                // Still navigate even if Firestore fails
                navigateToHome("Renter")
            }
    }
}
