package com.example.fitlink.ui.screens.dashboard

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.fitlink.R
import com.example.fitlink.data.models.*
import com.example.fitlink.ui.screens.notifications.NotificationsScreen
import com.example.fitlink.ui.screens.profile.ProfileScreen
import com.example.fitlink.ui.screens.settings.SettingsScreen
import com.example.fitlink.ui.screens.workouts.WorkoutPlansScreen
import com.example.fitlink.ui.viewmodels.AuthViewModel
import com.example.fitlink.ui.viewmodels.DashboardViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import androidx.navigation.compose.*
import com.example.fitlink.ui.screens.workouts.WorkoutSessionRoute

// ───────────────────────────────── COLORS ─────────────────────────────────
val PurpleGradientStart = Color(0xFF8B5CF6)
val PurpleGradientEnd = Color(0xFF6D28D9)
val LightPurpleBg = Color(0xFFEDE9FE)
val PrimaryGreen = Color(0xFF059669)
val LightGreenAccent = Color(0xFFDCFCE7)
val BackgroundSoft = Color(0xFFFAFAFA)
val TextDark = Color(0xFF1F2937)
val TextMedium = Color(0xFF6B7280)
val TextLight = Color(0xFF9CA3AF)
val CardWhite = Color.White
// Ocean Hydration Colors

// -------- HYDRATION MODERN COLOR SYSTEM --------

val HydrationCardBg = Color(0xFFF7FBFF)

val HydrationWaveStart = Color(0xFF3FA9F5)
val HydrationWaveEnd = Color(0xFF1E88E5)

val HydrationWaveGradient = Brush.verticalGradient(
    listOf(
        Color(0x883FA9F5),
        Color(0x551E88E5)
    )
)

val HydrationGlassFilled = Brush.horizontalGradient(
    listOf(
        Color(0xFF42A5F5),
        Color(0xFF1E88E5)
    )
)

val HydrationGlassEmpty = Color(0xFFEAF3FF)

val HydrationIndicatorGradient = Brush.sweepGradient(
    listOf(
        Color(0xFF42A5F5),
        Color(0xFF1E88E5)
    )
)

val HydrationTextPrimary = Color(0xFF1C2A39)
val HydrationTextSecondary = Color(0xFF7C8A97)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(

userId: String,
onLogout: () -> Unit,
viewModel: DashboardViewModel = hiltViewModel(),
authViewModel: AuthViewModel = hiltViewModel(),
onBottomBarVisibilityChange: (Boolean) -> Unit,

) {

    val scope = rememberCoroutineScope()

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    // Load data when userId changes
    LaunchedEffect(userId) {

        viewModel.loadUserData(userId)
    }

    // Collect ViewModel states
    val user by viewModel.user.collectAsState()
    var onboardingStep by remember { mutableStateOf(0) }
    LaunchedEffect(user) {
        user?.let {
            if (!it.onboardingCompleted && onboardingStep == 0) {
                onboardingStep = 1
            }
        }
    }

    LaunchedEffect(onboardingStep) {

        if (onboardingStep == 0) {
            onBottomBarVisibilityChange(false)
        }

    }
    val dailyStats by viewModel.dailyStats.collectAsState()
    val workouts by viewModel.todayWorkouts.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val weeklyStats by viewModel.weeklyStats.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }

    val navController = rememberNavController()
    val currentRoute =
        navController.currentBackStackEntryAsState().value?.destination?.route
            ?: "dashboard"
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModernDrawerContent(
                user = user,
                currentRoute = currentRoute,
                onLogoutClick = { showLogoutDialog = true },
                onNavigate = { route ->
                    scope.launch { drawerState.close() }
                    navController.navigate(route) {
                        launchSingleTop = true
                    }
                }
            )
        }
    ) {

        Scaffold(
            modifier = Modifier.background(BackgroundSoft),
            containerColor = BackgroundSoft
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "dashboard"
            ) {

                composable("dashboard") {

                    if (isLoading) {
                        DashboardShimmer()
                    } else {
                        DashboardContent(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = innerPadding.calculateBottomPadding()),
                            userName = user?.name ?: "User",
                            user = user,
                            dailyStats = dailyStats,
                            workouts = workouts,
                            chatMessages = chatMessages,
                            weeklyStats = weeklyStats,
                            viewModel = viewModel,
                            onCompleteWorkout = { workoutId, workoutTitle, calories, durationMinutes ->
                                scope.launch {
                                    viewModel.completeWorkout(workoutId, workoutTitle, calories, durationMinutes)
                                }
                            },
                            onMenuClick = { scope.launch { drawerState.open() } },
                            onNotificationClick = {
                                navController.navigate("notifications")
                            },
                            navController = navController
                        )
                    }
                }

                composable("profile") {
                    ProfileScreen(
                        userId = userId,
                        onClose = { scope.launch { drawerState.close() } }
                    )
                }

                composable("settings") {
                    SettingsScreen(
                        userId = userId,
                        onClose = { scope.launch { drawerState.close() } }
                    )
                }
                composable("notifications") {
                    NotificationsScreen(
                        userId = userId,
                        onBack = {
                            navController.popBackStack()
                        }
                    )
                }
                composable("workout/{workoutId}") { backStackEntry ->

                    val workoutId = backStackEntry.arguments?.getString("workoutId") ?: ""

                    WorkoutSessionRoute(
                        workoutId = workoutId,
                        onClose = { navController.popBackStack() }
                    )
                }
            }
        }
        when (onboardingStep) {

            1 -> {
                OnboardingBodyScreen(
                    onNext = { onboardingStep = 2 },
                    viewModel = viewModel,
                    onScreenOpened = { opened ->
                        onBottomBarVisibilityChange(true)
                    }
                )
            }

            2 -> {
                GoalsSetupScreen(
                    onNext = { onboardingStep = 3 },
                    viewModel = viewModel,
                    onScreenOpened = { opened ->
                        onBottomBarVisibilityChange(true)
                    }
                )
            }

            3 -> {
                ReminderSetupScreen(
                    onNext = { onboardingStep = 4 },
                    viewModel = viewModel,
                    onScreenOpened = { opened ->
                        onBottomBarVisibilityChange(true)
                    }
                )
            }

            4 -> {
                WaterSetupScreen(
                    onFinish = {
                        onboardingStep = 0
                        viewModel.finishOnboarding()

                        // ✅ RESTORE BOTTOM BAR
                        onBottomBarVisibilityChange(false)
                    },
                    viewModel = viewModel,
                    onScreenOpened = { opened ->
                        onBottomBarVisibilityChange(true)
                    }
                )
            }
        }
    }

    // Loading overlay

    // Logout confirmation dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        authViewModel.logout()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red
                    )
                ) {
                    Text("Logout")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false }
                ) {
                    Text("Cancel")
                }
            },
            title = {
                Text(
                    "Are you sure?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("You will be logged out from your account.")
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}



// ───────────────────────────────── MAIN DASHBOARD CONTENT ─────────────────────────────────
@Composable
fun DashboardContent(
    modifier: Modifier = Modifier,
    userName: String,
    user: User?,
    dailyStats: DailyStats?,
    workouts: List<Workout>,
    chatMessages: List<ChatMessageModel>,
    weeklyStats: List<DailyStats>,
    viewModel: DashboardViewModel,
    onCompleteWorkout: (String, String,Int,Int) -> Unit,
    onMenuClick: () -> Unit,
    onNotificationClick: () -> Unit,
    navController: NavController
){
    val greeting = remember(Unit) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 0..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
    }
    val activeWorkouts = workouts.filter { it.status != WorkoutStatus.COMPLETED }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            bottom = 80.dp
        )
    ){
        // 1. HEADER (purple gradient)
        item {
            HeaderSection(
                userName = userName,
                user = user,
                greeting = greeting,
                onMenuClick = onMenuClick,
                onNotificationClick = onNotificationClick
            )
        }

        item {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                // 2. STATS CARDS (using real data)
                StatsCards(dailyStats = dailyStats)

                Spacer(modifier = Modifier.height(20.dp))

                // 3. PROGRESS CHART (using weeklyStats from Firestore)
                ProgressChart(weeklyStats = weeklyStats)

                Spacer(modifier = Modifier.height(20.dp))

                // 4. STREAK CARD

                StreakCard(streakDays = user?.streak ?: 0)
                Spacer(modifier = Modifier.height(20.dp))
                AdvancedWaterIntakeCard(
                    stats = dailyStats,
                    onDrink = {

                        dailyStats?.let {

                            val current = it.waterIntake

                            viewModel.updateWaterIntake(current + 1)

                        }

                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 5. RECENT ACTIVITY
                RecentActivityCard(chatMessages = chatMessages)

                Spacer(modifier = Modifier.height(20.dp))

                // 6. TODAY'S WORKOUT (Today's Challenge card)
                if (activeWorkouts.isNotEmpty()) {
                    TodayWorkoutCard(
                        workout = activeWorkouts.first(),
                        onStartWorkout = {
                            navController.navigate("workout/${activeWorkouts.first().id}")
                        },
                        onFinish = { calories, elapsedSeconds ->
                            val workout = activeWorkouts.first()
                            onCompleteWorkout(
                                workout.id,
                                workout.title,
                                calories,
                                elapsedSeconds
                            )
                        }
                    )
                }
            }
        }
    }
}
@Composable
fun ModernDrawerContent(
    user: User?,
    currentRoute: String,
    onLogoutClick: () -> Unit,
    onNavigate: (String) -> Unit
){



    ModalDrawerSheet(
        modifier = Modifier.fillMaxWidth(0.85f),
        drawerContainerColor = Color.White,
        drawerShape = RectangleShape
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {

            // ───────── PROFILE HEADER ─────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(PurpleGradientStart, PurpleGradientEnd)
                        )
                    )
                    .padding(
                        top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                        bottom = 30.dp
                    ),
                contentAlignment = Alignment.Center
            ){

                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                    // Profile Image
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!user?.profileImageUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(user?.profileImageUrl)
                                    .diskCacheKey(user?.profileImageUrl)
                                    .memoryCacheKey(user?.profileImageUrl)
                                    .crossfade(true)
                                    .build(),
                                placeholder = painterResource(R.drawable.ic_user),
                                error = painterResource(R.drawable.ic_user),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.matchParentSize()
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.ic_user),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = user?.name ?: "User",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )

                    Text(
                        text = user?.email ?: "",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ───────── MENU ITEMS ─────────
            DrawerItem(
                title = "Dashboard",
                icon = R.drawable.ic_home,
                isSelected = currentRoute == "dashboard"
            ) {

                onNavigate("dashboard")
            }

            DrawerItem(
                title = "Profile",
                icon = R.drawable.ic_user,
                isSelected = currentRoute == "profile"
            ) {

                onNavigate("profile")
            }

            DrawerItem(
                title = "Settings",
                icon = R.drawable.ic_setting,
                isSelected = currentRoute == "settings"
            ) {

                onNavigate("settings")
            }

            Spacer(modifier = Modifier.weight(1f))

            HorizontalDivider(
                color = Color(0xFFE5E7EB),
                thickness = 1.dp
            )

            // ───────── LOGOUT ─────────
            DrawerItem(
                title = "Logout",
                icon = R.drawable.ic_logout,
                isSelected = false,
                isDestructive = true
            ) {
                onLogoutClick()
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
@Composable
fun DrawerItem(
    title: String,
    icon: Int,
    isSelected: Boolean,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {

    val backgroundColor by animateColorAsState(
        if (isSelected) PurpleGradientStart.copy(alpha = 0.1f)
        else Color.Transparent
    )

    val textColor by animateColorAsState(
        if (isDestructive) Color.Red
        else if (isSelected) PurpleGradientStart
        else TextDark
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(20.dp)
        )

        Spacer(Modifier.width(16.dp))

        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}
// ───────────────────────────────── HEADER SECTION ─────────────────────────────────
@Composable
fun HeaderSection(
    userName: String,
    user: User?,
    greeting: String,
    onMenuClick: () -> Unit,
    onNotificationClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(PurpleGradientStart, PurpleGradientEnd)
                ),
                shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
            )
            .statusBarsPadding()
            .padding(bottom = 32.dp, start = 20.dp, end = 20.dp)
    ) {
        Column {
            // Top bar with menu and notification icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onMenuClick()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_menu),
                        contentDescription = "Menu",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onNotificationClick,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_bell),
                        contentDescription = "Notifications",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Profile and greeting
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile image
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(LightPurpleBg)
                        .border(3.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                ) {
                    if (!user?.profileImageUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(user?.profileImageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Profile",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.ic_user),
                            contentDescription = null,
                            tint = PurpleGradientStart,
                            modifier = Modifier
                                .size(40.dp)
                                .align(Alignment.Center)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "$greeting,",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                    Text(
                        text = userName,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Motivational text
            Text(
                text = "Ready to crush today's goals? Let's make it happen! 💪",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp
            )
        }
    }
}

// ───────────────────────────────── STATS CARDS ─────────────────────────────────
@Composable
fun StatsCards(dailyStats: DailyStats?) {
    val caloriesBurned = dailyStats?.caloriesBurned ?: 0
    val caloriesGoal = dailyStats?.caloriesGoal ?: 2000
    val caloriesProgress = (caloriesBurned.toFloat() / caloriesGoal).coerceIn(0f, 1f)

    val activeMinutes = dailyStats?.activeMinutes ?: 0
    val activeGoal = 60
    val activeProgress = (activeMinutes.toFloat() / activeGoal).coerceIn(0f, 1f)

    val waterIntake = dailyStats?.waterIntake ?: 0
    val waterGoal = 8
    val waterProgress = (waterIntake.toFloat() / waterGoal).coerceIn(0f, 1f)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            icon = R.drawable.ic_flame,
            label = "Calories",
            value = caloriesBurned.toString(),
            progress = caloriesProgress,
            colorGradient = listOf(PurpleGradientStart, PurpleGradientEnd)
        )
        StatCard(
            modifier = Modifier.weight(1f),
            icon = R.drawable.ic_target,
            label = "Active",
            value = "${activeMinutes} min",
            progress = activeProgress,
            colorGradient = listOf(Color(0xFFEC4899), Color(0xFFF43F5E))
        )
        StatCard(
            modifier = Modifier.weight(1f),
            icon = R.drawable.ic_water,
            label = "Water",
            value = "${waterIntake} glasses",
            progress = waterProgress,
            colorGradient = listOf(Color(0xFF3B82F6), Color(0xFF06B6D4))
        )
    }
}

@Composable
fun StatCard(
    icon: Int,
    label: String,
    value: String,
    progress: Float,
    colorGradient: List<Color>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(2.dp),

    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(brush = Brush.horizontalGradient(colors = colorGradient)),
                contentAlignment = Alignment.Center
            ) {
                Icon(painter = painterResource(icon), contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = label, fontSize = 12.sp, color = TextMedium)
            Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(LightGreenAccent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(brush = Brush.horizontalGradient(colors = colorGradient))
                )
            }
        }
    }
}

// ───────────────────────────────── PROGRESS CHART ─────────────────────────────────
@Composable
fun ProgressChart(weeklyStats: List<DailyStats>) {
    var selectedIndex by remember { mutableStateOf(-1) }
    val calendar = Calendar.getInstance()
    val todayStart = with(calendar) {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        timeInMillis
    }
    val statsMap = weeklyStats.associateBy {
        val cal = Calendar.getInstance()
        cal.timeInMillis = it.date
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }
    val dayCalories = mutableListOf<Int>()
    for (i in 6 downTo 0) {
        val date = todayStart - i * 86400000L
        dayCalories.add(statsMap[date]?.caloriesBurned ?: 0)
    }
    val maxValue = dayCalories.maxOrNull()?.toFloat()?.coerceAtLeast(1f) ?: 1f

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = "Weekly Progress", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Text(text = "Your calorie burn this week", fontSize = 12.sp, color = TextMedium, modifier = Modifier.padding(bottom = 16.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->

                            val widthPerDay = size.width / dayCalories.size
                            selectedIndex = (offset.x / widthPerDay).toInt()

                        }
                    }
            ) {

                val width = size.width; val height = size.height; val padding = 20.dp.toPx()
                val graphWidth = width - 2 * padding; val graphHeight = height - 2 * padding
                val pointWidth = graphWidth / dayCalories.size
                for (i in 0..4) {
                    val y = padding + (graphHeight / 4) * i
                    drawLine(color = LightGreenAccent, start = Offset(padding, y), end = Offset(width - padding, y), strokeWidth = 1.dp.toPx())
                }
                val stepValue = maxValue / 4

                for (i in 0..4) {

                    val y = padding + (graphHeight / 4) * i

                    drawContext.canvas.nativeCanvas.drawText(
                        "${(maxValue - stepValue * i).toInt()}",
                        0f,
                        y + 10,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.GRAY
                            textSize = 28f
                        }
                    )

                }
                val path = androidx.compose.ui.graphics.Path()
                for (i in dayCalories.indices) {
                    val x = padding + i * pointWidth
                    val y = padding + graphHeight * (1 - (dayCalories[i] / maxValue))
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.lineTo(padding + (dayCalories.size - 1) * pointWidth, padding + graphHeight)
                path.lineTo(padding, padding + graphHeight); path.close()
                drawPath(path = path, brush = Brush.verticalGradient(colors = listOf(PurpleGradientStart.copy(alpha = 0.3f), Color.Transparent), startY = padding, endY = padding + graphHeight))
                for (i in 0 until dayCalories.size - 1) {
                    val startX = padding + i * pointWidth
                    val startY = padding + graphHeight * (1 - (dayCalories[i] / maxValue))
                    val endX = padding + (i + 1) * pointWidth
                    val endY = padding + graphHeight * (1 - (dayCalories[i + 1] / maxValue))
                    drawLine(color = PurpleGradientStart, start = Offset(startX, startY), end = Offset(endX, endY), strokeWidth = 3.dp.toPx())
                }
                for (i in dayCalories.indices) {
                    val x = padding + i * pointWidth
                    val y = padding + graphHeight * (1 - (dayCalories[i] / maxValue))
                    drawCircle(
                        color = if (i == selectedIndex) Color.Red else PurpleGradientStart,
                        radius = if (i == selectedIndex) 7.dp.toPx() else 4.dp.toPx(),
                        center = Offset(x, y)
                    )
                    drawCircle(color = Color.White, radius = 2.dp.toPx(), center = Offset(x, y))
                }
            }
            if (selectedIndex in dayCalories.indices) {

                val value = dayCalories[selectedIndex]

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Black),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 6.dp)
                ) {

                    Text(
                        "$value kcal",
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )

                }

            }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                val dayLabels = (6 downTo 0).map { i ->
                    val cal = Calendar.getInstance()
                    cal.add(Calendar.DAY_OF_YEAR, -i)
                    SimpleDateFormat("EEE", Locale.getDefault()).format(cal.time)
                }


                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    dayLabels.forEach { day ->
                        Text(
                            text = day,
                            fontSize = 11.sp,
                            color = TextLight
                        )
                    }
                }
            }
        }
    }
}

// ───────────────────────────────── STREAK CARD ─────────────────────────────────
// File: com/example/fitlink/ui/screens/dashboard/DashboardScreen.kt (StreakCard)
@Composable
fun AdvancedWaterIntakeCard(
    stats: DailyStats?,
    onDrink: () -> Unit
) {

    val intakeRaw = stats?.waterIntake ?: 0
    val goalRaw = stats?.waterGoal ?: 8

    val goal = goalRaw.coerceAtLeast(1)
    val intake = intakeRaw.coerceAtMost(goal)

    val progress = (intake.toFloat() / goal).coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(800),
        label = ""
    )

    val infiniteTransition = rememberInfiniteTransition()

    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing)
        ),
        label = ""
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(26.dp)),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = HydrationCardBg)
    ) {

        Box {

            Column(
                modifier = Modifier.padding(20.dp)
            ) {

                // HEADER
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        painter = painterResource(R.drawable.ic_water),
                        contentDescription = null,
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        "Hydration",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                }

                Spacer(Modifier.height(42.dp))

                // WATER WAVE CANVAS
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(HydrationGlassEmpty)
                ) {

                    Canvas(modifier = Modifier.fillMaxSize()) {

                        val waveHeight = size.height * (1f - animatedProgress)

                        val path = Path()

                        path.moveTo(0f, waveHeight)

                        for (i in 0..size.width.toInt()) {

                            val y = waveHeight +
                                    12 * kotlin.math.sin(
                                ((i + waveOffset) * Math.PI / 180)
                            ).toFloat()

                            path.lineTo(i.toFloat(), y)
                        }

                        path.lineTo(size.width, size.height)
                        path.lineTo(0f, size.height)
                        path.close()

                        drawPath(
                            path = path,
                            brush = HydrationWaveGradient

                        )
                    }

                    // center text
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Text(
                            "$intake / $goal",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )

                        Text(
                            "glasses",
                            fontSize = 12.sp,
                            color = CardWhite
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                // GLASS INDICATORS


                // ACTION AREA
                if (intake < goal) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                brush = Brush.horizontalGradient(
                                    listOf(PurpleGradientStart, PurpleGradientEnd)
                                )
                            )
                    ) {

                        Button(
                            onClick = onDrink,
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(0.dp)
                        ) {

                            Icon(
                                painter = painterResource(R.drawable.ic_add),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )

                            Spacer(Modifier.width(8.dp))

                            Text(
                                "Add Glass",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                } else {

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = LightGreenAccent
                    ) {

                        Box(contentAlignment = Alignment.Center) {

                            Text(
                                "Completed Today 🎉",
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreen
                            )
                        }
                    }
                }
            }



        }
    }
}
@Composable
fun StreakCard(streakDays: Int) {
    val goal = 15
    val progress = (streakDays.toFloat() / goal).coerceIn(0f, 1f)
    val remaining = (goal - streakDays).coerceAtLeast(0)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(brush = Brush.horizontalGradient(listOf(Color(0xFFF59E0B), Color(0xFFEF4444)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_flame),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Current Streak",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Text(text = " 🔥", fontSize = 16.sp)
                    }
                    Text(
                        text = if (remaining > 0)
                            "$remaining more days to reach your goal!"
                        else
                            "Goal achieved! 🎉",
                        fontSize = 12.sp,
                        color = TextMedium
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = streakDays.toString(),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = PurpleGradientStart
                    )
                    Text(text = "Days", fontSize = 12.sp, color = TextMedium)
                }
            }

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(LightGreenAccent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                brush = Brush.horizontalGradient(
                                    listOf(PurpleGradientStart, PurpleGradientEnd)
                                )
                            )
                    )
                }
            }
        }
    }
}
@Composable
fun DashboardShimmer() {

    val shimmerColors = listOf(
        LightPurpleBg.copy(alpha = 0.4f),
        CardWhite,
        LightPurpleBg.copy(alpha = 0.4f)
    )

    val transition = rememberInfiniteTransition()
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing)
        )
    )

    LazyColumn(
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {

        // HEADER SHIMMER
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(
                                PurpleGradientStart.copy(alpha = 0.6f),
                                PurpleGradientEnd.copy(alpha = 0.6f)
                            )
                        )
                    )
            )
        }

        item {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {

                Spacer(modifier = Modifier.height(20.dp))

                // STATS ROW SHIMMER
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(3) {
                        ShimmerCardBox(
                            modifier = Modifier
                                .weight(1f)
                                .height(110.dp),
                            translateX = translateAnim.value,
                            colors = shimmerColors
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // CHART SHIMMER
                ShimmerCardBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    translateX = translateAnim.value,
                    colors = shimmerColors
                )

                Spacer(modifier = Modifier.height(20.dp))

                // STREAK SHIMMER
                ShimmerCardBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    translateX = translateAnim.value,
                    colors = shimmerColors
                )

                Spacer(modifier = Modifier.height(20.dp))

                // RECENT ACTIVITY SHIMMER
                ShimmerCardBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    translateX = translateAnim.value,
                    colors = shimmerColors
                )

                Spacer(modifier = Modifier.height(20.dp))

                // TODAY WORKOUT SHIMMER
                ShimmerCardBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    translateX = translateAnim.value,
                    colors = shimmerColors
                )
            }
        }
    }
}@Composable
fun ShimmerCardBox(
    modifier: Modifier,
    translateX: Float,
    colors: List<Color>
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = colors,
                        startX = translateX,
                        endX = translateX + 400f
                    )
                )
        )
    }
}
// ───────────────────────────────── RECENT ACTIVITY CARD ─────────────────────────────────
@Composable
fun RecentActivityCard(chatMessages: List<ChatMessageModel>) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = "Recent Activity", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Text(text = "Your latest workouts", fontSize = 12.sp, color = TextMedium, modifier = Modifier.padding(bottom = 12.dp))

            if (chatMessages.isEmpty()) {
                Text(text = "No recent activity", fontSize = 14.sp, color = TextMedium, modifier = Modifier.padding(vertical = 16.dp))
            } else {
                chatMessages.take(3).forEachIndexed { index, message ->
                    ActivityItem(message = message)
                    if (index < chatMessages.take(3).size - 1) Divider(color = LightGreenAccent, thickness = 1.dp)
                }
            }
        }
    }
}

@Composable
fun ActivityItem(message: ChatMessageModel) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable(
        interactionSource = remember { MutableInteractionSource() }, indication = null
    ) { /* navigate */ }, verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(brush = Brush.horizontalGradient(listOf(PurpleGradientStart, PurpleGradientEnd))),
            contentAlignment = Alignment.Center) {
            Icon(painter = painterResource(
                when (message.type) {
                    MessageType.ACHIEVEMENT -> R.drawable.ic_award
                    MessageType.WORKOUT_COMPLETE -> R.drawable.ic_dumbbell
                    MessageType.STREAK_ALERT -> R.drawable.ic_trending_up
                    else -> R.drawable.ic_clock
                }
            ), contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = message.message, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextDark, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = formatTimeAgo(message.timestamp), fontSize = 12.sp, color = TextMedium)
        }
        if (!message.read) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(PrimaryGreen))
        }
    }
}

// ───────────────────────────────── TODAY'S CHALLENGE CARD (exactly as screenshot) ─────────────────────────────────
// ───────────────────────────────── TODAY'S CHALLENGE CARD (with timer) ─────────────────────────────────
// ───────────────────────────────── TODAY'S CHALLENGE CARD (with timer & local hide) ─────────────────────────────────
@Composable
fun TodayWorkoutCard(
    workout: Workout,
    onStartWorkout: () -> Unit,
    onFinish: (Int, Int) -> Unit
){
    var isRunning by remember { mutableStateOf(false) }
    var seconds by remember { mutableStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var isLocallyCompleted by remember { mutableStateOf(false) }

    // Hide if already completed from data or locally
    if (workout.status == WorkoutStatus.COMPLETED || isLocallyCompleted) {
        return
    }

    // Timer tick
    LaunchedEffect(isRunning, isPaused) {
        while (isRunning && !isPaused) {
            delay(1000)
            seconds++
        }
    }

    val minutes = seconds / 60
    val remainingSeconds = seconds % 60

    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .background(brush = Brush.horizontalGradient(listOf(PurpleGradientStart, PurpleGradientEnd)))
                .padding(20.dp)
        ) {
            Column {
                // Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(30.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Today's Challenge",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Title
                Text(
                    text = workout.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // Description
                Text(
                    text = workout.description.ifEmpty { "High intensity interval training to boost your metabolism" },
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Duration & Calories row
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_clock),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Duration",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "${workout.durationMinutes} min",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_flame),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Calories",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "${workout.caloriesBurnEstimate} kcal",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (!isRunning && seconds == 0) {
                    // Start button
                    Button(
                        onClick = {
                            onStartWorkout()},
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = PurpleGradientStart
                        ),
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_play),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Start Workout", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    // Timer display
                    Text(
                        text = String.format("%02d:%02d", minutes, remainingSeconds),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Pause/Resume and Finish buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { isPaused = !isPaused },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = PurpleGradientStart
                            ),
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            Text(
                                text = if (isPaused) "Resume" else "Pause",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                val workoutSecondsTotal = workout.durationMinutes * 60
                                val estimatedCalories = if (workoutSecondsTotal > 0) {
                                    ((workout.caloriesBurnEstimate.toFloat() * seconds) / workoutSecondsTotal).roundToInt()
                                } else {
                                    workout.caloriesBurnEstimate
                                }

                                // Stop timer
                                isRunning = false
                                isPaused = false

                                // Show success dialog
                                showDialog = true

                                // Update Firestore via callback – now passing both values
                                onFinish(estimatedCalories, seconds)

                                // Reset timer
                                seconds = 0

                                // Hide card immediately
                                isLocallyCompleted = true
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = PurpleGradientStart
                            ),
                            interactionSource = remember { MutableInteractionSource() },
                        ) {
                            Text(text = "Finish", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Success dialog
            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text("Workout Completed 🎉") },
                    text = { Text("Great job! Calories have been added to your daily progress.") },
                    confirmButton = {
                        TextButton(
                            onClick = { showDialog = false },
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            Text("OK", color = PurpleGradientStart)
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = CardWhite
                )
            }
        }
    }
}

// ───────────────────────────────── HELPER ─────────────────────────────────
fun formatTimeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60000 -> "Just now"
        diff < 3600000 -> "${diff / 60000}m ago"
        diff < 86400000 -> "${diff / 3600000}h ago"
        diff < 604800000 -> "${diff / 86400000}d ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}