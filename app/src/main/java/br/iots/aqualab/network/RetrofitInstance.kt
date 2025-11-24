package br.iots.aqualab.network

import android.R.attr.level
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    private const val BASE_URL = "https://api.openweathermap.org/"
    private const val BASE_URL_NEWS = "https://newsapi.org/"

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val weatherApiService: WeatherApiService by lazy {
        retrofit.create(WeatherApiService::class.java)
    }


    //Configuração para a API de IA (OpenAI)
    private const val CHAT_BASE_URL = "https://api.openai.com/"

    //Criamos um Interceptor para ver os logs das chamadas
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    private val chatHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    private val chatRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(CHAT_BASE_URL)
            .client(chatHttpClient) // Usando o cliente com logging
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val chatGptApiService: ChatGptApiService by lazy {
        chatRetrofit.create(ChatGptApiService::class.java)
    }

    val api: NewsApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL_NEWS)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NewsApiService::class.java)
    }
}
