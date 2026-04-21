package com.example.fitlink.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlink.data.models.*
import com.example.fitlink.data.service.FirebaseService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val firebaseService: FirebaseService
) : ViewModel() {

    // ================= STATE =================

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _dailyStats = MutableStateFlow<DailyStats?>(null)
    val dailyStats: StateFlow<DailyStats?> = _dailyStats.asStateFlow()

    private val _todayWorkouts = MutableStateFlow<List<Workout>>(emptyList())
    val todayWorkouts: StateFlow<List<Workout>> = _todayWorkouts.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessageModel>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessageModel>> = _chatMessages.asStateFlow()

    private val _achievements = MutableStateFlow<List<AchievementModel>>(emptyList())
    val achievements: StateFlow<List<AchievementModel>> = _achievements.asStateFlow()

    private val _weeklyStats = MutableStateFlow<List<DailyStats>>(emptyList())
    val weeklyStats: StateFlow<List<DailyStats>> = _weeklyStats.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var currentUserId: String = ""

    // ================= LOAD DATA =================

    fun loadUserData(userId: String) {

        if (currentUserId == userId && _user.value != null) return
        currentUserId = userId

        _isLoading.value = true

        // 🔹 USER REALTIME
        firebaseService.listenToUser(userId)
            .onEach { _user.value = it }
            .launchIn(viewModelScope)


        firebaseService.listenToDailyStats(userId)
            .onEach { stats -> _dailyStats.value = stats }   // remove the `if (stats != null)` check
            .launchIn(viewModelScope)

        // 🔹 TODAY WORKOUTS
        firebaseService.listenToTodayWorkouts(userId)
            .onEach { _todayWorkouts.value = it }
            .launchIn(viewModelScope)

        // 🔹 RECENT ACTIVITY
        firebaseService.listenToUserActivity(userId)
            .onEach { list ->
                Log.d("DashboardVM", "Activity list updated, size: ${list.size}")
                _chatMessages.value = list
            }
            .launchIn(viewModelScope)

        // 🔹 ACHIEVEMENTS
        firebaseService.listenToUserAchievements(userId)
            .onEach { _achievements.value = it }
            .launchIn(viewModelScope)

        // 🔹 WEEKLY STATS
        firebaseService.listenToWeeklyStats(userId)
            .onEach { stats ->
                _weeklyStats.value = stats // set even when empty so UI re-renders with zeros
            }
            .launchIn(viewModelScope)

        combine(
            user,
            dailyStats
        ) { u, d ->
            u != null
        }
            .onEach { ready ->
                _isLoading.value = !ready
            }
            .launchIn(viewModelScope)
    }

    // ================= COMPLETE WORKOUT =================
    fun completeWorkout(workoutId: String, workoutTitle: String, caloriesBurned: Int, durationSeconds: Int) {
        viewModelScope.launch {
            if (currentUserId.isBlank()) return@launch
            if (_isLoading.value) return@launch

            firebaseService.completeWorkoutAtomic(
                userId = currentUserId,
                workoutId = workoutId,
                workoutTitle = workoutTitle,
                calories = caloriesBurned,
                duration = durationSeconds // now directly seconds
            )
        }
    }

    // ================= UPDATE WATER =================

    fun updateWaterIntake(glasses: Int) {
        viewModelScope.launch {
            _dailyStats.value?.let {
                firebaseService.updateDailyStats(
                    it.copy(waterIntake = glasses)
                )
            }
        }
    }

    fun updateUserBodyDetails(
        age: Int,
        gender: String,
        height: Double,
        weight: Double
    ) {

        viewModelScope.launch {

            firebaseService.updateUser(
                currentUserId,
                mapOf(
                    "age" to age,
                    "gender" to gender,
                    "height" to height,
                    "weight" to weight
                )
            )

        }

    }

    fun updateUserGoals(
        calorieGoal: Int,
        weeklyWorkoutGoal: Int,
        fitnessGoal: String
    ) {

        viewModelScope.launch {

            firebaseService.updateUser(
                currentUserId,
                mapOf(
                    "dailyCalorieGoal" to calorieGoal,
                    "weeklyWorkoutGoal" to weeklyWorkoutGoal,
                    "fitnessGoal" to fitnessGoal
                )
            )

        }

    }

    fun updateReminderSettings(
        hour: Int,
        minute: Int,
        enabled: Boolean
    ) {

        viewModelScope.launch {

            firebaseService.updateUser(
                currentUserId,
                mapOf(
                    "preferences.reminderHour" to hour,
                    "preferences.reminderMinute" to minute,
                    "preferences.workoutReminderEnabled" to enabled
                )
            )

        }

    }

    fun updateWaterGoal(goal: Int) {

        viewModelScope.launch {

            _dailyStats.value?.let {

                firebaseService.updateDailyStats(
                    it.copy(waterGoal = goal)
                )

            }

        }

    }

    fun finishOnboarding() {

        viewModelScope.launch {

            firebaseService.updateUser(
                currentUserId,
                mapOf("onboardingCompleted" to true)
            )

        }

    }
    // ================= LOGOUT =================
    fun checkUserInactivity() {

        viewModelScope.launch {

            val user = _user.value ?: return@launch

            val lastActive = user.lastActive

            val threeDays = 3 * 24 * 60 * 60 * 1000

            if (System.currentTimeMillis() - lastActive > threeDays) {

                firebaseService.sendReminderNotification(
                    user.id,
                    "We miss you 💪",
                    "It's been a few days since your last workout!"
                )

            }

        }

    }
    fun logout() {
        firebaseService.logout()
    }
}