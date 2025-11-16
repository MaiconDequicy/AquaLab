package br.iots.aqualab.model

import com.google.gson.annotations.SerializedName

// Mapeia a resposta completa da API
data class WeatherResponse(
    @SerializedName("weather") val weather: List<WeatherDescription>,
    @SerializedName("main") val main: MainWeatherData
)

// Mapeia o objeto "weather", que é uma lista com a descrição do clima
data class WeatherDescription(
    @SerializedName("description") val description: String
)

// Mapeia o objeto "main", com os dados de temperatura e umidade
data class MainWeatherData(
    @SerializedName("temp") val temp: Double,
    @SerializedName("humidity") val humidity: Int
)