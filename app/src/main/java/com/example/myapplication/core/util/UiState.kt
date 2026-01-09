package com.example.myapplication.core.util

/**
 * UiState
 * - 모든 feature에서 재사용 가능한 UI 상태 추상화입니다.
 * - Loading: 로딩 상태
 * - Success<T>: 성공 상태와 함께 데이터를 포함
 * - Error: 오류 메시지 포함
 *
 * 사용 예시:
 * val state: UiState<List<Item>> = UiState.Loading
 * when(state) {
 *   is UiState.Loading -> // show progress
 *   is UiState.Success -> // render state.data
 *   is UiState.Error -> // show error message
 * }
 */
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

