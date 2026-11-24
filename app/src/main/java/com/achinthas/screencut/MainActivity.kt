package com.achinthas.screencut

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.achinthas.screencut.ui.theme.ScreenCutTheme
import com.achinthas.screenguard.LimitsScreen
import com.achinthas.screenguard.MyUsageScreen
import com.achinthas.screenguard.SettingsScreen
import kotlinx.serialization.Serializable
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        // splash screen implements
        installSplashScreen()

        super.onCreate(savedInstanceState)

        // work manager
        scheduleAppUsageWorker(this)

        setContent {
            ScreenCutTheme {
                MainScreen()
            }
        }
    }
}

// work manager schedule
fun scheduleAppUsageWorker(context: Context) {
    val constraints = Constraints.Builder()
        .setRequiresBatteryNotLow(true)
        .build()

    val usageWorkRequest = PeriodicWorkRequestBuilder<UsageWorker>(
        repeatInterval = 20,
        repeatIntervalTimeUnit = TimeUnit.MINUTES
    )
        .setConstraints(constraints)
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "AppUsageTrackingJob",
        ExistingPeriodicWorkPolicy.KEEP,
        usageWorkRequest
    )
}
@Serializable
data class AppDetailRoute(
    val packageName: String,
    val date: String
)


@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val bottomNavItems = listOf(Screens.MyUsage, Screens.Limits, Screens.Settings)
    Scaffold(modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar() {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(imageVector = screen.icon, contentDescription = screen.label) },
                        label = {Text(screen.label)},
                        selected = currentDestination?.hierarchy?.any() {it.route == screen.route} == true,
                        onClick = {
                            navController.navigate(screen.route){
                                popUpTo(navController.graph.findStartDestination().id){
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }

                    )
                }

            }
        }    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screens.MyUsage.route,
            modifier = Modifier.padding(innerPadding)
        ) {

            //
            composable(Screens.MyUsage.route) {
                MyUsageScreen(
                    onAppClick = { packageName, selectedDate ->
                        navController.navigate(
                            AppDetailRoute(
                                packageName = packageName,
                                date = selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                            )
                        )
                    }
                )
            }

            composable(Screens.Limits.route) { LimitsScreen() }
            composable(Screens.Settings.route) { SettingsScreen() }

            //
            composable<AppDetailRoute> { backStackEntry ->
                val args: AppDetailRoute = backStackEntry.toRoute()
                SelectedAppDetailScreen(
                    packageName = args.packageName,
                    dateStr = args.date,
                    onBack = { navController.popBackStack() }
                )
            }
        }

    }
}