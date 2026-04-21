package com.example.fitlinktrainer.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlinktrainer.data.model.Trainer
import com.example.fitlinktrainer.data.repository.TrainerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrainerProfileViewModel @Inject constructor(
    private val repository: TrainerRepository
) : ViewModel() {

    private val _trainer = MutableStateFlow<Trainer?>(null)
    val trainer: StateFlow<Trainer?> = _trainer

    private val _uploading = MutableStateFlow(false)
    val uploading: StateFlow<Boolean> = _uploading

    fun loadTrainer(trainerId: String) {
        viewModelScope.launch {
            repository.listenToTrainer(trainerId)
                .collect {
                    _trainer.value = it
                }
        }
    }

    fun updateTrainer(trainer: Trainer) {
        viewModelScope.launch {
            repository.updateTrainer(trainer)
        }
    }

    fun uploadProfileImage(
        context: Context,
        uri: Uri
    ) {

        viewModelScope.launch {

            _uploading.value = true

            val url = repository.uploadProfileImage(context, uri)

            val updated = _trainer.value?.copy(
                profileImageUrl = url
            )

            if (updated != null) {
                repository.updateTrainer(updated)
            }

            _uploading.value = false
        }
    }
}