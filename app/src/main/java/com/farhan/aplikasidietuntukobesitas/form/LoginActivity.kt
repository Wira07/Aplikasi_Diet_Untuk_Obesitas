package com.farhan.aplikasidietuntukobesitas.form

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.farhan.aplikasidietuntukobesitas.navigasi.MainActivity
import com.farhan.aplikasidietuntukobesitas.R
import com.farhan.aplikasidietuntukobesitas.admin.AdminActivity
import com.farhan.aplikasidietuntukobesitas.pelatih.PelatihActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvRegister: TextView

    private var retryCount = 0
    private val maxRetries = 2
    private var hasRedirected = false // Flag untuk mencegah redirect berulang

    companion object {
        const val EXTRA_DISABLE_AUTO_LOGIN = "disable_auto_login"

        // Fungsi helper untuk logout yang benar
        fun createLogoutIntent(context: android.content.Context): Intent {
            val intent = Intent(context, LoginActivity::class.java)
            intent.putExtra(EXTRA_DISABLE_AUTO_LOGIN, true)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        initViews()
        setupClickListeners()

        // Cek apakah auto login dinonaktifkan (misalnya setelah logout)
        val disableAutoLogin = intent.getBooleanExtra(EXTRA_DISABLE_AUTO_LOGIN, false)

        if (!disableAutoLogin && !hasRedirected) {
            // Cek apakah user sudah login sebelumnya hanya jika auto login tidak dinonaktifkan
            checkAutoLogin()
        } else {
            Log.d("LoginActivity", "Auto login disabled or already redirected, showing login form")
        }
    }

    override fun onStart() {
        super.onStart()
        // Hanya lakukan auto login jika belum pernah redirect dan auto login tidak dinonaktifkan
        val disableAutoLogin = intent.getBooleanExtra(EXTRA_DISABLE_AUTO_LOGIN, false)

        if (!disableAutoLogin && !hasRedirected) {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                Log.d("LoginActivity", "onStart: User still logged in: ${currentUser.uid}")
                // Langsung redirect tanpa menampilkan Toast lagi
                checkUserRoleAndRedirect(currentUser.uid, false)
            }
        } else {
            Log.d("LoginActivity", "onStart: Auto login disabled or already redirected")
        }
    }

    private fun initViews() {
        etEmail = findViewById(R.id.et_email)
        etPassword = findViewById(R.id.et_password)
        btnLogin = findViewById(R.id.btn_login)
        tvRegister = findViewById(R.id.tv_register)
    }

    private fun setupClickListeners() {
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Mohon isi semua field", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loginUser(email, password)
        }

        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun checkAutoLogin() {
        val currentUser = auth.currentUser
        if (currentUser != null && !hasRedirected) {
            Log.d("LoginActivity", "User already logged in: ${currentUser.uid}")
            Log.d("LoginActivity", "User email: ${currentUser.email}")

            // User sudah login, cek role dan redirect otomatis tanpa toast
            checkUserRoleAndRedirect(currentUser.uid, false)
        } else {
            Log.d("LoginActivity", "No user logged in or already redirected, showing login form")
            // Tampilkan form login jika belum login
        }
    }

    private fun loginUser(email: String, password: String) {
        // Tambahkan loading indicator
        btnLogin.isEnabled = false
        btnLogin.text = "Loading..."

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                // Reset button state
                btnLogin.isEnabled = true
                btnLogin.text = "Login"

                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let {
                        Log.d("LoginActivity", "Login successful for user: ${it.uid}")
                        checkUserRoleAndRedirect(it.uid, true)
                    }
                } else {
                    Log.e("LoginActivity", "Login failed", task.exception)
                    Toast.makeText(this, "Login gagal: ${task.exception?.message}",
                        Toast.LENGTH_SHORT).show()
                }
            }
    }

    // Fungsi baru yang menggabungkan checkUserRole dengan parameter untuk menampilkan toast
    private fun checkUserRoleAndRedirect(uid: String, showToast: Boolean) {
        Log.d("LoginActivity", "Checking role for user: $uid")

        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { document ->
                Log.d("LoginActivity", "Document exists: ${document.exists()}")

                if (document.exists()) {
                    val role = document.getString("role") ?: "user"
                    val email = document.getString("email") ?: ""

                    Log.d("LoginActivity", "User role: '$role', Email: '$email'")
                    Log.d("LoginActivity", "Role after lowercase: '${role.lowercase()}'")

                    // Tambahkan case insensitive check untuk role
                    when (role.lowercase().trim()) {
                        "admin" -> {
                            Log.d("LoginActivity", "Redirecting to AdminActivity")
                            if (showToast) {
                                Toast.makeText(this, "Selamat datang Admin!", Toast.LENGTH_SHORT).show()
                            }
                            redirectToAdmin()
                        }
                        "pelatih" -> {
                            Log.d("LoginActivity", "Redirecting to PelatihActivity")
                            if (showToast) {
                                Toast.makeText(this, "Selamat datang Pelatih!", Toast.LENGTH_SHORT).show()
                            }
                            redirectToPelatih()
                        }
                        "user" -> {
                            Log.d("LoginActivity", "Redirecting to MainActivity")
                            if (showToast) {
                                Toast.makeText(this, "Login berhasil!", Toast.LENGTH_SHORT).show()
                            }
                            redirectToUser()
                        }
                        else -> {
                            // Fallback untuk role yang tidak dikenali
                            Log.d("LoginActivity", "Unknown role: '$role', defaulting to user")
                            if (showToast) {
                                Toast.makeText(this, "Login berhasil!", Toast.LENGTH_SHORT).show()
                            }
                            redirectToUser()
                        }
                    }
                } else {
                    Log.w("LoginActivity", "Document doesn't exist, creating default user document")
                    createUserDocument(uid, showToast)
                }
            }
            .addOnFailureListener { e ->
                Log.e("LoginActivity", "Error getting user data", e)

                // Cek jika error adalah permission denied
                if (e.message?.contains("PERMISSION_DENIED") == true ||
                    e.message?.contains("Missing or insufficient permissions") == true) {
                    Log.w("LoginActivity", "Permission denied, creating user document with retry")
                    createUserDocument(uid, showToast)
                } else if (isNetworkError(e)) {
                    Log.w("LoginActivity", "Network error, retrying...")
                    retryCheckUserRole(uid, showToast)
                } else {
                    if (showToast) {
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                    // Fallback: redirect ke MainActivity jika gagal mengambil data
                    redirectToUser()
                }
            }
    }

    // Fungsi lama untuk backward compatibility
    private fun checkUserRole(uid: String) {
        checkUserRoleAndRedirect(uid, true)
    }

    private fun createUserDocument(uid: String, showToast: Boolean = true) {
        val currentEmail = auth.currentUser?.email ?: ""

        // Buat dokumen user default jika tidak ada
        val userMap = hashMapOf(
            "email" to currentEmail,
            "role" to "user",
            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            "uid" to uid
        )

        db.collection("users").document(uid)
            .set(userMap)
            .addOnSuccessListener {
                Log.d("LoginActivity", "User document created successfully")
                if (showToast) {
                    Toast.makeText(this, "Login berhasil!", Toast.LENGTH_SHORT).show()
                }
                redirectToUser()
            }
            .addOnFailureListener { e ->
                Log.e("LoginActivity", "Error creating user document", e)

                // Jika gagal membuat dokumen karena permission, tetap lanjutkan sebagai user biasa
                if (e.message?.contains("PERMISSION_DENIED") == true ||
                    e.message?.contains("Missing or insufficient permissions") == true) {
                    Log.w("LoginActivity", "Permission denied for creating document, proceeding as regular user")
                    if (showToast) {
                        Toast.makeText(this, "Login berhasil! (Mode terbatas)", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    if (showToast) {
                        Toast.makeText(this, "Login berhasil!", Toast.LENGTH_SHORT).show()
                    }
                }

                // Tetap redirect ke MainActivity meskipun gagal buat dokumen
                redirectToUser()
            }
    }

    private fun redirectToAdmin() {
        hasRedirected = true // Set flag sebelum redirect
        val intent = Intent(this, AdminActivity::class.java)
        // Clear activity stack agar tidak bisa kembali ke login dengan back button
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun redirectToPelatih() {
        hasRedirected = true // Set flag sebelum redirect
        val intent = Intent(this, PelatihActivity::class.java)
        // Clear activity stack agar tidak bisa kembali ke login dengan back button
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun redirectToUser() {
        hasRedirected = true // Set flag sebelum redirect
        val intent = Intent(this, MainActivity::class.java)
        // Clear activity stack agar tidak bisa kembali ke login dengan back button
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        // Keluar dari aplikasi jika di halaman login
        super.onBackPressed()
        finishAffinity()
    }

    private fun retryCheckUserRole(uid: String, showToast: Boolean = true) {
        if (retryCount < maxRetries) {
            retryCount++
            Log.d("LoginActivity", "Retrying checkUserRole, attempt: $retryCount")

            // Langsung retry tanpa delay
            checkUserRoleAndRedirect(uid, showToast)
        } else {
            Log.w("LoginActivity", "Max retries reached, proceeding as regular user")
            if (showToast) {
                Toast.makeText(this, "Login berhasil! (Mode offline)", Toast.LENGTH_SHORT).show()
            }
            redirectToUser()
        }
    }

    private fun isNetworkError(exception: Exception): Boolean {
        val message = exception.message?.lowercase() ?: ""
        return message.contains("network") ||
                message.contains("connection") ||
                message.contains("timeout") ||
                message.contains("unavailable")
    }
}