package com.example.fitlinktrainer.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.fitlinktrainer.R
import com.example.fitlinktrainer.ui.components.BottomNavigationBar
import com.example.fitlinktrainer.ui.screens.activity.TrainerActivityCenterScreen
import com.example.fitlinktrainer.ui.screens.dashboard.Home
import com.example.fitlinktrainer.ui.screens.dashboard.TrainerProfileScreen
import com.example.fitlinktrainer.ui.screens.messages.MessagesScreen
import com.example.fitlinktrainer.ui.screens.workout.UploadWorkoutScreen

@Composable
fun TrainerMainScreen(trainerId: String) {

    val navController = rememberNavController()

    val items = listOf(
        BottomNavItem("home", "Home", R.drawable.ic_dashboard),
        BottomNavItem("upload", "Upload", R.drawable.ic_upload),
        BottomNavItem("activity", "Activity", R.drawable.ic_trend_up),
        BottomNavItem("messages", "Messages", R.drawable.ic_chat)
    )

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                items = items,
                navController = navController
            )
        }
    ) { padding ->

        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(padding)
        ) {

            composable("home") {
                Home(trainerId = trainerId,onNavigateToProfile = {
                    navController.navigate("trainer_profile/$trainerId")
                })
            }

            composable("upload") {
                UploadWorkoutScreen(
                    trainerId = trainerId,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable("activity") {
                TrainerActivityCenterScreen()
            }
            composable("trainer_profile/{trainerId}") {

                TrainerProfileScreen(
                    trainerId = it.arguments?.getString("trainerId")!!
                )
            }
            composable("messages") {
                MessagesScreen(trainerId)
            }

        }
    }
}