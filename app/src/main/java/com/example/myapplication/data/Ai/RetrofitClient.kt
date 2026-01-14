package com.example.myapplication.data.Ai

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

// RetrofitClient.kt
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "http://10.249.69.39:8080/"

    // 🌟 타임아웃 설정을 포함한 OkHttpClient
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS) // 연결 대기 시간
        .readTimeout(60, TimeUnit.SECONDS)    // 데이터 읽기 대기 시간
        .writeTimeout(60, TimeUnit.SECONDS)   // 데이터 쓰기 대기 시간
        .build()

    val instance: AiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient) // 🌟 빌더에 클라이언트 연결
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AiApiService::class.java)
    }
}