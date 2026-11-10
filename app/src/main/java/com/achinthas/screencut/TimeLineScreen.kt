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
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs


@Composable
fun TimeLineScreen(){

    val context = LocalContext.current

    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val stringFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    var todayUsage by remember { mutableStateOf<PhoneUsageStatsEntity?>(null) }
    var yesterdayUsage by remember { mutableStateOf<PhoneUsageStatsEntity?>(null) }

    val database = ApplicationDatabase.getDatabase(context)
    val phoneUsageStatsDao = database.PhoneUsageStatsDao()

    // fetch data from db
    LaunchedEffect(selectedDate) {

        val selectedDateStr = selectedDate.format(stringFormatter)
        val previousDateStr = selectedDate.minusDays(1).format(stringFormatter)

        todayUsage = withContext(Dispatchers.IO) {
            phoneUsageStatsDao.getByDay(selectedDateStr)
        }

        yesterdayUsage = withContext(Dispatchers.IO) {
            phoneUsageStatsDao.getByDay(previousDateStr)
        }

    }

    Column(modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {

        // date controller
        DateControlBar(
            selectedDate = selectedDate,
            onDateSelected = { newDate ->
                selectedDate = newDate
            }
        )
        // usage card
        UsageCard(
            dateText = selectedDate,
            timeSpent = formatMillisToHoursAndMinutes(todayUsage?.totalMs ?: 0L),
            comparisonText = calculateScreenTimeChange(
                yesterdayUsage?.totalMs ?: 0L,
                todayUsage?.totalMs ?: 0L,
            ),
            todayUnlockCount = todayUsage?.unlockCount ?: 0
        )


    }
}


@Composable
private fun UsageCard(
    dateText: LocalDate,
    timeSpent: String,
    comparisonText: String,
    todayUnlockCount : Int
) {

    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)

    ){
        Column( modifier = Modifier.padding(20.dp)) {

            Text(
                text = dateText.format(dateFormatter),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = timeSpent,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(){
                    Text(
                        text = "Unlock Count : $todayUnlockCount",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 1.dp)
                    )
                    Text(
                        text = comparisonText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,

                        )

                }
            }
        }
    }
}


fun calculateScreenTimeChange(yesterdayMs: Long, todayMs: Long): String {

    if (yesterdayMs <= 0L) {
        return if (todayMs > 0L) "New usage today" else "Same as yesterday"
    }

    val diff = todayMs - yesterdayMs
    val percentage = (diff.toDouble() / yesterdayMs.toDouble()) * 100.0

    // Safeguard
    if (percentage.isNaN() || percentage.isInfinite()) {
        return "Same as yesterday"
    }

    val formattedPercentage = String.format(Locale.US, "%.0f%%", abs(percentage))

    return when {
        percentage > 0.0 -> "↑ $formattedPercentage vs yesterday"
        percentage < 0.0 -> "↓ $formattedPercentage vs yesterday"
        else -> "Same as yesterday"
    }
}

fun formatMillisToHoursAndMinutes(mills: Long):String {
    if(mills <= 0) return "0m"
    val totalMinutes = mills / (1000 * 60)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60

    return buildString {
        if(hours > 0) {
            append("${hours}h ")
        }
        if(minutes > 0 || hours == 0L){
            append("${minutes}m")
        }
    }.trim()
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateControlBar(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit

) {
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }

    val today = LocalDate.now()
    val canGoForward = selectedDate.isBefore(today)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous Button
        Surface(
            onClick = { onDateSelected(selectedDate.minusDays(1)) },
            modifier = Modifier.size(50.dp),
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

        if(showDatePicker){

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
                                //  Prevent selecting a future date from picker
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

        // Middle Date Picker Button
        Surface(
            onClick = {

                showDatePicker = true

            },
            modifier = Modifier
                .weight(1f)
                .height(50.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = selectedDate.format(dateFormatter),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Outlined.DateRange,
                    contentDescription = "Select Date",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Next Button
        Surface(
            onClick = {
                if(canGoForward){
                    onDateSelected(selectedDate.plusDays(1))
                }
            },
            modifier = Modifier.size(50.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next Day",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        //
    }
}

