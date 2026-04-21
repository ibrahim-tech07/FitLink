package com.example.fitlinktrainer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlinktrainer.data.model.Message
import com.example.fitlinktrainer.data.model.User
import com.example.fitlinktrainer.data.repository.ChatRepository
import com.example.fitlinktrainer.data.repository.TrainerRepository
import com.example.fitlinktrainer.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.collections.emptyList

@HiltViewModel
class TrainerDashboardViewModel @Inject constructor(
    private val trainerRepository: TrainerRepository,
    private val chatRepository: ChatRepository,
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    private val _clients = MutableStateFlow<List<User>>(emptyList())
    val clients: StateFlow<List<User>> = _clients

    private val _recentMessages = MutableStateFlow<List<Message>>(emptyList())
    val recentMessages: StateFlow<List<Message>> = _recentMessages

    private val _performanceData = MutableStateFlow<List<Float>>(emptyList())
    val performanceData: StateFlow<List<Float>> = _performanceData

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    var trainerName: String? = null
    var profileImageUrl: String? = null

    private var trainerId: String? = null

    // ================= INITIALIZE =================

    fun initialize(trainerId: String) {

        if (this.trainerId == trainerId) return

        this.trainerId = trainerId

        observeTrainer(trainerId)

        observeClients(trainerId)

        observeMessages(trainerId)
    }

    // ================= TRAINER INFO =================

    private fun observeTrainer(trainerId: String) {

        viewModelScope.launch {

            trainerRepository.listenToTrainer(trainerId)
                .collect { trainer ->

                    trainerName = trainer?.name
                    profileImageUrl = trainer?.profileImageUrl
                }
        }
    }

    // ================= CLIENTS =================

    private fun observeClients(trainerId: String) {

        viewModelScope.launch {

            trainerRepository.listenToClients(trainerId)
                .collect { users ->

                    _clients.value = users

                    if (users.isEmpty()) {
                        _performanceData.value = emptyList()
                        _isLoading.value = false
                    } else {
                        calculatePerformance(users)
                    }
                }
        }
    }

    // ================= PERFORMANCE CHART =================

    private fun calculatePerformance(users: List<User>) {

        viewModelScope.launch {

            val performance = mutableListOf<Float>()

            // We use flow.first() which is a suspend function.
            // Since it might involve network if not cached, we ensure it's handled properly.
            // Firebase SDK handles network on its own background threads, but .first() waits.
            
            users.forEach { user ->
                try {
                    val workouts = workoutRepository.listenToUserWorkouts(user.id).first()
                    performance.add(workouts.size.toFloat())
                } catch (e: Exception) {
                    performance.add(0f)
                }
            }

            _performanceData.value = performance.takeLast(7)
            _isLoading.value = false
        }
    }

    // ================= RECENT MESSAGES =================

    private fun observeMessages(trainerId: String) {

        viewModelScope.launch {

            chatRepository.listenTrainerChats(trainerId)
                .catch {
                    _recentMessages.value = emptyList()
                }
                .collect { chats ->

                    if (chats.isEmpty()) {
                        _recentMessages.value = emptyList()
                        return@collect
                    }

                    val allMessages = mutableListOf<Message>()

                    chats.forEach { chat ->

                        try {
                            // Using .first() on a Firestore flow can be tricky if it's the first time
                            // but usually it's fine. 
                            val messages = chatRepository.listenMessages(chat.id).first()
                            allMessages.addAll(messages)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    _recentMessages.value =
                        allMessages
                            .sortedByDescending { it.timestamp }
                            .take(5)
                }
        }
    }
}