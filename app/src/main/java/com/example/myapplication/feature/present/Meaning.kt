package com.example.myapplication.feature.present

/**
 * Meaning: 기록된 순간의 의미를 표현합니다.
 *
 * - REMEMBER: 기억하고 싶은 의미 있는 순간
 * - FORGET: 의미 없거나 넘어가고 싶은 순간
 */
enum class Meaning(val displayName: String) {
    REMEMBER("기억하기"),
    FORGET("잊기")
}

