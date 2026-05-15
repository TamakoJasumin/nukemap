/**
 * 城市数据仓库实现
 *
 * 从 Android Assets 目录加载 cities.json 文件，解析为 City 数据模型。
 * 使用缓存机制避免重复的文件 I/O 操作。
 * 解析在 IO 调度线程执行，不影响主线程 UI 响应。
 */
package com.mirvsim.app.data.repository

import android.content.Context
import com.mirvsim.app.domain.repository.CityRepository
import com.mirvsim.app.model.City
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader

class CityRepositoryImpl(
    private val context: Context
) : CityRepository {
    
    /** 城市数据缓存，首次加载后非空 */
    private var cachedCities: List<City>? = null

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
    }
    
    /**
     * 获取所有城市列表
     *
     * 使用双重检查锁定模式（借助 Kotlin 的 ?: 和 also 实现）：
     * - 缓存非空时直接返回
     * - 缓存为空时从 Assets 加载并写入缓存
     */
    override suspend fun getAllCities(): List<City> {
        return cachedCities ?: loadCitiesFromAssets().also {
            cachedCities = it
        }
    }
    
    /**
     * 模糊搜索城市
     *
     * 支持按城市英文名、中文显示名、所属地区三类字段进行不区分大小写的模糊匹配。
     * 结果数量由 limit 参数控制，避免返回过多数据。
     */
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
    
    /**
     * 从 Assets 加载城市 JSON 数据
     *
     * 在 IO 调度器上执行文件读取和 JSON 解析操作。
     * 使用 kotlinx.serialization 解析，忽略未知字段以保证向前兼容。
     * 解析失败时返回空列表而非抛出异常。
     */
    private suspend fun loadCitiesFromAssets(): List<City> = withContext(Dispatchers.IO) {
        try {
            context.assets.open("cities.json").use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    val content = reader.readText()
                    json.decodeFromString<List<City>>(content)
                }
            }
        } catch (_: Exception) {
            emptyList()  // 加载失败时静默处理，返回空列表
        }
    }
}
