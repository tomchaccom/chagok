package com.example.myapplication.core.network

import com.example.myapplication.data.present.PresentApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://api.example.com/" // TODO: Replace with your actual base URL

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val presentApi: PresentApi by lazy {
        retrofit.create(PresentApi::class.java)
    }
}