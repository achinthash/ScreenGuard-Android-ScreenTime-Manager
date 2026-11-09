package com.achinthas.screenguard

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.achinthas.screencut.ApplicationDatabase
import com.achinthas.screencut.PhoneUsageStatsEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyUsageScreen() {

    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("My Usage") })
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
                .padding(horizontal =  12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)

        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(4.dp)
                        .height(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selectedTab == 0) MaterialTheme.colorScheme.primary else  MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0f),)
                        .clickable { selectedTab = 0 },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Timeline",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selectedTab == 0 ) FontWeight.Bold else FontWeight.Medium,
                        color = if(selectedTab == 0)  MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(4.dp)
                        .height(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selectedTab == 1) MaterialTheme.colorScheme.primary else  MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0f),)
                        .clickable { selectedTab = 1 },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Summary",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                        color = if(selectedTab == 1)  MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(modifier = Modifier.height(1.dp))

            // Switch Composables
            when (selectedTab) {
                0 -> TimelineContent()
                1 -> SummaryContent()
            }
        }



    }

}


@Composable
fun TimelineContent() {

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


        // usage card
        UsageCard(
            dateText = selectedDate,
            timeSpent = formatMillisToHoursAndMinutes(todayUsage?.totalMs ?: 0L),
            comparisonText = calculateScreenTimeChange(yesterdayUsage?.totalMs ?: 0L,todayUsage?.totalMs ?: 0L, ),
            todayUnlockCount = todayUsage?.unlockCount ?: 0
        )
    }
}


@Composable
fun SummaryContent() {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {

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


