package com.farhan.aplikasidietuntukobesitas

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
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
    private var btnEditProfile: Button? = null

    // Card views for animations
    private var personalInfoCard: CardView? = null
    private var bmiCard: CardView? = null
    private var profilePictureCard: CardView? = null

    companion object {
        private const val TAG = "ProfileFragment"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        initViews(view)
        setupClickListeners()
        setupInitialAnimations()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Load data setelah view benar-benar siap dengan delay untuk animasi
        Handler(Looper.getMainLooper()).postDelayed({
            loadUserProfile()
        }, 300)
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
        btnEditProfile = view.findViewById(R.id.btn_edit_profile)

        // Card views
        personalInfoCard = view.findViewById(R.id.card_personal_info)
        bmiCard = view.findViewById(R.id.card_bmi)
        profilePictureCard = view.findViewById(R.id.card_profile_picture)
    }

    private fun setupInitialAnimations() {
        // Set initial states for animations
        personalInfoCard?.apply {
            alpha = 0f
            translationY = 100f
        }

        bmiCard?.apply {
            alpha = 0f
            translationY = 100f
        }

        profilePictureCard?.apply {
            scaleX = 0f
            scaleY = 0f
        }

        btnLogout?.apply {
            alpha = 0f
            translationX = 100f
        }

        btnEditProfile?.apply {
            alpha = 0f
            translationX = -100f
        }
    }

    private fun animateViewsIn() {
        // Profile picture animation with bounce effect
        profilePictureCard?.let { card ->
            val scaleX = ObjectAnimator.ofFloat(card, "scaleX", 0f, 1.2f, 1f)
            val scaleY = ObjectAnimator.ofFloat(card, "scaleY", 0f, 1.2f, 1f)
            val rotation = ObjectAnimator.ofFloat(card, "rotation", 0f, 360f)

            val profileAnimSet = AnimatorSet()
            profileAnimSet.playTogether(scaleX, scaleY, rotation)
            profileAnimSet.duration = 800
            profileAnimSet.interpolator = DecelerateInterpolator()
            profileAnimSet.start()
        }

        // Cards slide in animation
        Handler(Looper.getMainLooper()).postDelayed({
            animateCardIn(personalInfoCard, 0)
        }, 200)

        Handler(Looper.getMainLooper()).postDelayed({
            animateCardIn(bmiCard, 100)
        }, 400)

        // Buttons slide in animation
        Handler(Looper.getMainLooper()).postDelayed({
            animateButtonsIn()
        }, 600)

        // Text shimmer effect - DIHILANGKAN
        // Handler(Looper.getMainLooper()).postDelayed({
        //     startTextShimmerEffect()
        // }, 800)
    }

    private fun animateCardIn(card: CardView?, delay: Long = 0) {
        card?.let {
            Handler(Looper.getMainLooper()).postDelayed({
                val fadeIn = ObjectAnimator.ofFloat(it, "alpha", 0f, 1f)
                val slideUp = ObjectAnimator.ofFloat(it, "translationY", 100f, 0f)
                val scaleX = ObjectAnimator.ofFloat(it, "scaleX", 0.8f, 1f)
                val scaleY = ObjectAnimator.ofFloat(it, "scaleY", 0.8f, 1f)

                val animSet = AnimatorSet()
                animSet.playTogether(fadeIn, slideUp, scaleX, scaleY)
                animSet.duration = 600
                animSet.interpolator = DecelerateInterpolator()
                animSet.start()

                // Add subtle bounce animation
                Handler(Looper.getMainLooper()).postDelayed({
                    val bounce = ObjectAnimator.ofFloat(it, "translationY", 0f, -10f, 0f)
                    bounce.duration = 300
                    bounce.start()
                }, 600)
            }, delay)
        }
    }

    private fun animateButtonsIn() {
        btnEditProfile?.let { btn ->
            val fadeIn = ObjectAnimator.ofFloat(btn, "alpha", 0f, 1f)
            val slideIn = ObjectAnimator.ofFloat(btn, "translationX", -100f, 0f)

            val animSet = AnimatorSet()
            animSet.playTogether(fadeIn, slideIn)
            animSet.duration = 500
            animSet.interpolator = DecelerateInterpolator()
            animSet.start()
        }

        btnLogout?.let { btn ->
            val fadeIn = ObjectAnimator.ofFloat(btn, "alpha", 0f, 1f)
            val slideIn = ObjectAnimator.ofFloat(btn, "translationX", 100f, 0f)

            val animSet = AnimatorSet()
            animSet.playTogether(fadeIn, slideIn)
            animSet.duration = 500
            animSet.interpolator = DecelerateInterpolator()
            animSet.start()
        }
    }

    // FUNGSI INI MASIH ADA TAPI TIDAK DIPANGGIL
    private fun startTextShimmerEffect() {
        // Create shimmer effect for profile name
        tvProfileName?.let { textView ->
            val shimmer = ObjectAnimator.ofFloat(textView, "alpha", 1f, 0.6f, 1f)
            shimmer.duration = 2000
            shimmer.repeatCount = ValueAnimator.INFINITE
            shimmer.repeatMode = ValueAnimator.REVERSE
            shimmer.start()
        }
    }

    private fun animateBMIUpdate(newBmi: Double, newStatus: String) {
        // Animate BMI value change
        tvBmi?.let { bmiView ->
            val currentText = bmiView.text.toString()
            val currentValue = try {
                currentText.toDouble()
            } catch (e: NumberFormatException) {
                0.0
            }

            val animator = ValueAnimator.ofFloat(currentValue.toFloat(), newBmi.toFloat())
            animator.duration = 1500
            animator.interpolator = AccelerateDecelerateInterpolator()
            animator.addUpdateListener { animation ->
                val animatedValue = animation.animatedValue as Float
                bmiView.text = String.format("%.1f", animatedValue)
            }
            animator.start()

            // Scale animation for BMI
            val scaleX = ObjectAnimator.ofFloat(bmiView, "scaleX", 1f, 1.2f, 1f)
            val scaleY = ObjectAnimator.ofFloat(bmiView, "scaleY", 1f, 1.2f, 1f)
            val scaleSet = AnimatorSet()
            scaleSet.playTogether(scaleX, scaleY)
            scaleSet.duration = 800
            scaleSet.start()
        }

        // Animate BMI status with color change effect
        tvBmiStatus?.let { statusView ->
            val fadeOut = ObjectAnimator.ofFloat(statusView, "alpha", 1f, 0f)
            fadeOut.duration = 300
            fadeOut.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    statusView.text = newStatus
                    val fadeIn = ObjectAnimator.ofFloat(statusView, "alpha", 0f, 1f)
                    fadeIn.duration = 300
                    fadeIn.start()
                }
            })
            fadeOut.start()
        }
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

        // Show loading animation
        showLoadingAnimation()

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

                    // Animate data loading
                    animateDataLoading(name, email, age, gender, height, weight)

                } else {
                    Log.w(TAG, "Document does not exist for user: ${currentUser.uid}")
                    showToast("Data profil tidak ditemukan")
                    hideLoadingAnimation()
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error getting document: ", exception)
                if (isAdded) {
                    showToast("Gagal memuat profil: ${exception.message}")
                    hideLoadingAnimation()
                }
            }
    }

    private fun showLoadingAnimation() {
        // Loading animation dihilangkan - tidak ada animasi kedip-kedip
        // personalInfoCard?.let { card ->
        //     val pulse = ObjectAnimator.ofFloat(card, "alpha", 0.3f, 0.7f, 0.3f)
        //     pulse.duration = 1000
        //     pulse.repeatCount = ValueAnimator.INFINITE
        //     pulse.start()
        // }
    }

    private fun hideLoadingAnimation() {
        // Tidak ada loading animation yang perlu dihentikan
        // personalInfoCard?.let { card ->
        //     val pulseAnimator = card.tag as? ObjectAnimator
        //     pulseAnimator?.cancel()
        //     card.alpha = 1f
        // }
    }

    private fun animateDataLoading(
        name: String?,
        email: String?,
        age: Long?,
        gender: String?,
        height: Double?,
        weight: Double?
    ) {
        hideLoadingAnimation()

        // Set data dengan animasi typewriter effect
        val displayName = name ?: ""
        val displayEmail = email ?: ""

        // Animate text updates
        animateTextUpdate(tvProfileName, displayName)
        animateTextUpdate(tvProfileEmail, displayEmail)
        animateTextUpdate(tvName, displayName)
        animateTextUpdate(tvEmail, displayEmail)

        Handler(Looper.getMainLooper()).postDelayed({
            animateTextUpdate(tvAge, if (age != null) "${age} tahun" else "")
            animateTextUpdate(tvGender, gender ?: "")
        }, 200)

        Handler(Looper.getMainLooper()).postDelayed({
            animateTextUpdate(tvHeight, if (height != null) "${height.toInt()} cm" else "")
            animateTextUpdate(tvWeight, if (weight != null) "${weight.toInt()} kg" else "")
        }, 400)

        // Hitung dan tampilkan BMI dengan animasi
        if (height != null && weight != null && height > 0) {
            val bmiValue = calculateBMI(weight, height)
            val bmiStatus = getBMIStatus(bmiValue)

            Handler(Looper.getMainLooper()).postDelayed({
                animateBMIUpdate(bmiValue, bmiStatus)
            }, 600)
        }

        // Start entrance animations
        animateViewsIn()
    }

    private fun animateTextUpdate(textView: TextView?, newText: String) {
        textView?.let { tv ->
            if (newText.isEmpty()) return

            val fadeOut = ObjectAnimator.ofFloat(tv, "alpha", 1f, 0f)
            fadeOut.duration = 150
            fadeOut.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    tv.text = newText
                    val fadeIn = ObjectAnimator.ofFloat(tv, "alpha", 0f, 1f)
                    fadeIn.duration = 150
                    fadeIn.start()
                }
            })
            fadeOut.start()
        }
    }

    private fun setupClickListeners() {
        btnLogout?.setOnClickListener {
            // Add button press animation
            animateButtonPress(it) {
                auth.signOut()
                redirectToLogin()
            }
        }

        btnEditProfile?.setOnClickListener {
            // Add button press animation
            animateButtonPress(it) {
                // TODO: Navigate to edit profile
                showToast("Edit Profile akan segera tersedia")
            }
        }

        // Add card click animations
        personalInfoCard?.setOnClickListener {
            animateCardPress(it)
        }

        bmiCard?.setOnClickListener {
            animateCardPress(it)
        }
    }

    private fun animateButtonPress(view: View, action: () -> Unit) {
        val scaleDown = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.95f)
        val scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.95f)
        val scaleUp = ObjectAnimator.ofFloat(view, "scaleX", 0.95f, 1f)
        val scaleUpY = ObjectAnimator.ofFloat(view, "scaleY", 0.95f, 1f)

        val downSet = AnimatorSet()
        downSet.playTogether(scaleDown, scaleDownY)
        downSet.duration = 100

        val upSet = AnimatorSet()
        upSet.playTogether(scaleUp, scaleUpY)
        upSet.duration = 100

        downSet.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                upSet.start()
            }
        })

        upSet.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                action()
            }
        })

        downSet.start()
    }

    private fun animateCardPress(view: View) {
        val originalElevation = if (view is CardView) view.cardElevation else 0f
        val pulse = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.02f, 1f)
        val pulseY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.02f, 1f)

        val animSet = AnimatorSet()
        animSet.playTogether(pulse, pulseY)
        animSet.duration = 200
        animSet.start()
    }

    private fun redirectToLogin() {
        // Add exit animation before redirecting
        view?.let { rootView ->
            val fadeOut = ObjectAnimator.ofFloat(rootView, "alpha", 1f, 0f)
            fadeOut.duration = 300
            fadeOut.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    val intent = Intent(requireContext(), LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    activity?.finish()
                }
            })
            fadeOut.start()
        } ?: run {
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            activity?.finish()
        }
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
        // Refresh data ketika fragment kembali aktif dengan animasi
        if (auth.currentUser != null) {
            // Add subtle fade in animation on resume
            view?.let { rootView ->
                val fadeIn = ObjectAnimator.ofFloat(rootView, "alpha", 0.7f, 1f)
                fadeIn.duration = 300
                fadeIn.start()
            }
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
        btnEditProfile = null
        personalInfoCard = null
        bmiCard = null
        profilePictureCard = null
    }
}