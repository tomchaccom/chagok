package com.example.myapplication.data.Ai

import retrofit2.http.GET
import retrofit2.http.Query

// AiApiService.kt
interface AiApiService {
    @GET("chat")
    suspend fun getAnalysis(
        @Query("prompt") prompt: String
    ): String }