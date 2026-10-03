package com.example.lifeorganizer.calendar.api

import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

interface DirectionsApiService {
    @GET("maps/api/directions/json")
    suspend fun getDirections(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("mode") mode: String = "driving",
        @Query("departure_time") departureTime: String? = "now",
        // Transit routes are planned backwards from the arrival time (departure_time must then be omitted).
        @Query("arrival_time") arrivalTime: Long? = null,
        @Query("key") apiKey: String
    ): DirectionsResponse

    companion object {
        fun create(): DirectionsApiService {
            return Retrofit.Builder()
                .baseUrl("https://maps.googleapis.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(DirectionsApiService::class.java)
        }
    }
}

data class DirectionsResponse(
    val routes: List<Route>,
    val status: String
)

data class Route(
    val legs: List<Leg>
)

data class Leg(
    val duration: DurationData,
    val duration_in_traffic: DurationData?
)

data class DurationData(
    val text: String,
    val value: Int // duration in seconds
)
