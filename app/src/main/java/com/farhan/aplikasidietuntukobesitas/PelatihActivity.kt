package com.farhan.aplikasidietuntukobesitas

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class PelatihActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var tvPelatihName: TextView
    private lateinit var tvTotalTips: TextView
    private lateinit var tvActiveTips: TextView
    private lateinit var btnAddTip: Button
    private lateinit var btnLogout: Button
    private lateinit var rvTips: RecyclerView
    private lateinit var tipsAdapter: TipsAdapter
    private val tipsList = mutableListOf<TipData>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pelatih)

        // Inisialisasi Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        initViews()
        setupRecyclerView()
        setupClickListeners()
        loadPelatihData()
        loadTips()
    }

    private fun initViews() {
        tvPelatihName = findViewById(R.id.tv_pelatih_name)
        tvTotalTips = findViewById(R.id.tv_total_tips)
        tvActiveTips = findViewById(R.id.tv_active_tips)
        btnAddTip = findViewById(R.id.btn_add_tip)
        btnLogout = findViewById(R.id.btn_logout)
        rvTips = findViewById(R.id.rv_tips)
    }

    private fun setupRecyclerView() {
        tipsAdapter = TipsAdapter(tipsList) { tip, action ->
            when (action) {
                "edit" -> showEditTipDialog(tip)
                "delete" -> showDeleteConfirmation(tip)
            }
        }
        rvTips.layoutManager = LinearLayoutManager(this)
        rvTips.adapter = tipsAdapter
    }

    private fun setupClickListeners() {
        btnAddTip.setOnClickListener {
            showAddTipDialog()
        }

        btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun loadPelatihData() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            db.collection("users").document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val name = document.getString("name") ?: "Pelatih"
                        tvPelatihName.text = name
                        Log.d("PelatihActivity", "Nama Pelatih dimuat: $name")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("PelatihActivity", "Error saat memuat data pelatih", e)
                    tvPelatihName.text = "Pelatih"
                }
        }
    }

    private fun loadTips() {
        db.collection("daily_tips")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("PelatihActivity", "Error memuat tips", e)
                    return@addSnapshotListener
                }

                tipsList.clear()
                var activeTipsCount = 0

                snapshots?.forEach { document ->
                    val tip = TipData(
                        id = document.id,
                        text = document.getString("text") ?: "",
                        createdByName = document.getString("createdByName") ?: "Unknown",
                        createdAt = document.getDate("createdAt") ?: Date(),
                        isActive = document.getBoolean("isActive") ?: true
                    )
                    tipsList.add(tip)
                    if (tip.isActive) activeTipsCount++
                }

                // Update UI
                tvTotalTips.text = tipsList.size.toString()
                tvActiveTips.text = activeTipsCount.toString()
                tipsAdapter.notifyDataSetChanged()
            }
    }

    private fun showAddTipDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Tambah Tips Harian")

        val input = android.widget.EditText(this)
        input.hint = "Masukkan tips baru untuk pengguna..."
        input.maxLines = 5
        input.inputType = android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
        builder.setView(input)

        builder.setPositiveButton("Tambah") { _, _ ->
            val tipText = input.text.toString().trim()
            if (tipText.isNotEmpty()) {
                addTipToFirebase(tipText)
            } else {
                Toast.makeText(this, "Tips tidak boleh kosong", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Batal", null)
        builder.show()
    }

    private fun addTipToFirebase(tipText: String) {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val tipData = hashMapOf(
                "text" to tipText,
                "createdBy" to user.uid,
                "createdByName" to tvPelatihName.text.toString(),
                "createdAt" to FieldValue.serverTimestamp(),
                "isActive" to true
            )

            db.collection("daily_tips")
                .add(tipData)
                .addOnSuccessListener { documentReference ->
                    Log.d("PelatihActivity", "Tip ditambahkan dengan ID: ${documentReference.id}")
                    Toast.makeText(this, "Tips berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Log.e("PelatihActivity", "Error menambahkan tip", e)
                    Toast.makeText(this, "Error menambahkan tips: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showEditTipDialog(tip: TipData) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Edit Tips")

        val input = android.widget.EditText(this)
        input.setText(tip.text)
        input.maxLines = 5
        input.inputType = android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
        builder.setView(input)

        builder.setPositiveButton("Update") { _, _ ->
            val newText = input.text.toString().trim()
            if (newText.isNotEmpty()) {
                updateTip(tip.id, newText)
            } else {
                Toast.makeText(this, "Tips tidak boleh kosong", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Batal", null)
        builder.show()
    }

    private fun updateTip(tipId: String, newText: String) {
        db.collection("daily_tips").document(tipId)
            .update("text", newText, "updatedAt", FieldValue.serverTimestamp())
            .addOnSuccessListener {
                Toast.makeText(this, "Tips berhasil diupdate!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("PelatihActivity", "Error mengupdate tip", e)
                Toast.makeText(this, "Error mengupdate tip: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDeleteConfirmation(tip: TipData) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Hapus Tips")
        builder.setMessage("Apakah Anda yakin ingin menghapus tips ini?\n\n\"${tip.text.take(50)}...\"")

        builder.setPositiveButton("Ya") { _, _ ->
            deleteTip(tip.id)
        }

        builder.setNegativeButton("Tidak", null)
        builder.show()
    }

    private fun deleteTip(tipId: String) {
        db.collection("daily_tips").document(tipId)
            .update("isActive", false)
            .addOnSuccessListener {
                Toast.makeText(this, "Tips berhasil dihapus!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("PelatihActivity", "Error menghapus tip", e)
                Toast.makeText(this, "Error menghapus tip: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showLogoutConfirmation() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Logout")
        builder.setMessage("Apakah Anda yakin ingin logout?")

        builder.setPositiveButton("Ya") { _, _ ->
            performLogout()
        }

        builder.setNegativeButton("Tidak", null)
        builder.show()
    }

    private fun performLogout() {
        auth.signOut()
        val intent = Intent(this, LoginActivity::class.java)
        intent.putExtra(LoginActivity.EXTRA_DISABLE_AUTO_LOGIN, true)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    data class TipData(
        val id: String,
        val text: String,
        val createdByName: String,
        val createdAt: Date,
        val isActive: Boolean = true
    )
}