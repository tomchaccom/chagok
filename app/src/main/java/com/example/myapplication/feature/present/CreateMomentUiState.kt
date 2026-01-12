package com.example.myapplication.feature.present

/**
 * CreateMomentUiState
 *
 * CreateMomentFragment에서 관리하는 UI 상태입니다.
 *
 * @param selectedPhotoUri 사용자가 선택한 사진 URI (null이면 선택 안 됨)
 * @param memo 한 줄 메모 입력값
 * @param meaning 기억 or 잊기
 * @param isFeatured 오늘의 대표 기억 체크 여부
 * @param showFeaturedConflictDialog 대표 기억 중복 선택 안내 다이얼로그 노출 여부
 * @param allowFeaturedReplacement 대표 기억 교체 동의 여부
 * @param isLoading 저장 중 상태
 * @param savedSuccessfully 저장 완료 여부
 */
data class CreateMomentUiState(
    val selectedPhotoUri: String? = null,
    val memo: String = "",
    val cesInput: CesInput = CesInput(),
    val cesWeightedScore: Float = 3.0f,
    val cesDescription: String = "보통",
    val timeState: com.example.myapplication.core.util.TimeState = com.example.myapplication.core.util.TimeState.PRESENT,
    val meaning: Meaning = Meaning.REMEMBER,
    val isFeatured: Boolean = false,
    val showFeaturedConflictDialog: Boolean = false,
    val allowFeaturedReplacement: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val savedSuccessfully: Boolean = false,
    val editMode: Boolean = false
)
