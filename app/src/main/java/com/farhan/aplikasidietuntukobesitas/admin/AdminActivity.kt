package com.farhan.aplikasidietuntukobesitas.admin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.farhan.aplikasidietuntukobesitas.form.LoginActivity
import com.farhan.aplikasidietuntukobesitas.R
import com.farhan.aplikasidietuntukobesitas.data.User
import com.farhan.aplikasidietuntukobesitas.adapter.UserAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AdminActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var userAdapter: UserAdapter
    private lateinit var btnRefresh: Button
    private lateinit var btnLogout: Button

    // Stats TextViews
    private lateinit var tvTotalUsers: TextView
    private lateinit var tvActiveUsers: TextView
    private lateinit var tvAvgBmi: TextView
    private lateinit var tvLastUpdated: TextView

    private val userList = mutableListOf<User>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        supportActionBar?.title = "Admin Panel - Data Users"

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Check if user is authenticated before proceeding
        if (auth.currentUser == null) {
            Toast.makeText(this, "Anda harus login terlebih dahulu", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        initializeViews()
        setupRecyclerView()
        setupBottomNavigation()

        // Verify admin role before loading data
        verifyAdminRoleAndLoadData()
    }

    private fun initializeViews() {
        tvTotalUsers = findViewById(R.id.tv_total_users)
        tvActiveUsers = findViewById(R.id.tv_active_users)
        tvAvgBmi = findViewById(R.id.tv_avg_bmi)
        tvLastUpdated = findViewById(R.id.tv_last_updated)
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.rv_users)

        // Pass callback to refresh statistics when user is deleted
        userAdapter = UserAdapter(userList) {
            // Refresh statistics when a user is deleted
            updateStatisticsFromCurrentList()
        }

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@AdminActivity)
            adapter = userAdapter
        }
    }

    private fun setupBottomNavigation() {
        btnRefresh = findViewById(R.id.btn_refresh)
        btnLogout = findViewById(R.id.btn_logout)

        btnRefresh.setOnClickListener {
            Toast.makeText(this, "Memuat ulang data...", Toast.LENGTH_SHORT).show()
            verifyAdminRoleAndLoadData()
        }

        btnLogout.setOnClickListener {
            performLogout()
        }
    }

    private fun performLogout() {
        auth.signOut()
        Toast.makeText(this, "Berhasil logout", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun verifyAdminRoleAndLoadData() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Sesi telah berakhir, silakan login kembali", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        Log.d("AdminActivity", "Verifying admin role for user: ${currentUser.uid}")

        // First verify that current user has admin role
        db.collection("users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val userRole = document.getString("role") ?: "user"
                    Log.d("AdminActivity", "Current user role: $userRole")

                    if (userRole == "admin") {
                        loadUserData()
                    } else {
                        Toast.makeText(this, "Akses ditolak: Anda bukan admin", Toast.LENGTH_LONG).show()
                        performLogout()
                    }
                } else {
                    Log.e("AdminActivity", "User document not found")
                    Toast.makeText(this, "Data pengguna tidak ditemukan", Toast.LENGTH_SHORT).show()
                    performLogout()
                }
            }
            .addOnFailureListener { e ->
                Log.e("AdminActivity", "Error verifying admin role", e)
                Toast.makeText(this, "Error verifikasi: ${e.message}", Toast.LENGTH_SHORT).show()

                // If verification fails, try to reload user data anyway (fallback)
                loadUserData()
            }
    }

    private fun loadUserData() {
        Log.d("AdminActivity", "Starting to load user data...")

        // Add additional error handling and retry mechanism
        db.collection("users")
            .get()
            .addOnSuccessListener { querySnapshot ->
                Log.d("AdminActivity", "Successfully retrieved ${querySnapshot.size()} documents")

                userList.clear()
                var userCount = 0
                var totalBmi = 0.0
                var bmiCount = 0

                for (document in querySnapshot.documents) {
                    Log.d("AdminActivity", "Processing document: ${document.id}")
                    val role = document.getString("role") ?: "user"
                    Log.d("AdminActivity", "Document role: $role")

                    // Only include users with role "user" (exclude admin)
                    if (role == "user") {
                        try {
                            val bmi = document.getDouble("bmi") ?: 0.0
                            val user = User(
                                id = document.id,
                                name = document.getString("name") ?: document.getString("email")?.substringBefore("@") ?: "User",
                                email = document.getString("email") ?: "N/A",
                                weight = document.getDouble("weight") ?: 0.0,
                                height = document.getDouble("height") ?: 0.0,
                                age = document.getLong("age")?.toInt() ?: 0,
                                gender = document.getString("gender") ?: "N/A",
                                bmi = bmi
                            )
                            userList.add(user)
                            userCount++

                            // Calculate BMI statistics
                            if (bmi > 0) {
                                totalBmi += bmi
                                bmiCount++
                            }

                            Log.d("AdminActivity", "Added user: ${user.name} - ${user.email}")
                        } catch (e: Exception) {
                            Log.e("AdminActivity", "Error parsing user data for document ${document.id}", e)
                        }
                    }
                }

                Log.d("AdminActivity", "Total users loaded: ${userList.size}")

                // Update UI with statistics
                updateStatistics(userCount, bmiCount, totalBmi)
                updateLastUpdatedTime()

                userAdapter.notifyDataSetChanged()

                if (userList.isEmpty()) {
                    Toast.makeText(this, "Belum ada data user yang terdaftar", Toast.LENGTH_SHORT).show()
                    Log.d("AdminActivity", "No users found, loading all documents for debug...")
                    loadAllUsersForDebug()
                } else {
                    Toast.makeText(this, "Berhasil memuat $userCount data user", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("AdminActivity", "Error loading users", e)
                when {
                    e.message?.contains("PERMISSION_DENIED") == true -> {
                        Toast.makeText(this, "Akses ditolak: Periksa aturan Firebase Firestore", Toast.LENGTH_LONG).show()
                        // Try alternative approach with real-time listener
                        loadUserDataWithListener()
                    }
                    e.message?.contains("NETWORK") == true -> {
                        Toast.makeText(this, "Error jaringan: Periksa koneksi internet", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        Toast.makeText(this, "Error loading data: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    private fun updateStatistics(userCount: Int, bmiCount: Int, totalBmi: Double) {
        // Update total users
        tvTotalUsers.text = userCount.toString()

        // Update active users (simulate some active users - you can enhance this with real data)
        val activeUsers = (userCount * 0.6).toInt() // Simulate 60% active users
        tvActiveUsers.text = activeUsers.toString()

        // Update average BMI
        val avgBmi = if (bmiCount > 0) totalBmi / bmiCount else 0.0
        tvAvgBmi.text = String.format("%.1f", avgBmi)
    }

    // New method to update statistics from current list (used after deletion)
    private fun updateStatisticsFromCurrentList() {
        var userCount = userList.size
        var totalBmi = 0.0
        var bmiCount = 0

        userList.forEach { user ->
            if (user.bmi > 0) {
                totalBmi += user.bmi
                bmiCount++
            }
        }

        updateStatistics(userCount, bmiCount, totalBmi)
        updateLastUpdatedTime()
    }

    private fun updateLastUpdatedTime() {
        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        tvLastUpdated.text = "Updated at $currentTime"
    }

    private fun loadUserDataWithListener() {
        Log.d("AdminActivity", "Trying alternative approach with snapshot listener...")

        // Load all users first, then filter by role (original approach as fallback)
        db.collection("users")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("AdminActivity", "Error loading users with listener", e)
                    Toast.makeText(this, "Error loading data: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                Log.d("AdminActivity", "Snapshot received, documents count: ${snapshot?.size() ?: 0}")

                userList.clear()
                var userCount = 0
                var totalBmi = 0.0
                var bmiCount = 0

                snapshot?.documents?.forEach { document ->
                    Log.d("AdminActivity", "Processing document: ${document.id}")
                    val role = document.getString("role") ?: "user"
                    Log.d("AdminActivity", "Document role: $role")

                    // Only include users with role "user" (exclude admin)
                    if (role == "user") {
                        try {
                            val bmi = document.getDouble("bmi") ?: 0.0
                            val user = User(
                                id = document.id,
                                name = document.getString("name") ?: document.getString("email")?.substringBefore("@") ?: "User",
                                email = document.getString("email") ?: "N/A",
                                weight = document.getDouble("weight") ?: 0.0,
                                height = document.getDouble("height") ?: 0.0,
                                age = document.getLong("age")?.toInt() ?: 0,
                                gender = document.getString("gender") ?: "N/A",
                                bmi = bmi
                            )
                            userList.add(user)
                            userCount++

                            // Calculate BMI statistics
                            if (bmi > 0) {
                                totalBmi += bmi
                                bmiCount++
                            }

                            Log.d("AdminActivity", "Added user: ${user.name} - ${user.email}")
                        } catch (ex: Exception) {
                            Log.e("AdminActivity", "Error parsing user data for document ${document.id}", ex)
                        }
                    }
                }

                Log.d("AdminActivity", "Total users loaded: ${userList.size}")

                // Update UI with statistics
                updateStatistics(userCount, bmiCount, totalBmi)
                updateLastUpdatedTime()

                userAdapter.notifyDataSetChanged()

                if (userList.isEmpty()) {
                    Toast.makeText(this, "Belum ada data user yang terdaftar", Toast.LENGTH_SHORT).show()
                    Log.d("AdminActivity", "No users found, loading all documents for debug...")
                    loadAllUsersForDebug()
                } else {
                    Toast.makeText(this, "Berhasil memuat $userCount data user", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun loadAllUsersForDebug() {
        Log.d("AdminActivity", "Loading all users for debugging...")

        db.collection("users")
            .get()
            .addOnSuccessListener { querySnapshot ->
                Log.d("AdminActivity", "All users count: ${querySnapshot.size()}")
                querySnapshot.documents.forEach { document ->
                    Log.d("AdminActivity", "All users - ID: ${document.id}, Role: ${document.getString("role")}, Email: ${document.getString("email")}")
                }
            }
            .addOnFailureListener { e ->
                Log.e("AdminActivity", "Error loading all users for debug", e)
            }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.admin_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                Toast.makeText(this, "Memuat ulang data...", Toast.LENGTH_SHORT).show()
                verifyAdminRoleAndLoadData()
                true
            }
            R.id.action_logout -> {
                performLogout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning to this activity
        verifyAdminRoleAndLoadData()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up any listeners if needed
    }
}
