package com.example.fitlink.ui.screens.workouts

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.fitlink.R
import com.example.fitlink.data.models.Exercise
import com.example.fitlink.data.models.Workout
import com.example.fitlink.ui.screens.dashboard.TextLight
import com.example.fitlink.ui.theme.PurpleGrey40
import com.example.fitlink.ui.viewmodels.WorkoutExecutionState
import kotlinx.coroutines.delay

// ==================== SOUND MANAGER ====================
object SoundManager {

    private var currentMediaPlayer: MediaPlayer? = null

    fun playStart(context: Context) {
        playSound(context, R.raw.start_beep)
    }

    fun playHalfway(context: Context) {
        playSound(context, R.raw.halfway_beep)
    }

    fun playRest(context: Context) {
        playSound(context, R.raw.take_rest)
    }

    fun playWorkoutComplete(context: Context) {
        playSound(context, R.raw.complete_tune)
    }

    private fun playSound(context: Context, resId: Int) {
        try {
            currentMediaPlayer?.release()

            currentMediaPlayer = MediaPlayer.create(context, resId).apply {
                setOnCompletionListener {
                    release()
                }
                start()
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        currentMediaPlayer?.release()
        currentMediaPlayer = null
    }
}

// ==================== MAIN SCREEN ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutExecutionScreen(
    uiState: WorkoutExecutionState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCompleteExercise: () -> Unit,
    onSkipRest: () -> Unit,
    onFinishManually: () -> Unit,
    onDismissSuccess: () -> Unit,
    onClearError: () -> Unit,
    onClose: () -> Unit
) {
    val workout = uiState.workout
    val currentIndex = uiState.currentExerciseIndex
    val currentExercise = workout?.exercises?.getOrNull(currentIndex)
    val isWorkoutActive = uiState.isWorkoutActive
    val isResting = uiState.isResting
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    // Sound triggers
    LaunchedEffect(uiState.showSuccessDialog) {

        if (uiState.showSuccessDialog) {

            SoundManager.playWorkoutComplete(context)

        }
    }
    // Clean up sound on dispose
    DisposableEffect(Unit) {
        onDispose { SoundManager.release() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundSoft)
    ) {
        when {
            uiState.isLoading -> WorkoutLoadingShimmer()
            uiState.error != null -> ErrorContent(uiState.error, onClose)
            workout == null -> EmptyContent(onClose)
            !isWorkoutActive -> WorkoutIntro(workout, onStart, onClose)
            else -> {
                ActiveWorkoutContent(
                    uiState = uiState,
                    currentExercise = currentExercise,
                    onPause = onPause,
                    onResume = onResume,
                    onCompleteExercise = onCompleteExercise,
                    onSkipRest = onSkipRest,
                    onFinishManually = onFinishManually,
                    onClose = onClose,
                    context = context
                )
            }
        }

        // Success dialog
        if (uiState.showSuccessDialog) {
            WorkoutCompleteDialog(
                calories = workout?.caloriesBurnEstimate ?: 0,
                duration = workout?.durationMinutes ?: 0,
                onDismiss = {

                    onDismissSuccess()

                }
            )
        }

        // Error dialog
        if (uiState.completionError != null) {
            AlertDialog(
                onDismissRequest = onClearError,
                title = { Text("Save failed") },
                text = { Text(uiState.completionError) },
                confirmButton = {
                    TextButton(onClick = onClearError) { Text("OK") }
                }
            )
        }
    }
    if (uiState.isCompleting) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center
        ) {

            CircularProgressIndicator(
                color = PurpleGradientStart,
                strokeWidth = 4.dp
            )

        }
    }
}

@Composable
private fun ActiveWorkoutContent(
    uiState: WorkoutExecutionState,
    currentExercise: Exercise?,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCompleteExercise: () -> Unit,
    onSkipRest: () -> Unit,
    onFinishManually: () -> Unit,
    onClose: () -> Unit,
    context: Context
) {
    val isResting = uiState.isResting
    val haptic = LocalHapticFeedback.current
    var isMuted by remember { mutableStateOf(true) }
    val scrollState = rememberScrollState()
    var showNextExerciseOverlay by remember { mutableStateOf(false) }

    // Trigger next exercise overlay when exercise index changes (except initial load)
    var lastExerciseIndex by remember { mutableStateOf(-1) }

    LaunchedEffect(uiState.currentExerciseIndex) {

        if (uiState.currentExerciseIndex != lastExerciseIndex && !uiState.isResting) {

            SoundManager.playStart(context)

            showNextExerciseOverlay = true
            kotlinx.coroutines.delay(1500)
            showNextExerciseOverlay = false

            lastExerciseIndex = uiState.currentExerciseIndex
        }
    }
    var halfwayPlayed by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.exerciseSecondsRemaining) {

        val exercise = currentExercise ?: return@LaunchedEffect
        val halfTime = exercise.durationSeconds / 2
        val remaining = uiState.exerciseSecondsRemaining ?: return@LaunchedEffect

        if (!halfwayPlayed && remaining == halfTime) {

            SoundManager.playHalfway(context)
            halfwayPlayed = true

        }

        if (remaining > halfTime) {

            halfwayPlayed = false

        }
    }
    LaunchedEffect(isResting) {

        if (isResting) {

            SoundManager.playRest(context)

            haptic.performHapticFeedback(HapticFeedbackType.LongPress)

        }

    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp) // reduced bottom padding (FAB removed)
        ) {
            Spacer(modifier = Modifier.height(84.dp))

            // Workout progress section
            WorkoutProgressHeader(
                current = uiState.currentExerciseIndex + 1,
                total = uiState.totalExercises,
                progress = uiState.completedExercises.count { it }.toFloat() / uiState.totalExercises
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Exercise video section
            if (!isResting && currentExercise != null) {
                ExerciseVideoCard(
                    exercise = currentExercise,
                    isMuted = isMuted,
                    onToggleMute = { isMuted = !isMuted }
                )
            } else {
                // During rest, show a placeholder
                RestPlaceholder()
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Exercise information
            if (!isResting && currentExercise != null) {
                ExerciseInfo(exercise = currentExercise)

                Spacer(modifier = Modifier.height(16.dp))

                // Duration progress bar (if duration-based)
                if (currentExercise.durationSeconds > 0) {
                    DurationProgressBar(
                        secondsRemaining = uiState.exerciseSecondsRemaining ?: currentExercise.durationSeconds,
                        totalSeconds = currentExercise.durationSeconds
                    )
                } else {
                    // For set/reps, show a static indicator
                    SetRepsIndicator(exercise = currentExercise)
                }

                Spacer(modifier = Modifier.height(24.dp))


// Exercise duration progress
                if (!isResting && currentExercise != null && currentExercise.duration) {

                    val remaining = uiState.exerciseSecondsRemaining ?: currentExercise.durationSeconds
                    val total = currentExercise.durationSeconds

                    val progress = if (total > 0)
                        (total - remaining).toFloat() / total
                    else 0f

                    val minutes = total / 60
                    val seconds = total % 60

                    Column {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {

                            Text(
                                text = "Exercise Duration",
                                fontSize = 14.sp,
                                color = TextMedium
                            )

                            Text(
                                text = "${minutes}m ${seconds}s",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextDark
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = PrimaryGreen,
                            trackColor = LightPurpleBg
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                // Complete button
                CompleteButton(onClick = onCompleteExercise)
            } else if (isResting) {
                // Rest screen content with progress bar
                RestScreen(
                    restSeconds = uiState.restSecondsRemaining,
                    totalRestSeconds = currentExercise?.restSeconds ?: 60,
                    onSkip = onSkipRest
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Upcoming exercises (only when not resting)
            if (!isResting && (uiState.workout?.exercises?.size ?: 0) > 1) {
                UpcomingExercises(
                    exercises = uiState.workout!!.exercises,
                    currentIndex = uiState.currentExerciseIndex
                )
            }
        }

        // Top bar (floating over content)
        WorkoutTopBar(
            title = uiState.workout?.title ?: "",
            isPaused = uiState.isPaused,
            onClose = onClose,
            onPause = onPause,
            onResume = onResume
        )

        // Manual finish button REMOVED as requested

        // Next exercise overlay
        AnimatedVisibility(
            visible = showNextExerciseOverlay && !isResting,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f)),
                modifier = Modifier.padding(20.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "NEXT EXERCISE",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = currentExercise?.name ?: "",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// ==================== WORKOUT PROGRESS HEADER ====================
@Composable
private fun WorkoutProgressHeader(current: Int, total: Int, progress: Float) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Exercise $current of $total",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = TextDark
            )
            Text(
                text = "${(progress * 100).toInt()}%",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = TextDark
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = PurpleGradientStart,
            trackColor = LightPurpleBg
        )
    }
}

// ==================== EXERCISE VIDEO CARD ====================
@Composable
private fun ExerciseVideoCard(
    exercise: Exercise,
    isMuted: Boolean,
    onToggleMute: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(modifier = Modifier.height(240.dp)) {
            // Video/Image background
            if (!exercise.videoUrl.isNullOrBlank()) {
                VideoPlayer(exercise.videoUrl, isMuted)
            } else {
                val model = exercise.gifUrl ?: exercise.imageUrl
                if (!model.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(model)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(PurpleGradientStart, PurpleGradientEnd)
                                )
                            )
                    )
                }
            }

            // Mute/unmute button
            IconButton(
                onClick = onToggleMute,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .size(40.dp)
            ) {
                Icon(
                    painter = painterResource(
                        if (isMuted) R.drawable.ic_volume_off else R.drawable.ic_volume_up
                    ),
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun VideoPlayer(videoUrl: String, isMuted: Boolean) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(videoUrl)))
            repeatMode = Player.REPEAT_MODE_ONE
            volume = if (isMuted) 0f else 1f
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> exoPlayer.play()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    LaunchedEffect(isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    AndroidView(
        factory = {
            PlayerView(context).apply {
                player = exoPlayer
                useController = false
                keepScreenOn = true
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun RestPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(PurpleGradientStart.copy(alpha = 0.3f), BackgroundSoft)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "REST",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White.copy(alpha = 0.5f)
        )
    }
}

// ==================== EXERCISE INFO ====================
@Composable
private fun ExerciseInfo(exercise: Exercise) {
    Column {
        Text(
            text = exercise.name,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(

            text = "${exercise.sets} Sets • ${exercise.durationSeconds / 60} min",
            fontSize = 16.sp,
            color = TextMedium
        )
    }
}

// ==================== DURATION PROGRESS BAR ====================
@Composable
private fun DurationProgressBar(secondsRemaining: Int, totalSeconds: Int) {
    val progress = if (totalSeconds > 0) secondsRemaining.toFloat() / totalSeconds else 0f
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Time Remaining", fontSize = 14.sp, color = TextMedium)
            Text(formatTime(secondsRemaining), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = PurpleGradientStart,
            trackColor = LightPurpleBg
        )
    }
}

// ==================== SET/REPS INDICATOR (for non-duration exercises) ====================
@Composable
private fun SetRepsIndicator(exercise: Exercise) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LightPurpleBg),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Sets", fontSize = 12.sp, color = TextMedium)
                Text("${exercise.sets}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = PurpleGradientStart)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Reps", fontSize = 12.sp, color = TextMedium)
                Text("${exercise.reps}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = PurpleGradientStart)
            }
        }
    }
}

// ==================== REST SCREEN ====================
@Composable
private fun RestScreen(
    restSeconds: Int,
    totalRestSeconds: Int,
    onSkip: () -> Unit
) {
    val progress = if (totalRestSeconds > 0) restSeconds.toFloat() / totalRestSeconds else 0f
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "REST",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextDark,
            letterSpacing = 4.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = formatTime(restSeconds),
            fontSize = 56.sp,
            fontWeight = FontWeight.ExtraBold,
            color = PurpleGradientStart
        )
        Spacer(modifier = Modifier.height(16.dp))
        // Rest progress bar
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = PurpleGradientStart,
            trackColor = LightPurpleBg
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onSkip,
            colors = ButtonDefaults.buttonColors(containerColor = PurpleGradientStart),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(48.dp)
        ) {
            Text("Skip Rest", color = Color.White)
        }
    }
}

// ==================== COMPLETE BUTTON ====================
@Composable
private fun CompleteButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = PrimaryGreen,
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(40.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Text("Complete Set", fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

// ==================== UPCOMING EXERCISES ====================
@Composable
private fun UpcomingExercises(
    exercises: List<Exercise>,
    currentIndex: Int
) {
    Column {
        Text(
            text = "UP NEXT",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = TextMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(exercises) { index, exercise ->
                if (index > currentIndex) {
                    UpcomingExerciseItem(exercise = exercise, index = index + 1)
                }
            }
        }
    }
}

@Composable
private fun UpcomingExerciseItem(exercise: Exercise, index: Int) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = LightPurpleBg,
        modifier = Modifier.size(70.dp, 80.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(4.dp)
        ) {
            Text(
                text = "$index",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = PurpleGradientStart
            )
            Text(
                text = exercise.name,
                fontSize = 8.sp,
                fontWeight = FontWeight.Medium,
                color = TextLight,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ==================== TOP BAR ====================
@Composable
private fun WorkoutTopBar(
    title: String,
    isPaused: Boolean,
    onClose: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = "Close",
                tint = TextDark
            )
        }

        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = PurpleGrey40,
            maxLines = 1,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        )

        IconButton(
            onClick = if (isPaused) onResume else onPause
        ) {
            Icon(
                painter = painterResource(
                    if (isPaused) R.drawable.ic_play else R.drawable.ic_pause
                ),
                contentDescription = if (isPaused) "Resume" else "Pause",
                tint = TextDark
            )
        }
    }
}

// ==================== WORKOUT INTRO ====================
@Composable
private fun WorkoutIntro(
    workout: Workout,
    onStart: () -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(PurpleGradientStart, PurpleGradientEnd)
                )
            )
    ) {
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f))
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = "Close",
                tint = Color.White
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.size(100.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.ic_dumbbell),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(50.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = workout.title,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = workout.description,
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                DetailItem(icon = R.drawable.ic_clock, value = "${workout.durationMinutes} min")
                DetailItem(icon = R.drawable.ic_flame, value = "${workout.caloriesBurnEstimate} kcal")
                DetailItem(icon = R.drawable.ic_dumbbell, value = "${workout.exercises.size} exercises")
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onStart,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = PurpleGradientStart
                ),
                shape = RoundedCornerShape(40.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Start Workout", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DetailItem(icon: Int, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.9f)
        )
    }
}

// ==================== WORKOUT COMPLETE DIALOG ====================
@Composable
private fun WorkoutCompleteDialog(
    calories: Int,
    duration: Int,
    onDismiss: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(2000)
        onDismiss()
    }
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = LightGreenAccent,
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = null,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Great job!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextDark
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "You've completed the workout",
                    fontSize = 16.sp,
                    color = TextMedium
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$calories",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = PurpleGradientStart
                        )
                        Text("kcal burned", color = TextMedium, fontSize = 12.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$duration",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = PurpleGradientStart
                        )
                        Text("minutes", color = TextMedium, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PurpleGradientStart,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("Done", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ==================== LOADING SHIMMER ====================
@Composable
private fun WorkoutLoadingShimmer() {
    val shimmerColors = listOf(
        LightPurpleBg.copy(alpha = 0.3f),
        Color.White,
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(3) {
            WorkoutShimmerCard(translateAnim.value, shimmerColors)
        }
    }
}

@Composable
private fun WorkoutShimmerCard(translateX: Float, colors: List<Color>) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = colors,
                            startX = translateX,
                            endX = translateX + 300f
                        )
                    )
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(24.dp)
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

// ==================== ERROR & EMPTY ====================
@Composable
private fun ErrorContent(message: String, onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Error: $message", color = Color.Red, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = PurpleGradientStart)) {
            Text("Close")
        }
    }
}

@Composable
private fun EmptyContent(onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Workout not found", fontSize = 18.sp, color = TextDark)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = PurpleGradientStart)) {
            Text("Close")
        }
    }
}

// ==================== UTILS ====================
private fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}

// ==================== COLORS ====================
private val PurpleGradientStart = Color(0xFF8B5CF6)
private val PurpleGradientEnd = Color(0xFF6D28D9)
private val LightPurpleBg = Color(0xFFEDE9FE)
private val BackgroundSoft = Color(0xFFF9FAFB)
private val PrimaryGreen = Color(0xFF10B981)
private val LightGreenAccent = Color(0xFFD1FAE5)
private val TextDark = Color(0xFF1F2937)
private val TextMedium = Color(0xFF6B7280)