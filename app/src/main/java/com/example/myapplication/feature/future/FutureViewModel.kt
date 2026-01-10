package com.example.myapplication.feature.future

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class FutureViewModel : ViewModel() {

    private val _goals = MutableLiveData<List<Goal>>(GoalRepository.getAll())
    val goals: LiveData<List<Goal>> = _goals

    init {
        loadDummyGoals()
    }

    private fun loadDummyGoals() {
        _goals.value = listOf(
            Goal(1, "스페인어 배우기", 1),
            Goal(2, "마라톤 완주하기", 2),
            Goal(3, "일본 여행하기", 3),
            Goal(4, "토익 900점 달성", 4),
            Goal(5, "책 50권 읽기", 5),
            Goal(6, "저축 1천만 원", 6),
            Goal(7, "사이드 프로젝트 완성", 7),
            Goal(8, "운동 루틴 정착", 8),
            Goal(9, "사진 전시회 가기", 9)
        )
    }

    fun addGoal(title: String, dateMillis: Long) {
        val id = System.currentTimeMillis()
        val g = Goal(id = id, title = title, dateMillis = dateMillis)
        GoalRepository.add(g)
        _goals.value = GoalRepository.getAll()
    }
}
