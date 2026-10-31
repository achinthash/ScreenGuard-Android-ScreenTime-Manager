package com.achinthas.screencut

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey


@Entity(
    tableName = "app_usage_sessions",
    indices = [Index(value = ["packageName", "startTime"], unique = true)]
)
data class AppUsageSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val day: String,
    val packageName: String,
    val appName: String,
    val startTime: String,
    val endTime: String,
    val isScreenOff: Boolean = false
)
