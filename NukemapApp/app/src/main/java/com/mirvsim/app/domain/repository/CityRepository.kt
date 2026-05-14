package com.mirvsim.app.domain.repository

import com.mirvsim.app.model.City

/**
 * 城市数据仓库接口
 * 定义城市数据的获取和搜索操作
 */
interface CityRepository {
    /**
     * 获取所有城市列表
     */
    suspend fun getAllCities(): List<City>
    
    /**
     * 搜索城市
     * @param query 搜索关键词
     * @param limit 返回结果数量限制
     */
    suspend fun searchCities(query: String, limit: Int = 50): List<City>
}
