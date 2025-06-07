package com.farhan.aplikasidietuntukobesitas

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.cardview.widget.CardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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

                        // Set general tips
                        setGeneralTip()

                        // Calculate and display weight targets
                        calculateAndDisplayWeightTarget(weight, height)
                    }
                }
                .addOnFailureListener { exception ->
                    // Handle error gracefully
                    if (isAdded) {
                        tvWelcome.text = "Selamat datang!"
                        setGeneralTip()
                        setDefaultWeightTarget()
                    }
                }
        }
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
                    weightDifference > 5 -> R.color.status_obese // Need significant weight loss
                    weightDifference > 0 -> R.color.status_overweight // Need some weight loss
                    weightDifference < -2 -> R.color.status_underweight // Need weight gain
                    else -> R.color.status_normal // Maintain weight
                }
                tvWeightToLose.setTextColor(ContextCompat.getColor(ctx, textColor))
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

    // General tips for healthy living
    private fun setGeneralTip() {
        val generalTips = listOf(
            "💧 Minum air putih minimal 8 gelas per hari untuk menjaga hidrasi tubuh. Air sangat penting untuk fungsi tubuh yang optimal, termasuk pencernaan, penyerapan nutrisi, dan pengaturan suhu tubuh. Cobalah untuk selalu membawa botol air kemanapun Anda pergi agar mudah mengingat untuk tetap terhidrasi.",
            "🥗 Konsumsi buah dan sayuran berwarna-warni setiap hari. Makanan dengan berbagai warna mengandung berbagai macam vitamin dan mineral yang baik untuk kesehatan tubuh Anda. Buah dan sayuran yang kaya serat juga dapat membantu menjaga pencernaan yang sehat dan mengurangi risiko penyakit.",
            "🏃‍♂️ Olahraga ringan selama 30 menit setiap hari dapat meningkatkan metabolisme dan membantu tubuh membakar kalori lebih efektif. Cobalah untuk berjalan cepat, bersepeda, atau mengikuti kelas yoga untuk menjaga kebugaran tubuh Anda tanpa merasa kelelahan.",
            "😴 Tidur yang cukup antara 7-8 jam per hari sangat penting untuk pemulihan tubuh yang optimal. Tidur yang berkualitas dapat meningkatkan daya tahan tubuh, memperbaiki mood, dan meningkatkan konsentrasi serta produktivitas pada keesokan harinya.",
            "🧘‍♀️ Luangkan waktu untuk relaksasi dan mengurangi stres. Stres yang berlebihan dapat memengaruhi kesehatan fisik dan mental Anda. Cobalah meditasi, pernapasan dalam, atau sekedar beristirahat di alam terbuka untuk menenangkan pikiran dan meredakan ketegangan.",
            "🍽️ Makan dengan porsi kecil tapi sering dapat membantu menjaga metabolisme tubuh tetap aktif. Alih-alih makan dalam porsi besar dalam sekali makan, cobalah untuk makan 4-6 kali sehari dengan porsi yang lebih kecil untuk menjaga energi tubuh tetap stabil sepanjang hari.",
            "🚶‍♀️ Jalan kaki setelah makan dapat membantu proses pencernaan dan mencegah perut kembung. Cobalah berjalan kaki selama 10-15 menit setelah makan untuk membantu tubuh mencerna makanan dengan lebih baik.",
            "🥜 Konsumsi protein sehat seperti kacang-kacangan, ikan, dan sumber protein nabati lainnya. Protein sangat penting untuk pembentukan otot, memperbaiki jaringan tubuh, dan meningkatkan metabolisme. Ikan seperti salmon dan tuna kaya akan asam lemak omega-3 yang bermanfaat bagi kesehatan jantung.",
            "🍌 Buah-buahan segar lebih baik dari jus buah kemasan. Buah segar mengandung serat alami yang membantu pencernaan dan memberi rasa kenyang lebih lama. Jus buah kemasan seringkali mengandung tambahan gula yang dapat meningkatkan kadar kalori secara signifikan.",
            "⏰ Buat jadwal makan yang teratur setiap hari untuk membantu tubuh mengatur pola makan dan pencernaan. Dengan jadwal makan yang teratur, tubuh akan lebih mudah mencerna makanan dan menjaga keseimbangan energi sepanjang hari."
        )

        val randomTip = generalTips.random()
        tvDailyTip.text = randomTip
        animateTypewriter(tvDailyTip, randomTip)
    }


    private fun animateTypewriter(textView: TextView, fullText: String) {
        textView.text = ""
        var currentIndex = 0

        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                if (currentIndex <= fullText.length && isAdded) {
                    textView.text = fullText.substring(0, currentIndex)
                    currentIndex++
                    if (currentIndex <= fullText.length) {
                        handler.postDelayed(this, 30) // Adjust speed here
                    }
                }
            }
        }

        // Start animation after a small delay
        handler.postDelayed(runnable, 500)
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when fragment becomes visible
        loadUserData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up any running animations or handlers
        tvDailyTip.handler?.removeCallbacksAndMessages(null)
    }

    // Method to refresh daily tip with smooth transition
    fun refreshDailyTip() {
        tvDailyTip.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                setGeneralTip()
                tvDailyTip.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start()
            }
            .start()
    }
}