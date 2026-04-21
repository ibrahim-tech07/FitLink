package com.example.fitlinktrainer.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlinktrainer.data.model.*
import com.example.fitlinktrainer.data.repository.TrainerRepository
import com.example.fitlinktrainer.data.repository.WorkoutRepository
import com.example.fitlinktrainer.data.service.CloudinaryService
import com.example.fitlinktrainer.ui.screens.components.FitnessCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class UploadWorkoutViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val trainerRepository: TrainerRepository,
    private val cloudinaryService: CloudinaryService
) : ViewModel() {

    private val _uiState = MutableStateFlow(UploadWorkoutUiState())
    val uiState: StateFlow<UploadWorkoutUiState> = _uiState.asStateFlow()

    private val _clients = MutableStateFlow<List<User>>(emptyList())
    val clients: StateFlow<List<User>> = _clients.asStateFlow()

    private var selectedUser: User? = null

    fun initialize(trainerId: String) {
        loadClients(trainerId)
    }

    private fun loadClients(trainerId: String) {

        viewModelScope.launch {

            trainerRepository.listenToClients(trainerId)
                .catch {

                    _uiState.update {
                        it.copy(error = "Failed to load clients")
                    }

                }
                .collect { users ->

                    _clients.value = users

                    val selectedId = _uiState.value.selectedUserId
                    selectedUser = users.find { it.id == selectedId }

                }
        }
    }

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun updateDescription(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    fun selectUser(userId: String?) {

        val user = _clients.value.find { it.id == userId }
        selectedUser = user

        _uiState.update {
            it.copy(selectedUserId = userId)
        }
    }

    fun addExercise(exercise: Exercise) {

        val weight = selectedUser?.weight ?: 70.0

        val durationSeconds =
            if (exercise.durationSeconds > 0)
                exercise.durationSeconds
            else
                60   // default 1 minute per exercise

        val calories = FitnessCalculator.calculateCalories(
            met = 8.0,
            weightKg = weight,
            durationSeconds = durationSeconds
        )

        val updatedExercise = exercise.copy(
            id = UUID.randomUUID().toString(),
            duration = true,
            durationSeconds = durationSeconds,
            caloriesBurn = calories
        )

        _uiState.update {
            it.copy(
                exercises = it.exercises + updatedExercise
            )
        }
    }

    fun removeExercise(exercise: Exercise) {

        val updated = _uiState.value.exercises.toMutableList()

        updated.remove(exercise)

        _uiState.update {
            it.copy(exercises = updated)
        }
    }

    fun duplicateExercise(exercise: Exercise) {

        val copy = exercise.copy(
            id = UUID.randomUUID().toString()
        )

        _uiState.update {
            it.copy(exercises = it.exercises + copy)
        }
    }

    fun moveExercise(from: Int, to: Int) {

        val list = _uiState.value.exercises.toMutableList()

        val item = list.removeAt(from)

        list.add(to, item)

        _uiState.update {
            it.copy(exercises = list)
        }
    }

    fun updateExercise(updated: Exercise) {

        val list = _uiState.value.exercises.map {

            if (it.id == updated.id) updated else it
        }

        _uiState.update {
            it.copy(exercises = list)
        }
    }

    fun uploadExerciseVideo(
        context: android.content.Context,
        uri: Uri,
        onUploaded: (String) -> Unit
    ) {
        viewModelScope.launch {

            try {

                _uiState.update { it.copy(uploadingMedia = true) }

                val url = cloudinaryService.uploadWorkoutVideo(context, uri)

                Log.d("UploadWorkout", "Video uploaded: $url")

                onUploaded(url)

            } catch (e: Exception) {

                _uiState.update {
                    it.copy(error = e.message ?: "Video upload failed")
                }

            } finally {

                _uiState.update {
                    it.copy(uploadingMedia = false)
                }
            }
        }
    }

    fun uploadExerciseGif(
        context: android.content.Context,
        uri: Uri,
        onUploaded: (String) -> Unit
    ) {

        viewModelScope.launch {

            try {

                _uiState.update { it.copy(uploadingMedia = true) }

                val url = cloudinaryService.uploadWorkoutImage(context, uri)

                onUploaded(url)

            } catch (e: Exception) {

                _uiState.update {
                    it.copy(error = e.message ?: "GIF upload failed")
                }

            } finally {

                _uiState.update {
                    it.copy(uploadingMedia = false)
                }
            }
        }
    }
    fun setTemplate(exercises: List<Exercise>) {

        val user = selectedUser
        val weight = user?.weight ?: 70.0

        val processedExercises = exercises.map { exercise ->

            val durationSeconds =
                if (exercise.durationSeconds > 0)
                    exercise.durationSeconds
                else
                    60

            val calories = FitnessCalculator.calculateCalories(
                met = 8.0,
                weightKg = weight,
                durationSeconds = durationSeconds
            )

            exercise.copy(
                id = UUID.randomUUID().toString(),
                duration = true,
                durationSeconds = durationSeconds,
                caloriesBurn = calories
            )
        }

        _uiState.update {
            it.copy(exercises = processedExercises)
        }
    }
    fun setImage(uri: Uri?) {
        _uiState.update { it.copy(imageUri = uri) }
    }

    fun setVideo(uri: Uri?) {
        _uiState.update { it.copy(videoUri = uri) }
    }

    fun updateDifficulty(level: String) {
        _uiState.update { it.copy(difficulty = level) }
    }

    fun uploadWorkout(
        context: android.content.Context,
        trainerId: String,
        onResult: (Boolean, String?) -> Unit
    ) {

        val state = _uiState.value

        if (state.title.isBlank()) {
            _uiState.update { it.copy(error = "Workout title required") }
            return
        }

        if (state.selectedUserId == null) {
            _uiState.update { it.copy(error = "Select a client") }
            return
        }

        if (state.exercises.isEmpty()) {
            _uiState.update { it.copy(error = "Add exercises") }
            return
        }

        viewModelScope.launch {

            try {

                val authTrainerId =
                    com.google.firebase.auth.FirebaseAuth
                        .getInstance()
                        .currentUser?.uid ?: return@launch
                _uiState.update { it.copy(isLoading = true) }

                val imageUrl =
                    state.imageUri?.let { cloudinaryService.uploadWorkoutImage(context, it) }

                val videoUrl =
                    state.videoUri?.let { cloudinaryService.uploadWorkoutVideo(context, it) }

                val calories =
                    state.exercises.sumOf { it.caloriesBurn }

                val totalSeconds =
                    state.exercises.sumOf { if (it.durationSeconds > 0) it.durationSeconds else 60 }

                val workout = Workout(
                    id = UUID.randomUUID().toString(),
                    trainerId = authTrainerId,
                    userId = state.selectedUserId!!,
                    title = state.title,
                    description = state.description,
                    exercises = state.exercises,
                    durationMinutes = totalSeconds / 60,
                    caloriesBurnEstimate = calories,
                    scheduledDate = System.currentTimeMillis(),
                    mediaUrl = imageUrl,
                    videoUrl = videoUrl,
                    status = WorkoutStatus.SCHEDULED,
                    difficulty = state.difficulty
                )

                val result = workoutRepository.createWorkout(workout)

                _uiState.update { it.copy(isLoading = false) }
                println("Exercises before save = ${state.exercises}")
                result.onSuccess {

                    _uiState.value = UploadWorkoutUiState(
                        success = true
                    )

                    onResult(true, it)
                }

                result.onFailure { exception ->

                    _uiState.update { state ->
                        state.copy(
                            error = exception.message ?: "Workout upload failed"
                        )
                    }

                    onResult(false, exception.message)
                }

            } catch (e: Exception) {

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message
                    )
                }

                onResult(false, e.message)
            }
        }
    }

    fun clearMessages() {

        _uiState.update {
            it.copy(
                error = null,
                success = false
            )
        }
    }
}
data class UploadWorkoutUiState(

    val title: String = "",
    val description: String = "",

    val difficulty: String = "Beginner",

    val selectedUserId: String? = null,

    val exercises: List<Exercise> = emptyList(),

    val imageUri: Uri? = null,
    val videoUri: Uri? = null,

    val isLoading: Boolean = false,
    val uploadingMedia: Boolean = false,   // ✅ FIXED

    val error: String? = null,
    val success: Boolean = false
)