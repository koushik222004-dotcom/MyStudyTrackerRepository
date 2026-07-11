package com.mystudytracker.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
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

    NavHost(navController = navController, startDestination = "calendar") {
        composable("calendar") {
            val viewModel: CalendarViewModel = viewModel(factory = CalendarViewModel.factory(repository))
            CalendarScreen(
                viewModel = viewModel,
                onDateSelected = { date -> navController.navigate("checklist/$date") }
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
                onBack = { navController.popBackStack() }
            )
        }
    }
}
