package com.achinthas.screencut

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
@Entity(
    tableName = "phone_usage_stats",
    indices = [Index(value = ["day"], unique = true)]
)
data class PhoneUsageStatsEntity (

    @PrimaryKey(autoGenerate = true)
    val id : Long = 0,
    val day : String,
    val totalMs : Long,
    val unlockCount : Int,
    val firstUnlockAt : Long,
    val lastUnlockAt : Long

)
