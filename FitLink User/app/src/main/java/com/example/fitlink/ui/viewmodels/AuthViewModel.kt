// File: com/example/fitlink/ui/viewmodels/AuthViewModel.kt
package com.example.fitlink.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlink.data.models.User
import com.example.fitlink.data.repositories.AuthRepository
import com.example.fitlink.data.repositories.UserRepository
import com.example.fitlink.utlis.NotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val message: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    init {
        checkAuthStatus()
    }

    private fun checkAuthStatus() {
        val firebaseUser = authRepository.getCurrentUser()

        if (firebaseUser != null) {
            viewModelScope.launch {
                _authState.value = AuthState.Loading

                val loaded = loadUserData(firebaseUser.uid)

                if (loaded) {
                    _isLoggedIn.value = true
                    _authState.value = AuthState.Success("Auto login")
                } else {
                    _authState.value = AuthState.Idle
                }
            }
        }
    }


    fun login(email: String, password: String) {
        viewModelScope.launch {

            _authState.value = AuthState.Loading

            val result = authRepository.login(email, password)

            if (result.isSuccess) {

                val firebaseUser = result.getOrNull()

                if (firebaseUser != null) {

                    val loaded = loadUserData(firebaseUser.uid)

                    if (loaded) {
                        _isLoggedIn.value = true
                        _authState.value = AuthState.Success("Login successful!")
                    } else {
                        _authState.value = AuthState.Error("Failed to load user data")
                    }
                }

            } else {
                val error = result.exceptionOrNull()
                _authState.value = AuthState.Error(error?.message ?: "Login failed")
            }
        }
    }

    fun register(email: String, password: String, name: String, phone: String) {
        viewModelScope.launch {

            _authState.value = AuthState.Loading

            val result = authRepository.register(email, password, name, phone)

            if (result.isSuccess) {

                val firebaseUser = result.getOrNull()

                if (firebaseUser != null) {

                    val loaded = loadUserData(firebaseUser.uid)

                    if (loaded) {
                        _isLoggedIn.value = true
                        _authState.value = AuthState.Success("Registration successful!")
                    } else {
                        _authState.value = AuthState.Error("Failed to load user data")
                    }
                }

            } else {
                _authState.value =
                    AuthState.Error(result.exceptionOrNull()?.message ?: "Registration failed")
            }
        }
    }



    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading

            val result = authRepository.loginWithGoogle(idToken)

            if (result.isSuccess) {
                val firebaseUser = result.getOrNull()
                firebaseUser?.let {
                    loadUserData(it.uid)
                    _isLoggedIn.value = true
                    _authState.value = AuthState.Success("Login successful!")
                }
            } else {
                _authState.value = AuthState.Error("Google login failed")
            }
        }
    }


    private suspend fun loadUserData(userId: String): Boolean {

        val result = userRepository.getUser(userId)

        if (result.isSuccess) {

            var user = result.getOrNull()

            // If user document does not exist create it
            if (user == null) {

                user = User(
                    id = userId,
                    onboardingCompleted = false
                )

                userRepository.createUser(user)

            }

            // Ensure onboarding field always exists
            if (user.onboardingCompleted == null) {

                userRepository.updateUser(
                    userId,
                    mapOf("onboardingCompleted" to false)
                )

            }

            _currentUser.value = user

            return true
        }

        return false
    }


    fun logout() {
        authRepository.logout()
        _isLoggedIn.value = false
        _currentUser.value = null
        _authState.value = AuthState.Idle
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.sendPasswordResetEmail(email)

            if (result.isSuccess) {
                _authState.value = AuthState.Success("Password reset email sent")
            } else {
                _authState.value =
                    AuthState.Error(result.exceptionOrNull()?.message ?: "Failed to send reset email")
            }
        }
    }

    fun afterLogin(context: Context) {

        // Subscribe to notification topics
        NotificationHelper.subscribeToTopics()

        val user = _currentUser.value

        user?.let {
            if (it.preferences.workoutReminderEnabled) {
                NotificationHelper.scheduleDailyWorkoutReminder(
                    context,
                    it.preferences.reminderHour,
                    it.preferences.reminderMinute
                )
            }
        }
    }

    fun clearError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.Idle
        }
    }
}