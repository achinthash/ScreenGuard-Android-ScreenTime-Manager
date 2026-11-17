package com.achinthas.screencut

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import java.time.temporal.WeekFields
import java.util.Locale
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class RangeMode { DAILY, WEEKLY }

@Composable
fun SummaryScreen() {

    val context = LocalContext.current

    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var mode by remember { mutableStateOf(RangeMode.DAILY) }
    val stringFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    var appUsage by remember { mutableStateOf<List<AppUsageStatsEntity>>(emptyList()) }

    val database = ApplicationDatabase.getDatabase(context)
    val appUsageStatsDao = database.AppUsageStatsDao()

    LaunchedEffect(selectedDate) {

        val selectedDateStr = selectedDate.format(stringFormatter)

        appUsage = withContext(Dispatchers.IO){
            appUsageStatsDao.getByDay(selectedDateStr)
        }
    }

    // date controller
    DateControlBar(
        selectedDate = selectedDate,
        onDateSelected = { newDate ->
            selectedDate = newDate
        },
        mode = mode,
        onModeSelected = { newMode ->
            mode = newMode
        }
    )

    AppUsageList(appUsage)
}

@Composable
fun AppUsageList(apps: List<AppUsageStatsEntity>) {
    val totalUsageTime = apps.sumOf { it.totalTimeMs }.coerceAtLeast(1L)

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        items(apps, key = { it.id }) { app ->
            val progress = (app.totalTimeMs.toFloat() / totalUsageTime.toFloat()).coerceIn(0f, 1f)
            val percentage = (progress * 100).toInt()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App icon
                AppIcon(app.packageName)

                Spacer(modifier = Modifier.width(12.dp))

                // App name and usage progress
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
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // precentage
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateControlBar(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    mode: RangeMode,
    onModeSelected: (RangeMode) -> Unit

) {
    var showDatePicker by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }
    val today = LocalDate.now()

    // Calculate Week boundaries based on selectedDate
    val weekFields = WeekFields.of(Locale.getDefault())
    val startOfWeek = selectedDate.with(weekFields.dayOfWeek(), 1)
    val endOfWeek = selectedDate.with(weekFields.dayOfWeek(), 7)

    // Future check for buttons
    val currentWeekStart = today.with(weekFields.dayOfWeek(), 1)
    val canGoForward = if (mode == RangeMode.DAILY) {
        selectedDate.isBefore(today)
    } else {
        startOfWeek.isBefore(currentWeekStart)
    }


    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous Day Button
        Surface(
            onClick = { if (mode == RangeMode.DAILY) {
                onDateSelected(selectedDate.minusDays(1))
            } else {
                onDateSelected(selectedDate.minusWeeks(1))
            } },
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous Day",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Date Display Button
        Surface(
            onClick = { showDatePicker = true },
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                val displayText = if (mode == RangeMode.DAILY) {
                    selectedDate.format(dateFormatter)
                } else {
                    val startFormat = startOfWeek.format(DateTimeFormatter.ofPattern("MMM d"))
                    val endFormat = endOfWeek.format(DateTimeFormatter.ofPattern("MMM d"))
                    "$startFormat - $endFormat"
                }

                Text(
                    text = displayText,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
        }

        Box {
            Surface(
                onClick = { menuOpen = true },
                modifier = Modifier.height(48.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (mode == RangeMode.WEEKLY) "Weekly" else "Daily",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (mode == RangeMode.DAILY){
                        Spacer(modifier = Modifier.width(11.dp))
                    } else {
                        Spacer(modifier = Modifier.width(0.dp))
                    }

                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select Mode",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Dropdown Menu
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Daily") },
                    onClick = {
                        onModeSelected(RangeMode.DAILY)
                        menuOpen = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Weekly") },
                    onClick = {
                        onModeSelected(RangeMode.WEEKLY)
                        menuOpen = false
                    }
                )
            }
        }


        // Next Day Button
        Surface(
            onClick = {
                if (canGoForward) {
                    if (mode == RangeMode.DAILY) {
                        onDateSelected(selectedDate.plusDays(1))
                    } else {
                        onDateSelected(selectedDate.plusWeeks(1))
                    }
                }
            },
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            enabled = canGoForward
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next Day",
                    tint = if (canGoForward) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val newDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            if (!newDate.isAfter(today)) {
                                onDateSelected(newDate)
                            }
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
