package com.dotmatrix.app.weather

import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

// ── Data Models ───────────────────────────────────────────────────────────────

data class HourForecast(
    val time: String,       // "Now", "8 PM", etc.
    val temp: Float,
    val weatherCode: Int
)

data class DayForecast(
    val date: String,       // "Today", "Thu", etc.
    val dateLabel: String,  // "Mar 25"
    val maxTemp: Float,
    val minTemp: Float,
    val weatherCode: Int,
    val precipChance: Int,
    val isToday: Boolean
)

data class WeatherForecast(
    val city: String,
    val country: String,
    val currentTemp: Float,
    val currentCondition: String,
    val currentCode: Int,
    val todayMin: Float,
    val todayMax: Float,
    val hours: List<HourForecast>,   // next 12 hours
    val days: List<DayForecast>,     // 7 days
    val fetchedAt: Long = System.currentTimeMillis()
)

// ── Open-Meteo response shapes ────────────────────────────────────────────────
private data class OpenMeteoResponse(
    val current: CurrentData?,
    val hourly: HourlyData?,
    val daily: DailyData?
)
private data class CurrentData(
    val temperature_2m: Float?,
    val weathercode: Int?
)
private data class HourlyData(
    val time: List<String>?,
    val temperature_2m: List<Float>?,
    val weathercode: List<Int>?
)
private data class DailyData(
    val time: List<String>?,
    @SerializedName("temperature_2m_max") val maxTemps: List<Float>?,
    @SerializedName("temperature_2m_min") val minTemps: List<Float>?,
    @SerializedName("weathercode") val weatherCodes: List<Int>?,
    @SerializedName("precipitation_probability_max") val precipChances: List<Int>?
)

// ── Repository ────────────────────────────────────────────────────────────────
class WeatherRepository(private val context: Context) {
    private val TAG = "WeatherRepository"
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    suspend fun fetch7DayForecast(location: Location): Result<WeatherForecast> =
        withContext(Dispatchers.IO) {
            try {
                val lat = location.latitude
                val lon = location.longitude
                val (city, country) = resolveCityName(lat, lon)

                val url = "https://api.open-meteo.com/v1/forecast" +
                        "?latitude=$lat&longitude=$lon" +
                        "&current=temperature_2m,weathercode" +
                        "&hourly=temperature_2m,weathercode" +
                        "&daily=temperature_2m_max,temperature_2m_min,weathercode,precipitation_probability_max" +
                        "&timezone=auto&forecast_days=7"

                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) return@withContext Result.failure(IOException("HTTP ${response.code}"))
                val body = response.body?.string() ?: return@withContext Result.failure(IOException("Empty response"))

                val parsed = gson.fromJson(body, OpenMeteoResponse::class.java)
                val daily = parsed.daily ?: return@withContext Result.failure(IOException("No daily data"))

                val now = LocalDateTime.now()
                val today = now.toLocalDate()
                val dateFmt = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())

                // ── Daily ──────────────────────────────────────────────────
                val days = (0 until 7).mapNotNull { i ->
                    val dateStr = daily.time?.getOrNull(i) ?: return@mapNotNull null
                    val date = LocalDate.parse(dateStr)
                    val isToday = date == today
                    val label = when {
                        isToday -> "Today"
                        date == today.plusDays(1) -> "Tomorrow"
                        else -> date.dayOfWeek.toShort()
                    }
                    DayForecast(
                        date = label,
                        dateLabel = date.format(dateFmt),
                        maxTemp = daily.maxTemps?.getOrNull(i) ?: Float.NaN,
                        minTemp = daily.minTemps?.getOrNull(i) ?: Float.NaN,
                        weatherCode = daily.weatherCodes?.getOrNull(i) ?: 0,
                        precipChance = daily.precipChances?.getOrNull(i) ?: 0,
                        isToday = isToday
                    )
                }

                // ── Hourly (next 12 hours from now) ───────────────────────
                val hourFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
                val hours = parsed.hourly?.let { h ->
                    val times = h.time ?: emptyList()
                    var selectedCount = 0
                    times.mapIndexedNotNull { i, timeStr ->
                        if (selectedCount >= 12) return@mapIndexedNotNull null
                        val dt = runCatching { LocalDateTime.parse(timeStr, hourFmt) }.getOrNull() ?: return@mapIndexedNotNull null
                        if (dt.isBefore(now.minusMinutes(30))) return@mapIndexedNotNull null
                        selectedCount++
                        val label = when {
                            selectedCount == 1 && dt.hour == now.hour -> "Now"
                            else -> {
                                val h12 = if (dt.hour % 12 == 0) 12 else dt.hour % 12
                                val ampm = if (dt.hour < 12) "AM" else "PM"
                                "$h12 $ampm"
                            }
                        }
                        HourForecast(
                            time = label,
                            temp = h.temperature_2m?.getOrNull(i) ?: Float.NaN,
                            weatherCode = h.weathercode?.getOrNull(i) ?: 0
                        )
                    }
                } ?: emptyList()

                val currentTemp = parsed.current?.temperature_2m ?: days.firstOrNull()?.maxTemp ?: Float.NaN
                val currentCode = parsed.current?.weathercode ?: days.firstOrNull()?.weatherCode ?: 0
                val (currentCondition, _) = wmoCodeToDescription(currentCode)

                Result.success(
                    WeatherForecast(
                        city = city,
                        country = country,
                        currentTemp = currentTemp,
                        currentCondition = currentCondition,
                        currentCode = currentCode,
                        todayMin = days.firstOrNull()?.minTemp ?: Float.NaN,
                        todayMax = days.firstOrNull()?.maxTemp ?: Float.NaN,
                        hours = hours,
                        days = days
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch forecast", e)
                Result.failure(e)
            }
        }

    @Suppress("DEPRECATION")
    private suspend fun resolveCityName(lat: Double, lon: Double): Pair<String, String> =
        withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    var result: Pair<String, String>? = null
                    val latch = java.util.concurrent.CountDownLatch(1)
                    geocoder.getFromLocation(lat, lon, 1) { addresses ->
                        val addr = addresses.firstOrNull()
                        result = Pair(addr?.locality ?: addr?.subAdminArea ?: "Unknown", addr?.countryCode ?: "")
                        latch.countDown()
                    }
                    latch.await(5, TimeUnit.SECONDS)
                    result ?: Pair("Unknown", "")
                } else {
                    val addresses = geocoder.getFromLocation(lat, lon, 1)
                    val addr = addresses?.firstOrNull()
                    Pair(addr?.locality ?: addr?.subAdminArea ?: "Unknown", addr?.countryCode ?: "")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Geocoder failed", e)
                Pair("Unknown", "")
            }
        }

    private fun DayOfWeek.toShort(): String = when (this) {
        DayOfWeek.MONDAY -> "Mon"; DayOfWeek.TUESDAY -> "Tue"; DayOfWeek.WEDNESDAY -> "Wed"
        DayOfWeek.THURSDAY -> "Thu"; DayOfWeek.FRIDAY -> "Fri"
        DayOfWeek.SATURDAY -> "Sat"; DayOfWeek.SUNDAY -> "Sun"
    }
}

/** WMO weather code → condition label + emoji */
fun wmoCodeToDescription(code: Int): Pair<String, String> = when (code) {
    0 -> "Clear Sky" to "☀️"
    1 -> "Mainly Clear" to "🌤️"
    2 -> "Partly Cloudy" to "⛅"
    3 -> "Overcast" to "☁️"
    45, 48 -> "Foggy" to "🌫️"
    51, 53, 55 -> "Drizzle" to "🌦️"
    61, 63, 65 -> "Rain" to "🌧️"
    71, 73, 75 -> "Snow" to "❄️"
    77 -> "Snow Grains" to "🌨️"
    80, 81, 82 -> "Showers" to "🌦️"
    85, 86 -> "Snow Showers" to "🌨️"
    95 -> "Thunderstorm" to "⛈️"
    96, 99 -> "Hail Storm" to "⛈️"
    else -> "Unknown" to "🌡️"
}
