// File: com/example/fitlink/ui/viewmodels/WorkoutViewModel.kt
package com.example.fitlink.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlink.data.models.Workout
import com.example.fitlink.data.models.WorkoutStatus
import com.example.fitlink.data.repositories.WorkoutRepository
import com.example.fitlink.data.service.FirebaseService
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val firebaseService: FirebaseService
) : ViewModel() {

    private val _allWorkouts = MutableStateFlow<List<Workout>>(emptyList())
    val allWorkouts: StateFlow<List<Workout>> = _allWorkouts.asStateFlow()

    private val _filteredWorkouts = MutableStateFlow<List<Workout>>(emptyList())
    val filteredWorkouts: StateFlow<List<Workout>> = _filteredWorkouts.asStateFlow()

    private val _selectedDifficulty = MutableStateFlow("All")
    val selectedDifficulty: StateFlow<String> = _selectedDifficulty.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _workoutStats = MutableStateFlow<Map<String, Int>>(emptyMap())
    val workoutStats: StateFlow<Map<String, Int>> = _workoutStats.asStateFlow()

    // SharedFlow for completion result events (UI subscribes)
    private val _completionEvents = MutableSharedFlow<Result<Boolean>>(replay = 0)
    val completionEvents = _completionEvents.asSharedFlow()
    private val _completingWorkoutId = MutableStateFlow<String?>(null)
    val completingWorkoutId: StateFlow<String?> = _completingWorkoutId
    private val _refreshStats = MutableSharedFlow<Unit>()
    val refreshStats = _refreshStats.asSharedFlow()
    // keep track of workouts currently being completed to prevent duplicates
    private val inFlightWorkouts = mutableSetOf<String>()

    private var currentUserId: String = ""

    fun initialize(userId: String) {
        if (userId.isBlank()) return
        currentUserId = userId
        loadAllWorkouts()
    }

    fun loadAllWorkouts() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            // Double‑check authentication
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                _error.value = "User not authenticated"
                _isLoading.value = false
                return@launch
            }
            if (currentUser.uid != currentUserId) {
                _error.value = "User ID mismatch"
                _isLoading.value = false
                return@launch
            }

            firebaseService.firestore
                .collection("workouts")
                .whereEqualTo("userId", currentUserId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        _error.value = error.message
                        _isLoading.value = false
                        return@addSnapshotListener
                    }
                    val workouts = snapshot?.documents?.mapNotNull {
                        it.toObject(Workout::class.java)
                    } ?: emptyList()

                    _allWorkouts.value = workouts
                    applyFilters()
                    updateStats()
                    _isLoading.value = false
                }
        }
    }

    fun filterByDifficulty(difficulty: String) {
        _selectedDifficulty.value = difficulty
        applyFilters()
    }

    private fun applyFilters() {
        val difficulty = _selectedDifficulty.value
        _filteredWorkouts.value =
            if (difficulty == "All") _allWorkouts.value
            else _allWorkouts.value.filter { it.difficulty == difficulty }
    }

    fun startWorkout(workoutId: String) {
        viewModelScope.launch {
            workoutRepository.updateWorkout(workoutId, mapOf("status" to WorkoutStatus.IN_PROGRESS.name))
        }
    }

    /**
     * Complete workout with atomic transaction. Emits a Result<Boolean> to completionEvents for UI.
     * Prevents duplicate concurrent requests for same workout.
     */
    // File: com/example/fitlink/ui/viewmodels/WorkoutViewModel.kt

    fun completeWorkout(workoutId: String, workoutTitle: String, calories: Int, durationSeconds: Int) {
        println("FINISH BUTTON CLICKED -> $workoutId")

        if (inFlightWorkouts.contains(workoutId)) return

        inFlightWorkouts.add(workoutId)
        _completingWorkoutId.value = workoutId

        viewModelScope.launch {
            val result = workoutRepository.completeWorkoutAtomic(
                currentUserId,
                workoutId,
                workoutTitle,
                calories,
                durationSeconds
            )

            result.onSuccess {
                _completionEvents.emit(Result.success(true))
                _refreshStats.emit(Unit)
                loadAllWorkouts()
            }.onFailure {
                _completionEvents.emit(Result.failure(it))
            }

            inFlightWorkouts.remove(workoutId)
            _completingWorkoutId.value = null
        }
    }

    fun getWorkoutById(workoutId: String): Flow<Workout?> = flow {
        val result = workoutRepository.getWorkoutById(workoutId)
        result.onSuccess { emit(it) }.onFailure { emit(null) }
    }

    private fun updateStats(){
        val workouts = _allWorkouts.value
        _workoutStats.value = mapOf(
            "total" to workouts.size,
            "completed" to workouts.count { it.status == WorkoutStatus.COMPLETED },
            "inProgress" to workouts.count { it.status == WorkoutStatus.IN_PROGRESS },
            "scheduled" to workouts.count { it.status == WorkoutStatus.SCHEDULED },
            "missed" to workouts.count { it.status == WorkoutStatus.MISSED },
            "totalCalories" to workouts.sumOf { it.caloriesBurnEstimate }
        )
    }

    fun clearError() { _error.value = null }
}