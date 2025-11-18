package br.iots.aqualab.repository

import br.iots.aqualab.BuildConfig
import br.iots.aqualab.model.WeatherResponse

import br.iots.aqualab.network.RetrofitInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WeatherRepository {
    suspend fun getCurrentWeather(lat: Double, lon: Double): Result<WeatherResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = RetrofitInstance.weatherApiService.getCurrentWeather(
                    lat = lat,
                    lon = lon,
                    apiKey = BuildConfig.OPENWEATHER_API_KEY
                )
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
     