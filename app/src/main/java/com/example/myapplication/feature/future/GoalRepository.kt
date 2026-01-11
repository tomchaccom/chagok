package com.example.myapplication.feature.future

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDate

@RequiresApi(Build.VERSION_CODES.O)
object GoalRepository {
    private val items = mutableListOf<Goal>()

    fun getAll(): List<Goal> = items.toList()

    init {
        loadDummyGoals()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadDummyGoals() {
        items.addAll(listOf(
            Goal("스페인어 배우기",
                LocalDate.of(2026, 12, 31)),
            Goal("마라톤 완주하기",
                LocalDate.of(2026, 5, 4)),
            Goal("일본 여행하기",
                LocalDate.of(2026, 3, 2)),
            Goal("토익 900점 달성",
                LocalDate.of(2026, 2, 2)),
            Goal("책 50권 읽기",
                LocalDate.of(2026, 8, 31)),
            Goal("저축 1천만 원",
                LocalDate.of(2026, 10, 26)),
            Goal("사이드 프로젝트 완성",
                LocalDate.of(2026, 1, 14)),
            Goal("운동 루틴 정착",
                LocalDate.of(2026, 1, 30)),
            Goal("사진 전시회 가기",
                LocalDate.of(2026, 7, 7))
            )
        )
    }

    fun add(goal: Goal) {
        // 가장 위에 추가되게 0번 인덱스에 삽입
        items.add(0, goal)
    }
}