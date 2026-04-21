package com.example.fitlinktrainer.ui.screens.messages

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.fitlinktrainer.R
import com.example.fitlinktrainer.data.model.Chat
import com.example.fitlinktrainer.data.model.Message
import com.example.fitlinktrainer.data.model.MessageStatus
import com.example.fitlinktrainer.viewmodel.ChatsUiState
import com.example.fitlinktrainer.viewmodel.ClientsUiState
import com.example.fitlinktrainer.viewmodel.MessagesUiState
import com.example.fitlinktrainer.viewmodel.MessagesViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    trainerId: String,
    viewModel: MessagesViewModel = hiltViewModel()
) {
    val chatsUiState by viewModel.chatsUiState.collectAsStateWithLifecycle()
    val selectedChat by viewModel.selectedChatFlow.collectAsStateWithLifecycle()  // real-time
    val messagesUiState by viewModel.messagesUiState.collectAsStateWithLifecycle()
    val videoChannel by viewModel.videoCallChannel.collectAsStateWithLifecycle()
    var showNewChatDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(trainerId) {
        viewModel.loadChats(trainerId)
    }

    // Handle incoming call status changes
    LaunchedEffect(selectedChat?.callStatus, selectedChat?.videoChannelId) {
        val chat = selectedChat
        if (chat != null) {
            when {
                chat.callStatus == "accepted" && chat.videoChannelId.isNotBlank() && videoChannel == null -> {
                    viewModel.joinVideoCall(chat.videoChannelId)
                }
                chat.callStatus == "ended" && videoChannel != null -> {
                    viewModel.endVideoCall()
                }
            }
        }
    }

    // Incoming call dialog
    var showIncomingCallDialog by remember { mutableStateOf(false) }
    LaunchedEffect(selectedChat?.isVideoCalling, selectedChat?.callStatus, selectedChat?.callStartedBy) {
        val chat = selectedChat
        showIncomingCallDialog = chat != null &&
                chat.isVideoCalling &&
                chat.callStatus == "ringing" &&
                chat.callStartedBy != trainerId
    }

    if (showIncomingCallDialog && selectedChat != null) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Incoming Video Call") },
            text = { Text("${selectedChat!!.userName} is calling you") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showIncomingCallDialog = false
                        viewModel.acceptVideoCall(selectedChat!!.id, selectedChat!!.videoChannelId)
                    }
                ) {
                    Text("Accept")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showIncomingCallDialog = false
                        viewModel.declineVideoCall(selectedChat!!.id)
                    }
                ) {
                    Text("Decline")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        content = { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                if (videoChannel != null) {
                    VideoCallScreen(
                        channelName = videoChannel!!,
                        onEndCall = { viewModel.endVideoCall() }
                    )
                } else if (selectedChat == null) {
                    ChatListScreen(
                        uiState = chatsUiState,
                        onChatClick = { viewModel.openChat(it) },
                        onNewChatClick = { showNewChatDialog = true },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    ChatDetailScreen(
                        chat = selectedChat!!,
                        uiState = messagesUiState,
                        currentUserId = trainerId,
                        onBack = { viewModel.closeChat() },
                        onSend = { viewModel.sendMessage(it) },
                        onVideoCall = { chatId ->
                            viewModel.startVideoCall(chatId)
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedChat == null) {
                FloatingActionButton(
                    onClick = { showNewChatDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add),
                        contentDescription = "New Chat"
                    )
                }
            }
        }
    )

    if (showNewChatDialog) {
        NewChatDialog(
            onDismiss = { showNewChatDialog = false },
            onClientSelected = { clientId ->
                viewModel.createChat(clientId) {
                    showNewChatDialog = false
                }
            },
            viewModel = viewModel
        )
    }
}

// ========== Chat List (unchanged) ==========
@Composable
fun ChatListScreen(
    uiState: ChatsUiState,
    onChatClick: (Chat) -> Unit,
    onNewChatClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is ChatsUiState.Loading -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is ChatsUiState.Success -> {
            if (uiState.chats.isEmpty()) {
                Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No conversations yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = onNewChatClick) {
                            Text("Start a new chat")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = modifier,
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(uiState.chats) { chat ->
                        ChatListItem(chat) { onChatClick(chat) }
                    }
                }
            }
        }
        is ChatsUiState.Error -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Error: ${uiState.message}",
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { /* retry */ }) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}

@Composable
fun ChatListItem(chat: Chat, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (!chat.userImage.isNullOrEmpty()) {
                    AsyncImage(
                        model = chat.userImage,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = chat.userName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chat.userName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = chat.lastMessage.ifEmpty { "No messages yet" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatTimestamp(chat.lastMessageTime),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (chat.unreadCountTrainer > 0) {
                    Badge(
                        modifier = Modifier.padding(top = 4.dp),
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = chat.unreadCountTrainer.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

// ========== Enhanced Chat Detail (unchanged) ==========
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    chat: Chat,
    uiState: MessagesUiState,
    currentUserId: String,
    onBack: () -> Unit,
    onSend: (String) -> Unit,
    onVideoCall: (String) -> Unit
) {
    val context = LocalContext.current
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(uiState) {
        if (uiState is MessagesUiState.Success && uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    // Simulate online status based on last message time (updates every minute)
    val onlineStatus by produceState(initialValue = "Offline") {
        while (true) {
            val lastMsgTime = (uiState as? MessagesUiState.Success)?.messages?.lastOrNull()?.timestamp ?: 0L
            value = if (System.currentTimeMillis() - lastMsgTime < 2 * 60 * 1000) "Online"
            else "Last seen ${formatLastSeen(lastMsgTime)}"
            delay(60_000)
        }
    }

    Column(Modifier.fillMaxSize()) {
        // WhatsApp-style TopAppBar
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!chat.userImage.isNullOrEmpty()) {
                            AsyncImage(
                                model = chat.userImage,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = chat.userName.take(1).uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = chat.userName,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = onlineStatus,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = "Back"
                    )
                }
            },
            actions = {
                // Phone call – requires chat.phoneNumber (populated by ViewModel)
                IconButton(onClick = {
                    val phoneNumber = chat.phoneNumber
                    if (phoneNumber.isNullOrBlank()) {
                        Toast.makeText(context, "Phone number not available", Toast.LENGTH_SHORT).show()
                    } else {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
                        context.startActivity(intent)
                    }
                }) {
                    Icon(painter = painterResource(R.drawable.ic_phone), contentDescription = "Call")
                }
                // Video call
                IconButton(onClick = {
                    onVideoCall(chat.id)
                }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_video),
                        contentDescription = "Video call"
                    )
                }
            }
        )

        // Messages area
        Box(Modifier.weight(1f)) {
            when (uiState) {
                is MessagesUiState.Loading -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
                is MessagesUiState.Success -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        reverseLayout = false
                    ) {
                        items(
                            items = uiState.messages,
                            key = { it.id }
                        ) { message ->
                            AnimatedContent(
                                targetState = message,
                                transitionSpec = {
                                    fadeIn(
                                        animationSpec = tween(300)
                                    ) togetherWith fadeOut(
                                        animationSpec = tween(150)
                                    )
                                },
                                label = "messageAnimation"
                            ) { targetMessage ->
                                MessageBubble(
                                    message = targetMessage,
                                    isMe = targetMessage.senderId == currentUserId
                                )
                            }
                        }
                    }
                }
                else -> {}
            }
        }

        // Message input bar (WhatsApp style)
        Row(
            Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { /* TODO: attachment */ }) {
                Icon(
                    painter = painterResource(R.drawable.ic_attach_file),
                    contentDescription = "Attach"
                )
            }

            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message") },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.width(8.dp))

            var sendButtonScale by remember { mutableStateOf(1f) }
            FloatingActionButton(
                onClick = {
                    if (messageText.isNotBlank()) {
                        onSend(messageText)
                        messageText = ""
                        coroutineScope.launch {
                            sendButtonScale = 1.2f
                            delay(100)
                            sendButtonScale = 1f
                        }
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .scale(sendButtonScale),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_send),
                    contentDescription = "Send"
                )
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: Message,
    isMe: Boolean
) {
    val bubbleColor = if (isMe) Color(0xFFDCF8C6) else Color.White
    val alignment = if (isMe) Alignment.End else Alignment.Start
    val cornerShape = if (isMe) {
        RoundedCornerShape(18.dp, 4.dp, 18.dp, 18.dp)
    } else {
        RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp)
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalAlignment = alignment
    ) {
        Surface(
            shape = cornerShape,
            color = bubbleColor,
            shadowElevation = 2.dp
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isMe) Color.Black else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                ) {
                    Text(
                        text = formatTime(message.timestamp),
                        fontSize = 11.sp,
                        color = if (isMe) Color.DarkGray else Color.Gray
                    )
                    if (isMe && message.status == MessageStatus.READ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_double_check),
                            contentDescription = "Read",
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF34B7F1)
                        )
                    }
                }
            }
        }
    }
}

// ========== New Chat Dialog (unchanged) ==========
@Composable
fun NewChatDialog(
    onDismiss: () -> Unit,
    onClientSelected: (String) -> Unit,
    viewModel: MessagesViewModel
) {
    val clientsUiState by viewModel.clientsUiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadClients()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Start a new chat") },
        text = {
            when (val state = clientsUiState) {
                is ClientsUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is ClientsUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                    ) {
                        items(state.users) { user ->
                            ListItem(
                                headlineContent = { Text(user.name) },
                                supportingContent = { Text(user.email) },
                                modifier = Modifier.clickable {
                                    onClientSelected(user.id)
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                }
                is ClientsUiState.Error -> {
                    Text(
                        text = "Error: ${state.message}",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                ClientsUiState.Initial -> {}
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ========== Helper functions (unchanged) ==========
private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "Now"
        diff < 3600_000 -> "${diff / 60_000}m"
        diff < 86400_000 -> "${diff / 3600_000}h"
        else -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(timestamp))
    }
}

private fun formatTime(timestamp: Long): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
}

private fun formatLastSeen(timestamp: Long): String {
    val today = Calendar.getInstance()
    val msgDate = Calendar.getInstance().apply { timeInMillis = timestamp }
    return if (today.get(Calendar.DAY_OF_YEAR) == msgDate.get(Calendar.DAY_OF_YEAR)) {
        "today at ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))}"
    } else {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}