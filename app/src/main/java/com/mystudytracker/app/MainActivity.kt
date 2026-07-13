package com.mystudytracker.app

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                MyStudyTrackerRoot(repository)
            }
        }
    }
}

/**
 * The manifest locks this activity to portrait, but some devices override that (large-screen
 * "ignore orientation request" compatibility settings, some foldables/manufacturer skins, or a
 * user forcing rotation via accessibility/system tools) and hand the app a landscape
 * configuration anyway. `configChanges` includes "orientation" so the activity survives that
 * instead of recreating - we just detect it here and swap the entire UI for a blocking message
 * rather than letting the real layout render sideways or squashed.
 */
@Composable
private fun MyStudyTrackerRoot(repository: ProgressRepository) {
    val configuration = LocalConfiguration.current
    if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        LandscapeBlockedScreen()
    } else {
        MyStudyTrackerNavHost(repository)
    }
}

@Composable
private fun LandscapeBlockedScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "This application does not support landscape mode. Please switch to portrait mode.",
            color = Color.White,
            fontSize = 21.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
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
