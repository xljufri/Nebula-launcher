package com.nebula.launcher.core.model

data class WeatherData(
    val temperature: Int,
    val condition: String,
    val city: String = "Jakarta",
    val description: String = "Cerah"
)
