package com.example.myapplication.feature.past

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.data.past.PastRepository

class PastViewModelFactory(private val repo: PastRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PastViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PastViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
