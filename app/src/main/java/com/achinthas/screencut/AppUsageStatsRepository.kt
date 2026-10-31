package com.achinthas.screencut

class AppUsageStatsRepository(private val appUsageStatsDao: AppUsageStatsDao) {

    suspend fun insertAppUsageStats(appUsageStatsEntity: List<AppUsageStatsEntity>): List<Long> {
        return appUsageStatsDao.insertAppUsageStats(appUsageStatsEntity)
    }

}