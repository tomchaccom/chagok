package com.example.myapplication.feature.present

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.core.network.RetrofitClient
import com.example.myapplication.data.present.PresentRepository

class PresentViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PresentViewModel::class.java)) {
            // 🌟 PresentRepository 생성 시 applicationContext를 넘겨줍니다.
            val repository = PresentRepository(RetrofitClient.presentApi, application.applicationContext)
            @Suppress("UNCHECKED_CAST")
            return PresentViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}