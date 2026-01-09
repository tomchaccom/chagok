package com.example.myapplication.core.base

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.myapplication.R

/**
 * BaseActivity
 *
 * - 단일 Fragment 컨테이너(`R.id.container`)를 제공하는 기본 액티비티입니다.
 * - 중앙의 ProgressBar(`R.id.progressBar`)를 통해 공통 로딩 표시를 제어합니다.
 * - 프래그먼트 교체 유틸리티 `replaceFragment` 를 제공합니다.
 *
 * 팀 가이드:
 * - 프래그먼트 태그로는 `core.navigation.NavRoutes` 상수를 사용하는 것을 권장합니다.
 * - Activity 레이아웃은 `res/layout/activity_base.xml` (single container + progressBar)을 사용합니다.
 */
open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 기본 레이아웃: single fragment container + progress bar
        setContentView(R.layout.activity_base)
    }

    /**
     * 공통 로딩 표시/숨김
     * - ProgressBar는 activity_base.xml 에 정의되어 있어야 합니다 (id = progressBar).
     */
    fun setLoadingVisibility(isVisible: Boolean) {
        val progress = findViewById<ProgressBar?>(R.id.progressBar)
        progress?.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    /**
     * 단일 컨테이너에 프래그먼트를 교체합니다.
     * - tag는 선택적이며, 필요시 NavRoutes 상수를 사용하세요.
     */
    fun replaceFragment(fragment: Fragment, addToBackStack: Boolean = false, tag: String? = null) {
        val tx = supportFragmentManager.beginTransaction()
        tx.replace(R.id.container, fragment, tag)
        if (addToBackStack) tx.addToBackStack(tag)
        tx.commit()
    }
}
