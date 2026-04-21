package com.example.fitlinktrainer.navigation

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.fitlinktrainer.ui.screens.auth.PendingApprovalScreen
import com.example.fitlinktrainer.ui.screens.auth.TrainerLoginScreen
import com.example.fitlinktrainer.ui.screens.auth.TrainerRegisterScreen
import com.example.fitlinktrainer.ui.screens.components.LoadingScreen
import com.example.fitlinktrainer.ui.screens.dashboard.AdminDashboardScreen


import com.example.fitlinktrainer.viewmodel.TrainerAuthState
import com.example.fitlinktrainer.viewmodel.TrainerAuthViewModel

@Composable
fun TrainerNavGraph() {

    val navController = rememberNavController()
    val authViewModel: TrainerAuthViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsState()

    /**
     * Decide navigation AFTER auth state loads
     */
    LaunchedEffect(authState) {

        when (authState) {

            is TrainerAuthState.Verified -> {

                val trainer =
                    (authState as TrainerAuthState.Verified).trainer

                if (trainer.email == "admin@fitlink.com") {

                    navController.navigate("admin_dashboard") {
                        popUpTo("loading") { inclusive = true }
                    }

                } else {

                    navController.navigate("dashboard/${trainer.id}") {
                        popUpTo("loading") { inclusive = true }
                    }
                }
            }

            is TrainerAuthState.Unverified -> {

                navController.navigate("pending") {
                    popUpTo("loading") { inclusive = true }
                }
            }

            is TrainerAuthState.Unauthenticated -> {

                navController.navigate("auth") {
                    popUpTo("loading") { inclusive = true }
                }
            }

            else -> {}
        }
    }

    NavHost(
        navController = navController,
        startDestination = "loading"
    ) {

        /**
         * LOADING SCREEN
         */
        composable("loading") {
            LoadingScreen()
        }

        /**
         * LOGIN
         */
        composable("auth") {

            TrainerLoginScreen(
                onNavigateToRegister = {
                    navController.navigate("register")
                },
                onLoginSuccess = {},
                viewModel = authViewModel
            )
        }

        /**
         * REGISTER
         */
        composable("register") {

            TrainerRegisterScreen(
                onBack = { navController.popBackStack() },
                onRegistrationSuccess = {
                    navController.navigate("auth") {
                        popUpTo("register") { inclusive = true }
                    }
                }
            )
        }

        /**
         * PENDING APPROVAL
         */
        composable("pending") {

            PendingApprovalScreen(
                onLogout = {

                    authViewModel.logout()

                    navController.navigate("auth") {
                        popUpTo("pending") { inclusive = true }
                    }
                }
            )
        }

        /**
         * TRAINER DASHBOARD
         */
        composable(
            route = "dashboard/{trainerId}",
            arguments = listOf(
                navArgument("trainerId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->

            val trainerId =
                backStackEntry.arguments?.getString("trainerId") ?: ""

            TrainerMainScreen(trainerId = trainerId)
        }

        /**
         * ADMIN DASHBOARD
         */
        composable("admin_dashboard") {

            AdminDashboardScreen(
                onLogout = {

                    authViewModel.logout()

                    navController.navigate("auth") {
                        popUpTo("admin_dashboard") { inclusive = true }
                    }
                }
            )
        }
    }
}