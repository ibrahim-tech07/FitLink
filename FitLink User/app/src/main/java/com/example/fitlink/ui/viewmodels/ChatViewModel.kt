package com.example.fitlink.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlink.data.models.Chat
import com.example.fitlink.data.models.Message
import com.example.fitlink.data.models.Trainer
import com.example.fitlink.data.repositories.ChatRepository
import com.example.fitlink.data.repositories.TrainerRepository
import com.example.fitlink.data.repositories.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val trainerRepository: TrainerRepository
) : ViewModel() {

    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats.asStateFlow()

    private val _enhancedChats = MutableStateFlow<List<Pair<Chat, Trainer>>>(emptyList())
    val enhancedChats: StateFlow<List<Pair<Chat, Trainer>>> = _enhancedChats.asStateFlow()

    private val _videoCallChannel = MutableStateFlow<String?>(null)
    val videoCallChannel: StateFlow<String?> = _videoCallChannel

    private val _selectedChatId = MutableStateFlow<String?>(null)
    val selectedChatId: StateFlow<String?> = _selectedChatId.asStateFlow()

    // Real-time selected chat flow
    private val _selectedChatFlow = MutableStateFlow<Chat?>(null)
    val selectedChatFlow: StateFlow<Chat?> = _selectedChatFlow

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _selectedTrainer = MutableStateFlow<Trainer?>(null)
    val selectedTrainer: StateFlow<Trainer?> = _selectedTrainer.asStateFlow()

    private var currentUserId: String = ""
    private var messageJob: Job? = null
    private var selectedChatJob: Job? = null

    private val trainerCache = mutableMapOf<String, Trainer>()

    fun initialize(userId: String) {
        currentUserId = userId
        loadChats()
    }

    fun loadChats() {
        viewModelScope.launch {
            chatRepository.getChatsStream(currentUserId)
                .catch { exception ->
                    _error.value = "Failed to load chats: ${exception.message}"
                }
                .collect { chats ->
                    _chats.value = chats.sortedByDescending { it.lastMessageTime }
                    enhanceChatsWithTrainers(chats)
                }
        }
    }

    private suspend fun enhanceChatsWithTrainers(chats: List<Chat>) {
        val enhanced = mutableListOf<Pair<Chat, Trainer>>()
        for (chat in chats) {
            val trainer = getTrainerWithCache(chat.trainerId)
            if (trainer != null) {
                enhanced.add(chat to trainer)
            }
        }
        _enhancedChats.value = enhanced.sortedByDescending { it.first.lastMessageTime }
    }

    private suspend fun getTrainerWithCache(trainerId: String): Trainer? {
        return trainerCache[trainerId] ?: run {
            val trainer = trainerRepository.getTrainerById(trainerId)
            trainer?.let { trainerCache[trainerId] = it }
            trainer
        }
    }

    fun selectChat(chatId: String) {
        _selectedChatId.value = chatId

        // Start listening to real-time updates for this chat
        selectedChatJob?.cancel()
        selectedChatJob = viewModelScope.launch {
            chatRepository.listenChat(chatId)
                .collect { updatedChat ->
                    _selectedChatFlow.value = updatedChat
                }
        }

        messageJob?.cancel()
        messageJob = chatRepository
            .getMessagesStream(chatId)
            .onEach { _messages.value = it }
            .launchIn(viewModelScope)

        markMessagesAsRead(chatId)

        viewModelScope.launch {
            val chat = _chats.value.find { it.id == chatId }
            chat?.trainerId?.let { trainerId ->
                val trainer = getTrainerWithCache(trainerId)
                _selectedTrainer.value = trainer
            }
        }
    }

    fun sendMessage(chatId: String, content: String) {
        if (content.isBlank()) return

        viewModelScope.launch {
            chatRepository.sendMessage(chatId, currentUserId, content)
                .onFailure {
                    _error.value = it.message
                }
        }
    }

    private fun markMessagesAsRead(chatId: String) {
        viewModelScope.launch {
            chatRepository.markMessagesAsRead(chatId, currentUserId)
                .onSuccess {
                    // Update unread count in chat list
                }
        }
    }

    fun createNewChat(trainerId: String) {
        viewModelScope.launch {
            userRepository.getCurrentUser()
                .onSuccess { user ->
                    val connectedTrainerId = user?.connectedTrainerId
                    if (connectedTrainerId == null) {
                        _error.value = "No trainer connected"
                        return@onSuccess
                    }
                    if (trainerId != connectedTrainerId) {
                        _error.value = "You can only chat with your assigned trainer"
                        return@onSuccess
                    }

                    val trainer = trainerRepository.getTrainerById(trainerId)
                    if (trainer == null) {
                        _error.value = "Trainer not found"
                        return@onSuccess
                    }

                    chatRepository.createChat(
                        currentUserId,
                        trainerId,
                        trainer.name,
                        trainer.profileImageUrl
                    ).onSuccess { chatId ->
                        selectChat(chatId)
                    }.onFailure {
                        _error.value = it.message
                    }
                }
                .onFailure {
                    _error.value = it.message
                }
        }
    }

    fun getUnreadCount(): Flow<Int> = flow {
        val result = chatRepository.getUnreadCount(currentUserId)
        result.onSuccess { count ->
            emit(count)
        }.onFailure {
            emit(0)
        }
    }

    fun startVideoCall(chatId: String) {
        viewModelScope.launch {
            chatRepository.startVideoCall(chatId, currentUserId)
                .onSuccess { channelId ->
                    // The listener will update
                }
                .onFailure {
                    _error.value = it.message
                }
        }
    }

    fun joinVideoCall(channelId: String) {
        _videoCallChannel.value = channelId
    }

    fun acceptVideoCall(chatId: String, channelId: String) {
        viewModelScope.launch {
            chatRepository.acceptVideoCall(chatId)
                .onSuccess {
                    // The listener will set the channel
                }
                .onFailure {
                    _error.value = it.message
                }
        }
    }

    fun declineVideoCall(chatId: String) {
        viewModelScope.launch {
            chatRepository.declineVideoCall(chatId)
                .onSuccess {
                    // Listener will clear
                }
                .onFailure {
                    _error.value = it.message
                }
        }
    }

    fun endVideoCall() {
        val chatId = _selectedChatId.value ?: return

        viewModelScope.launch {
            chatRepository.endVideoCall(chatId)
                .onSuccess {
                    _videoCallChannel.value = null
                }
                .onFailure {
                    _error.value = it.message
                }
        }
    }

    fun clearSelectedChat() {
        selectedChatJob?.cancel()
        _selectedChatId.value = null
        _selectedChatFlow.value = null
        _messages.value = emptyList()
        _selectedTrainer.value = null
    }

    fun clearError() {
        _error.value = null
    }
}