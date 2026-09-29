package com.forrest.titanlauncher.weather

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume
import kotlin.math.roundToInt

data class WeatherSnapshot(
    val temperatureF: Int,
    val condition: String
)

private data class CachedWeather(
    val snapshot: WeatherSnapshot,
    val latitude: Double,
    val longitude: Double,
    val savedAt: Long
)

class WeatherRepository(
    private val context: Context
) {

    private val preferences =
        context.getSharedPreferences(
            WEATHER_PREFS,
            Context.MODE_PRIVATE
        )

    suspend fun loadCurrentWeather(): WeatherSnapshot? {
        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }

        val cached =
            loadCachedWeather()

        val location =
            getBestLocation()

        /*
         * If Android temporarily cannot provide a location,
         * use a recent cached result instead of showing nothing.
         */
        if (
            location == null
        ) {
            return cached
                ?.takeIf {
                    System.currentTimeMillis() - it.savedAt <=
                            FALLBACK_CACHE_AGE_MS
                }
                ?.snapshot
        }

        val now =
            System.currentTimeMillis()

        /*
         * Only reuse the short-term cache if the device is still
         * physically near the location that produced that weather.
         *
         * This prevents weather from another city following the user
         * after Android's location changes.
         */
        if (
            cached != null &&
            now - cached.savedAt <=
            FRESH_CACHE_AGE_MS &&
            distanceMeters(
                firstLatitude = location.latitude,
                firstLongitude = location.longitude,
                secondLatitude = cached.latitude,
                secondLongitude = cached.longitude
            ) <= FRESH_CACHE_DISTANCE_METERS
        ) {
            return cached.snapshot
        }

        val freshWeather =
            loadWeather(
                latitude =
                    location.latitude,
                longitude =
                    location.longitude
            )

        if (
            freshWeather != null
        ) {
            saveCachedWeather(
                snapshot =
                    freshWeather,
                latitude =
                    location.latitude,
                longitude =
                    location.longitude
            )

            return freshWeather
        }

        /*
         * If Open-Meteo has a temporary network failure,
         * fall back only when the old weather came from roughly
         * the same area.
         */
        return cached
            ?.takeIf {
                now - it.savedAt <=
                        FALLBACK_CACHE_AGE_MS &&
                        distanceMeters(
                            firstLatitude = location.latitude,
                            firstLongitude = location.longitude,
                            secondLatitude = it.latitude,
                            secondLongitude = it.longitude
                        ) <= FALLBACK_CACHE_DISTANCE_METERS
            }
            ?.snapshot
    }

    private suspend fun getBestLocation(): Location? {
        val locationManager =
            context.getSystemService(
                Context.LOCATION_SERVICE
            ) as? LocationManager
                ?: return null

        /*
         * GPS intentionally comes first.
         *
         * This matters especially on the Android emulator because
         * NETWORK_PROVIDER can return a stale/default location even when
         * the emulator's GPS location has been set manually.
         */
        val providers =
            buildList {
                if (
                    isProviderEnabled(
                        locationManager,
                        LocationManager.GPS_PROVIDER
                    )
                ) {
                    add(
                        LocationManager.GPS_PROVIDER
                    )
                }

                if (
                    isProviderEnabled(
                        locationManager,
                        LocationManager.NETWORK_PROVIDER
                    )
                ) {
                    add(
                        LocationManager.NETWORK_PROVIDER
                    )
                }
            }

        if (
            providers.isEmpty()
        ) {
            return null
        }

        val lastKnownLocations =
            providers.mapNotNull {
                    provider ->

                try {
                    locationManager
                        .getLastKnownLocation(
                            provider
                        )
                } catch (
                    _: SecurityException
                ) {
                    null
                } catch (
                    _: Exception
                ) {
                    null
                }
            }

        val newestLastKnown =
            lastKnownLocations
                .maxByOrNull {
                    it.time
                }

        /*
         * getCurrentLocation() was introduced in Android 11.
         * Older Android versions use the newest known location.
         */
        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.R
        ) {
            return newestLastKnown
        }

        /*
         * Try each enabled provider instead of stopping after the first.
         *
         * GPS is attempted first. If GPS cannot provide a current location,
         * then Prompt Launcher falls back to network location.
         */
        providers.forEach {
                provider ->

            val current =
                getCurrentLocation(
                    locationManager =
                        locationManager,
                    provider =
                        provider
                )

            if (
                current != null
            ) {
                return current
            }
        }

        return newestLastKnown
    }

    private fun isProviderEnabled(
        locationManager: LocationManager,
        provider: String
    ): Boolean {
        return runCatching {
            locationManager
                .isProviderEnabled(
                    provider
                )
        }
            .getOrDefault(
                false
            )
    }

    private suspend fun getCurrentLocation(
        locationManager: LocationManager,
        provider: String
    ): Location? {
        return withTimeoutOrNull(
            LOCATION_TIMEOUT_MS
        ) {
            suspendCancellableCoroutine<Location?> {
                    continuation ->

                try {
                    locationManager
                        .getCurrentLocation(
                            provider,
                            null,
                            context.mainExecutor
                        ) {
                                location ->

                            if (
                                continuation.isActive
                            ) {
                                continuation.resume(
                                    location
                                )
                            }
                        }
                } catch (
                    _: SecurityException
                ) {
                    if (
                        continuation.isActive
                    ) {
                        continuation.resume(
                            null
                        )
                    }
                } catch (
                    _: Exception
                ) {
                    if (
                        continuation.isActive
                    ) {
                        continuation.resume(
                            null
                        )
                    }
                }
            }
        }
    }

    private suspend fun loadWeather(
        latitude: Double,
        longitude: Double
    ): WeatherSnapshot? {
        return withContext(
            Dispatchers.IO
        ) {
            val url =
                URL(
                    "https://api.open-meteo.com/v1/forecast" +
                            "?latitude=$latitude" +
                            "&longitude=$longitude" +
                            "&current=temperature_2m,weather_code" +
                            "&temperature_unit=fahrenheit" +
                            "&timezone=auto"
                )

            val connection =
                url.openConnection() as HttpURLConnection

            try {
                connection.requestMethod =
                    "GET"

                connection.connectTimeout =
                    10_000

                connection.readTimeout =
                    10_000

                connection.setRequestProperty(
                    "Accept",
                    "application/json"
                )

                if (
                    connection.responseCode !in
                    200..299
                ) {
                    return@withContext null
                }

                val response =
                    connection
                        .inputStream
                        .bufferedReader()
                        .use {
                            it.readText()
                        }

                val current =
                    JSONObject(
                        response
                    )
                        .optJSONObject(
                            "current"
                        )
                        ?: return@withContext null

                val temperature =
                    current.optDouble(
                        "temperature_2m",
                        Double.NaN
                    )

                if (
                    temperature.isNaN()
                ) {
                    return@withContext null
                }

                val weatherCode =
                    current.optInt(
                        "weather_code",
                        -1
                    )

                WeatherSnapshot(
                    temperatureF =
                        temperature.roundToInt(),
                    condition =
                        conditionForCode(
                            weatherCode
                        )
                )
            } catch (
                _: Exception
            ) {
                null
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun saveCachedWeather(
        snapshot: WeatherSnapshot,
        latitude: Double,
        longitude: Double
    ) {
        preferences
            .edit()
            .putInt(
                KEY_TEMPERATURE,
                snapshot.temperatureF
            )
            .putString(
                KEY_CONDITION,
                snapshot.condition
            )
            .putLong(
                KEY_LATITUDE,
                latitude.toBits()
            )
            .putLong(
                KEY_LONGITUDE,
                longitude.toBits()
            )
            .putLong(
                KEY_SAVED_AT,
                System.currentTimeMillis()
            )
            .apply()
    }

    private fun loadCachedWeather(): CachedWeather? {
        if (
            !preferences.contains(
                KEY_TEMPERATURE
            ) ||
            !preferences.contains(
                KEY_LATITUDE
            ) ||
            !preferences.contains(
                KEY_LONGITUDE
            )
        ) {
            return null
        }

        val condition =
            preferences
                .getString(
                    KEY_CONDITION,
                    null
                )
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: return null

        val savedAt =
            preferences
                .getLong(
                    KEY_SAVED_AT,
                    0L
                )

        if (
            savedAt <= 0L
        ) {
            return null
        }

        val latitude =
            Double.fromBits(
                preferences.getLong(
                    KEY_LATITUDE,
                    0L
                )
            )

        val longitude =
            Double.fromBits(
                preferences.getLong(
                    KEY_LONGITUDE,
                    0L
                )
            )

        return CachedWeather(
            snapshot =
                WeatherSnapshot(
                    temperatureF =
                        preferences.getInt(
                            KEY_TEMPERATURE,
                            0
                        ),
                    condition =
                        condition
                ),
            latitude =
                latitude,
            longitude =
                longitude,
            savedAt =
                savedAt
        )
    }

    private fun distanceMeters(
        firstLatitude: Double,
        firstLongitude: Double,
        secondLatitude: Double,
        secondLongitude: Double
    ): Float {
        val results =
            FloatArray(
                1
            )

        Location.distanceBetween(
            firstLatitude,
            firstLongitude,
            secondLatitude,
            secondLongitude,
            results
        )

        return results[0]
    }

    private fun conditionForCode(
        code: Int
    ): String {
        return when (
            code
        ) {
            0 ->
                "Sunny"

            1 ->
                "Mostly sunny"

            2 ->
                "Partly cloudy"

            3 ->
                "Cloudy"

            45,
            48 ->
                "Foggy"

            51,
            53,
            55,
            56,
            57 ->
                "Drizzle"

            61,
            63,
            65,
            66,
            67 ->
                "Rain"

            71,
            73,
            75,
            77 ->
                "Snow"

            80,
            81,
            82 ->
                "Showers"

            85,
            86 ->
                "Snow showers"

            95,
            96,
            99 ->
                "Thunderstorms"

            else ->
                "Weather"
        }
    }

    companion object {
        private const val WEATHER_PREFS =
            "prompt_launcher_weather"

        private const val KEY_TEMPERATURE =
            "temperature_f"

        private const val KEY_CONDITION =
            "condition"

        private const val KEY_LATITUDE =
            "latitude_bits"

        private const val KEY_LONGITUDE =
            "longitude_bits"

        private const val KEY_SAVED_AT =
            "saved_at"

        private const val LOCATION_TIMEOUT_MS =
            4_000L

        /*
         * Reuse weather briefly if the phone has not meaningfully moved.
         */
        private const val FRESH_CACHE_AGE_MS =
            5 * 60 * 1000L

        /*
         * Roughly 15 miles.
         */
        private const val FRESH_CACHE_DISTANCE_METERS =
            24_000f

        /*
         * Temporary network failure fallback.
         */
        private const val FALLBACK_CACHE_AGE_MS =
            2 * 60 * 60 * 1000L

        /*
         * Roughly 30 miles.
         */
        private const val FALLBACK_CACHE_DISTANCE_METERS =
            48_000f
    }
}