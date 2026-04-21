package com.example.fitlink.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlink.data.models.Notification
import com.example.fitlink.data.repositories.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repository: NotificationRepository
) : ViewModel() {

    companion object {
        private const val TAG = "NotificationsViewModel"
    }

    // ================= UI STATE =================

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    private val _selectedType = MutableStateFlow<String?>(null)
    val selectedType: StateFlow<String?> = _selectedType.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var currentUserId: String? = null

    // ================= FILTERED LIST =================

    val filteredNotifications: StateFlow<List<Notification>> =
        combine(_notifications, _selectedType) { list, type ->
            if (type.isNullOrEmpty()) {
                list
            } else {
                list.filter { it.type.name == type }
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    // ================= UNREAD COUNT =================

    val unreadCount: StateFlow<Int> =
        _notifications
            .map { list -> list.count { !it.isRead } }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                0
            )

    // ================= INITIALIZE =================

    fun initialize(userId: String) {

        if (currentUserId == userId) return

        currentUserId = userId
        observeNotifications(userId)
    }

    // ================= OBSERVE REALTIME =================

    private fun observeNotifications(userId: String) {

        _isLoading.value = true

        repository.getNotificationsStream(userId)
            .onEach { list ->
                _notifications.value = list
                _isLoading.value = false
            }
            .catch { e ->
                _error.value = e.message
                _isLoading.value = false
            }
            .launchIn(viewModelScope)

    }

    // ================= REFRESH =================

    fun refresh() {
        currentUserId?.let { userId ->
            viewModelScope.launch {
                _isLoading.value = true
                repository.getNotifications(userId)
                    .onSuccess { list ->
                        _notifications.value = list
                    }
                    .onFailure { e ->
                        _error.value = e.message
                    }
                _isLoading.value = false
            }
        }
    }

    // ================= FILTER =================

    fun filterByType(type: String?) {
        _selectedType.value = type
    }

    // ================= DELETE =================

    fun deleteNotification(id: String) {
        viewModelScope.launch {
            repository.deleteNotification(id)
                .onFailure { e ->
                    _error.value = e.message
                }
        }
    }

    // ================= MARK AS READ =================

    fun markAsRead(id: String) {
        viewModelScope.launch {
            repository.markAsRead(id)
                .onFailure { e ->
                    _error.value = e.message
                }
        }
    }

    fun markAllAsRead() {
        currentUserId?.let { userId ->
            viewModelScope.launch {
                repository.markAllAsRead(userId)
                    .onFailure { e ->
                        _error.value = e.message
                    }
            }
        }
    }

    // ================= PAGINATION SUPPORT =================

    fun loadMore(lastTimestamp: Long) {
        currentUserId?.let { userId ->
            viewModelScope.launch {
                repository.getMoreNotifications(userId, lastTimestamp)
                    .onSuccess { more ->
                        _notifications.value =
                            _notifications.value + more
                    }
                    .onFailure { e ->
                        _error.value = e.message
                    }
            }
        }
    }

    // ================= ERROR =================

    fun clearError() {
        _error.value = null
    }
}