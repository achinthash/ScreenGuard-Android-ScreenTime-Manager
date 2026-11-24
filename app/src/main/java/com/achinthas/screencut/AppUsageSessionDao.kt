package com.achinthas.screencut

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppUsageSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppUsageSessions(appUsageSessionEntity: List<AppUsageSessionEntity>): List<Long>

    @Query("SELECT * FROM app_usage_sessions ORDER BY endTime DESC LIMIT 1")
    suspend fun getLastSession(): AppUsageSessionEntity?
    @Query("SELECT * FROM app_usage_sessions WHERE day = :day ")
    suspend fun getByDay(day: String): List<AppUsageSessionEntity>

    @Query("SELECT * FROM app_usage_sessions WHERE packageName = :packageName AND day = :day")
    suspend fun getByPackageAndDay(packageName: String, day: String) : List<AppUsageSessionEntity>

}