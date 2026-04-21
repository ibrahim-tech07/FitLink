package com.example.fitlink.ui.screens.workouts

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.fitlink.ui.viewmodels.WorkoutExecutionViewModel
import com.example.fitlink.ui.viewmodels.WorkoutViewModel

@Composable
fun WorkoutSessionRoute(
    workoutId: String,
    onClose: () -> Unit,
    viewModel: WorkoutExecutionViewModel = hiltViewModel(),
    workoutViewModel: WorkoutViewModel = hiltViewModel()
) {

    val uiState by viewModel.uiState.collectAsState()

    // Load workout
    LaunchedEffect(workoutId) {
        viewModel.loadWorkout(workoutId)
    }

    // Listen completion events
    LaunchedEffect(Unit) {
        workoutViewModel.completionEvents.collect { result ->

            result.onSuccess {
                viewModel.onCompletionResult(true, null)
            }.onFailure { ex ->
                viewModel.onCompletionResult(false, ex.message)
            }

        }
    }

    WorkoutExecutionScreen(
        uiState = uiState,
        onStart = { viewModel.startWorkout() },
        onPause = { viewModel.pauseWorkout() },
        onResume = { viewModel.resumeWorkout() },
        onCompleteExercise = { viewModel.completeCurrentExercise() },
        onSkipRest = { viewModel.skipRest() },
        onFinishManually = { viewModel.finishWorkoutManually() },

        // Navigate back after success
        onDismissSuccess = {
            viewModel.dismissSuccessDialog()
            onClose()
        },

        onClearError = { viewModel.clearError() },
        onClose = onClose
    )
}