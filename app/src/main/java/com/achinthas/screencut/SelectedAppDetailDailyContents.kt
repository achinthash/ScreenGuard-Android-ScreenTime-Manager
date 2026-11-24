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
import androidx.compose.runtime.mutableIntStateOf
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
import kotlinx.coroutines.invoke
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalQueries.zoneId
import java.time.temporal.WeekFields
import java.util.Calendar
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
import kotlin.time.Instant

@Composable
fun SelectedAppDetailDailyContents(packageName: String,selectedDate : LocalDate) {

    val context = LocalContext.current
    val database = ApplicationDatabase.getDatabase(context)
    val appUsageStatsDao = database.AppUsageStatsDao()
    val appUsageSessionDao = database.AppUsageSessionDao()

    var appUsageData by remember { mutableStateOf<AppUsageStatsEntity?>(null) }
    var appUsageSessionsData by remember { mutableStateOf<List<AppUsageSessionEntity>>(emptyList()) }

    val stringFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val selectedDateStr = selectedDate.format(stringFormatter)
    LaunchedEffect(selectedDate) {

        appUsageData = withContext(Dispatchers.IO) {
            appUsageStatsDao.getPackageByDay(packageName,selectedDateStr)
        }

        appUsageSessionsData = withContext(Dispatchers.IO){
            appUsageSessionDao.getByPackageAndDay(packageName, selectedDateStr)
        }
    }

    val totalUsage = appUsageData?.totalTimeMs ?: 0L
    val totalSession = appUsageData?.sessionsCount ?: 0
    val firstOpen = appUsageData?.firstOpenAt ?: 0
    val lastOpen = appUsageData?.lastOpenAt ?: 0

    val hourlyMinutes = remember(appUsageSessionsData) {
        sessionsToHourlyMinutes(appUsageSessionsData)
    }

    val hourLabels = remember {
        (0..23).map { hour -> hour.toString() }
    }

    val currentHour = remember {
        Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(2.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // empty data view
        if (appUsageSessionsData.isEmpty() && appUsageData == null ) {
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
                totalUsage = formatMillisToHoursAndMinutes(totalUsage),
                totalSession = totalSession,
                first = formatEpochToDateTimeToMin(firstOpen),
                last = formatEpochToDateTimeToMin(lastOpen),
                modifier = Modifier.padding(top = 14.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))
            HourlyAxisBarChart(
                values = hourlyMinutes,
                labels = hourLabels,
                highlightIndex = currentHour,
                isHourly = true
            )
        }
    }
}


fun sessionsToHourlyMinutes(sessions: List<AppUsageSessionEntity>): List<Int> {
    val hourlyTotals = (0..23).associateWith { 0L }.toMutableMap()
    val calendar = Calendar.getInstance()

    for (session in sessions) {
        if (session.isScreenOff || session.packageName == "system.home" || session.endTime <= session.startTime) {
            continue
        }

        var currentStart = session.startTime
        while (currentStart < session.endTime) {
            calendar.timeInMillis = currentStart
            val hour = calendar.get(Calendar.HOUR_OF_DAY)

            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.add(Calendar.HOUR_OF_DAY, 1)
            val nextHourStart = calendar.timeInMillis

            val currentEnd = minOf(session.endTime, nextHourStart)
            hourlyTotals[hour] = hourlyTotals.getValue(hour) + (currentEnd - currentStart)

            currentStart = currentEnd
        }
    }

    return (0..23).map { hour ->
        val ms = hourlyTotals[hour] ?: 0L
        if (ms > 0L) maxOf(1, (ms / 60_000L).toInt()) else 0 // keeps brief usage visible as 1m
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



@Composable
fun HourlyAxisBarChart(
    values: List<Int>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    highlightIndex: Int = -1,
    isHourly: Boolean = false,
    chartHeight: Dp = 220.dp,
    barColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    selectedColor: Color = MaterialTheme.colorScheme.primary
) {
    val count = minOf(values.size, labels.size)
    if (count == 0) return

    val safeValues = values.take(count)
    val safeLabels = labels.take(count)

    val textMeasurer = rememberTextMeasurer()
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val strongColor = MaterialTheme.colorScheme.onSurface

    val maxVal = max(safeValues.maxOrNull() ?: 0, 1)
    val candidates = if (isHourly) {
        listOf(10, 15, 20, 30, 60)
    } else {
        listOf(30, 60, 120, 180, 240, 360, 480, 720)
    }
    val step = candidates.firstOrNull { ceil(maxVal / it.toDouble()) <= 4 } ?: maxOf(60, candidates.last())
    val tickCount = ceil(maxVal / step.toDouble()).toInt().coerceAtLeast(1)
    val niceMax = tickCount * step

    val peak = if (safeValues.all { it == 0 }) -1 else safeValues.indexOf(safeValues.max())
    var selected by remember(safeValues) { mutableIntStateOf(peak) }

    val leftPad =  25.dp
    val rightPad = 8.dp
    val topPad =  8.dp
    val bottomPad =  8.dp

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(chartHeight)
            .pointerInput(safeValues) {
                detectTapGestures { tap ->
                    val leftPx = leftPad.toPx()
                    val plotWidth = size.width - leftPx - rightPad.toPx()
                    val slot = plotWidth / count.toFloat()
                    val index = ((tap.x - leftPx) / slot).toInt()
                    if (tap.x >= leftPx && index in 0 until count) {
                        selected = index
                    }
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
                topLeft = Offset(
                    left - 6.dp.toPx() - layout.size.width,
                    y - layout.size.height / 2f
                )
            )
        }

        // Bars + X axis labels
        val slot = plotW / count.toFloat()
        val barW = slot * if (isHourly) 0.62f else 0.55f
        val radius = if (isHourly) 3.dp.toPx() else 6.dp.toPx()

        safeValues.forEachIndexed { i, minutes ->
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
                    TextStyle(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = strongColor
                    )
                )
                val centeredX = x + barW / 2f - layout.size.width / 2f
                val clampedX = centeredX.coerceIn(0f, (size.width - layout.size.width).coerceAtLeast(0f))
                drawText(
                    layout,
                    topLeft = Offset(
                        clampedX,
                        (baseY - barH - layout.size.height - 4.dp.toPx()).coerceAtLeast(0f)
                    )
                )
            }

            // Show every label on weekly chart
            val showLabel = !isHourly || (i % 3 == 0) || isSelected
            if (showLabel) {
                val isHighlighted = i == highlightIndex
                val labelLayout = textMeasurer.measure(
                    safeLabels[i],
                    TextStyle(
                        fontSize = 10.sp,
                        fontWeight = if (isHighlighted || isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isHighlighted || isSelected) strongColor else labelColor
                    )
                )
                drawText(
                    labelLayout,
                    topLeft = Offset(
                        left + slot * i + slot / 2f - labelLayout.size.width / 2f,
                        baseY + 6.dp.toPx()
                    )
                )
            }
        }
    }
}

private fun axisLabel(minutes: Int): String = when {
    minutes == 0 -> "0"
    minutes < 60 -> "${minutes}m"
    minutes % 60 == 0 -> "${minutes / 60}h"
    else -> "${minutes / 60}h ${minutes % 60}m"
}