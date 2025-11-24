package br.iots.aqualab.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface NewsApiService {
    @GET("v2/everything")
    suspend fun getNoticias(
        @Query("q") query: String,
        @Query("language") language: String = "pt",
        @Query("sortBy") sortBy: String = "publishedAt",
        @Query("apiKey") apiKey: String = "260f773ed1ec4a66a6c4fe7a1be014cb"
    ): Response<NewsResponse>
}