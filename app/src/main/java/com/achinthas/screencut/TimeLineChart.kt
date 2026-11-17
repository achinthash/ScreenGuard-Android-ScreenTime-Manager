package com.achinthas.screencut

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import java.text.SimpleDateFormat
import java.util.Locale

data class UsageSession(
    val sessionId: Int,
    val sessionStart: Long,
    val sessionEnd: Long,
    val isScreenOffGroup: Boolean,
    val items: List<AppUsageSessionEntity>
)

@Composable
fun TimeLineChart(appUsageSessionData: List<AppUsageSessionEntity>){


    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        items(groupUsageSessionsIntoBlocks(appUsageSessionData), key = { it.sessionId }) { session ->


            val durationText = formatDuration(session.sessionStart, session.sessionEnd)
            val hasItems = session.items.size > 1

            if (session.isScreenOffGroup) {
                GapRow(formatEpochToDateTime(session.sessionStart), durationText)
            }

            else if(!hasItems && session.items[0].appName == "Home Screen"){
                GapRow(formatEpochToDateTime(session.sessionStart), durationText, dashed = true)
            }

            else {
                var isExpanded by remember(session.sessionId) { mutableStateOf(false) }

                if (!isExpanded) {
                    // collapsed summary
                    val main = if (hasItems && session.items[0].appName == "Home Screen") {
                        session.items[1]
                    } else {
                        session.items.firstOrNull()
                    }

                    AppRow(
                        time = formatEpochToDateTime(session.sessionStart),
                        appName = main?.appName ?: "Unknown App",
                        packageName = main?.packageName.orEmpty(),
                        durationText = durationText,
                        extraCount = (session.items.size - 1).coerceAtLeast(0),
                        onClick = if (hasItems) ({ isExpanded = true }) else null
                    )
                } else {
                    // one row per sub-session
                    session.items.forEach { sub ->
                        val subDuration = formatDuration(sub.startTime, sub.endTime)

                        if (sub.appName == "Home Screen") {
                            GapRow(formatEpochToDateTime(sub.startTime), subDuration, dashed = true)
                        } else {
                            AppRow(
                                time = formatEpochToDateTime(sub.startTime),
                                appName = sub.appName,
                                packageName = sub.packageName,
                                durationText = subDuration,
                                onClick = { isExpanded = false }
                            )
                        }
                    }
                }
            }
        }
    }
}

// grouped into all sessions
fun groupUsageSessionsIntoBlocks(rawSessions: List<AppUsageSessionEntity>): List<UsageSession> {
    val result = mutableListOf<UsageSession>()
    val currentActiveItems = mutableListOf<AppUsageSessionEntity>()
    var nextSessionId = 1

    fun flushActiveGroup() {
        if (currentActiveItems.isNotEmpty()) {
            val start = currentActiveItems.first().startTime
            val end = currentActiveItems.last().endTime
            result.add(
                UsageSession(
                    sessionId = nextSessionId++,
                    sessionStart = start,
                    sessionEnd = end,
                    isScreenOffGroup = false,
                    items = currentActiveItems.toList()
                )
            )
            currentActiveItems.clear()
        }
    }

    for (session in rawSessions) {
        if (session.isScreenOff) {
            // Flush any accumulated active app sessions first
            flushActiveGroup()

            // Add the screen-off period as its own standalone session group
            result.add(
                UsageSession(
                    sessionId = nextSessionId++,
                    sessionStart = session.startTime,
                    sessionEnd = session.endTime,
                    isScreenOffGroup = true,
                    items = listOf(session)
                )
            )
        } else {
            // Accumulate active app usage
            currentActiveItems.add(session)
        }
    }

    // Flush any remaining
    flushActiveGroup()

    return result
}



//

fun formatEpochToDateTime(timeInMillis: Long): String {
    val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return formatter.format(java.util.Date(timeInMillis))
}

fun formatDuration(start: Long, end: Long): String {
    val diffSeconds = maxOf(0L, (end - start) / 1000)
    val hours = diffSeconds / 3600
    val minutes = (diffSeconds % 3600) / 60
    val seconds = diffSeconds % 60

    return when {
        hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}

// timeLines row
@Composable
private fun TimelineLine(height: Dp, color: Color, dashed: Boolean) {
    Canvas(
        modifier = Modifier
            .width(4.dp)
            .height(height)
    ) {
        drawLine(
            color = color,
            start = Offset(size.width / 2f, 0f),
            end = Offset(size.width / 2f, size.height),
            pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f) else null,
            strokeWidth = 4f
        )
    }
}

@Composable
fun AppIcon(packageName: String) {
    val context = LocalContext.current
    val icon = remember(packageName) {
        if (packageName.isNotEmpty()) {
            getAppIcon(context, packageName)?.toBitmap()?.asImageBitmap()
        } else null
    }
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = "App Icon",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}


// TimelineRow
@Composable
private fun TimelineRow(
    time: String,
    rowHeight: Dp,
    lineColor: Color,
    modifier: Modifier = Modifier,
    dashedLine: Boolean = false,
    cardColor: Color =  MaterialTheme.colorScheme.surfaceContainerHigh,
    content: @Composable RowScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(rowHeight)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(bottom = 1.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Time column
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(contentAlignment = Alignment.TopCenter) {
                    Text(
                        text = time,
                        style = TextStyle(
                            color = Color.Gray,
                            fontSize = 12.sp,
                            lineHeight = 12.sp,
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                            lineHeightStyle = LineHeightStyle(
                                alignment = LineHeightStyle.Alignment.Top,
                                trim = LineHeightStyle.Trim.Both
                            )
                        ),
                        modifier = Modifier.wrapContentSize()
                    )
                }
            }

            // Line column
            Column(
                modifier = Modifier
                    .width(4.dp)
                    .padding(start = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                TimelineLine(rowHeight, lineColor, dashedLine)
            }

            // Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(rowHeight)
                    .padding(start = 8.dp),
                shape = RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    content = content
                )
            }
        }
    }
}

// screen-off periods and Home Screen
@Composable
private fun GapRow(time: String, durationText: String, dashed: Boolean = false) {
    TimelineRow(
        time = time,
        rowHeight = 40.dp,
        lineColor = Color.Gray,
        dashedLine = dashed,
        cardColor = Color.Transparent
    ) {
        Text(text = durationText, color = Color.Gray, fontSize = 11.sp)
    }
}

// app data row
@Composable
private fun AppRow(
    time: String,
    appName: String,
    packageName: String,
    durationText: String,
    extraCount: Int = 0,
    onClick: (() -> Unit)? = null
) {
    TimelineRow(
        time = time,
        rowHeight = 60.dp,
        lineColor = MaterialTheme.colorScheme.primary,
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 4.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row {
                Text(
                    text = appName,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                if (extraCount > 0) {
                    Text(
                        modifier = Modifier.padding(start = 4.dp),
                        text = "+ $extraCount",
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                }
            }
            Text(text = durationText, color = Color.Gray, fontSize = 11.sp)
        }

        AppIcon(packageName)
    }
}

// app icons getter
fun getAppIcon(context: Context, packageName: String): Drawable? {
    return try {
        context.packageManager.getApplicationIcon(packageName)
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
        null
    }
}