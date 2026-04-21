package com.example.fitlinktrainer.viewmodel



import androidx.lifecycle.ViewModel
import com.example.fitlinktrainer.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    fun logout() {
        authRepository.logout()
    }
}