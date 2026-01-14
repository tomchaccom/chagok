package com.example.myapplication.data.Ai

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

// RetrofitClient.kt
object RetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:8080/" // 에뮬레이터용 주소

    val instance: AiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(ScalarsConverterFactory.create()) // String 결과값을 위해 필요
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AiApiService::class.java)
    }
}