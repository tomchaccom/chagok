package com.example.myapplication.feature.present

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.core.network.RetrofitClient
import com.example.myapplication.data.present.PresentRepository

class PresentViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PresentViewModel::class.java)) {
            val repository = PresentRepository(RetrofitClient.presentApi)
            @Suppress("UNCHECKED_CAST")
            return PresentViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}