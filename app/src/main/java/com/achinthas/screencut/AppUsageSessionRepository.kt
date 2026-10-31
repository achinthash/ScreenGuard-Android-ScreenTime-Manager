package com.achinthas.screencut

class AppUsageSessionRepository(private val appUsageSessionDao: AppUsageSessionDao ) {

    suspend fun insertDailyAppSessions(appUsageSessionEntity: List<AppUsageSessionEntity>): List<Long> {
        return appUsageSessionDao.insertAppUsageSessions(appUsageSessionEntity)
    }

}