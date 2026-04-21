package com.example.fitlink.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlink.data.models.Exercise
import com.example.fitlink.data.models.Workout
import com.example.fitlink.data.repositories.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkoutExecutionViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutExecutionState())
    val uiState: StateFlow<WorkoutExecutionState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    // Load the workout from repository
    fun loadWorkout(workoutId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = workoutRepository.getWorkoutById(workoutId)
            result.onSuccess { workout ->
                if (workout != null) {
                    _uiState.update {
                        it.copy(
                            workout = workout,
                            isLoading = false,
                            totalExercises = workout.exercises.size,
                            completedExercises = List(workout.exercises.size) { false }
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Workout not found") }
                }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, error = error.message) }
            }
        }
    }

    // Start the first exercise
    fun startWorkout() {
        timerJob?.cancel()
        val workout = _uiState.value.workout ?: return
        if (workout.exercises.isEmpty()) return

        _uiState.update {
            it.copy(
                isWorkoutActive = true,
                isPaused = false,
                currentExerciseIndex = 0
            )
        }
        startExerciseTimer(workout.exercises[0])
    }

    // Mark the current exercise as completed
    fun completeCurrentExercise() {
        val state = _uiState.value
        val workout = state.workout ?: return
        val currentIndex = state.currentExerciseIndex

        if (currentIndex < 0 || currentIndex >= workout.exercises.size) return

        // Mark this exercise as done
        val updatedCompleted = state.completedExercises.toMutableList()
        updatedCompleted[currentIndex] = true

        val progress =
            ((updatedCompleted.count { it }.toFloat() /
                    workout.exercises.size) * 100).toInt()

        // Check if it was the last exercise
        if (currentIndex == workout.exercises.lastIndex) {
            // Workout finished
            finishWorkout()
            return
        }

        // Move to next exercise, but start rest first
        val nextIndex = currentIndex + 1
        _uiState.update {
            it.copy(
                completedExercises = updatedCompleted,
                isResting = true,
                restSecondsRemaining = workout.exercises[currentIndex].restSeconds
            )
        }
        startRestTimer()
    }

    // Skip the rest period and go to next exercise
    fun skipRest() {
        timerJob?.cancel()
        val state = _uiState.value
        val workout = state.workout ?: return
        val nextIndex = state.currentExerciseIndex + 1

        if (nextIndex < workout.exercises.size) {
            _uiState.update {
                it.copy(
                    isResting = false,
                    currentExerciseIndex = nextIndex,
                    restSecondsRemaining = 0
                )
            }
            startExerciseTimer(workout.exercises[nextIndex])
        }
    }

    // Pause all timers
    fun pauseWorkout() {
        timerJob?.cancel()
        _uiState.update { it.copy(isPaused = true) }
    }

    // Resume timers
    fun resumeWorkout() {
        _uiState.update { it.copy(isPaused = false) }
        val state = _uiState.value
        if (state.isResting) {
            startRestTimer()
        } else {
            val workout = state.workout ?: return
            val exercise = workout.exercises.getOrNull(state.currentExerciseIndex) ?: return
            startExerciseTimer(exercise)
        }
    }

    // Reset everything (e.g. when user closes)
    fun resetWorkout() {
        timerJob?.cancel()
        _uiState.update {
            WorkoutExecutionState(
                workout = it.workout,
                isLoading = false
            )
        }
    }

    // Called when the user manually finishes the whole workout (e.g. from a "Finish" button)
    fun finishWorkoutManually() {
        finishWorkout()
    }

    // Internal finish logic – updates repository and shows success
    private fun finishWorkout() {

        timerJob?.cancel()

        val workout = _uiState.value.workout ?: return

        val totalCalories = workout.caloriesBurnEstimate
        val totalDuration = workout.durationMinutes * 60
        val title = workout.title

        // SHOW LOADER
        _uiState.update {
            it.copy(isCompleting = true)
        }

        viewModelScope.launch {

            val result = workoutRepository.completeWorkoutAtomic(
                workout.userId,
                workout.id,
                title,
                totalCalories,
                totalDuration
            )

            if (result.isSuccess) {

                _uiState.update {
                    it.copy(
                        isCompleting = false,
                        showSuccessDialog = true,
                        isWorkoutActive = false,
                        isResting = false
                    )
                }

            } else {

                _uiState.update {
                    it.copy(
                        isCompleting = false,
                        completionError = result.exceptionOrNull()?.message
                            ?: "Failed to save workout"
                    )
                }

            }
        }
    }
    // Timer for exercise duration
    private fun startExerciseTimer(exercise: Exercise) {

        timerJob?.cancel()

        if (exercise.duration) {

            var secondsLeft =
                if (exercise.durationSeconds > 0)
                    exercise.durationSeconds
                else
                    exercise.reps   // fallback

            _uiState.update {
                it.copy(exerciseSecondsRemaining = secondsLeft)
            }

            timerJob = viewModelScope.launch {

                while (
                    secondsLeft > 0 &&
                    _uiState.value.isWorkoutActive &&
                    !_uiState.value.isPaused
                ) {

                    delay(1000)
                    secondsLeft--

                    _uiState.update {
                        it.copy(exerciseSecondsRemaining = secondsLeft)
                    }
                }

                if (
                    secondsLeft == 0 &&
                    _uiState.value.isWorkoutActive &&
                    !_uiState.value.isPaused
                ) {
                    completeCurrentExercise()
                }
            }

        } else {

            _uiState.update {
                it.copy(exerciseSecondsRemaining = null)
            }
        }
    }

    private fun startRestTimer() {

        timerJob?.cancel()

        var restLeft = _uiState.value.restSecondsRemaining.coerceAtLeast(0)

        timerJob = viewModelScope.launch {

            while (_uiState.value.isWorkoutActive) {

                if (_uiState.value.isPaused) {
                    delay(500)
                    continue
                }

                if (restLeft <= 0) break

                delay(1000)
                restLeft--

                _uiState.update {
                    it.copy(restSecondsRemaining = restLeft)
                }
            }

            if (
                restLeft == 0 &&
                _uiState.value.isWorkoutActive &&
                !_uiState.value.isPaused
            ) {
                skipRest()
            }
        }
    }
    fun onCompletionResult(success: Boolean, error: String?) {

        if (success) {
            _uiState.update {
                it.copy(
                    isCompleting = false,
                    showSuccessDialog = true,
                    isWorkoutActive = false,
                    isResting = false
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    isCompleting = false,
                    completionError = error ?: "Workout completion failed"
                )
            }
        }
    }
    fun dismissSuccessDialog() {
        _uiState.update { it.copy(showSuccessDialog = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(completionError = null) }
    }

    override fun onCleared() {
        timerJob?.cancel()
        super.onCleared()
    }
}

data class WorkoutExecutionState(
    val workout: Workout? = null,
    val isLoading: Boolean = true,
    val error: String? = null,

    // Workout progress
    val isWorkoutActive: Boolean = false,
    val isPaused: Boolean = false,
    val currentExerciseIndex: Int = -1,
    val totalExercises: Int = 0,
    val completedExercises: List<Boolean> = emptyList(),

    // Exercise / rest timers
    val exerciseSecondsRemaining: Int? = null,  // null means no timer (sets/reps)
    val isResting: Boolean = false,
    val restSecondsRemaining: Int = 0,

    // Completion state
    val isCompleting: Boolean = false,
    val showSuccessDialog: Boolean = false,
    val completionError: String? = null
)