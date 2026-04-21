package com.example.fitlinktrainer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlinktrainer.data.model.Notification
import com.example.fitlinktrainer.data.model.NotificationType
import com.example.fitlinktrainer.data.model.User
import com.example.fitlinktrainer.data.model.Workout
import com.example.fitlinktrainer.data.model.WorkoutStatus
import com.example.fitlinktrainer.data.repository.UserRepository
import com.example.fitlinktrainer.data.repository.WorkoutRepository
import com.example.fitlinktrainer.data.service.FirebaseService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class TrainerActivityState(
    val isLoading: Boolean = true,
    val clients: List<User> = emptyList(),
    val workouts: List<Workout> = emptyList(),
    val activeClientsCount: Int = 0,
    val inactiveClientsCount: Int = 0,
    val missedWorkoutsCount: Int = 0,
    val weeklyCompletionPercent: Int = 0,
    val atRiskClients: List<User> = emptyList(),
    val weeklyActivityData: Map<String, Int> = emptyMap(),
    val weeklyCaloriesData: Map<String, Int> = emptyMap(),
    val errorMessage: String? = null
)

@HiltViewModel
class TrainerActivityCenterViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    private val trainerId = FirebaseService().auth.currentUser?.uid ?: ""

    private val _state = MutableStateFlow(TrainerActivityState())
    val state: StateFlow<TrainerActivityState> = _state.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {

            combine(
                userRepository.listenToClients(trainerId),
                workoutRepository.listenToTrainerWorkouts(trainerId)
            ) { clients, workouts ->

                computeState(clients, workouts)

            }.catch { exception ->

                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = exception.message
                    )
                }

            }.collect { newState ->

                _state.value = newState

                notifyInactiveUsers() // check inactive users automatically
            }
        }
    }

    private fun computeState(
        clients: List<User>,
        workouts: List<Workout>
    ): TrainerActivityState {

        val now = System.currentTimeMillis()
        val fortyEightHours = 48 * 60 * 60 * 1000L

        val activeClients =
            clients.filter { now - it.lastActive < fortyEightHours }

        val inactiveClients =
            clients.filter { now - it.lastActive >= fortyEightHours }

        val missedWorkouts =
            workouts.count { it.status == WorkoutStatus.MISSED }

        val dayNames =
            listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

        val activityMap =
            dayNames.associateWith { 0 }.toMutableMap()

        val caloriesMap =
            dayNames.associateWith { 0 }.toMutableMap()

        workouts
            .filter { it.status == WorkoutStatus.COMPLETED }
            .forEach { workout ->

                val cal = Calendar.getInstance().apply {
                    timeInMillis = workout.scheduledDate
                }

                val day =
                    dayNames[cal.get(Calendar.DAY_OF_WEEK) - 1]

                activityMap[day] =
                    activityMap.getValue(day) + 1

                caloriesMap[day] =
                    caloriesMap.getValue(day) + workout.caloriesBurnEstimate
            }

        return TrainerActivityState(
            isLoading = false,
            clients = clients,
            workouts = workouts,
            activeClientsCount = activeClients.size,
            inactiveClientsCount = inactiveClients.size,
            missedWorkoutsCount = missedWorkouts,
            weeklyCompletionPercent = calculateWeeklyCompletion(workouts),
            atRiskClients = inactiveClients,
            weeklyActivityData = activityMap,
            weeklyCaloriesData = caloriesMap
        )
    }

    private fun calculateWeeklyCompletion(
        workouts: List<Workout>
    ): Int {

        val calendar = Calendar.getInstance()

        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val weekStart = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_WEEK, 7)

        val weekEnd = calendar.timeInMillis

        val weekWorkouts =
            workouts.filter {
                it.scheduledDate in weekStart..weekEnd
            }

        if (weekWorkouts.isEmpty()) return 0

        val completed =
            weekWorkouts.count {
                it.status == WorkoutStatus.COMPLETED
            }

        return (completed * 100) / weekWorkouts.size
    }

    fun sendReminder(userId: String) {

        viewModelScope.launch {

            val notificationId =
                FirebaseService()
                    .firestore
                    .collection("notifications")
                    .document()
                    .id

            val notification = Notification(
                id = notificationId,
                userId = userId,
                title = "Workout Reminder 💪",
                message = "You haven't worked out for a while. Time to get back!",
                type = NotificationType.WORKOUT_REMINDER,
                timestamp = System.currentTimeMillis(),
                isRead = false
            )

            FirebaseService().createNotification(notification)


        }
    }
    fun notifyInactiveUsers() {

        viewModelScope.launch {

            val now = System.currentTimeMillis()
            val inactiveLimit = 48 * 60 * 60 * 1000L

            state.value.clients.forEach { user ->

                val inactive = now - user.lastActive > inactiveLimit

                if (inactive) {

                    val notification = Notification(
                        id = FirebaseService()
                            .firestore
                            .collection("notifications")
                            .document()
                            .id,
                        userId = user.id,
                        title = "We miss you at FitLink 💪",
                        message = "You haven't worked out in 48 hours. Let's get back on track!",
                        type = NotificationType.TRAINER_MESSAGE,
                        timestamp = System.currentTimeMillis(),
                        isRead = false
                    )

                    FirebaseService().createNotification(notification)


                }
            }
        }
    }
    fun sendMotivation(userId: String) {

        viewModelScope.launch {

            val notification = Notification(
                id = FirebaseService()
                    .firestore
                    .collection("notifications")
                    .document()
                    .id,
                userId = userId,
                title = "Trainer Message",
                message = "You're doing great! Keep pushing!",
                type = NotificationType.TRAINER_MESSAGE,
                timestamp = System.currentTimeMillis(),
                isRead = false
            )

            FirebaseService().createNotification(notification)


        }
    }

    fun assignWorkout(userId: String) {

        // navigate to workout assign screen
    }
}