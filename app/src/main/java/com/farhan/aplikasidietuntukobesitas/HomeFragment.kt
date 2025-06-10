package com.farhan.aplikasidietuntukobesitas

import android.animation.ObjectAnimator
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class HomeFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var tvWelcome: TextView
    private lateinit var tvDailyTip: TextView
    private lateinit var tvCurrentWeight: TextView
    private lateinit var tvTargetWeight: TextView
    private lateinit var tvWeightToLose: TextView
    private var currentWeight: Double = 0.0
    private var currentHeight: Double = 0.0

    // Listener untuk real-time updates
    private var tipsListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        initViews(view)
        loadUserData()
        animateCards(view)

        return view
    }

    private fun initViews(view: View) {
        tvWelcome = view.findViewById(R.id.tv_welcome)
        tvDailyTip = view.findViewById(R.id.tv_daily_tip)
        tvCurrentWeight = view.findViewById(R.id.tv_current_weight)
        tvTargetWeight = view.findViewById(R.id.tv_target_weight)
        tvWeightToLose = view.findViewById(R.id.tv_weight_to_lose)
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            db.collection("users").document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (!isAdded) return@addOnSuccessListener

                    if (document.exists()) {
                        val name = document.getString("name") ?: "User"
                        val weight = document.getDouble("weight") ?: 0.0
                        val height = document.getDouble("height") ?: 0.0

                        currentWeight = weight
                        currentHeight = height

                        // Animate welcome text
                        tvWelcome.text = "Selamat datang, $name!"
                        animateTextChange(tvWelcome)

                        // Load tips from Firebase (managed by pelatih) with real-time listener
                        setupTipsListener()

                        // Calculate and display weight targets
                        calculateAndDisplayWeightTarget(weight, height)
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("HomeFragment", "Error loading user data", exception)
                    // Handle error gracefully
                    if (isAdded) {
                        tvWelcome.text = "Selamat datang!"
                        setupTipsListener() // Still try to load tips
                        setDefaultWeightTarget()
                    }
                }
        }
    }

    private fun setupTipsListener() {
        Log.d("HomeFragment", "Setting up real-time tips listener...")

        // Remove any existing listener
        tipsListener?.remove()

        // Setup new listener for real-time updates
        tipsListener = db.collection("daily_tips")
            .whereEqualTo("isActive", true)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(15) // Get latest 15 active tips
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("HomeFragment", "Error listening to tips: ${e.message}", e)
                    if (isAdded) {
                        setFallbackTip()
                    }
                    return@addSnapshotListener
                }

                if (!isAdded) return@addSnapshotListener

                val firebaseTips = mutableListOf<String>()

                snapshots?.documents?.forEach { document ->
                    try {
                        val tipText = document.getString("text")
                        val createdBy = document.getString("createdByName") ?: "Pelatih"
                        val isActive = document.getBoolean("isActive") ?: false

                        if (tipText != null && tipText.isNotBlank() && isActive) {
                            // Format tip dengan informasi pembuat
                            val formattedTip = "💡 $tipText\n\n- $createdBy"
                            firebaseTips.add(formattedTip)
                            Log.d("HomeFragment", "Added tip: ${tipText.take(30)}...")
                        }
                    } catch (ex: Exception) {
                        Log.w("HomeFragment", "Error parsing tip document: ${ex.message}")
                    }
                }

                if (firebaseTips.isNotEmpty()) {
                    Log.d("HomeFragment", "Successfully loaded ${firebaseTips.size} tips from Firebase")
                    // Pilih tip secara random dari tips yang aktif
                    val randomTip = firebaseTips.random()
                    updateTipWithAnimation(randomTip)
                } else {
                    Log.d("HomeFragment", "No active tips found, using fallback")
                    setFallbackTip()
                }
            }
    }

    private fun updateTipWithAnimation(newTip: String) {
        // Cek apakah tip berbeda untuk menghindari animasi yang tidak perlu
        if (tvDailyTip.text.toString() != newTip) {
            Log.d("HomeFragment", "Updating tip with animation")

            // Fade out current tip, then fade in new tip
            tvDailyTip.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction {
                    if (isAdded) {
                        tvDailyTip.text = newTip
                        tvDailyTip.animate()
                            .alpha(1f)
                            .setDuration(300)
                            .start()
                    }
                }
                .start()
        } else {
            // If same text, just make sure it's visible
            tvDailyTip.alpha = 1f
        }
    }

    private fun setFallbackTip() {
        Log.d("HomeFragment", "Using fallback tips")
        val fallbackTips = listOf(
            "💧 Minum air putih minimal 8 gelas per hari untuk menjaga hidrasi tubuh.",
            "🥗 Konsumsi buah dan sayuran berwarna-warni setiap hari untuk nutrisi yang seimbang.",
            "🏃‍♂️ Lakukan olahraga ringan selama 30 menit setiap hari untuk menjaga kebugaran.",
            "😴 Tidur cukup 7-8 jam per hari untuk pemulihan tubuh yang optimal.",
            "🧘‍♀️ Luangkan waktu untuk relaksasi dan mengurangi stres dalam kehidupan harian.",
            "🚶‍♀️ Berjalan kaki minimal 10.000 langkah setiap hari untuk kesehatan jantung.",
            "🍎 Pilih makanan segar daripada makanan olahan untuk nutrisi terbaik."
        )

        val randomTip = fallbackTips.random() + "\n\n- Tips Default"
        updateTipWithAnimation(randomTip)
    }

    private fun calculateAndDisplayWeightTarget(weight: Double, height: Double) {
        if (weight > 0 && height > 0) {
            // Calculate ideal weight range (BMI 18.5-24.9)
            val heightInMeters = height / 100.0
            val idealWeightMin = 18.5 * heightInMeters * heightInMeters
            val idealWeightMax = 24.9 * heightInMeters * heightInMeters
            val idealWeightTarget = (idealWeightMin + idealWeightMax) / 2

            // Current weight
            tvCurrentWeight.text = String.format("%.1f kg", weight)
            animateWeightValue(tvCurrentWeight, 0.0, weight)

            // Target weight - set to ideal weight
            tvTargetWeight.text = String.format("%.1f kg", idealWeightTarget)
            animateWeightValue(tvTargetWeight, 0.0, idealWeightTarget)

            // Weight difference
            val weightDifference = weight - idealWeightTarget
            val weightToLoseText = when {
                weightDifference > 1 -> {
                    "Turun ${String.format("%.1f", weightDifference)} kg"
                }
                weightDifference < -1 -> {
                    "Naik ${String.format("%.1f", Math.abs(weightDifference))} kg"
                }
                else -> {
                    "Pertahankan berat badan"
                }
            }

            tvWeightToLose.text = weightToLoseText
            animateTextChange(tvWeightToLose)

            // Set color based on weight goal
            context?.let { ctx ->
                val textColor = when {
                    weightDifference > 5 -> R.color.status_obese
                    weightDifference > 0 -> R.color.status_overweight
                    weightDifference < -2 -> R.color.status_underweight
                    else -> R.color.status_normal
                }
                try {
                    tvWeightToLose.setTextColor(ContextCompat.getColor(ctx, textColor))
                } catch (e: Exception) {
                    // Fallback color if resource not found
                    tvWeightToLose.setTextColor(ContextCompat.getColor(ctx, android.R.color.black))
                }
            }
        }
    }

    private fun setDefaultWeightTarget() {
        tvCurrentWeight.text = "-- kg"
        tvTargetWeight.text = "-- kg"
        tvWeightToLose.text = "Data tidak tersedia"
    }

    private fun animateWeightValue(textView: TextView, startValue: Double, targetValue: Double) {
        val animator = ObjectAnimator.ofFloat(startValue.toFloat(), targetValue.toFloat())
        animator.duration = 1200
        animator.interpolator = DecelerateInterpolator()

        animator.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Float
            textView.text = String.format("%.1f kg", animatedValue)
        }

        // Add small delay to stagger animations
        animator.startDelay = 200
        animator.start()
    }

    private fun animateTextChange(textView: TextView) {
        textView.alpha = 0f
        textView.animate()
            .alpha(1f)
            .setDuration(800)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun animateCards(view: View) {
        val cards = listOf<View>(
            view.findViewById(R.id.tip_card),
            view.findViewById(R.id.stats_card),
            view.findViewById(R.id.weight_target_card)
        )

        cards.forEachIndexed { index, card ->
            card?.let {
                it.alpha = 0f
                it.translationY = 100f

                it.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(600)
                    .setStartDelay((index * 150).toLong())
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("HomeFragment", "Fragment resumed, refreshing data...")
        // Refresh data when fragment becomes visible
        loadUserData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("HomeFragment", "Fragment destroyed, cleaning up...")

        // Clean up any running animations or handlers
        tvDailyTip.handler?.removeCallbacksAndMessages(null)

        // Remove Firestore listener to prevent memory leaks
        tipsListener?.remove()
        tipsListener = null
    }

    override fun onPause() {
        super.onPause()
        Log.d("HomeFragment", "Fragment paused")
        // Listener tetap aktif saat pause untuk menjaga sinkronisasi real-time
    }
}