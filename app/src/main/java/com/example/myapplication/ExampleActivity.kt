package com.example.myapplication

import android.os.Bundle
import com.example.myapplication.core.base.BaseActivity
import com.example.myapplication.feature.present.PresentFragment

class ExampleActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 테스트 목적: 항상 PresentFragment 를 붙여 화면 확인을 쉽게 합니다.
        replaceFragment(PresentFragment())
    }
}
