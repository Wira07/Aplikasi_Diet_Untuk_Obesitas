package com.farhan.aplikasidietuntukobesitas.navigasi

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.farhan.aplikasidietuntukobesitas.R
import com.farhan.aplikasidietuntukobesitas.users.HomeFragment
import com.farhan.aplikasidietuntukobesitas.users.ProfileFragment
import com.farhan.aplikasidietuntukobesitas.users.ProgressFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var bottomNavigation: BottomNavigationView
    private var currentFragment: Fragment? = null
    private var isFragmentLoaded = false // Flag untuk tracking apakah fragment sudah di-load

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Initialize Firebase
        auth = FirebaseAuth.getInstance()

        // Initialize views
        initializeViews()

        // Setup navigation (tidak load fragment awal)
        setupBottomNavigation()

        // Apply entrance animations
        applyEntranceAnimations()

        // Set default selection tanpa load fragment
        bottomNavigation.selectedItemId = R.id.nav_home
    }

    private fun initializeViews() {
        bottomNavigation = findViewById(R.id.bottom_navigation)

        // Apply beautiful styling
        window.statusBarColor = ContextCompat.getColor(this, R.color.primary_dark)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.background_light)
    }

    private fun loadFragmentForFirstTime(fragment: Fragment) {
        if (!isFragmentLoaded) {
            // Load fragment pertama kali dengan animasi khusus
            supportFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_up,
                    R.anim.slide_out_down,
                    R.anim.slide_in_down,
                    R.anim.slide_out_up
                )
                .replace(R.id.fragment_container, fragment)
                .commit()

            currentFragment = fragment
            isFragmentLoaded = true
        } else {
            // Load fragment biasa dengan animasi transisi
            replaceFragmentWithAnimation(fragment)
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            val selectedFragment = when (item.itemId) {
                R.id.nav_home -> {
                    animateBottomNavSelection(0)
                    HomeFragment()
                }
                R.id.nav_progress -> {
                    animateBottomNavSelection(1)
                    ProgressFragment()
                }
                R.id.nav_profile -> {
                    animateBottomNavSelection(2)
                    ProfileFragment()
                }
                else -> null
            }

            selectedFragment?.let { fragment ->
                // Cek apakah fragment yang dipilih berbeda dengan fragment saat ini
                if (currentFragment?.javaClass != fragment.javaClass) {
                    // Load fragment (pertama kali atau ganti fragment)
                    loadFragmentForFirstTime(fragment)
                    currentFragment = fragment
                }
            }
            true
        }
    }

    private fun replaceFragmentWithAnimation(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.fade_slide_in,
                R.anim.fade_slide_out,
                R.anim.fade_slide_in,
                R.anim.fade_slide_out
            )
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun animateBottomNavSelection(position: Int) {
        // Create ripple effect for selected item
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = 300
        animator.interpolator = FastOutSlowInInterpolator()
        animator.start()
    }

    private fun applyEntranceAnimations() {
        // Animate bottom navigation entrance
        bottomNavigation.translationY = bottomNavigation.height.toFloat()
        bottomNavigation.animate()
            .translationY(0f)
            .setDuration(600)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    override fun onBackPressed() {
        if (bottomNavigation.selectedItemId != R.id.nav_home) {
            // Jika bukan di home, pindah ke home
            bottomNavigation.selectedItemId = R.id.nav_home
        } else {
            // Jika sudah di home atau belum ada fragment yang di-load, keluar dari aplikasi
            super.onBackPressed()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Simpan state fragment yang sudah di-load
        outState.putBoolean("fragment_loaded", isFragmentLoaded)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        // Restore state fragment
        isFragmentLoaded = savedInstanceState.getBoolean("fragment_loaded", false)
    }
}