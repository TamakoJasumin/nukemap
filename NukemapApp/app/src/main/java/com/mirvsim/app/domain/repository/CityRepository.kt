/**
 * 城市数据仓库接口
 *
 * 定义城市数据的获取和搜索操作抽象，遵循依赖倒置原则（DIP）。
 * 具体实现由 data 层提供，domain 层不依赖于具体实现细节。
 */
package com.mirvsim.app.domain.repository

import com.mirvsim.app.model.City

interface CityRepository {
    /**
     * 获取所有城市列表
     *
     * 首次调用时从数据源加载并缓存，后续调用直接返回缓存数据。
     */
    suspend fun getAllCities(): List<City>
    
    /**
     * 搜索城市
     *
     * 支持按城市名称（name）、显示名称（display）和所在地区（group）进行模糊匹配。
     *
     * @param query 搜索关键词，为空时返回前 limit 个城市
     * @param limit 返回结果数量上限
     * @return 匹配的城市列表
     */
    suspend fun searchCities(query: String, limit: Int = 50): List<City>
}
