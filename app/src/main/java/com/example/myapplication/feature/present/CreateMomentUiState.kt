package com.example.myapplication.feature.present

/**
 * CreateMomentUiState
 *
 * CreateMomentFragment에서 관리하는 UI 상태입니다.
 *
 * @param selectedPhotoUri 사용자가 선택한 사진 URI (null이면 선택 안 됨)
 * @param memo 한 줄 메모 입력값
 * @param score 점수 (1~10)
 * @param meaning 기억 or 잊기
 * @param isFeatured 오늘의 대표 기억 체크 여부
 * @param isLoading 저장 중 상태
 * @param savedSuccessfully 저장 완료 여부
 */
data class CreateMomentUiState(
    val selectedPhotoUri: String? = null,
    val memo: String = "",
    val score: Int = 5, // 기본값: 중간값
    val meaning: Meaning = Meaning.REMEMBER,
    val isFeatured: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val savedSuccessfully: Boolean = false
)
