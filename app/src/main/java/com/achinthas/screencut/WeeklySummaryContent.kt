package com.achinthas.screencut

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.abs


@Composable
fun WeeklySummaryContent(selectedDate: LocalDate) {

    val weekFields = WeekFields.of(Locale.getDefault())
    val startOfWeek = selectedDate.with(weekFields.dayOfWeek(), 1)
    val endOfWeek = selectedDate.with(weekFields.dayOfWeek(), 7)
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")


    val context = LocalContext.current
    val database = ApplicationDatabase.getDatabase(context)

    var phoneUsages by remember{ mutableStateOf<List<PhoneUsageStatsEntity>>(emptyList()) }
    var previousWeekUsages by remember{ mutableStateOf<List<PhoneUsageStatsEntity>>(emptyList()) }


    val phoneUsageStatsDao = database.PhoneUsageStatsDao()


    LaunchedEffect(selectedDate) {
        val prevStart = startOfWeek.minusWeeks(1)
        val prevEnd = endOfWeek.minusWeeks(1)

        withContext(Dispatchers.IO) {
            phoneUsages = phoneUsageStatsDao.getByWeek(startOfWeek.format(formatter), endOfWeek.format(formatter))
            previousWeekUsages = phoneUsageStatsDao.getByWeek(prevStart.format(formatter), prevEnd.format(formatter))
        }
    }




    val weeklyTotal = phoneUsages.sumOf { it.totalMs }
    val lastWeekTotal = previousWeekUsages.sumOf { it.totalMs }

    // Assumes one row per day; adjust if your table has one row per app per day
    val daysWithData = phoneUsages.size.coerceAtLeast(1)
    val dailyAverageMs = weeklyTotal / daysWithData


    Column(modifier = Modifier
        .fillMaxSize()
        .padding(2.dp)
        .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    )  {

        // status summary
        StatsRow(weeklyTotal, formatMillisToHoursAndMinutes(dailyAverageMs) , calculateWeeklyChange(lastWeekTotal, weeklyTotal) ,modifier = Modifier.padding(top = 14.dp))

    }

}

fun calculateWeeklyChange(lastWeekMs: Long, thisWeekMs: Long): String {
    if (lastWeekMs <= 0L) return if (thisWeekMs > 0L) "New" else "-"

    val percentage = (thisWeekMs - lastWeekMs).toDouble() / lastWeekMs * 100.0
    val formatted = String.format(Locale.US, "%.0f%%", abs(percentage))

    return when {
        percentage > 0.5 -> "↑ $formatted"
        percentage < -0.5 -> "↓ $formatted"
        else -> "0%"
    }
}

@Composable
fun StatsRow(weeklyTotalUsage: Long, dailyAverage: String, comparison: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        StatItem(formatMillisToHoursAndMinutes(weeklyTotalUsage), "Weekly Usage", Modifier.weight(1f))
        StatDivider()
        StatItem(dailyAverage, "Daily Avg", Modifier.weight(1f))
        StatDivider()
        StatItem(comparison, "Vs Last Week", Modifier.weight(1f))
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