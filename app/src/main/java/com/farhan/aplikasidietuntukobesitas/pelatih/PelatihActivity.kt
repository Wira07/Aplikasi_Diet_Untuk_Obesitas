package com.farhan.aplikasidietuntukobesitas.pelatih

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
import com.farhan.aplikasidietuntukobesitas.form.LoginActivity
import com.farhan.aplikasidietuntukobesitas.R
import com.farhan.aplikasidietuntukobesitas.adapter.TipsAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
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
        // PERBAIKAN: Load semua tips (active dan inactive) untuk panel pelatih
        db.collection("daily_tips")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("PelatihActivity", "Error memuat tips", e)
                    return@addSnapshotListener
                }

                tipsList.clear()
                var activeTipsCount = 0
                var totalTipsCount = 0

                snapshots?.forEach { document ->
                    val tip = TipData(
                        id = document.id,
                        text = document.getString("text") ?: "",
                        createdByName = document.getString("createdByName") ?: "Unknown",
                        createdAt = document.getDate("createdAt") ?: Date(),
                        isActive = document.getBoolean("isActive") ?: true
                    )
                    tipsList.add(tip)
                    totalTipsCount++
                    if (tip.isActive) activeTipsCount++

                    Log.d("PelatihActivity", "Loaded tip: ${tip.text.take(30)}..., Active: ${tip.isActive}")
                }

                // Update UI
                tvTotalTips.text = totalTipsCount.toString()
                tvActiveTips.text = activeTipsCount.toString()
                tipsAdapter.notifyDataSetChanged()

                Log.d("PelatihActivity", "Total tips: $totalTipsCount, Active tips: $activeTipsCount")
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
            // PERBAIKAN: Pastikan struktur data yang konsisten
            val tipData = hashMapOf(
                "text" to tipText,
                "createdBy" to user.uid,
                "createdByName" to tvPelatihName.text.toString(),
                "createdAt" to FieldValue.serverTimestamp(),
                "isActive" to true  // Pastikan tips baru selalu aktif
            )

            Log.d("PelatihActivity", "Adding tip to Firebase: $tipText")
            Log.d("PelatihActivity", "Created by: ${tvPelatihName.text}")

            db.collection("daily_tips")
                .add(tipData)
                .addOnSuccessListener { documentReference ->
                    Log.d("PelatihActivity", "Tip ditambahkan dengan sukses! ID: ${documentReference.id}")
                    Toast.makeText(this, "Tips berhasil ditambahkan dan akan segera muncul di halaman user!", Toast.LENGTH_LONG).show()
                }
                .addOnFailureListener { e ->
                    Log.e("PelatihActivity", "Error menambahkan tip", e)
                    Toast.makeText(this, "Error menambahkan tips: ${e.message}", Toast.LENGTH_LONG).show()
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
        Log.d("PelatihActivity", "Updating tip with ID: $tipId")

        db.collection("daily_tips").document(tipId)
            .update(
                "text", newText,
                "updatedAt", FieldValue.serverTimestamp(),
                "isActive", true  // Pastikan tip yang diupdate tetap aktif
            )
            .addOnSuccessListener {
                Log.d("PelatihActivity", "Tip berhasil diupdate!")
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
        builder.setMessage("Apakah Anda yakin ingin menghapus tips ini secara permanen?\n\n\"${tip.text.take(50)}...\"")

        builder.setPositiveButton("Ya") { _, _ ->
            deleteTip(tip.id)
        }

        builder.setNegativeButton("Tidak", null)
        builder.show()
    }

    private fun deleteTip(tipId: String) {
        Log.d("PelatihActivity", "Deleting tip with ID: $tipId")

        // Hapus dokumen secara permanen dari Firestore
        db.collection("daily_tips").document(tipId)
            .delete()
            .addOnSuccessListener {
                Log.d("PelatihActivity", "Tips berhasil dihapus permanen dengan ID: $tipId")
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