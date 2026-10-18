package com.achinthas.screencut

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed

sealed class Screens(val route: String, val label: String, val icon: ImageVector) {

    object MyUsage : Screens("myusagescreen", "MyUsage", Icons.Filled.BarChart )
    object Limits : Screens("LimitsScreen", "Limits", Icons.Filled.Speed )
    object Settings : Screens("settings", "Settings", Icons.Filled.Settings )
}

