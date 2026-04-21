package com.example.fitlink.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlink.data.models.NotificationSettings
import com.example.fitlink.data.repositories.NotificationSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val repository: NotificationSettingsRepository
) : ViewModel() {

    // ---------------- STATE ----------------

    private val _settings = MutableStateFlow(NotificationSettings())
    val settings: StateFlow<NotificationSettings> =
        _settings.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> =
        _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> =
        _error.asStateFlow()

    private var currentUserId: String? = null
    private var initialized = false

    // ---------------- INITIALIZE ----------------

    fun initialize(userId: String) {
        if (initialized) return
        initialized = true
        currentUserId = userId
        loadSettings()
    }

    // ---------------- LOAD ----------------

    private fun loadSettings() {
        val userId = currentUserId ?: return

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.getSettings(userId)
                .onSuccess { settings ->
                    _settings.value = settings
                }
                .onFailure {
                    _error.value = it.message ?: "Failed to load settings"
                }

            _isLoading.value = false
        }
    }

    // ---------------- UPDATE (LOCAL STATE) ----------------

    fun updateSettings(newSettings: NotificationSettings) {
        _settings.value = newSettings
    }

    // ---------------- SAVE ----------------

    fun saveSettings() {
        val userId = currentUserId ?: return

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.saveSettings(userId, _settings.value)
                .onFailure {
                    _error.value = it.message ?: "Failed to save settings"
                }

            _isLoading.value = false
        }
    }

    // ---------------- CLEAR ERROR ----------------

    fun clearError() {
        _error.value = null
    }
}