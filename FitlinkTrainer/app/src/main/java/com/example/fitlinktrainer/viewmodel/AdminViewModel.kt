package com.example.fitlinktrainer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlinktrainer.data.model.Activity
import com.example.fitlinktrainer.data.model.DashboardStats
import com.example.fitlinktrainer.data.model.Trainer
import com.example.fitlinktrainer.data.repository.AdminRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val dashboardStats: StateFlow<DashboardStats> =
        adminRepository.getDashboardStats()
            .onEach { _isLoading.value = false }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = DashboardStats()
            )

    val pendingTrainers: StateFlow<List<Trainer>> =
        adminRepository.getPendingTrainers()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )



    // No-op; flows are already active due to stateIn
    fun startListening() = Unit

    fun approveTrainer(trainerId: String) {
        viewModelScope.launch {
            adminRepository.updateTrainerStatus(trainerId, "approved")
        }
    }

    fun rejectTrainer(trainerId: String) {
        viewModelScope.launch {
            adminRepository.updateTrainerStatus(trainerId, "rejected")
        }
    }
}