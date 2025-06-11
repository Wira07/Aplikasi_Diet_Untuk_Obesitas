package com.farhan.aplikasidietuntukobesitas.users

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
import com.farhan.aplikasidietuntukobesitas.R
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
        Log.d("HomeFragment", "========= STARTING TIPS LISTENER SETUP =========")

        // Remove any existing listener
        tipsListener?.remove()

        // First, let's check if we can connect to Firestore
        db.collection("daily_tips")
            .get()
            .addOnSuccessListener { documents ->
                Log.d("HomeFragment", "✅ Successfully connected to Firestore")
                Log.d("HomeFragment", "📊 Total documents in daily_tips collection: ${documents.size()}")

                // Log all documents for debugging
                documents.forEachIndexed { index, document ->
                    val text = document.getString("text")
                    val isActive = document.getBoolean("isActive")
                    val createdBy = document.getString("createdBy")
                    val createdByName = document.getString("createdByName")
                    val createdAt = document.getDate("createdAt")

                    Log.d("HomeFragment", "📝 Document $index:")
                    Log.d("HomeFragment", "   - ID: ${document.id}")
                    Log.d("HomeFragment", "   - Text: $text")
                    Log.d("HomeFragment", "   - IsActive: $isActive")
                    Log.d("HomeFragment", "   - CreatedBy: $createdBy")
                    Log.d("HomeFragment", "   - CreatedByName: $createdByName")
                    Log.d("HomeFragment", "   - CreatedAt: $createdAt")
                }

                // Now setup the real-time listener
                setupRealTimeListener()
            }
            .addOnFailureListener { e ->
                Log.e("HomeFragment", "❌ Failed to connect to Firestore", e)
                setFallbackTip()
            }
    }

    private fun setupRealTimeListener() {
        Log.d("HomeFragment", "🔄 Setting up real-time listener...")

        // Try different query approaches
        tipsListener = db.collection("daily_tips")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("HomeFragment", "❌ Error in snapshot listener: ${e.message}", e)

                    // Try alternative approach without orderBy
                    setupAlternativeListener()
                    return@addSnapshotListener
                }

                if (!isAdded) {
                    Log.w("HomeFragment", "⚠️ Fragment not added, ignoring snapshot")
                    return@addSnapshotListener
                }

                Log.d("HomeFragment", "📥 Received snapshot update")
                processSnapshot(snapshots)
            }
    }

    private fun setupAlternativeListener() {
        Log.d("HomeFragment", "🔄 Setting up alternative listener (without orderBy)...")

        tipsListener?.remove()
        tipsListener = db.collection("daily_tips")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("HomeFragment", "❌ Error in alternative listener: ${e.message}", e)
                    setFallbackTip()
                    return@addSnapshotListener
                }

                if (!isAdded) return@addSnapshotListener

                Log.d("HomeFragment", "📥 Received alternative snapshot update")
                processSnapshot(snapshots)
            }
    }

    private fun processSnapshot(snapshots: com.google.firebase.firestore.QuerySnapshot?) {
        Log.d("HomeFragment", "🔍 Processing snapshot...")

        if (snapshots == null) {
            Log.w("HomeFragment", "⚠️ Snapshots is null")
            setFallbackTip()
            return
        }

        Log.d("HomeFragment", "📊 Snapshot contains ${snapshots.documents.size} documents")

        val allTips = mutableListOf<String>()
        val activeTips = mutableListOf<String>()

        snapshots.documents.forEachIndexed { index, document ->
            try {
                val tipText = document.getString("text")
                val createdBy = document.getString("createdBy")
                val createdByName = document.getString("createdByName") ?: "Pelatih"
                val isActive = document.getBoolean("isActive") ?: true

                Log.d("HomeFragment", "📋 Processing tip $index:")
                Log.d("HomeFragment", "   - Text: ${tipText?.take(50)}...")
                Log.d("HomeFragment", "   - CreatedBy: $createdBy")
                Log.d("HomeFragment", "   - CreatedByName: $createdByName")
                Log.d("HomeFragment", "   - IsActive: $isActive")

                if (tipText != null && tipText.isNotBlank()) {
                    val formattedTip = "💡 $tipText\n\n- $createdByName"
                    allTips.add(formattedTip)

                    if (isActive) {
                        activeTips.add(formattedTip)
                        Log.d("HomeFragment", "✅ Added active tip: ${tipText.take(30)}...")
                    } else {
                        Log.d("HomeFragment", "⏸️ Skipped inactive tip: ${tipText.take(30)}...")
                    }
                }
            } catch (ex: Exception) {
                Log.e("HomeFragment", "❌ Error parsing tip document at index $index", ex)
            }
        }

        Log.d("HomeFragment", "📈 Summary:")
        Log.d("HomeFragment", "   - Total tips: ${allTips.size}")
        Log.d("HomeFragment", "   - Active tips: ${activeTips.size}")

        when {
            activeTips.isNotEmpty() -> {
                val randomTip = activeTips.random()
                Log.d("HomeFragment", "🎯 Displaying active tip: ${randomTip.take(50)}...")
                updateTipWithAnimation(randomTip)
            }
            allTips.isNotEmpty() -> {
                val randomTip = allTips.random()
                Log.d("HomeFragment", "🎯 Displaying any available tip: ${randomTip.take(50)}...")
                updateTipWithAnimation(randomTip)
            }
            else -> {
                Log.d("HomeFragment", "❌ No tips found, showing fallback")
                setFallbackTip()
            }
        }
    }

    private fun updateTipWithAnimation(newTip: String) {
        Log.d("HomeFragment", "🎬 Updating tip with animation")
        Log.d("HomeFragment", "   - Current tip: ${tvDailyTip.text}")
        Log.d("HomeFragment", "   - New tip: $newTip")

        // Check if the tip is different to avoid unnecessary animations
        if (tvDailyTip.text.toString() != newTip) {
            Log.d("HomeFragment", "✨ Animating tip change")

            // Fade out the current tip, then fade in the new tip
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
            Log.d("HomeFragment", "🔄 Tip unchanged, keeping current display")
        }
    }

    private fun setFallbackTip() {
        Log.d("HomeFragment", "🔔 Showing 'no tips' message")
        val noTipsMessage = "🔔 Belum ada tips dari pelatih.\n\nSilakan tunggu pelatih menambahkan tips untuk Anda!"
        updateTipWithAnimation(noTipsMessage)
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
        Log.d("HomeFragment", "🔄 Fragment resumed, refreshing data...")
        // Refresh data when fragment becomes visible
        loadUserData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("HomeFragment", "🧹 Fragment destroyed, cleaning up...")

        // Clean up any running animations or handlers
        tvDailyTip.handler?.removeCallbacksAndMessages(null)

        // Remove Firestore listener to prevent memory leaks
        tipsListener?.remove()
        tipsListener = null
    }

    override fun onPause() {
        super.onPause()
        Log.d("HomeFragment", "⏸️ Fragment paused")
        // Listener tetap aktif saat pause untuk menjaga sinkronisasi real-time
    }
}