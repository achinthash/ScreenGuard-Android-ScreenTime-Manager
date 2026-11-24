package com.achinthas.screencut

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.WeekFields
import java.util.Locale

@Composable
fun SelectedAppDetailWeeklyContents(packageName: String, selectedDate : LocalDate) {

    val context = LocalContext.current
    val database = ApplicationDatabase.getDatabase(context)

    val appUsageSessionDao = database.AppUsageSessionDao()
    val appUsageStatsDao = database.AppUsageStatsDao()

    val weekFields = WeekFields.of(Locale.getDefault())
    val startOfWeek = selectedDate.with(weekFields.dayOfWeek(), 1)
    val endOfWeek = selectedDate.with(weekFields.dayOfWeek(), 7)
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    var appUsageStatsData by remember { mutableStateOf<List<AppUsageStatsEntity>>(emptyList())}
    var appUsageSessionsData by remember { mutableStateOf<List<AppUsageSessionEntity>>(emptyList()) }


    LaunchedEffect(selectedDate) {

        appUsageStatsData = withContext(Dispatchers.IO){
            appUsageStatsDao.getPackageByWeekRange(packageName, startOfWeek.format(formatter), endOfWeek.format(formatter))
        }

        appUsageSessionsData = withContext(Dispatchers.IO){
            appUsageSessionDao.getByPackageAndWeek(packageName, startOfWeek.format(formatter), endOfWeek.format(formatter))
        }
    }

    val weeklyTotalUsage = appUsageStatsData.sumOf { it.totalTimeMs }
    val weeklyTotalSessionCount = appUsageStatsData.sumOf { it.sessionsCount }
    val daysWithData = appUsageStatsData.size.coerceAtLeast(1)
    val dailyAverage = weeklyTotalUsage /daysWithData
    val weeklyBusiestDay = findBusiestDay(appUsageStatsData)



    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(2.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StatsRow(
            totalUsage = formatMillisToHoursAndMinutes(weeklyTotalUsage),
            totalSession = weeklyTotalSessionCount,
            dailyAverage = formatMillisToHoursAndMinutes(dailyAverage),
            busiestDay = weeklyBusiestDay,
            modifier = Modifier.padding(top = 14.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))
    }

}


private fun findBusiestDay(appStatsData : List<AppUsageStatsEntity>) : Pair<String, Long>? {

    return appStatsData
        .groupBy { it.day }
        .mapValues { (_, entries) -> entries.sumOf { it.totalTimeMs } }
        .maxByOrNull { it.value }
        ?.toPair()
}


@Composable
private fun StatsRow(totalUsage: String, totalSession: Int, dailyAverage: String, busiestDay: Pair<String, Long>?, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        val busiestDayText = remember(busiestDay) {
            if (busiestDay == null) {
                "-"
            } else {
                try {
                    val shortDay = LocalDate.parse(busiestDay.first)
                        .format(DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH))
                        shortDay
                } catch (e: DateTimeParseException) {
                    "-"
                }
            }
        }

        StatItem(totalUsage, "Total Usage", Modifier.weight(1f))
        StatDivider()
        StatItem(totalSession.toString(), "Sessions", Modifier.weight(1f))
        StatDivider()
        StatItem(dailyAverage, "Daily Average", Modifier.weight(1f))
        StatDivider()
        StatItem(busiestDayText, "Busiest Day", Modifier.weight(1f))
    }
}

@Composable
private fun StatItem(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(label, fontSize = 12.sp)
    }
}

@Composable
private fun StatDivider() {
    Box(
        Modifier
            .width(1.dp)
            .height(32.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
    )
}
