package com.example.fitlink.ui.screens.workouts

import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.fitlink.R
import com.example.fitlink.data.models.Workout
import com.example.fitlink.data.models.WorkoutStatus
import com.example.fitlink.ui.screens.dashboard.*
import com.example.fitlink.ui.viewmodels.WorkoutViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutPlansScreen(
    userId: String,
    onClose: () -> Unit,
    onStartWorkout: (String) -> Unit,
    viewModel: WorkoutViewModel = hiltViewModel()
) {
    val workouts by viewModel.filteredWorkouts.collectAsState()
    val selectedDifficulty by viewModel.selectedDifficulty.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val stats by viewModel.workoutStats.collectAsState()

    val lazyListState = rememberLazyListState()

    LaunchedEffect(userId) {
        if (userId.isNotBlank()) {
            viewModel.initialize(userId)
        }
    }

    Scaffold(
        containerColor = BackgroundSoft,
        topBar = {
            TopAppBar(
                title = {
                    Column {

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Workouts",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextDark
                            )


                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Push your limits today 💪",
                            fontSize = 13.sp,
                            color = TextMedium
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundSoft
                )
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 20.dp)
                .fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 🔥 Premium Stats Card
            PremiumStatsCard(stats = stats)

            Spacer(modifier = Modifier.height(24.dp))

            // 🔥 Filter Row
            FilterChipsRow(
                selectedDifficulty = selectedDifficulty,
                onDifficultySelected = { viewModel.filterByDifficulty(it) }
            )

            Spacer(modifier = Modifier.height(20.dp))

            when {
                isLoading -> LoadingShimmer()
                error != null -> ErrorMessage(error!!, { viewModel.loadAllWorkouts() })
                workouts.isEmpty() -> EmptyWorkoutsMessage()
                else -> {
                    LazyColumn(
                        state = lazyListState,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(
                            top = 8.dp,
                            bottom = 120.dp
                        )
                    ){
                        items(workouts, key = { it.id }) { workout ->
                            ProWorkoutCard(
                                workout = workout,
                                onStart = {
                                    viewModel.startWorkout(workout.id)
                                    onStartWorkout(workout.id)
                                },
                                onComplete = {
                                    viewModel.completeWorkout(workout.id, workout.title, workout.caloriesBurnEstimate, workout.durationMinutes)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== PREMIUM STATS CARD ====================
@Composable
fun PremiumStatsCard(stats: Map<String, Int>) {
    ElevatedCard(
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 22.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            PremiumStatItem(
                icon = R.drawable.ic_dumbbell,
                value = stats["total"]?.toString() ?: "0",
                label = "Total"
            )
            DividerVertical()
            PremiumStatItem(
                icon = R.drawable.ic_check,
                value = stats["completed"]?.toString() ?: "0",
                label = "Done"
            )
            DividerVertical()
            PremiumStatItem(
                icon = R.drawable.ic_flame,
                value = stats["totalCalories"]?.toString() ?: "0",
                label = "Calories"
            )
        }
    }
}

@Composable
fun PremiumStatItem(icon: Int, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = LightPurpleBg,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = PurpleGradientStart,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextMedium
        )
    }
}

@Composable
fun DividerVertical() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(36.dp)
            .background(LightPurpleBg)
    )
}

// ==================== FILTER CHIPS ====================
@Composable
fun FilterChipsRow(
    selectedDifficulty: String,
    onDifficultySelected: (String) -> Unit
) {
    val difficulties = listOf("All", "Beginner", "Intermediate", "Advanced")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        difficulties.forEach { difficulty ->
            val isSelected = selectedDifficulty == difficulty
            val backgroundColor by animateColorAsState(
                targetValue = if (isSelected) PurpleGradientStart else CardWhite,
                animationSpec = tween(300)
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else TextDark,
                animationSpec = tween(300)
            )

            Surface(
                modifier = Modifier
                    .wrapContentWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null   // No ripple on filter chips
                    ) { onDifficultySelected(difficulty) },
                color = backgroundColor,
                shadowElevation = if (isSelected) 4.dp else 0.dp,
                tonalElevation = if (isSelected) 4.dp else 0.dp
            ) {
                Text(
                    text = difficulty,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = textColor,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}

// ==================== PRO WORKOUT CARD ====================
@Composable
fun ProWorkoutCard(
    workout: Workout,
    onStart: () -> Unit,
    onComplete: () -> Unit
) {
    val isCompleted = workout.status == WorkoutStatus.COMPLETED
    val isInProgress = workout.status == WorkoutStatus.IN_PROGRESS
    val isMissed = workout.status == WorkoutStatus.MISSED

    val statusAccentColor = when (workout.status) {
        WorkoutStatus.COMPLETED -> PrimaryGreen
        WorkoutStatus.IN_PROGRESS -> Color(0xFFD97706)
        WorkoutStatus.MISSED -> Color.Red
        else -> PurpleGradientStart
    }

    val elevation by animateDpAsState(
        targetValue = if (isInProgress) 12.dp else 8.dp,
        animationSpec = tween(200),
        label = "cardElevation"
    )


    Card(
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = elevation,
                shape = RoundedCornerShape(30.dp),
                clip = false
            )
    ){
        Box {
            // Status Accent Strip
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(statusAccentColor)
                    .align(Alignment.CenterStart)
            )

            Column(
                modifier = Modifier
                    .padding(start = 18.dp, end = 20.dp, top = 20.dp, bottom = 20.dp)
            ) {
                // Header: Title + Badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = workout.title,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (workout.trainerId != null) "Trainer Guided" else "Self Workout",
                            fontSize = 12.sp,
                            color = TextMedium
                        )
                    }
                    StatusBadge(status = workout.status)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Description
                Text(
                    text = workout.description,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = TextMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    WorkoutStatItem(
                        icon = R.drawable.ic_clock,
                        value = "${workout.durationMinutes} min"
                    )
                    WorkoutStatItem(
                        icon = R.drawable.ic_flame,
                        value = "${workout.caloriesBurnEstimate} kcal"
                    )
                    WorkoutStatItem(
                        icon = R.drawable.ic_dumbbell,
                        value = "${workout.exercises.size} exercises"
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                if (isInProgress) {

                    Spacer(modifier = Modifier.height(6.dp))

                    val progress = if (workout.exercises.isNotEmpty())
                        workout.progress.toFloat() / workout.exercises.size
                    else 0f

                    Column {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {

                            Text(
                                text = "Workout Progress",
                                fontSize = 13.sp,
                                color = TextMedium
                            )

                            Text(
                                text = "${workout.progress} / ${workout.exercises.size}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextDark
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            color = PrimaryGreen,
                            trackColor = LightPurpleBg
                        )
                    }
                }
                Spacer(modifier = Modifier.height(22.dp))
                // Bottom Row: Difficulty + Action Button
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = getDifficultyBadgeColor(workout.difficulty),
                        tonalElevation = 2.dp
                    ) {
                        Text(
                            text = workout.difficulty,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = getDifficultyTextColor(workout.difficulty),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = {

                            when (workout.status) {

                                WorkoutStatus.SCHEDULED -> {
                                    onStart()
                                }

                                WorkoutStatus.IN_PROGRESS -> {
                                    // resume workout
                                    onStart()
                                }

                                else -> {}
                            }
                        },
                        enabled =
                            workout.status != WorkoutStatus.COMPLETED &&
                                    workout.status != WorkoutStatus.MISSED &&
                                    workout.scheduledDate <= System.currentTimeMillis(),
                        shape = RoundedCornerShape(22.dp),
                        elevation = ButtonDefaults.buttonElevation(4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when {
                                isCompleted -> LightGreenAccent
                                isInProgress -> PrimaryGreen
                                else -> PurpleGradientStart
                            },
                            contentColor = when {
                                isCompleted -> PrimaryGreen
                                else -> Color.White
                            },
                            disabledContainerColor = LightGreenAccent.copy(alpha = 0.4f),
                            disabledContentColor = PrimaryGreen.copy(alpha = 0.7f)
                        ),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text(
                            text = when {
                                isCompleted -> "Completed"
                                isMissed -> "Missed"
                                isInProgress -> "Resume Workout"
                                else -> "Start Workout"
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    // Subtle elevation change on press (micro‑interaction)

}

@Composable
fun WorkoutStatItem(icon: Int, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = LightPurpleBg,
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = PurpleGradientStart,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = TextDark
        )
    }
}

@Composable
fun StatusBadge(status: WorkoutStatus) {
    val (backgroundColor, textColor, text) = when (status) {
        WorkoutStatus.COMPLETED -> Triple(LightGreenAccent, PrimaryGreen, "Completed")
        WorkoutStatus.IN_PROGRESS -> Triple(Color(0xFFFEF3C7), Color(0xFFD97706), "In Progress")
        WorkoutStatus.MISSED -> Triple(Color(0xFFFEE2E2), Color.Red, "Missed")
        else -> Triple(LightPurpleBg, PurpleGradientStart, "Scheduled")
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

// ==================== LOADING SHIMMER ====================
@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun LoadingShimmer() {
    val shimmerColors = listOf(
        LightPurpleBg.copy(alpha = 0.3f),
        CardWhite,
        LightPurpleBg.copy(alpha = 0.3f)
    )

    val transition = rememberInfiniteTransition()
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing)
        )
    )

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(
            top = 12.dp,
            bottom = 120.dp,
            start = 4.dp,
            end = 4.dp
        )
    ) {
        items(4) {
            ShimmerCard(translateAnim.value, shimmerColors)
        }
    }
}

@Composable
fun ShimmerCard(translateX: Float, colors: List<Color>) {
    Card(
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(start = 18.dp, end = 20.dp, top = 20.dp, bottom = 20.dp)) {
            // Fake status strip
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(PurpleGradientStart.copy(alpha = 0.3f))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                // Title shimmer
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(20.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = colors,
                                startX = translateX,
                                endX = translateX + 300f
                            )
                        )
                )
                Spacer(modifier = Modifier.height(6.dp))
                // Subtitle shimmer
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = colors,
                                startX = translateX,
                                endX = translateX + 300f
                            )
                        )
                )
                Spacer(modifier = Modifier.height(14.dp))
                // Description shimmer (2 lines)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = colors,
                                startX = translateX,
                                endX = translateX + 300f
                            )
                        )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = colors,
                                startX = translateX,
                                endX = translateX + 300f
                            )
                        )
                )
                Spacer(modifier = Modifier.height(18.dp))
                // Stats row shimmer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    repeat(3) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = colors,
                                            startX = translateX,
                                            endX = translateX + 300f
                                        )
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(40.dp, 16.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = colors,
                                            startX = translateX,
                                            endX = translateX + 300f
                                        )
                                    )
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(22.dp))
                // Bottom row shimmer
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(70.dp, 28.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = colors,
                                    startX = translateX,
                                    endX = translateX + 300f
                                )
                            )
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .size(100.dp, 48.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = colors,
                                    startX = translateX,
                                    endX = translateX + 300f
                                )
                            )
                    )
                }
            }
        }
    }
}

// ==================== ERROR MESSAGE ====================
@Composable
fun ErrorMessage(error: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = LightPurpleBg,
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_info),
                    contentDescription = null,
                    tint = PurpleGradientStart,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Unable to load workouts",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = error,
            fontSize = 14.sp,
            color = TextMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = PurpleGradientStart),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Try Again", color = Color.White)
        }
    }
}

// ==================== EMPTY STATE ====================
@Composable
fun EmptyWorkoutsMessage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = LightPurpleBg,
            modifier = Modifier.size(120.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_dumbbell),
                    contentDescription = null,
                    tint = PurpleGradientStart,
                    modifier = Modifier.size(56.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "No Workouts Found",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Adjust your filter or check back later for new workouts.",
            fontSize = 16.sp,
            color = TextMedium,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
    }
}

// ==================== HELPER FUNCTIONS ====================
fun getDifficultyBadgeColor(difficulty: String): Color {
    return when (difficulty.lowercase()) {
        "beginner" -> LightGreenAccent
        "intermediate" -> Color(0xFFFEF3C7)
        "advanced" -> Color(0xFFFEE2E2)
        else -> LightPurpleBg
    }
}

fun getDifficultyTextColor(difficulty: String): Color {
    return when (difficulty.lowercase()) {
        "beginner" -> PrimaryGreen
        "intermediate" -> Color(0xFFD97706)
        "advanced" -> Color.Red
        else -> PurpleGradientStart
    }
}