package com.achinthas.screencut

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PhoneUsageStatsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoneUsageStats(phoneUsageStatsEntity: List<PhoneUsageStatsEntity>) : List<Long>

    @Query("SELECT * FROM phone_usage_stats WHERE day = :day LIMIT 1")
    suspend fun getByDay(day: String): PhoneUsageStatsEntity?

    @Query("SELECT * FROM phone_usage_stats WHERE day BETWEEN :startDay AND :endDate")
    suspend fun getByWeek(startDay: String, endDate: String) : List<PhoneUsageStatsEntity>


}