package com.aikuaibao.model

import com.google.gson.annotations.SerializedName

data class FeedResponse(
    @SerializedName("generated_at") val generatedAt: String = "",
    val count: Int = 0,
    val sources: Map<String, Int> = emptyMap(),
    val articles: List<Article> = emptyList()
)

data class Article(
    val id: String = "",
    val title: String = "",
    val summary: String = "",
    val source: String = "",
    @SerializedName("source_name") val sourceName: String = "",
    val url: String = "",
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("video_url") val videoUrl: String? = null,
    val author: String? = null,
    @SerializedName("published_at") val publishedAt: String? = null,
    val score: Int = 0,
    val comments: Int? = null,
    val tags: List<String> = emptyList()
)
