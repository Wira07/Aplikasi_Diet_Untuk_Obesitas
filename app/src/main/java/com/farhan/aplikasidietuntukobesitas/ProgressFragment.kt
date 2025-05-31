package com.farhan.aplikasidietuntukobesitas

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProgressFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var tvCurrentWeight: TextView
    private lateinit var tvTargetWeight: TextView
    private lateinit var tvWeightLoss: TextView

    // Additional UI elements for enhanced design
    private lateinit var progressBar: ProgressBar
    private lateinit var tvProgressPercentage: TextView
    private lateinit var tvMotivationalText: TextView
    private lateinit var cardCurrentWeight: CardView
    private lateinit var cardTargetWeight: CardView
    private lateinit var cardWeightLoss: CardView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_progress, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        initViews(view)
        setupInitialAnimations()
        loadProgressData()

        return view
    }

    private fun initViews(view: View) {
        tvCurrentWeight = view.findViewById(R.id.tv_current_weight)
        tvTargetWeight = view.findViewById(R.id.tv_target_weight)
        tvWeightLoss = view.findViewById(R.id.tv_weight_loss)

        // Initialize additional views if they exist in layout
        try {
            progressBar = view.findViewById(R.id.progress_bar_weight)
            tvProgressPercentage = view.findViewById(R.id.tv_progress_percentage)
            tvMotivationalText = view.findViewById(R.id.tv_motivational_text)
            cardCurrentWeight = view.findViewById(R.id.card_current_weight)
            cardTargetWeight = view.findViewById(R.id.card_target_weight)
            cardWeightLoss = view.findViewById(R.id.card_weight_loss)
        } catch (e: Exception) {
            // Views don't exist in current layout, continue with basic functionality
        }
    }

    private fun setupInitialAnimations() {
        // Fade in animation for cards
        val cards = listOf(
            view?.findViewById<CardView>(R.id.card_current_weight),
            view?.findViewById<CardView>(R.id.card_target_weight),
            view?.findViewById<CardView>(R.id.card_weight_loss)
        )

        cards.forEachIndexed { index, card ->
            card?.let {
                it.alpha = 0f
                it.translationY = 100f

                Handler(Looper.getMainLooper()).postDelayed({
                    ObjectAnimator.ofFloat(it, "alpha", 0f, 1f).apply {
                        duration = 600
                        interpolator = AccelerateDecelerateInterpolator()
                        start()
                    }

                    ObjectAnimator.ofFloat(it, "translationY", 100f, 0f).apply {
                        duration = 600
                        interpolator = AccelerateDecelerateInterpolator()
                        start()
                    }
                }, (index * 150).toLong())
            }
        }
    }

    private fun loadProgressData() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            db.collection("users").document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val currentWeight = document.getDouble("weight") ?: 0.0
                        val height = document.getDouble("height") ?: 0.0

                        // Calculate ideal weight (simple formula) - Original logic preserved
                        val idealWeight = (height - 100) - ((height - 100) * 0.1)
                        val weightToLose = currentWeight - idealWeight

                        // Animate weight values
                        animateWeightValue(tvCurrentWeight, currentWeight, "kg")
                        animateWeightValue(tvTargetWeight, idealWeight, "kg")

                        if (weightToLose > 0) {
                            animateWeightValue(tvWeightLoss, weightToLose, "kg")
                            updateMotivationalText(weightToLose)
                            updateProgressBar(currentWeight, idealWeight)
                        } else {
                            tvWeightLoss.text = "Target tercapai!"
                            animateSuccessState()
                        }
                    }
                }
                .addOnFailureListener {
                    // Handle error gracefully
                    showErrorState()
                }
        }
    }

    private fun animateWeightValue(textView: TextView, targetValue: Double, unit: String) {
        val animator = ValueAnimator.ofFloat(0f, targetValue.toFloat())
        animator.duration = 1000
        animator.interpolator = AccelerateDecelerateInterpolator()

        animator.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Float
            textView.text = "${animatedValue.toInt()} $unit"
        }

        // Add slight delay for staggered effect
        Handler(Looper.getMainLooper()).postDelayed({
            animator.start()
        }, 300)
    }

    private fun updateProgressBar(currentWeight: Double, targetWeight: Double) {
        try {
            val totalWeightToLose = currentWeight - targetWeight
            val progressPercentage = if (totalWeightToLose > 0) {
                ((currentWeight - targetWeight) / totalWeightToLose * 100).toInt()
            } else {
                100
            }

            // Animate progress bar
            val animator = ObjectAnimator.ofInt(progressBar, "progress", 0, progressPercentage)
            animator.duration = 1500
            animator.interpolator = AccelerateDecelerateInterpolator()
            animator.start()

            // Animate percentage text
            val percentAnimator = ValueAnimator.ofInt(0, progressPercentage)
            percentAnimator.duration = 1500
            percentAnimator.addUpdateListener { animation ->
                tvProgressPercentage.text = "${animation.animatedValue}%"
            }
            percentAnimator.start()

        } catch (e: Exception) {
            // Progress bar elements don't exist, continue without them
        }
    }

    private fun updateMotivationalText(weightToLose: Double) {
        try {
            val motivationalMessages = listOf(
                "Kamu bisa melakukannya! 💪",
                "Setiap langkah mendekatkan pada tujuan! 🌟",
                "Konsistensi adalah kunci sukses! 🗝️",
                "Percaya pada prosesnya! ✨",
                "Kamu lebih kuat dari yang kamu kira! 🔥"
            )

            val message = when {
                weightToLose <= 2 -> "Hampir sampai target! Pertahankan! 🎯"
                weightToLose <= 5 -> "Progress yang luar biasa! Terus semangat! 🚀"
                weightToLose <= 10 -> motivationalMessages.random()
                else -> "Perjalanan dimulai dari satu langkah! 👣"
            }

            tvMotivationalText.text = message

            // Add gentle pulse animation
            val pulseAnimator = ObjectAnimator.ofFloat(tvMotivationalText, "alpha", 0.7f, 1f)
            pulseAnimator.duration = 1000
            pulseAnimator.repeatCount = ObjectAnimator.INFINITE
            pulseAnimator.repeatMode = ObjectAnimator.REVERSE
            pulseAnimator.start()

        } catch (e: Exception) {
            // Motivational text doesn't exist, continue without it
        }
    }

    private fun animateSuccessState() {
        try {
            // Create celebration effect
            val cards = listOf(cardCurrentWeight, cardTargetWeight, cardWeightLoss)
            cards.forEach { card ->
                val scaleX = ObjectAnimator.ofFloat(card, "scaleX", 1f, 1.1f, 1f)
                val scaleY = ObjectAnimator.ofFloat(card, "scaleY", 1f, 1.1f, 1f)

                scaleX.duration = 600
                scaleY.duration = 600

                scaleX.start()
                scaleY.start()
            }

            tvMotivationalText.text = "🎉 Selamat! Target tercapai! 🎉"
            progressBar.progress = 100
            tvProgressPercentage.text = "100%"

        } catch (e: Exception) {
            // Continue with basic success message
        }
    }

    private fun showErrorState() {
        tvCurrentWeight.text = "-- kg"
        tvTargetWeight.text = "-- kg"
        tvWeightLoss.text = "-- kg"

        try {
            tvMotivationalText.text = "Gagal memuat data. Coba lagi nanti."
        } catch (e: Exception) {
            // Continue without motivational text
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when fragment becomes visible
        loadProgressData()
    }
}