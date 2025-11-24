package br.iots.aqualab.network

import com.google.gson.annotations.SerializedName

data class NewsResponse(
    val status: String,
    val articles: List<ArticleDto>
)

data class ArticleDto(
    val title: String?,
    val description: String?,
    val url: String?,
    val urlToImage: String?,
    val publishedAt: String?
)