package com.achinthas.screencut

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import kotlinx.coroutines.invoke
import kotlinx.coroutines.withContext
import java.time.LocalDate

@Composable
fun SelectedAppDetailDailyContents(packageName: String,selectedDate : String) {

    val context = LocalContext.current
    val database = ApplicationDatabase.getDatabase(context)
    val appUsageStatsDao = database.AppUsageStatsDao()

    var appUsageData by remember { mutableStateOf<AppUsageStatsEntity?>(null) }

    LaunchedEffect(selectedDate) {

        appUsageData = withContext(Dispatchers.IO) {
            appUsageStatsDao.getPackageByDay(packageName,selectedDate)
        }

    }

    val totalUsage = appUsageData?.totalTimeMs ?: 0L
    val totalSession = appUsageData?.sessionsCount ?: 0
    val firstOpen = appUsageData?.firstOpenAt ?: 0
    val lastOpen = appUsageData?.lastOpenAt ?: 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(2.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StatsRow(
            totalUsage = formatMillisToHoursAndMinutes(totalUsage),
            totalSession = totalSession,
            first = formatEpochToDateTimeToMin(firstOpen),
            last = formatEpochToDateTimeToMin(lastOpen),
            modifier = Modifier.padding(top = 14.dp)
        )

    }
}


@Composable
private fun StatsRow(totalUsage: String, totalSession: Int, first: String, last: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        StatItem(totalUsage, "Total Usage", Modifier.weight(1f))
        StatDivider()
        StatItem(totalSession.toString(), "Sessions", Modifier.weight(1f))
        StatDivider()
        StatItem(first, "First Open", Modifier.weight(1f))
        StatDivider()
        StatItem(last, "Last Open", Modifier.weight(1f))
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