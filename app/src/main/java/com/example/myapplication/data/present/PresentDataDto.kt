package com.example.myapplication.data.present

data class PresentDataDto(
    val userProfile: UserProfileDto,
    val practices: List<PracticeDto>,
    val practicesLeft: Int,
    val recordStatus: String // "EMPTY" or "RECORDED"
)

data class UserProfileDto(
    val greeting: String,
    val prompt: String
)

data class PracticeDto(
    val id: String,
    val title: String,
    val subtitle: String,
    val isAchieved: Boolean
)
