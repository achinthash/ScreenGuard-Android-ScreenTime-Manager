package com.achinthas.screencut

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PhoneUsageStatsEntity::class,
        AppUsageStatsEntity::class,
        AppUsageSessionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ApplicationDatabase: RoomDatabase() {

    abstract fun PhoneUsageStatsDao() : PhoneUsageStatsDao
    abstract fun AppUsageStatsDao() : AppUsageStatsDao
    abstract fun AppUsageSessionDao() : AppUsageSessionDao


    companion object {

        const val DATABASE_NAME = "screencut_database"
        @Volatile
        private var INSTANCE: ApplicationDatabase? = null

        fun getDatabase(context: Context): ApplicationDatabase{
            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ApplicationDatabase::class.java,
                    DATABASE_NAME
                ).build()

                INSTANCE = instance

                instance
            }
        }
    }
}
