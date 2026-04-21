package com.example.fitlinktrainer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlinktrainer.data.model.Message
import com.example.fitlinktrainer.data.model.Trainer
import com.example.fitlinktrainer.data.model.User
import com.example.fitlinktrainer.data.repository.ChatRepository
import com.example.fitlinktrainer.data.repository.TrainerRepository
import com.example.fitlinktrainer.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import javax.inject.Inject

@HiltViewModel
class TrainerDashboardViewModel @Inject constructor(
    private val trainerRepository: TrainerRepository,
    private val chatRepository: ChatRepository,
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    // ================= TRAINER =================

    private val _trainer = MutableStateFlow<Trainer?>(null)
    val trainer: StateFlow<Trainer?> = _trainer
    // ================= CLIENTS =================

    private val _clients = MutableStateFlow<List<User>>(emptyList())
    val clients: StateFlow<List<User>> = _clients.asStateFlow()

    // ================= RECENT MESSAGES =================

    private val _recentMessages = MutableStateFlow<List<Message>>(emptyList())
    val recentMessages: StateFlow<List<Message>> = _recentMessages.asStateFlow()

    // ================= PERFORMANCE DATA =================

    private val _performanceData = MutableStateFlow<List<Float>>(emptyList())
    val performanceData: StateFlow<List<Float>> = _performanceData.asStateFlow()

    // ================= LOADING STATE =================

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

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
                .flowOn(Dispatchers.IO)
                .collect { trainer ->

                    _trainer.value = trainer
                }
        }
    }

    // ================= CLIENTS =================

    private fun observeClients(trainerId: String) {

        viewModelScope.launch {

            trainerRepository.listenToClients(trainerId)
                .flowOn(Dispatchers.IO)
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

            val performance = users.map { user ->

                workoutRepository
                    .listenToUserWorkouts(user.id)
                    .first()
                    .size
                    .toFloat()

            }

            _performanceData.value = performance.takeLast(7)

            _isLoading.value = false
        }
    }

    // ================= RECENT MESSAGES =================

    private fun observeMessages(trainerId: String) {

        viewModelScope.launch {

            chatRepository.listenTrainerChats(trainerId)
                .flowOn(Dispatchers.IO)
                .catch {

                    _recentMessages.value = emptyList()
                }
                .collect { chats ->

                    if (chats.isEmpty()) {

                        _recentMessages.value = emptyList()
                        return@collect
                    }

                    try {

                        val messages = chats.map { chat ->

                            async {

                                chatRepository
                                    .listenMessages(chat.id)
                                    .first()

                            }

                        }.awaitAll()

                        _recentMessages.value =
                            messages
                                .flatten()
                                .sortedByDescending { it.timestamp }
                                .take(5)

                    } catch (e: Exception) {

                        e.printStackTrace()
                        _recentMessages.value = emptyList()
                    }
                }
        }
    }
}