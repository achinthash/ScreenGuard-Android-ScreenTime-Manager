package com.achinthas.screencut

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.focusModifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

val ChartColors = listOf(
    Color(0xFF4285F4), // Blue
    Color(0xFFEA4335), // Red
    Color(0xFFFBBC05), // Yellow
    Color(0xFF34A853), // Green
    Color(0xFF9C27B0), // Purple
    Color(0xFF00ACC1), // Cyan
    Color(0xFFFF7043), // Deep Orange
    Color(0xFF8D6E63), // Brown
    Color(0xFFE91E63), // Pink
    Color(0xFF3F51B5), // Indigo
    Color(0xFF8BC34A), // Light Green
    Color(0xFFFFC107), // Amber
    Color(0xFF009688), // Teal
    Color(0xFF795548), // Dark Brown
    Color(0xFF607D8B), // Blue Grey
    Color(0xFFCDDC39), // Lime
    Color(0xFF673AB7), // Deep Purple
    Color(0xFF03A9F4), // Light Blue
    Color(0xFFFF5722), // Vermilion
    Color(0xFF9E9E9E)  // Grey
)

@Composable
fun DailySummaryContent(selectedDate : LocalDate) {

    val context = LocalContext.current

    val stringFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    var appUsage by remember { mutableStateOf<List<AppUsageStatsEntity>>(emptyList()) }
    var phoneUsage by remember { mutableStateOf<PhoneUsageStatsEntity?>(null) }

    val database = ApplicationDatabase.getDatabase(context)
    val appUsageStatsDao = database.AppUsageStatsDao()
    val phoneUsageStatsDao = database.PhoneUsageStatsDao()

    val unlockCount = phoneUsage?.unlockCount ?: 0
    val appsCount = appUsage.size
    val fistUnlock = phoneUsage?.firstUnlockAt ?: 0
    val lastUnlock = phoneUsage?.lastUnlockAt ?: 0

    LaunchedEffect(selectedDate) {

        val selectedDateStr = selectedDate.format(stringFormatter)

        appUsage = withContext(Dispatchers.IO){
            appUsageStatsDao.getByDay(selectedDateStr)
        }

        phoneUsage = withContext(Dispatchers.IO){
            phoneUsageStatsDao.getByDay(selectedDateStr)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(2.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        StatsRow(unlocks = unlockCount, apps = appsCount, first = formatEpochToDateTimeToMin(fistUnlock), last = formatEpochToDateTimeToMin(lastUnlock), modifier = Modifier.padding(top = 14.dp))

        Spacer(modifier = Modifier.height(14.dp))

        UsageDonutChart(appUsage, ChartColors)

        Spacer(modifier = Modifier.height(14.dp))

        AppUsageList(appUsage)

    }

}

fun formatEpochToDateTimeToMin(timeInMillis: Long): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(java.util.Date(timeInMillis))
}
@Composable
fun StatsRow(unlocks: Int, apps: Int, first: String, last: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        StatItem(unlocks.toString(), "Unlocks", Modifier.weight(1f))
        StatDivider()
        StatItem(apps.toString(), "Apps used", Modifier.weight(1f))
        StatDivider()
        StatItem(first, "First unlock", Modifier.weight(1f))
        StatDivider()
        StatItem(last, "Last Unlock", Modifier.weight(1f))
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

@Composable
fun UsageDonutChart(
    data: List<AppUsageStatsEntity>,
    colors: List<Color> = ChartColors,
    modifier: Modifier = Modifier,

    ) {
    val totalTime = data.sumOf { it.totalTimeMs }
    val total = data.sumOf { it.totalTimeMs.toDouble() }.toFloat()
    if (total <= 0f) return

    Box(
        modifier = modifier.size(170.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val strokeWidth = 22.dp.toPx()
            var startAngle = -90f
            val gapDegrees = if (data.size > 1) 1.5f else 0f

            data.forEachIndexed { index, item ->
                val sliceColor = colors[index % colors.size]
                val sweepAngle = (item.totalTimeMs / total) * 360f

                if (sweepAngle > 0f) {
                    drawArc(
                        color = sliceColor,
                        startAngle = startAngle + (gapDegrees / 2f),
                        sweepAngle = (sweepAngle - gapDegrees).coerceAtLeast(0.5f),
                        useCenter = false,
                        style = Stroke(
                            width = strokeWidth,
                            cap = StrokeCap.Butt
                        ),
                        topLeft = Offset(
                            strokeWidth / 2f,
                            strokeWidth / 2f
                        ),
                        size = Size(
                            size.width - strokeWidth,
                            size.height - strokeWidth
                        )
                    )
                }

                startAngle += sweepAngle
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Total Usage",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Text(
                text = formatMillisToHoursAndMinutes(totalTime),
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}



@Composable
fun AppUsageList(
    apps: List<AppUsageStatsEntity>,
    colors: List<Color> = ChartColors
) {
    val totalUsageTime = apps.sumOf { it.totalTimeMs }.coerceAtLeast(1L)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        apps.forEachIndexed  { index, app ->
            val progress = (app.totalTimeMs.toFloat() / totalUsageTime.toFloat()).coerceIn(0f, 1f)
            val percentage = (progress * 100).toInt()
            val appColor = colors[index % colors.size]

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIcon(app.packageName)

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Text(
                        text = app.appName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(CircleShape),
                        color = appColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.width(48.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = formatMillisToHoursAndMinutes(app.totalTimeMs),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )

                    Text(
                        text = "$percentage%",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}