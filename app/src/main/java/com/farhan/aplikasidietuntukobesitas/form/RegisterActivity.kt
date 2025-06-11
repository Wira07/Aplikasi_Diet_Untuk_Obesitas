package com.farhan.aplikasidietuntukobesitas.form

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.farhan.aplikasidietuntukobesitas.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etWeight: EditText
    private lateinit var etHeight: EditText
    private lateinit var etAge: EditText
    private lateinit var rgGender: RadioGroup
    private lateinit var btnRegister: Button
    private lateinit var tvLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        etName = findViewById(R.id.et_name)
        etEmail = findViewById(R.id.et_email)
        etPassword = findViewById(R.id.et_password)
        etWeight = findViewById(R.id.et_weight)
        etHeight = findViewById(R.id.et_height)
        etAge = findViewById(R.id.et_age)
        rgGender = findViewById(R.id.rg_gender)
        btnRegister = findViewById(R.id.btn_register)
        tvLogin = findViewById(R.id.tv_login)
    }

    private fun setupClickListeners() {
        btnRegister.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val weight = etWeight.text.toString().trim()
            val height = etHeight.text.toString().trim()
            val age = etAge.text.toString().trim()

            if (validateInput(name, email, password, weight, height, age)) {
                registerUser(name, email, password, weight.toDouble(), height.toDouble(), age.toInt())
            }
        }

        tvLogin.setOnClickListener {
            finish()
        }
    }

    private fun validateInput(name: String, email: String, password: String,
                              weight: String, height: String, age: String): Boolean {
        if (name.isEmpty() || email.isEmpty() || password.isEmpty() ||
            weight.isEmpty() || height.isEmpty() || age.isEmpty()) {
            Toast.makeText(this, "Mohon isi semua field", Toast.LENGTH_SHORT).show()
            return false
        }

        if (rgGender.checkedRadioButtonId == -1) {
            Toast.makeText(this, "Mohon pilih jenis kelamin", Toast.LENGTH_SHORT).show()
            return false
        }

        if (password.length < 6) {
            Toast.makeText(this, "Password minimal 6 karakter", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun registerUser(name: String, email: String, password: String,
                             weight: Double, height: Double, age: Int) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let {
                        saveUserData(it.uid, name, email, weight, height, age)
                    }
                } else {
                    Toast.makeText(this, "Registrasi gagal: ${task.exception?.message}",
                        Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun saveUserData(uid: String, name: String, email: String,
                             weight: Double, height: Double, age: Int) {
        val gender = if (rgGender.checkedRadioButtonId == R.id.rb_male) "Laki-laki" else "Perempuan"
        val bmi = calculateBMI(weight, height)

        val userData = hashMapOf(
            "name" to name,
            "email" to email,
            "weight" to weight,
            "height" to height,
            "age" to age,
            "gender" to gender,
            "bmi" to bmi,
            "role" to "user",
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("users").document(uid)
            .set(userData)
            .addOnSuccessListener {
                Toast.makeText(this, "Registrasi berhasil! Silakan masuk dengan akun Anda.", Toast.LENGTH_LONG).show()

                // Sign out user after successful registration
                auth.signOut()

                // Navigate to LoginActivity
                val intent = Intent(this, LoginActivity::class.java)
                // Clear all previous activities and start fresh
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun calculateBMI(weight: Double, height: Double): Double {
        val heightInMeter = height / 100
        return weight / (heightInMeter * heightInMeter)
    }
}