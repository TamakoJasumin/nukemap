package com.mirvsim.app.data.repository

import android.content.Context
import com.mirvsim.app.domain.repository.CityRepository
import com.mirvsim.app.model.City
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * 城市数据仓库实现
 * 从 Assets 加载城市数据并提供缓存
 */
class CityRepositoryImpl(
    private val context: Context
) : CityRepository {
    
    private var cachedCities: List<City>? = null
    
    override suspend fun getAllCities(): List<City> {
        return cachedCities ?: loadCitiesFromAssets().also {
            cachedCities = it
        }
    }
    
    override suspend fun searchCities(query: String, limit: Int): List<City> {
        val cities = getAllCities()
        
        return if (query.isBlank()) {
            cities.take(limit)
        } else {
            cities.filter { city ->
                city.name.contains(query, ignoreCase = true) ||
                city.display?.contains(query, ignoreCase = true) == true ||
                city.group.contains(query, ignoreCase = true)
            }.take(limit)
        }
    }
    
    private suspend fun loadCitiesFromAssets(): List<City> = withContext(Dispatchers.IO) {
        try {
            context.assets.open("cities.json").use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    val json = reader.readText()
                    Json {
                        ignoreUnknownKeys = true
                    }.decodeFromString<List<City>>(json)
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
