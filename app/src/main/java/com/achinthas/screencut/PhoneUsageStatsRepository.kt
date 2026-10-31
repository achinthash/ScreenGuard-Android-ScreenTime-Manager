package com.achinthas.screencut

class PhoneUsageStatsRepository(private val phoneUsageStatsDao: PhoneUsageStatsDao) {

    suspend fun insertPhoneUsageStats(phoneUsageStatsEntity: List<PhoneUsageStatsEntity>): List<Long> {
        return phoneUsageStatsDao.insertPhoneUsageStats(phoneUsageStatsEntity)
    }
}