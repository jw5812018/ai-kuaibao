package com.aikuaibao.network

import com.aikuaibao.model.FeedResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

// 云端地址：GitHub Actions 每天聚合后通过 Pages 公开 articles.json
const val BASE_URL = "https://jw5812018.github.io/ai-kuaibao/"

interface ApiService {
    @GET("articles.json")
    suspend fun getArticles(): FeedResponse
}

object Api {
    val service: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
