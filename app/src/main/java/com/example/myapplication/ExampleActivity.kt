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

        scheduleDailyCleanup()

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

    private fun scheduleDailyCleanup() {
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 23)
            set(java.util.Calendar.MINUTE, 59)
            set(java.util.Calendar.SECOND, 0)
        }

        // 이미 시간이 지났다면 내일 밤으로 설정
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }

        val initialDelay = calendar.timeInMillis - System.currentTimeMillis()

        val workRequest = androidx.work.OneTimeWorkRequestBuilder<com.example.myapplication.data.future.DailyCleanupWorker>()
            .setInitialDelay(initialDelay, java.util.concurrent.TimeUnit.MILLISECONDS)
            .build()

        androidx.work.WorkManager.getInstance(this).enqueueUniqueWork(
            "DailyPastMigration",
            androidx.work.ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
}
