package com.example.fitlinktrainer.viewmodel



import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlinktrainer.data.model.Trainer
import com.example.fitlinktrainer.data.repository.AuthRepository
import com.example.fitlinktrainer.data.repository.TrainerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class TrainerAuthState {

    object Unauthenticated : TrainerAuthState()

    object Idle : TrainerAuthState()

    object Loading : TrainerAuthState()

    object Unverified : TrainerAuthState()

    data class Verified(
        val trainer: Trainer
    ) : TrainerAuthState()

    data class Error(
        val message: String
    ) : TrainerAuthState()
}

@HiltViewModel
class TrainerAuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val trainerRepository: TrainerRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<TrainerAuthState>(
        TrainerAuthState.Unauthenticated
    )
    val authState: StateFlow<TrainerAuthState> = _authState

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        restoreSession()
    }

    /**
     * Restore Firebase session when app restarts
     */
    private fun restoreSession() {

        val user = authRepository.getCurrentUser()

        if (user == null) {
            _authState.value = TrainerAuthState.Unauthenticated
            return
        }

        viewModelScope.launch(Dispatchers.IO) {

            // ADMIN LOGIN
            if (user.email == "admin@fitlink.com") {

                val adminTrainer = Trainer(
                    id = user.uid,
                    name = "Admin",
                    email = user.email ?: "",
                    status = "approved"
                )

                _authState.value =
                    TrainerAuthState.Verified(adminTrainer)

                return@launch
            }

            // TRAINER LOGIN
            val result = trainerRepository.getTrainer(user.uid)

            if (result.isSuccess) {

                val trainer = result.getOrNull()

                if (trainer?.status == "approved") {

                    _authState.value =
                        TrainerAuthState.Verified(trainer)

                } else {

                    _authState.value =
                        TrainerAuthState.Unverified

                }

            } else {

                _authState.value =
                    TrainerAuthState.Unauthenticated
            }
        }
    }
    fun login(email: String, password: String) {

        viewModelScope.launch(Dispatchers.IO) {

            _isLoading.value = true

            val result = authRepository.login(email, password)

            if (result.isSuccess) {

                val user = result.getOrNull()

                // ADMIN LOGIN
                if (email == "admin@fitlink.com") {

                    val adminTrainer = Trainer(
                        id = user!!.uid,
                        name = "Admin",
                        email = email,
                        status = "approved"
                    )

                    _authState.value =
                        TrainerAuthState.Verified(adminTrainer)

                    _isLoading.value = false
                    return@launch
                }

                // TRAINER LOGIN
                val trainerResult =
                    trainerRepository.getTrainer(user!!.uid)

                if (trainerResult.isSuccess) {

                    val trainer = trainerResult.getOrNull()

                    if (trainer?.status == "approved") {

                        _authState.value =
                            TrainerAuthState.Verified(trainer)

                    } else {

                        _authState.value =
                            TrainerAuthState.Unverified

                    }

                } else {

                    _authState.value =
                        TrainerAuthState.Error("Trainer profile not found")

                }

            } else {

                _authState.value =
                    TrainerAuthState.Error(
                        result.exceptionOrNull()?.message ?: "Login failed"
                    )
            }

            _isLoading.value = false
        }
    }
    // Register method (simplified, would need image upload etc.)
    fun register(
        email: String,
        password: String,
        name: String,
        specialties: List<String>,
        certifications: List<String>,

    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _authState.value = TrainerAuthState.Loading
            val authResult = authRepository.register(email, password)
            if (authResult.isSuccess) {
                val firebaseUser = authResult.getOrNull()
                if (firebaseUser != null) {
                    val trainer = Trainer(
                        id = firebaseUser.uid,
                        name = name,
                        email = email,
                        specialties = specialties,
                        certifications = certifications,
                        status = "pending"
                    )
                    val createResult = trainerRepository.createTrainer(trainer)
                    if (createResult.isSuccess) {
                        _authState.value = TrainerAuthState.Unverified
                    } else {
                        _authState.value = TrainerAuthState.Error("Failed to create trainer profile")
                    }
                } else {
                    _authState.value = TrainerAuthState.Error("Registration failed")
                }
            } else {
                _authState.value = TrainerAuthState.Error(authResult.exceptionOrNull()?.message ?: "Registration failed")
            }
        }
    }
    fun resetPassword(email: String) {
        viewModelScope.launch {
            // Call authRepository.sendPasswordResetEmail(email)
////            val result = authRepository.sendPasswordResetEmail(email)
//            if (result.isSuccess) {
//                // Show success message
//            } else {
//                _authState.value = TrainerAuthState.Error(result.exceptionOrNull()?.message ?: "Failed to send reset email")
//            }
        }
    }
    fun logout() {

        authRepository.logout()

        _authState.value = TrainerAuthState.Unauthenticated

    }
}