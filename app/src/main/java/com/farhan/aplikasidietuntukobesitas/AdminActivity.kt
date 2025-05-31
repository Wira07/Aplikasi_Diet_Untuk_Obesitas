package com.farhan.aplikasidietuntukobesitas

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class AdminActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var userAdapter: UserAdapter
    private lateinit var btnRefresh: Button
    private lateinit var btnLogout: Button
    private val userList = mutableListOf<User>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        supportActionBar?.title = "Admin Panel - Data Users"

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupRecyclerView()
        setupBottomNavigation()
        loadUserData()
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.rv_users)
        userAdapter = UserAdapter(userList)
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
            loadUserData()
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

    private fun loadUserData() {
        Log.d("AdminActivity", "Starting to load user data...")

        // Load all users first, then filter by role
        db.collection("users")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("AdminActivity", "Error loading users", e)
                    Toast.makeText(this, "Error loading data: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                Log.d("AdminActivity", "Snapshot received, documents count: ${snapshot?.size() ?: 0}")

                userList.clear()
                var userCount = 0

                snapshot?.documents?.forEach { document ->
                    Log.d("AdminActivity", "Processing document: ${document.id}")
                    val role = document.getString("role") ?: "user"
                    Log.d("AdminActivity", "Document role: $role")

                    // Only include users with role "user" (exclude admin)
                    if (role == "user") {
                        val user = User(
                            id = document.id,
                            name = document.getString("name") ?: document.getString("email")?.substringBefore("@") ?: "User",
                            email = document.getString("email") ?: "N/A",
                            weight = document.getDouble("weight") ?: 0.0,
                            height = document.getDouble("height") ?: 0.0,
                            age = document.getLong("age")?.toInt() ?: 0,
                            gender = document.getString("gender") ?: "N/A",
                            bmi = document.getDouble("bmi") ?: 0.0
                        )
                        userList.add(user)
                        userCount++
                        Log.d("AdminActivity", "Added user: ${user.name} - ${user.email}")
                    }
                }

                Log.d("AdminActivity", "Total users loaded: ${userList.size}")
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
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("AdminActivity", "Error loading all users", e)
                    return@addSnapshotListener
                }

                Log.d("AdminActivity", "All users count: ${snapshot?.size() ?: 0}")
                snapshot?.documents?.forEach { document ->
                    Log.d("AdminActivity", "All users - ID: ${document.id}, Role: ${document.getString("role")}, Email: ${document.getString("email")}")
                }
            }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.admin_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                performLogout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}