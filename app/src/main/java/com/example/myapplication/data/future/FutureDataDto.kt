package com.example.myapplication.data.future


data class Goal(
    val id: String = java.util.UUID.randomUUID().toString(), // 고유 ID 추가
    val title: String,
    val date: java.time.LocalDate,
    val isAchieved: Boolean = false // 실천 여부 추가
)