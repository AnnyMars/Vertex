package com.example.vertex.data.remote

import com.example.vertex.data.models.Configuration
import com.example.vertex.data.models.UserResponse
import retrofit2.http.GET
import retrofit2.http.Url

interface ApiService {

    @GET("test.json")
    suspend fun getConfiguration(): Configuration

    @GET("")
    suspend fun getFormResponse(@Url url: String): UserResponse
}