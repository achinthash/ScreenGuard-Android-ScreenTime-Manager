package com.achinthas.screencut

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "app_usage_stats",
    indices = [Index(value = ["packageName", "day"], unique = true)]
)
data class AppUsageStatsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val day: String,
    val packageName: String,
    val appName: String,
    val totalTimeMs: Long,
    val sessionsCount: Int,
    val firstOpenAt: Long,
    val lastOpenAt: Long
)