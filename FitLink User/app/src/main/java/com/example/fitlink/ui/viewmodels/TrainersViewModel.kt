package com.example.fitlink.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlink.data.models.Trainer
import com.example.fitlink.data.repositories.TrainerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrainersViewModel @Inject constructor(
    private val trainerRepository: TrainerRepository
) : ViewModel() {

    data class TrainerUiState(
        val trainers: List<Trainer> = emptyList(),
        val filteredTrainers: List<Trainer> = emptyList(),
        val selectedSpecialty: String = "All",
        val searchQuery: String = "",
        val isLoading: Boolean = false,
        val error: String? = null,
        val connectingTrainerId: String? = null,
        val connectedTrainerId: String? = null
    )

    private val _uiState = MutableStateFlow(TrainerUiState())
    val uiState: StateFlow<TrainerUiState> = _uiState

    private val _events = Channel<String>()
    val events = _events.receiveAsFlow()

    private var currentUserId: String? = null
    private var trainersLoadJob: Job? = null   // ← track the active load coroutine

    fun initialize(userId: String) {
        // Update current user ID (allow re‑initialization)
        currentUserId = userId

        // Cancel any previous load job to avoid duplicate collectors
        trainersLoadJob?.cancel()
        trainersLoadJob = viewModelScope.launch {
            loadTrainers()
        }
    }

    private suspend fun loadTrainers() {
        trainerRepository.listenToTrainers()
            .onStart {
                _uiState.update { it.copy(isLoading = true) }
            }
            .catch { e ->
                _uiState.update {
                    it.copy(isLoading = false, error = e.message)
                }
            }
            .collect { trainers ->
                val userId = currentUserId

                // Find the trainer this user is connected to
                val connectedTrainer = trainers.firstOrNull { trainer ->
                    val clientIds = trainer.clientIds ?: emptyList()
                    clientIds.any { id ->
                        id.trim().equals(userId?.trim(), ignoreCase = true)
                    }
                }

                _uiState.update {
                    it.copy(
                        trainers = trainers,
                        filteredTrainers = trainers,
                        isLoading = false,
                        error = null,
                        connectedTrainerId = connectedTrainer?.id
                    )
                }

                applyFilters()
            }
    }

    fun refresh() {
        // Cancel current load and restart
        trainersLoadJob?.cancel()
        trainersLoadJob = viewModelScope.launch {
            loadTrainers()
        }
    }

    fun search(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    fun filterBySpecialty(specialty: String) {
        _uiState.update { it.copy(selectedSpecialty = specialty) }
        applyFilters()
    }

    private fun applyFilters() {
        val state = _uiState.value

        val filtered = state.trainers.filter { trainer ->
            val matchesSearch =
                state.searchQuery.isBlank() ||
                        trainer.name.contains(state.searchQuery, true) ||
                        trainer.specialties.any {
                            it.contains(state.searchQuery, true)
                        }

            val matchesSpecialty =
                state.selectedSpecialty == "All" ||
                        trainer.specialties.any {
                            it.equals(state.selectedSpecialty, true)
                        }

            matchesSearch && matchesSpecialty
        }

        _uiState.update { it.copy(filteredTrainers = filtered) }
    }

    fun connectWithTrainer(trainerId: String, trainerName: String) {
        val userId = currentUserId ?: return

        // Prevent multiple connections
        if (_uiState.value.connectedTrainerId != null) {
            viewModelScope.launch {
                _events.send("Already connected to a trainer")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(connectingTrainerId = trainerId) }

            trainerRepository.connectWithTrainer(userId, trainerId, "FIT123")
                .onSuccess {
                    _events.send("Connected with $trainerName 🎉")
                    _uiState.update { it.copy(connectingTrainerId = null) }
                    // The snapshot listener will update connectedTrainerId automatically
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            connectingTrainerId = null,
                            error = e.message ?: "Connection failed"
                        )
                    }
                }
        }
    }

    fun disconnectTrainer(trainerId: String) {
        val userId = currentUserId ?: return

        viewModelScope.launch {
            trainerRepository.disconnectTrainer(userId, trainerId)
                .onSuccess {
                    _events.send("Disconnected successfully")
                    // The snapshot listener will update connectedTrainerId automatically
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(error = e.message ?: "Disconnect failed")
                    }
                }
        }
    }
}