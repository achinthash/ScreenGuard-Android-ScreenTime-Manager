package com.achinthas.screencut

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PhoneUsageStatsDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPhoneUsageStats(phoneUsageStatsEntity: PhoneUsageStatsEntity) : Long

    @Query("SELECT * FROM phone_usage_stats WHERE day = :day LIMIT 1")
    suspend fun getByDay(day: String): PhoneUsageStatsEntity?

    @Query("SELECT * FROM phone_usage_stats WHERE day BETWEEN :startDay AND :endDate")
    suspend fun getByWeek(startDay: String, endDate: String) : List<PhoneUsageStatsEntity>

    @Query("""
    UPDATE phone_usage_stats
    SET totalMs = totalMs + :totalMs,
        unlockCount = unlockCount + :unlockCount,
        firstUnlockAt = MIN(firstUnlockAt, :firstUnlockAt),
        lastUnlockAt = MAX(lastUnlockAt, :lastUnlockAt)
    WHERE day = :day
""")
    suspend fun addPhoneUsage(
        day: String,
        totalMs: Long,
        unlockCount: Int,
        firstUnlockAt: Long?,
        lastUnlockAt: Long?
    ) :  Int

}