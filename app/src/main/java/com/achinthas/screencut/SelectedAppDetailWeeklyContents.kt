package com.achinthas.screencut

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

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
    var previousWeekUsages by remember{ mutableStateOf<List<AppUsageStatsEntity>>(emptyList()) }

    LaunchedEffect(selectedDate) {

        val prevStart = startOfWeek.minusWeeks(1)
        val prevEnd = endOfWeek.minusWeeks(1)

        appUsageStatsData = withContext(Dispatchers.IO){
            appUsageStatsDao.getPackageByWeekRange(packageName, startOfWeek.format(formatter), endOfWeek.format(formatter))
        }
        previousWeekUsages = withContext(Dispatchers.IO){
            appUsageStatsDao.getPackageByWeekRange(packageName, prevStart.format(formatter), prevEnd.format(formatter))
        }

    }

    val weeklyTotalUsage = appUsageStatsData.sumOf { it.totalTimeMs }
    val lastWeekTotal = previousWeekUsages.sumOf { it.totalTimeMs }
    val weeklyTotalSessionCount = appUsageStatsData.sumOf { it.sessionsCount }
    val daysWithData = appUsageStatsData.size.coerceAtLeast(1)
    val dailyAverage = weeklyTotalUsage /daysWithData

    val weekDays = (0..6).map { startOfWeek.plusDays(it.toLong()) }

    val minutesPerDay: List<Int> = remember(appUsageStatsData, startOfWeek) {
        val msByDay = appUsageStatsData.associate { it.day to it.totalTimeMs }

        weekDays.map { date ->
            val ms = msByDay[date.format(formatter)] ?: 0L
            (ms / 60_000L).toInt()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(2.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // empty data view
        if (appUsageStatsData.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(vertical = 5.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.BarChart,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No recorded data available",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }

        } else {
            StatsRow(
                totalUsage = formatMillisToHoursAndMinutes(weeklyTotalUsage),
                totalSession = weeklyTotalSessionCount,
                dailyAverage = formatMillisToHoursAndMinutes(dailyAverage),
                comparison = calculateWeeklyChanges(lastWeekTotal, weeklyTotalUsage),
                modifier = Modifier.padding(top = 14.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // weekly chart
            WeeklyTotalAxisBarChart(
                minutesPerDay = minutesPerDay,
                todayIndex = weekDays.indexOf(LocalDate.now()),
                modifier = Modifier.padding(16.dp)
            )

        }
    }

}

private fun calculateWeeklyChanges(lastWeekMs: Long, thisWeekMs: Long): String {
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
private fun StatsRow(totalUsage: String, totalSession: Int, dailyAverage: String, comparison: String, modifier: Modifier = Modifier) {
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
        StatItem(dailyAverage, "Daily Average", Modifier.weight(1f))
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



@Composable
private fun WeeklyTotalAxisBarChart(
    minutesPerDay: List<Int>,
    modifier: Modifier = Modifier,
    todayIndex: Int = -1,
    chartHeight: Dp = 220.dp,
    barColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    selectedColor: Color = MaterialTheme.colorScheme.primary
) {
    val textMeasurer = rememberTextMeasurer()
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val strongColor = MaterialTheme.colorScheme.onSurface

    val dayLabels = remember {
        val locale = Locale.getDefault()
        val first = WeekFields.of(locale).firstDayOfWeek
        (0L..6L).map { first.plus(it).getDisplayName(java.time.format.TextStyle.SHORT, locale) }
    }

    // Y axis: pick a round step so there are at most 4 gridlines above zero
    val maxMinutes = max(minutesPerDay.maxOrNull() ?: 0, 1)
    val step = listOf(30, 60, 120, 180, 240, 360, 480, 720)
        .firstOrNull { ceil(maxMinutes / it.toDouble()) <= 4 } ?: 1440
    val tickCount = ceil(maxMinutes / step.toDouble()).toInt().coerceAtLeast(1)
    val niceMax = tickCount * step

    val peak = if (minutesPerDay.all { it == 0 }) -1 else minutesPerDay.indexOf(minutesPerDay.max())
    var selected by remember(minutesPerDay) { mutableStateOf(peak) }

    val leftPad = 8.dp
    val rightPad = 8.dp
    val topPad = 8.dp
    val bottomPad = 8.dp

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(chartHeight)
            .pointerInput(minutesPerDay) {
                detectTapGestures { tap ->
                    val plotWidth = size.width - leftPad.toPx() - rightPad.toPx()
                    val slot = plotWidth / 7f
                    val index = ((tap.x - leftPad.toPx()) / slot).toInt()
                    if (tap.x >= leftPad.toPx() && index in 0..6) selected = index
                }
            }
    ) {
        val left = leftPad.toPx()
        val top = topPad.toPx()
        val plotW = size.width - left - rightPad.toPx()
        val plotH = size.height - top - bottomPad.toPx()
        val baseY = top + plotH
        val labelStyle = TextStyle(fontSize = 11.sp, color = labelColor)

        // Y axis: gridlines + labels
        for (i in 0..tickCount) {
            val value = i * step
            val y = baseY - plotH * (value.toFloat() / niceMax)
            drawLine(
                color = gridColor,
                start = Offset(left, y),
                end = Offset(left + plotW, y),
                strokeWidth = if (i == 0) 1.5.dp.toPx() else 1.dp.toPx()
            )
            val layout = textMeasurer.measure(axisLabel(value), labelStyle)
            drawText(
                layout,
                topLeft = Offset(left - 8.dp.toPx() - layout.size.width, y - layout.size.height / 2f)
            )
        }

        // Bars + X axis labels
        val slot = plotW / 7f
        val barW = slot * 0.55f
        val radius = 6.dp.toPx()

        minutesPerDay.forEachIndexed { i, minutes ->
            val barH = plotH * (minutes.toFloat() / niceMax)
            val x = left + slot * i + (slot - barW) / 2f
            val isSelected = i == selected

            if (barH > 0f) {
                val path = Path().apply {
                    addRoundRect(
                        RoundRect(
                            rect = Rect(x, baseY - barH, x + barW, baseY),
                            topLeft = CornerRadius(radius),
                            topRight = CornerRadius(radius),
                            bottomLeft = CornerRadius.Zero,
                            bottomRight = CornerRadius.Zero
                        )
                    )
                }
                drawPath(path, if (isSelected) selectedColor else barColor)
            }

            // Value above the selected bar
            if (isSelected && minutes > 0) {
                val layout = textMeasurer.measure(
                    axisLabel(minutes),
                    TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = strongColor)
                )
                drawText(
                    layout,
                    topLeft = Offset(
                        x + barW / 2f - layout.size.width / 2f,
                        baseY - barH - layout.size.height - 4.dp.toPx()
                    )
                )
            }

            // Day label
            val isToday = i == todayIndex
            val dayLayout = textMeasurer.measure(
                dayLabels[i],
                TextStyle(
                    fontSize = 11.sp,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (isToday) strongColor else labelColor
                )
            )
            drawText(
                dayLayout,
                topLeft = Offset(
                    left + slot * i + slot / 2f - dayLayout.size.width / 2f,
                    baseY + 8.dp.toPx()
                )
            )
        }
    }
}

private fun axisLabel(minutes: Int): String = when {
    minutes == 0 -> "0"
    minutes < 60 -> "${minutes}m"
    minutes % 60 == 0 -> "${minutes / 60}h"
    else -> "${minutes / 60}h ${minutes % 60}m"
}