package com.example.myapplication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import android.widget.TextView


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // throw RuntimeException("Test Crash") // Force a crash
        setContentView(R.layout.activity_main)

        val etMemo = findViewById<EditText>(R.id.etMemo)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val tvResult = findViewById<TextView>(R.id.tvResult)

        btnSave.setOnClickListener {
            val text = etMemo.text.toString().trim()
            if (text.isEmpty()) {
                tvResult.text = getString(R.string.no_input)
            } else {
                tvResult.text = getString(R.string.saved, text)
            }
        }
    }
}
