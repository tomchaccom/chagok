package com.example.myapplication.data.past

import com.example.myapplication.feature.present.DailyRecord

/**
 * 하루 단위 기록
 */

data class DayEntry(
    val id: Long,
    val dateLabel: String,      // "2024년 3월 20일"
    val photos: List<DailyRecord>
) {
    // 대표 사진은 isFeatured=true를 우선으로, 없으면 첫 번째 사진
    val representativePhoto: DailyRecord?
        get() = photos.firstOrNull { it.isFeatured } ?: photos.firstOrNull()
}
