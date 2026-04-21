package com.example.fitlinktrainer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlinktrainer.data.model.Chat
import com.example.fitlinktrainer.data.model.Message
import com.example.fitlinktrainer.data.model.User
import com.example.fitlinktrainer.data.repository.ChatRepository
import com.example.fitlinktrainer.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MessagesViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _chatsUiState = MutableStateFlow<ChatsUiState>(ChatsUiState.Loading)
    val chatsUiState: StateFlow<ChatsUiState> = _chatsUiState

    private val _selectedChat = MutableStateFlow<Chat?>(null)
    val selectedChat: StateFlow<Chat?> = _selectedChat  // kept for backward compatibility

    // Real-time selected chat flow
    private val _selectedChatFlow = MutableStateFlow<Chat?>(null)
    val selectedChatFlow: StateFlow<Chat?> = _selectedChatFlow

    private val _messagesUiState = MutableStateFlow<MessagesUiState>(MessagesUiState.Initial)
    val messagesUiState: StateFlow<MessagesUiState> = _messagesUiState

    private val _clientsUiState = MutableStateFlow<ClientsUiState>(ClientsUiState.Initial)
    val clientsUiState: StateFlow<ClientsUiState> = _clientsUiState

    private val _videoCallChannel = MutableStateFlow<String?>(null)
    val videoCallChannel: StateFlow<String?> = _videoCallChannel

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var currentTrainerId: String = ""
    private var selectedChatJob: Job? = null

    fun loadChats(trainerId: String) {
        currentTrainerId = trainerId

        viewModelScope.launch {
            _chatsUiState.value = ChatsUiState.Loading

            chatRepository.listenTrainerChats(trainerId)
                .catch { e ->
                    _chatsUiState.value = ChatsUiState.Error(e.message ?: "Unknown error")
                    _snackbarMessage.emit("Failed to load chats: ${e.message}")
                }
                .collect { chats ->
                    _chatsUiState.value = ChatsUiState.Success(chats)

                    val currentSelectedId = _selectedChatFlow.value?.id
                    if (currentSelectedId != null) {
                        _selectedChatFlow.value = chats.firstOrNull { it.id == currentSelectedId }
                    }
                }
        }
    }

    fun openChat(chat: Chat) {
        _selectedChat.value = chat
        _selectedChatFlow.value = chat

        // Start listening to real-time updates for this chat
        selectedChatJob?.cancel()
        selectedChatJob = viewModelScope.launch {
            chatRepository.listenChat(chat.id)
                .collect { updatedChat ->
                    _selectedChatFlow.value = updatedChat
                    // Also update in list if needed
                }
        }

        viewModelScope.launch {
            _messagesUiState.value = MessagesUiState.Loading

            chatRepository.listenMessages(chat.id)
                .catch { e ->
                    _messagesUiState.value = MessagesUiState.Error(e.message ?: "Unknown error")
                    _snackbarMessage.emit("Failed to load messages: ${e.message}")
                }
                .collect { messages ->
                    _messagesUiState.value = MessagesUiState.Success(messages)

                    chatRepository.markChatAsRead(chat.id, isTrainer = true)
                        .onFailure { e ->
                            _snackbarMessage.emit("Failed to mark chat as read: ${e.message}")
                        }
                }
        }
    }

    fun closeChat() {
        selectedChatJob?.cancel()
        _selectedChat.value = null
        _selectedChatFlow.value = null
        _messagesUiState.value = MessagesUiState.Initial
        _videoCallChannel.value = null
    }

    fun sendMessage(text: String) {
        val chat = _selectedChatFlow.value ?: return

        viewModelScope.launch {
            chatRepository.sendMessage(chat.id, currentTrainerId, text)
        }
    }

    fun loadClients() {
        viewModelScope.launch {
            _clientsUiState.value = ClientsUiState.Loading

            userRepository.listenToClients(currentTrainerId)
                .catch { e ->
                    _clientsUiState.value = ClientsUiState.Error(e.message ?: "Unknown error")
                    _snackbarMessage.emit("Failed to load users: ${e.message}")
                }
                .collect { users ->
                    _clientsUiState.value = ClientsUiState.Success(users)
                }
        }
    }

    fun startVideoCall(chatId: String) {
        viewModelScope.launch {
            chatRepository.startVideoCall(chatId, currentTrainerId)
                .onSuccess { channelId ->
                    // No need to set _videoCallChannel here; it will be set by the listener
                }
                .onFailure {
                    _snackbarMessage.emit(it.message ?: "Failed to start video call")
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
                    _snackbarMessage.emit(it.message ?: "Failed to accept call")
                }
        }
    }

    fun declineVideoCall(chatId: String) {
        viewModelScope.launch {
            chatRepository.declineVideoCall(chatId)
                .onSuccess {
                    // The listener will clear the channel
                }
                .onFailure {
                    _snackbarMessage.emit(it.message ?: "Failed to decline call")
                }
        }
    }

    fun endVideoCall() {
        val chatId = _selectedChatFlow.value?.id ?: return

        viewModelScope.launch {
            chatRepository.endVideoCall(chatId)
                .onSuccess {
                    _videoCallChannel.value = null
                }
                .onFailure {
                    _snackbarMessage.emit(it.message ?: "Failed to end call")
                }
        }
    }

    fun createChat(userId: String, onChatCreated: (Chat) -> Unit) {
        viewModelScope.launch {
            try {
                val userResult = userRepository.getUser(userId)
                val user = userResult.getOrNull()
                if (userResult.isFailure || user == null) {
                    _snackbarMessage.emit("Failed to fetch user details")
                    return@launch
                }

                if (user.connectedTrainerId != currentTrainerId) {
                    _snackbarMessage.emit("User is not your client")
                    return@launch
                }

                val trainerName = ""
                val trainerImage = ""
                val userName = user.name

                val result = chatRepository.createChat(
                    currentTrainerId,
                    userId,
                    trainerName,
                    trainerImage,
                    userName
                )

                result.onSuccess { chatId ->
                    val chat = Chat(
                        id = chatId,
                        trainerId = currentTrainerId,
                        userId = userId,
                        userName = userName
                    )
                    openChat(chat)
                    onChatCreated(chat)
                }.onFailure { e ->
                    _snackbarMessage.emit("Failed to create chat: ${e.message}")
                }
            } catch (e: Exception) {
                _snackbarMessage.emit("Failed to create chat: ${e.message}")
            }
        }
    }
}

sealed class ChatsUiState {
    object Loading : ChatsUiState()
    data class Success(val chats: List<Chat>) : ChatsUiState()
    data class Error(val message: String) : ChatsUiState()
}

sealed class MessagesUiState {
    object Initial : MessagesUiState()
    object Loading : MessagesUiState()
    data class Success(val messages: List<Message>) : MessagesUiState()
    data class Error(val message: String) : MessagesUiState()
}

sealed class ClientsUiState {
    object Initial : ClientsUiState()
    object Loading : ClientsUiState()
    data class Success(val users: List<User>) : ClientsUiState()
    data class Error(val message: String) : ClientsUiState()
}