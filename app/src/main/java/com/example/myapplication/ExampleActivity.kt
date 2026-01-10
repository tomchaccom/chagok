package com.example.myapplication

import android.os.Bundle
import com.example.myapplication.core.base.BaseActivity

class ExampleActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            replaceFragment(ExampleFragment())
        }
    }
}