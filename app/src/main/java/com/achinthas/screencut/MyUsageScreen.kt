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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp


@Composable
fun MyUsageScreen() {

    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(

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
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {

    }
}


@Composable
fun SummaryContent() {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {

    }
}


