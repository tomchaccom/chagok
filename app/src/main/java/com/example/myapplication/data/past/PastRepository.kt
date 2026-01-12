package com.example.myapplication.data.past

import android.content.Context

class PastRepository(private val context: Context) {
    // for dummy data
    fun loadPastEntries(): List<DayEntry> {
        val pkg = context.packageName

        fun drawableUri(name: String): String =
            "android.resource://$pkg/drawable/$name"

        return listOf(
            DayEntry(
                dateLabel = "2024년 3월 20일",
                dayMemo = "해변에서 본 아름다운 석양.",
                photos = listOf(
                    PhotoItem(drawableUri("photo1"), "오늘 석양이 정말 멋졌어요."),
                    PhotoItem(drawableUri("photo2"), "구름이 인상적이었다."),
                    PhotoItem(drawableUri("photo3"), "혼자 바라본 풍경."),
                    PhotoItem(drawableUri("photo5"), "산책 중에 찍은 사진.")
                )
            ),
            DayEntry(
                dateLabel = "2024년 3월 19일",
                dayMemo = "사라와 함께한 커피 시간.",
                photos = listOf(
                    PhotoItem(drawableUri("photo4"), "카페 분위기 좋았다."),
                    PhotoItem(drawableUri("photo5"), "산책 중에 찍은 사진.")
                )
            )
        )
    }
}
