package com.example.myapplication.data.present

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface PresentApi {
    @GET("/present")
    suspend fun getPresentData(): PresentDataDto

    @PUT("/present/goal/{goalId}")
    suspend fun updateGoalState(@Path("goalId") goalId: String, @Body isAchieved: Boolean)

    @POST("/present/goal")
    suspend fun addGoal(@Body text: String)
}