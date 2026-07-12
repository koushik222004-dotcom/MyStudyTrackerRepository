package com.mystudytracker.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mystudytracker.app.data.ProgressRepository
import com.mystudytracker.app.ui.calendar.CalendarScreen
import com.mystudytracker.app.ui.calendar.CalendarViewModel
import com.mystudytracker.app.ui.checklist.ChecklistScreen
import com.mystudytracker.app.ui.checklist.ChecklistViewModel
import com.mystudytracker.app.ui.splash.SplashScreen
import com.mystudytracker.app.ui.theme.MyStudyTrackerTheme
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = (application as MyStudyTrackerApplication).repository
        setContent {
            MyStudyTrackerTheme {
                MyStudyTrackerNavHost(repository)
            }
        }
    }
}

@Composable
private fun MyStudyTrackerNavHost(repository: ProgressRepository) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "launchSplash") {
        // Shown once on cold start, before the calendar - covers the brief window while Room's
        // first emission and the date-integrity anchor are still loading.
        composable("launchSplash") {
            SplashScreen(
                onFinished = {
                    navController.navigate("calendar") {
                        popUpTo("launchSplash") { inclusive = true }
                    }
                }
            )
        }
        composable("calendar") {
            val context = LocalContext.current
            val viewModel: CalendarViewModel = viewModel(factory = CalendarViewModel.factory(repository, context.applicationContext))
            CalendarScreen(
                viewModel = viewModel,
                onDateSelected = { date -> navController.navigate("dateSplash/$date") }
            )
        }
        // Shown every time a date is opened - covers the brief window while that day's checklist
        // data is still loading from Room.
        composable(
            route = "dateSplash/{date}",
            arguments = listOf(navArgument("date") { type = NavType.StringType })
        ) { backStackEntry ->
            val dateArg = backStackEntry.arguments?.getString("date") ?: LocalDate.now().toString()
            SplashScreen(
                onFinished = {
                    navController.navigate("checklist/$dateArg") {
                        popUpTo("dateSplash/$dateArg") { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = "checklist/{date}",
            arguments = listOf(navArgument("date") { type = NavType.StringType })
        ) { backStackEntry ->
            val dateArg = backStackEntry.arguments?.getString("date") ?: LocalDate.now().toString()
            val viewModel: ChecklistViewModel = viewModel(factory = ChecklistViewModel.factory(repository, dateArg))
            ChecklistScreen(
                date = LocalDate.parse(dateArg),
                viewModel = viewModel,
                onBack = { navController.popBackStack("calendar", inclusive = false) }
            )
        }
    }
}
