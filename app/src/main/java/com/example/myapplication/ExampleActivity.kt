package com.example.myapplication

import android.os.Bundle
import com.example.myapplication.R
import com.example.myapplication.core.base.BaseActivity
import com.example.myapplication.feature.future.FutureFragment
import com.example.myapplication.feature.highlight.HighlightFragment
import com.example.myapplication.feature.past.PastFragment
import com.example.myapplication.feature.present.PresentFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class ExampleActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupBottomNavigation()
        if (savedInstanceState == null) {
            findViewById<BottomNavigationView>(R.id.bottomNavigation).selectedItemId =
                R.id.navigation_present
        }
    }

    private fun setupBottomNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_past -> {
                    replaceFragment(PastFragment(), tag = "past")
                    true
                }
                R.id.navigation_present -> {
                    replaceFragment(PresentFragment(), tag = "present")
                    true
                }
                R.id.navigation_highlight -> {
                    replaceFragment(HighlightFragment(), tag = "highlight")
                    true
                }
                R.id.navigation_future -> {
                    replaceFragment(FutureFragment(), tag = "future")
                    true
                }
                else -> false
            }
        }
    }
}
