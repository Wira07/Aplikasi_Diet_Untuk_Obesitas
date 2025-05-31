package com.farhan.aplikasidietuntukobesitas

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var tvName: TextView? = null
    private var tvEmail: TextView? = null
    private var tvProfileName: TextView? = null
    private var tvProfileEmail: TextView? = null
    private var tvBmi: TextView? = null
    private var tvBmiStatus: TextView? = null
    private var tvAge: TextView? = null
    private var tvGender: TextView? = null
    private var tvHeight: TextView? = null
    private var tvWeight: TextView? = null
    private var btnLogout: Button? = null

    companion object {
        private const val TAG = "ProfileFragment"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        initViews(view)
        setupClickListeners()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Load data setelah view benar-benar siap
        loadUserProfile()
    }

    private fun initViews(view: View) {
        // ID yang sesuai dengan layout XML
        tvProfileName = view.findViewById(R.id.tv_profile_name)
        tvProfileEmail = view.findViewById(R.id.tv_profile_email)
        tvName = view.findViewById(R.id.tv_name) // Jika ada di layout lain
        tvEmail = view.findViewById(R.id.tv_email) // Jika ada di layout lain
        tvAge = view.findViewById(R.id.tv_age)
        tvGender = view.findViewById(R.id.tv_gender)
        tvHeight = view.findViewById(R.id.tv_height)
        tvWeight = view.findViewById(R.id.tv_weight)
        tvBmi = view.findViewById(R.id.tv_bmi)
        tvBmiStatus = view.findViewById(R.id.tv_bmi_status)
        btnLogout = view.findViewById(R.id.btn_logout)
    }

    private fun loadUserProfile() {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            Log.w(TAG, "No current user found")
            // Redirect ke login jika tidak ada user
            redirectToLogin()
            return
        }

        Log.d(TAG, "Loading profile for user: ${currentUser.uid}")

        db.collection("users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                // ✅ Cek apakah fragment masih terhubung ke activity & view masih valid
                if (!isAdded || view == null) {
                    Log.w(TAG, "Fragment not attached or view is null")
                    return@addOnSuccessListener
                }

                if (document.exists()) {
                    Log.d(TAG, "Document exists, loading data...")

                    // Ambil data dengan pengecekan null yang lebih detail
                    val name = document.getString("name")
                    val email = document.getString("email")
                    val age = document.getLong("age")
                    val gender = document.getString("gender")
                    val height = document.getDouble("height")
                    val weight = document.getDouble("weight")

                    Log.d(TAG, "Retrieved data - Name: $name, Email: $email, Age: $age")

                    // Set data ke TextView - hanya tampilkan jika data ada
                    val displayName = name ?: ""
                    val displayEmail = email ?: ""

                    // Update nama dan email di header
                    tvProfileName?.text = displayName
                    tvProfileEmail?.text = displayEmail

                    // Update nama dan email jika ada TextView lain
                    tvName?.text = displayName
                    tvEmail?.text = displayEmail

                    // Update data lainnya
                    tvAge?.text = if (age != null) "${age} tahun" else ""
                    tvGender?.text = gender ?: ""
                    tvHeight?.text = if (height != null) "${height.toInt()} cm" else ""
                    tvWeight?.text = if (weight != null) "${weight.toInt()} kg" else ""

                    // Hitung dan tampilkan BMI
                    if (height != null && weight != null && height > 0) {
                        val bmiValue = calculateBMI(weight, height)
                        tvBmi?.text = String.format("%.1f", bmiValue)
                        tvBmiStatus?.text = getBMIStatus(bmiValue)
                    }

                } else {
                    Log.w(TAG, "Document does not exist for user: ${currentUser.uid}")
                    showToast("Data profil tidak ditemukan")
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error getting document: ", exception)
                if (isAdded) {
                    showToast("Gagal memuat profil: ${exception.message}")
                }
            }
    }

    private fun setupClickListeners() {
        btnLogout?.setOnClickListener {
            auth.signOut()
            redirectToLogin()
        }
    }

    private fun redirectToLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        activity?.finish()
    }

    private fun calculateBMI(weight: Double, height: Double): Double {
        val heightInMeter = height / 100
        return weight / (heightInMeter * heightInMeter)
    }

    private fun getBMIStatus(bmi: Double): String {
        return when {
            bmi < 18.5 -> "Underweight"
            bmi < 25.0 -> "Normal"
            bmi < 30.0 -> "Overweight"
            else -> "Obese"
        }
    }

    private fun showToast(message: String) {
        if (isAdded) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh data ketika fragment kembali aktif
        if (auth.currentUser != null) {
            loadUserProfile()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // ✅ Bersihkan referensi ke view agar tidak menyebabkan crash setelah fragment dihancurkan
        tvName = null
        tvEmail = null
        tvProfileName = null
        tvProfileEmail = null
        tvAge = null
        tvGender = null
        tvHeight = null
        tvWeight = null
        tvBmi = null
        tvBmiStatus = null
        btnLogout = null
    }
}