package com.achinthas.screencut

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy

@Dao
interface PhoneUsageStatsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoneUsageStats(phoneUsageStatsEntity: List<PhoneUsageStatsEntity>) : List<Long>


}