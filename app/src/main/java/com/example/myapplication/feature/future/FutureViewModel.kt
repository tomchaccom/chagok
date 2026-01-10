package com.example.myapplication.feature.future

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class FutureViewModel : ViewModel() {

    private val _goals = MutableLiveData<List<Goal>>(GoalRepository.getAll())
    val goals: LiveData<List<Goal>> = _goals

    fun addGoal(title: String, dateMillis: Long) {
        val id = System.currentTimeMillis()
        val g = Goal(id = id, title = title, dateMillis = dateMillis)
        GoalRepository.add(g)
        _goals.value = GoalRepository.getAll()
    }
}
