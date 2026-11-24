package com.achinthas.screencut

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class UsageResult(
    val sessions: List<AppUsageSessionEntity>,
    val unlockTimes: List<Long>
)

class UsageWorker(appContext: Context,
                  workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {

        val database = ApplicationDatabase.getDatabase(applicationContext)

        // Dao's
        val appUsageStatsDao = database.AppUsageStatsDao()
        val phoneUsageStatsDao = database.PhoneUsageStatsDao()
        val appUsageSessionDao = database.AppUsageSessionDao()

        return try {

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val lastSession = appUsageSessionDao.getLastSession()
            val startTime = lastSession?.endTime ?: calendar.timeInMillis
            val endTime = System.currentTimeMillis()

            if (endTime <= startTime) {
                return Result.success()
            }

            // get app usages sessions
            val result = getAppUsageSessionsData(applicationContext, startTime, endTime)

            if (result.sessions.isEmpty() && result.unlockTimes.isEmpty()) {
                return Result.success()
            }

            // build for apps stats
            val appStats = buildUsageStats(result.sessions)

            // build for phone stats
            val phoneStats = buildPhoneUsageStatsByDay(result)

            // insert apps usage sessions
            if (result.sessions.isNotEmpty()) {
                appUsageSessionDao.insertAppUsageSessions(result.sessions)
            }

            // insert app usage stats
            appStats.forEach { new ->
                val updated = appUsageStatsDao.addUsage(
                    day = new.day,
                    packageName = new.packageName,
                    totalTimeMs = new.totalTimeMs,
                    sessionsCount = new.sessionsCount,
                    firstOpenAt = new.firstOpenAt,
                    lastOpenAt = new.lastOpenAt
                )
                if (updated == 0) appUsageStatsDao.insertAppUsageStats(listOf(new))
            }

            // insert phone usage stats
            phoneStats.forEach { p ->
                val updated = phoneUsageStatsDao.addPhoneUsage(
                    day = p.day,
                    totalMs = p.totalMs,
                    unlockCount = p.unlockCount,
                    firstUnlockAt = p.firstUnlockAt,
                    lastUnlockAt = p.lastUnlockAt
                )
                if (updated == 0) phoneUsageStatsDao.insertPhoneUsageStats(p)
            }

            Result.success()

        } catch (e: Exception) {
            Result.retry()
        }
    }
}

private fun buildUsageStats(sessions: List<AppUsageSessionEntity>): List<AppUsageStatsEntity> {
    return sessions
        .filter { !it.isScreenOff } //  && it.packageName != "system.home"
        .groupBy { it.day to it.packageName }
        .map { (key, list) ->
            AppUsageStatsEntity(
                day = key.first,
                packageName = key.second,
                appName = list.first().appName,
                totalTimeMs = list.sumOf { it.endTime - it.startTime },
                sessionsCount = list.size,
                firstOpenAt = list.minOf { it.startTime },
                lastOpenAt = list.maxOf { it.startTime }
            )
        }
        .sortedByDescending { it.totalTimeMs }
}

fun buildPhoneUsageStatsByDay(result: UsageResult): List<PhoneUsageStatsEntity> {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    val usageByDay = result.sessions
        .filter { !it.isScreenOff }
        .groupBy { it.day }
        .mapValues { (_, list) -> list.sumOf { it.endTime - it.startTime } }

    val unlocksByDay = result.unlockTimes.groupBy { dateFormat.format(it) }

    return (usageByDay.keys + unlocksByDay.keys).toSortedSet().map { day ->
        val unlocks = unlocksByDay[day].orEmpty()
        PhoneUsageStatsEntity(
            day = day,
            totalMs = usageByDay[day] ?: 0L,
            unlockCount = unlocks.size,
            firstUnlockAt = unlocks.minOrNull(),
            lastUnlockAt = unlocks.maxOrNull()
        )
    }
}

private fun getAppUsageSessionsData(context: Context, startTime: Long, endTime: Long ): UsageResult {

    val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val events = usm.queryEvents(startTime, endTime)
    val pm = context.packageManager

    val launcherPkg = pm.resolveActivity(
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
        PackageManager.MATCH_DEFAULT_ONLY
    )?.activityInfo?.packageName

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val labelCache = mutableMapOf<String, String>()

    fun label(pkg: String): String = labelCache.getOrPut(pkg) {
        try {
            val info = pm.getApplicationInfo(pkg, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            pkg
        }
    }

    val sessions = mutableListOf<AppUsageSessionEntity>()

    fun addSession(
        pkg: String,
        name: String,
        start: Long,
        end: Long,
        screenOff: Boolean
    ) {
        if (end <= start) return

        val calendar = Calendar.getInstance()
        var currentStart = start

        while (currentStart < end) {
            // calculate the next midnight after currentStart
            calendar.timeInMillis = currentStart
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            val nextMidnight = calendar.timeInMillis

            val currentEnd = minOf(end, nextMidnight)

            sessions.add(
                AppUsageSessionEntity(
                    day = dateFormat.format(currentStart),
                    packageName = pkg,
                    appName = name,
                    startTime = currentStart,
                    endTime = currentEnd,
                    isScreenOff = screenOff
                )
            )

            currentStart = currentEnd
        }
    }
    fun addApp(pkg: String, start: Long, end: Long) {
        val isHome = pkg == launcherPkg
        addSession(
            pkg = if (isHome) "system.home" else pkg,
            name = if (isHome) "Home Screen" else label(pkg),
            start = start,
            end = end,
            screenOff = false
        )
    }


    fun addIdle(start: Long, end: Long) {
        addSession(
            pkg = "system.screen_off",
            name = "Screen Off (Idle)",
            start = start,
            end = end,
            screenOff = true
        )
    }


    val event = UsageEvents.Event()
    val openApps = mutableMapOf<String, Long>()
    var screenOffTime: Long? = null

    val unlockTimes = mutableListOf<Long>()

    while (events.hasNextEvent()) {
        events.getNextEvent(event)
        val pkg = event.packageName ?: continue
        val ts = event.timeStamp

        // Real unlock
        if (event.eventType == UsageEvents.Event.KEYGUARD_HIDDEN) {
            unlockTimes.add(ts)
            continue
        }

        when (event.eventType) {
            UsageEvents.Event.ACTIVITY_RESUMED -> {
                screenOffTime?.let { addIdle(it, ts); screenOffTime = null }
                if (pkg !in openApps) openApps[pkg] = ts
            }

            UsageEvents.Event.ACTIVITY_PAUSED -> {
                openApps.remove(pkg)?.let { addApp(pkg, it, ts) }
            }

            UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                // Save open apps as REAL usage ending at screen-off
                for ((openPkg, openedTime) in openApps) addApp(openPkg, openedTime, ts)
                openApps.clear()
                screenOffTime = ts
            }

            UsageEvents.Event.SCREEN_INTERACTIVE -> {
                screenOffTime?.let { addIdle(it, ts); screenOffTime = null }
            }
        }
    }

    // Close anything still open
    //    for ((openPkg, openedTime) in openApps) addApp(openPkg, openedTime, endTime)
    //    screenOffTime?.let { addIdle(it, endTime) }

    // Merge consecutive sessions of the same package
    val sorted = sessions.sortedBy { it.startTime }
    val merged = mutableListOf<AppUsageSessionEntity>()
    for (s in sorted) {
        val last = merged.lastOrNull()
        if (last != null &&
            last.packageName == s.packageName &&
            s.startTime - last.endTime <= 1000
        ) {
            merged[merged.lastIndex] = last.copy(endTime = maxOf(last.endTime, s.endTime))
        } else {
            merged.add(s)
        }
    }

    return UsageResult(
        sessions = merged.sortedBy { it.startTime },
        unlockTimes = unlockTimes.sorted()
    )
}
