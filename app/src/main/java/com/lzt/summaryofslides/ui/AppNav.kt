package com.lzt.summaryofslides.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lzt.summaryofslides.ui.daily.DailyHistoryScreen
import com.lzt.summaryofslides.ui.daily.DailyHomeScreen
import com.lzt.summaryofslides.ui.daily.DailyReportScreen
import com.lzt.summaryofslides.ui.daily.DailySettingsScreen

object Routes {
    const val DailyHome = "daily_home"
    const val DailyHistory = "daily_history"
    const val DailyReport = "daily_report"
    const val DailySettings = "daily_settings"
}

@Composable
fun AppNav() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Routes.DailyHome,
    ) {
        composable(Routes.DailyHome) {
            DailyHomeScreen(
                onBack = {},
                onOpenHistory = { navController.navigate(Routes.DailyHistory) },
                onOpenSettings = { navController.navigate(Routes.DailySettings) },
                onOpenReport = { date -> navController.navigate("${Routes.DailyReport}/$date") },
            )
        }
        composable(Routes.DailyHistory) {
            DailyHistoryScreen(
                onBack = { navController.popBackStack() },
                onOpenReport = { date -> navController.navigate("${Routes.DailyReport}/$date") },
            )
        }
        composable(
            route = "${Routes.DailyReport}/{reportDate}",
            arguments = listOf(navArgument("reportDate") { type = NavType.StringType }),
        ) { backStackEntry ->
            val reportDate = requireNotNull(backStackEntry.arguments?.getString("reportDate"))
            DailyReportScreen(
                reportDate = reportDate,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.DailySettings) {
            DailySettingsScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}
