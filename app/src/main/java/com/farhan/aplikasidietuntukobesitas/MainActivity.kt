package com.farhan.aplikasidietuntukobesitas

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.farhan.aplikasidietuntukobesitas.HomeFragment
import com.farhan.aplikasidietuntukobesitas.ProfileFragment
import com.farhan.aplikasidietuntukobesitas.ProgressFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var bottomNavigation: BottomNavigationView
    private var currentFragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()

        // Initialize views
        initializeViews()

        // Setup initial fragment with animation
        loadInitialFragment()

        // Setup navigation
        setupBottomNavigation()

        // Apply entrance animations
        applyEntranceAnimations()
    }

    private fun initializeViews() {
        bottomNavigation = findViewById(R.id.bottom_navigation)

        // Apply beautiful styling
        window.statusBarColor = ContextCompat.getColor(this, R.color.primary_dark)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.background_light)
    }

    private fun loadInitialFragment() {
        val homeFragment = HomeFragment()
        currentFragment = homeFragment

        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_up,
                R.anim.slide_out_down,
                R.anim.slide_in_down,
                R.anim.slide_out_up
            )
            .replace(R.id.fragment_container, homeFragment)
            .commit()
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
                if (currentFragment?.javaClass != fragment.javaClass) {
                    replaceFragmentWithAnimation(fragment)
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
            bottomNavigation.selectedItemId = R.id.nav_home
        } else {
            super.onBackPressed()
        }
    }
}