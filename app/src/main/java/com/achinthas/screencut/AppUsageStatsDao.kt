package com.achinthas.screencut

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppUsageStatsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppUsageStats(appUsageStatsEntity: List<AppUsageStatsEntity>): List<Long>

}