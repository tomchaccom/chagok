package com.example.myapplication.data.past

/**
 * 개별 사진 + 사진 메모
 * imageUri 는 다음을 모두 수용 가능
 * - android.resource:// 패키지명/drawable/파일명
 * - file://...
 * - content://...
 * - https://...
 */

data class PhotoItem(
    val imageUri: String,
    val memo: String
)

/**
 * 하루 단위 기록
 */
data class DayEntry(
    val dateLabel: String,      // "2024년 3월 20일"
    val dayMemo: String,        // 일자 기본 메모
    val photos: List<PhotoItem>
) {
    val representativePhoto: PhotoItem?
        get() = photos.firstOrNull()
}
