package com.example.fitlink.ui.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlink.data.models.AchievementModel
import com.example.fitlink.data.models.User
import com.example.fitlink.data.service.CloudinaryService
import com.example.fitlink.data.service.FirebaseService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val firebaseService: FirebaseService,
    private val cloudinaryService: CloudinaryService
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _achievements = MutableStateFlow<List<AchievementModel>>(emptyList())
    val achievements: StateFlow<List<AchievementModel>> = _achievements.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _uploadProgress = MutableStateFlow(0f)
    val uploadProgress: StateFlow<Float> = _uploadProgress.asStateFlow()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun loadUserData(userId: String) {

        _isLoading.value = true

        firebaseService.listenToUser(userId)
            .onEach { _user.value = it }
            .launchIn(viewModelScope)

        firebaseService.listenToUserAchievements(userId)
            .onEach { _achievements.value = it }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            delay(500)
            _isLoading.value = false
        }
    }

    fun updateUser(userId: String, updatedUser: User) {

        viewModelScope.launch {

            _isLoading.value = true

            val result = firebaseService.updateUser(
                userId,
                mapOf(
                    "name" to updatedUser.name,
                    "age" to updatedUser.age,
                    "weight" to updatedUser.weight,
                    "height" to updatedUser.height,
                    "fitnessGoal" to updatedUser.fitnessGoal
                )
            )

            if (result.isSuccess) {
                _saveSuccess.value = true
            } else {
                _errorMessage.value =
                    "Failed to update profile: ${result.exceptionOrNull()?.message}"
            }

            _isLoading.value = false
        }
    }

    fun uploadProfileImage(userId: String, uri: Uri) {

        _isLoading.value = true
        _uploadProgress.value = 0.2f

        cloudinaryService.uploadImage(
            imageUri = uri,

            onSuccess = { imageUrl ->

                viewModelScope.launch {

                    firebaseService.updateUser(
                        userId,
                        mapOf("profileImageUrl" to imageUrl)
                    )

                    _uploadProgress.value = 1f

                    delay(400)

                    _uploadProgress.value = 0f
                    _isLoading.value = false
                }
            },

            onError = { error ->

                viewModelScope.launch {

                    _errorMessage.value = error
                    _uploadProgress.value = 0f
                    _isLoading.value = false
                }
            }
        )
    }

    fun resetMessages() {
        _saveSuccess.value = false
        _errorMessage.value = null
    }
}