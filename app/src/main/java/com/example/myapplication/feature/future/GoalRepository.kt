package com.example.myapplication.feature.future

object GoalRepository {
    private val items = mutableListOf<Goal>()

    fun getAll(): List<Goal> = items.toList()

    fun add(goal: Goal) {
        // 가장 위에 추가되게 0번 인덱스에 삽입
        items.add(0, goal)
    }
}