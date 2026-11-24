package com.achinthas.screencut

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppUsageStatsDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAppUsageStats(appUsageStatsEntity: List<AppUsageStatsEntity>): List<Long>

    @Query("SELECT * FROM app_usage_stats WHERE day = :day ORDER BY totalTimeMs DESC")
    suspend fun getByDay(day: String): List<AppUsageStatsEntity>

    @Query("SELECT * FROM app_usage_stats WHERE day BETWEEN :startDate AND :endDate")
    suspend fun getByWeekRange(startDate: String, endDate: String): List<AppUsageStatsEntity>

    @Query("SELECT * FROM app_usage_stats WHERE packageName = :packageName AND day = :day")
    suspend fun getPackageByDay(packageName: String,day: String): AppUsageStatsEntity?

    @Query("SELECT * FROM app_usage_stats WHERE packageName =:packageName AND day BETWEEN :startDate AND :endDate")
    suspend fun getPackageByWeekRange(packageName: String, startDate: String, endDate: String): List<AppUsageStatsEntity>

    @Query("""
    UPDATE app_usage_stats
    SET totalTimeMs = totalTimeMs + :totalTimeMs,
        sessionsCount = sessionsCount + :sessionsCount,
        firstOpenAt = MIN(firstOpenAt, :firstOpenAt),
        lastOpenAt = MAX(lastOpenAt, :lastOpenAt)
    WHERE day = :day AND packageName = :packageName
""")
    suspend fun addUsage(
        day: String,
        packageName: String,
        totalTimeMs: Long,
        sessionsCount: Int,
        firstOpenAt: Long?,
        lastOpenAt: Long?
    ) : Int
}
