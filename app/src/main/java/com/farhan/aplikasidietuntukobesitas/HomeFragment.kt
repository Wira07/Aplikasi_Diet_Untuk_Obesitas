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
    private lateinit var tvBMI: TextView
    private lateinit var tvBMIStatus: TextView
    private lateinit var tvDailyTip: TextView

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
        setDailyTip()
        animateCards(view)

        return view
    }

    private fun initViews(view: View) {
        tvWelcome = view.findViewById(R.id.tv_welcome)
        tvBMI = view.findViewById(R.id.tv_bmi)
        tvBMIStatus = view.findViewById(R.id.tv_bmi_status)
        tvDailyTip = view.findViewById(R.id.tv_daily_tip)
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            db.collection("users").document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (!isAdded) return@addOnSuccessListener  // 🔒 Cegah crash jika fragment tidak aktif

                    if (document.exists()) {
                        val name = document.getString("name") ?: "User"
                        val bmi = document.getDouble("bmi") ?: 0.0

                        // Animate welcome text
                        tvWelcome.text = "Selamat datang, $name!"
                        animateTextChange(tvWelcome)

                        // Animate BMI value
                        animateBMIValue(bmi)

                        val bmiStatus = getBMIStatus(bmi)
                        tvBMIStatus.text = bmiStatus

                        // Set color and background based on BMI status with animation
                        setBMIStatusStyle(bmi, bmiStatus)
                    }
                }
                .addOnFailureListener { exception ->
                    // Handle error gracefully
                    if (isAdded) {
                        tvWelcome.text = "Selamat datang!"
                        tvBMI.text = "0.0"
                        tvBMIStatus.text = "Data tidak tersedia"
                    }
                }
        }
    }

    private fun setBMIStatusStyle(bmi: Double, status: String) {
        context?.let { ctx ->
            val (statusColor, backgroundColor) = when {
                bmi < 18.5 -> Pair(
                    R.color.status_underweight,
                    R.drawable.status_underweight_background
                )

                bmi < 25 -> Pair(R.color.status_normal, R.drawable.status_normal_background)
                bmi < 30 -> Pair(R.color.status_overweight, R.drawable.status_overweight_background)
                else -> Pair(R.color.status_obese, R.drawable.status_obese_background)
            }

            tvBMIStatus.setTextColor(ContextCompat.getColor(ctx, statusColor))
            tvBMIStatus.setBackgroundResource(backgroundColor)

            // Add subtle animation to status change
            animateTextChange(tvBMIStatus)
        }
    }

    private fun animateBMIValue(targetBMI: Double) {
        val animator = ObjectAnimator.ofFloat(0f, targetBMI.toFloat())
        animator.duration = 1500
        animator.interpolator = DecelerateInterpolator()

        animator.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Float
            tvBMI.text = String.format("%.1f", animatedValue)
        }

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
            view.findViewById(R.id.bmi_card),
            view.findViewById(R.id.tip_card),
            view.findViewById(R.id.stats_card)
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

    private fun getBMIStatus(bmi: Double): String {
        return when {
            bmi < 18.5 -> "Kurus"
            bmi < 25 -> "Normal"
            bmi < 30 -> "Kelebihan Berat Badan"
            else -> "Obesitas"
        }
    }

    private fun setDailyTip() {
        val healthyTips = listOf(
            "💧 Minum air putih minimal 8 gelas per hari untuk menjaga hidrasi tubuh",
            "🥗 Konsumsi buah dan sayuran berwarna-warni setiap hari",
            "🏃‍♂️ Olahraga ringan 30 menit setiap hari dapat meningkatkan metabolisme",
            "🚫 Hindari makanan berlemak tinggi dan makanan olahan",
            "🍽️ Makan dalam porsi kecil tapi sering untuk menjaga metabolisme",
            "😴 Tidur yang cukup 7-8 jam per hari untuk pemulihan optimal",
            "🧘‍♀️ Luangkan waktu untuk relaksasi dan mengurangi stress",
            "📱 Batasi waktu screen time terutama sebelum tidur",
            "🚶‍♀️ Jalan kaki minimal 10.000 langkah per hari",
            "🥛 Konsumsi protein berkualitas tinggi untuk menjaga massa otot"
        )

        val randomTip = healthyTips.random()
        tvDailyTip.text = randomTip

        // Add typewriter animation effect
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
        setDailyTip()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up any running animations or handlers
        tvDailyTip.handler?.removeCallbacksAndMessages(null)
    }

    // Helper function to add pulse animation to important elements
    private fun addPulseAnimation(view: View) {
        val pulseAnimator = ObjectAnimator.ofFloat(view, "scaleX", 1.0f, 1.05f, 1.0f)
        val pulseAnimatorY = ObjectAnimator.ofFloat(view, "scaleY", 1.0f, 1.05f, 1.0f)

        pulseAnimator.duration = 1000
        pulseAnimatorY.duration = 1000
        pulseAnimator.repeatCount = ObjectAnimator.INFINITE
        pulseAnimatorY.repeatCount = ObjectAnimator.INFINITE

        pulseAnimator.start()
        pulseAnimatorY.start()
    }

    // Method to refresh daily tip with smooth transition
    fun refreshDailyTip() {
        tvDailyTip.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                setDailyTip()
                tvDailyTip.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start()
            }
            .start()
    }
}