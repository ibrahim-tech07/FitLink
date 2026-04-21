package com.example.fitlink

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.fitlink.data.models.User
import com.example.fitlink.ui.components.BottomNavigationBar
import com.example.fitlink.ui.screens.*
import com.example.fitlink.ui.screens.auth.LoginScreen
import com.example.fitlink.ui.screens.auth.RegisterScreen
import com.example.fitlink.ui.screens.calendar.CalendarScreen
import com.example.fitlink.ui.screens.chat.ChatScreen
import com.example.fitlink.ui.screens.dashboard.DashboardScreen
import com.example.fitlink.ui.screens.notifications.NotificationsScreen
import com.example.fitlink.ui.screens.trainers.TrainersScreen
import com.example.fitlink.ui.screens.workouts.WorkoutPlansScreen
import com.example.fitlink.ui.screens.workouts.WorkoutSessionRoute
import com.example.fitlink.ui.theme.FitLinkTheme
import com.example.fitlink.ui.viewmodels.AuthState
import com.example.fitlink.ui.viewmodels.AuthViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestPermission =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                Log.d("Notification", "Permission granted")
            } else {
                Log.d("Notification", "Permission denied")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        requestNotificationPermission()
        FirebaseMessaging.getInstance().token.addOnSuccessListener {
            Log.d("FCM_DEBUG", "Current token: $it")
        }
        setContent {
            FitLinkTheme {
                FitLinkApp()
            }
        }
    }

    /**
     * Request notification permission for Android 13+
     */
    private fun requestNotificationPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                requestPermission.launch(
                    Manifest.permission.POST_NOTIFICATIONS
                )

            }

        }
    }
}

@Composable
fun FitLinkApp(
    authViewModel: AuthViewModel = hiltViewModel()
) {

    val authState by authViewModel.authState.collectAsState()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val context = LocalContext.current

    /**
     * After login
     * Save FCM token to Firestore
     */

    LaunchedEffect(isLoggedIn, currentUser) {

        if (isLoggedIn && currentUser != null) {

            authViewModel.afterLogin(context)

            // Ensure FCM is enabled
            FirebaseMessaging.getInstance().isAutoInitEnabled = true

            // Delete old token to avoid stale tokens
            FirebaseMessaging.getInstance().deleteToken()

            // Fetch new token
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token ->

                    Log.d("FCM_DEBUG", "Device Token: $token")

                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(currentUser!!.id)
                        .set(
                            mapOf("fcmToken" to token),
                            SetOptions.merge()
                        )
                        .addOnSuccessListener {
                            Log.d("FCM_DEBUG", "Token saved to Firestore")
                        }
                        .addOnFailureListener {
                            Log.e("FCM_DEBUG", "Failed to save token", it)
                        }

                }
                .addOnFailureListener {
                    Log.e("FCM_DEBUG", "Token fetch failed", it)
                }
        }
    }

    when (authState) {

        is AuthState.Loading -> {

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {

                CircularProgressIndicator(
                    color = Color(0xFF059669)
                )

            }

        }

        else -> {

            if (isLoggedIn && currentUser != null) {

                MainApp(
                    currentUser = currentUser!!,
                    authViewModel = authViewModel,
                    onLogout = { authViewModel.logout() }
                )

            } else {

                AuthNavigation(
                    authViewModel = authViewModel,
                    authState = authState
                )

            }

        }
    }
}

@Composable
fun AuthNavigation(
    authViewModel: AuthViewModel,
    authState: AuthState
) {

    var currentAuthPage by remember { mutableStateOf("login") }

    LaunchedEffect(authState) {
        if (authState is AuthState.Error) {
            println("Auth error: ${authState.message}")
        }
    }

    when (currentAuthPage) {

        "login" -> LoginScreen(
            onNavigate = { page -> currentAuthPage = page },
            onLoginClick = { email, password ->
                authViewModel.login(email, password)
            },
            onGoogleLoginClick = { idToken ->
                authViewModel.loginWithGoogle(idToken)
            },
            onForgotPasswordClick = { email ->
                authViewModel.resetPassword(email)
            },
            isLoading = authState is AuthState.Loading,
            errorMessage =
                if (authState is AuthState.Error)
                    authState.message
                else null
        )

        "register" -> RegisterScreen(
            onNavigate = { page -> currentAuthPage = page },
            onRegisterClick = { email, password, name, phone ->
                authViewModel.register(email, password, name, phone)
            },
            isLoading = authState is AuthState.Loading,
            errorMessage =
                if (authState is AuthState.Error)
                    authState.message
                else null
        )
    }
}

@Composable
fun MainApp(
    currentUser: User,
    authViewModel: AuthViewModel,
    onLogout: () -> Unit
) {

    var currentPage by remember { mutableStateOf("dashboard") }
    var selectedWorkoutId by remember { mutableStateOf<String?>(null) }
    var hideBottomBar by remember { mutableStateOf(false) }

    Scaffold(

        bottomBar = {

            val mainScreens = listOf(
                "dashboard",
                "workouts",
                "trainers",
                "calendar",
                "chat"
            )

            if (currentPage in mainScreens && !hideBottomBar) {

                BottomNavigationBar(
                    currentPage = currentPage,
                    onTabClick = { page ->
                        currentPage = page
                    }
                )

            }

        }

    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            when (currentPage) {

                "dashboard" -> DashboardScreen(
                    userId = currentUser.id,
                    onLogout = onLogout,
                    onBottomBarVisibilityChange = { hide ->
                        hideBottomBar = hide
                    }
                )

                "trainers" ->
                    TrainersScreen(userId = currentUser.id)

                "workouts" ->
                    WorkoutPlansScreen(
                        userId = currentUser.id,
                        onClose = { currentPage = "dashboard" },
                        onStartWorkout = { workoutId ->
                            selectedWorkoutId = workoutId
                            currentPage = "workout_session"
                        }
                    )

                "workout_session" -> {

                    selectedWorkoutId?.let { workoutId ->

                        WorkoutSessionRoute(
                            workoutId = workoutId,
                            onClose = {
                                currentPage = "workouts"
                                selectedWorkoutId = null
                            }
                        )

                    }

                }

                "calendar" ->
                    CalendarScreen(userId = currentUser.id)

                "chat" ->
                    ChatScreen(
                        userId = currentUser.id,
                        onChatOpened = { opened ->
                            hideBottomBar = opened
                        }
                    )

                "notifications" ->
                    NotificationsScreen(
                        userId = currentUser.id,
                        onBack = {
                            currentPage = "dashboard"
                        }
                    )
            }
        }
    }
}