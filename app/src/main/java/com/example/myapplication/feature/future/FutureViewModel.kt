package com.example.myapplication.feature.future

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myapplication.data.future.GoalRepository
import java.time.LocalDate

@RequiresApi(Build.VERSION_CODES.O)
class FutureViewModel : ViewModel() {

    private val _goals = MutableLiveData<List<Goal>>(GoalRepository.getAll())
    val goals: LiveData<List<Goal>> = _goals

    fun addGoal(title: String, date: LocalDate) {
        val g = Goal(title = title, date = date)
        GoalRepository.add(g)
        _goals.value = GoalRepository.getAll()
    }
}
