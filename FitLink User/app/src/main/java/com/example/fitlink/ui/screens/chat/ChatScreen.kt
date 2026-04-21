package com.example.fitlink.ui.screens.chat

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.fitlink.R
import com.example.fitlink.data.models.Chat
import com.example.fitlink.data.models.Message
import com.example.fitlink.data.models.MessageStatus
import com.example.fitlink.data.models.Trainer
import com.example.fitlink.ui.screens.dashboard.*
import com.example.fitlink.ui.viewmodels.ChatViewModel
import com.example.fitlink.ui.viewmodels.TrainersViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    userId: String,
    onChatOpened: (Boolean) -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val selectedChatId by viewModel.selectedChatId.collectAsState()
    val trainersViewModel: TrainersViewModel = hiltViewModel()
    val uiState by trainersViewModel.uiState.collectAsState()
    val videoChannel by viewModel.videoCallChannel.collectAsState()

    LaunchedEffect(userId) {
        viewModel.initialize(userId)
        trainersViewModel.initialize(userId)
    }

    LaunchedEffect(selectedChatId) {
        onChatOpened(selectedChatId != null)
    }

    if (videoChannel != null) {
        VideoCallScreen(
            channelName = videoChannel!!,
            onEndCall = { viewModel.endVideoCall() }
        )
    } else if (selectedChatId == null) {
        ChatListScreen(
            userId = userId,
            trainers = uiState.filteredTrainers,
            onChatSelect = { viewModel.selectChat(it) }
        )
    } else {
        selectedChatId?.let {
            ChatDetailScreen(
                chatId = it,
                userId = userId,
                onBack = { viewModel.clearSelectedChat() }
            )
        }
    }
}

// ==================== CHAT LIST SCREEN (unchanged) ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    userId: String,
    trainers: List<Trainer>,
    onChatSelect: (String) -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val enhancedChats by viewModel.enhancedChats.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }

    val filteredChats by remember(enhancedChats, searchQuery) {
        derivedStateOf {
            if (searchQuery.isBlank()) enhancedChats
            else enhancedChats.filter { (chat, trainer) ->
                trainer.name.contains(searchQuery, ignoreCase = true) ||
                        chat.lastMessage.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Messages",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Text(
                            "Stay connected with your trainers",
                            fontSize = 13.sp,
                            color = TextMedium
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundSoft
                ),
                actions = {
                    IconButton(
                        onClick = { showDialog = true },
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_edit),
                            contentDescription = "New chat",
                            tint = PrimaryGreen
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundSoft)
                .padding(paddingValues)
        ) {
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )

            when {
                isLoading -> LoadingChatList()
                error != null -> ErrorState(
                    error = error!!,
                    onRetry = { viewModel.loadChats() }
                )
                filteredChats.isEmpty() -> EmptyChatList(searchQuery)
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            top = 8.dp,
                            bottom = 80.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = filteredChats,
                            key = { it.first.id }
                        ) { (chat, trainer) ->
                            ChatListItem(
                                chat = chat,
                                trainer = trainer,
                                currentUserId = userId,
                                onClick = { onChatSelect(chat.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Select Trainer") },
            text = {
                Column {
                    trainers.forEach { trainer ->
                        Text(
                            text = trainer.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showDialog = false
                                    viewModel.createNewChat(trainer.id)
                                }
                                .padding(12.dp)
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_search),
                contentDescription = null,
                tint = TextMedium,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    color = TextDark
                ),
                cursorBrush = SolidColor(PrimaryGreen),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (query.isEmpty()) {
                            Text(
                                "Search messages...",
                                fontSize = 16.sp,
                                color = TextLight
                            )
                        }
                        innerTextField()
                    }
                },
                singleLine = true
            )
            if (query.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { onQueryChange("") },
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = "Clear",
                        tint = TextMedium,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatListItem(
    chat: Chat,
    trainer: Trainer,
    currentUserId: String,
    onClick: () -> Unit
) {
    val unreadCount =
        if (chat.userId == currentUserId) chat.unreadCountUser else chat.unreadCountTrainer

    val formattedTime = remember(chat.lastMessageTime) {
        val date = Date(chat.lastMessageTime)
        if (isToday(date)) {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
        } else {
            SimpleDateFormat("MMM dd", Locale.getDefault()).format(date)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(CardWhite)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = trainer.profileImageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = trainer.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark,
                    maxLines = 1
                )

                Text(
                    text = formattedTime,
                    fontSize = 12.sp,
                    color = TextLight
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = chat.lastMessage,
                    fontSize = 14.sp,
                    color = if (unreadCount > 0) TextDark else TextMedium,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )

                if (unreadCount > 0) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(PrimaryGreen, CircleShape)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (unreadCount > 99) "99+" else "$unreadCount",
                            color = Color.White,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }

    Divider(color = Color.LightGray.copy(alpha = 0.3f), thickness = 0.5.dp)
}

// ==================== CHAT DETAIL SCREEN ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    chatId: String,
    userId: String,
    onBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val chats by viewModel.chats.collectAsState()
    val selectedTrainer by viewModel.selectedTrainer.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val chat = chats.firstOrNull { it.id == chatId }
    val videoChannel by viewModel.videoCallChannel.collectAsStateWithLifecycle()
    var showIncomingCallDialog by remember { mutableStateOf(false) }

    LaunchedEffect(chat?.isVideoCalling, chat?.callStatus, chat?.videoChannelId) {
        if (
            chat?.isVideoCalling == true &&
            chat.callStatus == "ringing" &&
            chat.callStartedBy != userId
        ) {
            showIncomingCallDialog = true
        } else {
            showIncomingCallDialog = false
        }
    }

    if (showIncomingCallDialog && chat != null) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Incoming Video Call") },
            text = { Text("Trainer is calling you") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showIncomingCallDialog = false
                        viewModel.acceptVideoCall(chat.id, chat.videoChannelId)
                    }
                ) {
                    Text("Accept")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showIncomingCallDialog = false
                        viewModel.declineVideoCall(chat.id)
                    }
                ) {
                    Text("Decline")
                }
            }
        )
    }

    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var newMessage by remember { mutableStateOf("") }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val onPhoneClick = {
        val phone = selectedTrainer?.phoneNumber ?: ""

        if (phone.isNotBlank()) {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:$phone")
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "Phone not available", Toast.LENGTH_SHORT).show()
        }
    }

    val onVideoClick = {
        viewModel.startVideoCall(chatId)
    }

    if (chat == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PrimaryGreen)
        }
        return
    }

    Scaffold(
        topBar = {
            ChatDetailTopBar(
                chat = chat,
                trainer = selectedTrainer,
                onBack = onBack,
                onPhoneClick = onPhoneClick,
                onVideoClick = onVideoClick
            )
        },
        bottomBar = {
            MessageInputBar(
                message = newMessage,
                onMessageChange = { newMessage = it },
                onSend = {
                    viewModel.sendMessage(chatId, newMessage)
                    newMessage = ""
                    focusManager.clearFocus()
                },
                focusRequester = focusRequester
            )
        },
        containerColor = BackgroundSoft
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (messages.isEmpty() && !isLoading) {
                EmptyMessagesPlaceholder()
            } else {
                MessagesList(
                    messages = messages,
                    currentUserId = userId,
                    listState = listState,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun ChatDetailTopBar(
    chat: Chat,
    trainer: Trainer?,
    onBack: () -> Unit,
    onPhoneClick: () -> Unit,
    onVideoClick: () -> Unit
) {
    val avatarColor = remember(chat.trainerName) {
        val colors = listOf(PrimaryGreen, Color(0xFF3B82F6), Color(0xFFF59E0B), Color(0xFF8B5CF6))
        colors[kotlin.math.abs(chat.trainerName.hashCode()) % colors.size]
    }

    Surface(
        color = CardWhite,
        shadowElevation = 4.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .statusBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back),
                    contentDescription = "Back",
                    tint = TextDark
                )
            }

            Box(modifier = Modifier.size(44.dp)) {
                if (trainer?.profileImageUrl?.isNotEmpty() == true) {
                    AsyncImage(
                        model = trainer.profileImageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Surface(
                        shape = CircleShape,
                        color = avatarColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painterResource(R.drawable.ic_user),
                                contentDescription = null,
                                tint = avatarColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
                if (chat.isOnline) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(PrimaryGreen)
                            .border(2.dp, CardWhite, CircleShape)
                            .align(Alignment.BottomEnd)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = trainer?.name ?: chat.trainerName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark
                )
                Text(
                    text = if (chat.isOnline) "Online" else "Offline",
                    fontSize = 12.sp,
                    color = if (chat.isOnline) PrimaryGreen else TextLight
                )
            }

            IconButton(
                onClick = onPhoneClick,
                enabled = trainer?.phoneNumber?.isNotBlank() == true
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_phone),
                    contentDescription = "Call",
                    tint = PrimaryGreen
                )
            }

            IconButton(onClick = onVideoClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_video),
                    contentDescription = "Video call",
                    tint = PrimaryGreen
                )
            }
        }
    }
}

@Composable
fun MessagesList(
    messages: List<Message>,
    currentUserId: String,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    val groupedMessages = remember(messages) {
        messages.groupBy { message ->
            val cal = Calendar.getInstance()
            cal.timeInMillis = message.timestamp
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
        }.toSortedMap()
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        reverseLayout = false
    ) {
        groupedMessages.forEach { (date, msgs) ->
            item {
                DateSeparator(date = date)
            }
            items(msgs, key = { it.id }) { message ->
                MessageBubble(
                    message = message,
                    isMyMessage = message.senderId == currentUserId
                )
            }
        }
    }
}

@Composable
fun DateSeparator(date: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = LightPurpleBg,
            modifier = Modifier.wrapContentSize()
        ) {
            Text(
                text = when (date) {
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) -> "Today"
                    else -> date
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextMedium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun MessageBubble(message: Message, isMyMessage: Boolean) {
    val formattedTime = remember(message.timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isMyMessage) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMyMessage) 16.dp else 4.dp,
                bottomEnd = if (isMyMessage) 4.dp else 16.dp
            ),
            color = if (isMyMessage) PrimaryGreen else LightPurpleBg,
            modifier = Modifier
                .wrapContentWidth()
                .animateContentSize()
        ) {
            Text(
                text = message.content,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                color = if (isMyMessage) Color.White else TextDark,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }

        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formattedTime,
                fontSize = 11.sp,
                color = TextLight
            )
            if (isMyMessage) {
                Spacer(modifier = Modifier.width(4.dp))
                MessageStatusIcon(status = message.status)
            }
        }
    }
}

@Composable
fun MessageStatusIcon(status: MessageStatus) {
    val (icon, tint) = when (status) {
        MessageStatus.SENT -> R.drawable.ic_check to TextLight
        MessageStatus.DELIVERED -> R.drawable.ic_double_check to TextLight
        MessageStatus.READ -> R.drawable.ic_double_check to PrimaryGreen
        MessageStatus.SENDING -> R.drawable.ic_clock to TextLight
        MessageStatus.FAILED -> R.drawable.ic_close to Color.Red
    }
    Icon(
        painter = painterResource(icon),
        contentDescription = status.name,
        modifier = Modifier.size(12.dp),
        tint = tint
    )
}

@Composable
fun MessageInputBar(
    message: String,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit,
    focusRequester: FocusRequester
) {
    Surface(
        color = CardWhite,
        shadowElevation = 8.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 12.dp,
                    end = 12.dp,
                    top = 8.dp,
                    bottom = 8.dp
                )
                .navigationBarsPadding(),
            verticalAlignment = Alignment.Bottom
        ) {
            IconButton(
                onClick = { /* Attach */ },
                interactionSource = remember { MutableInteractionSource() }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_attach_file),
                    contentDescription = "Attach",
                    tint = TextMedium
                )
            }

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = BackgroundSoft),
                modifier = Modifier.weight(1f)
            ) {
                BasicTextField(
                    value = message,
                    onValueChange = onMessageChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        color = TextDark
                    ),
                    cursorBrush = SolidColor(PrimaryGreen),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (message.isEmpty()) {
                                Text(
                                    "Type a message...",
                                    fontSize = 16.sp,
                                    color = TextLight
                                )
                            }
                            innerTextField()
                        }
                    },
                    maxLines = 5,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (message.isNotBlank()) {
                                onSend()
                            }
                        }
                    )
                )
            }

            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = CircleShape,
                color = if (message.isNotBlank()) PrimaryGreen else TextLight.copy(alpha = 0.2f),
                modifier = Modifier.size(48.dp),
                shadowElevation = if (message.isNotBlank()) 4.dp else 0.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    IconButton(
                        onClick = onSend,
                        enabled = message.isNotBlank(),
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_send),
                            contentDescription = "Send",
                            tint = if (message.isNotBlank()) Color.White else TextLight
                        )
                    }
                }
            }
        }
    }
}

// ==================== EMPTY & ERROR STATES (unchanged) ====================
@Composable
fun EmptyChatList(searchQuery: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = LightPurpleBg,
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_chat_bubble_badged),
                    contentDescription = null,
                    tint = PurpleGradientStart,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (searchQuery.isNotEmpty()) "No matches" else "No conversations yet",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (searchQuery.isNotEmpty())
                "No chats match \"$searchQuery\""
            else
                "Start a conversation with your trainer",
            fontSize = 14.sp,
            color = TextMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun EmptyMessagesPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = LightPurpleBg,
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_message),
                    contentDescription = null,
                    tint = PurpleGradientStart,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No messages yet",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Start the conversation by sending a message",
            fontSize = 14.sp,
            color = TextMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ErrorState(error: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = LightPurpleBg,
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_message_alert),
                    contentDescription = null,
                    tint = PurpleGradientStart,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Something went wrong",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = error,
            fontSize = 14.sp,
            color = TextMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = PurpleGradientStart),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Try Again")
        }
    }
}

@Composable
fun LoadingChatList() {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(5) {
            ShimmerChatItem()
        }
    }
}

@Composable
fun ShimmerChatItem() {
    val shimmerColors = listOf(
        LightPurpleBg.copy(alpha = 0.3f),
        CardWhite,
        LightPurpleBg.copy(alpha = 0.3f)
    )

    val transition = rememberInfiniteTransition()
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing)
        )
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = shimmerColors,
                            startX = translateAnim.value,
                            endX = translateAnim.value + 300f
                        )
                    )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = shimmerColors,
                                startX = translateAnim.value,
                                endX = translateAnim.value + 300f
                            )
                        )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = shimmerColors,
                                startX = translateAnim.value,
                                endX = translateAnim.value + 300f
                            )
                        )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = shimmerColors,
                                startX = translateAnim.value,
                                endX = translateAnim.value + 300f
                            )
                        )
                )
            }
        }
    }
}

private fun isToday(date: Date): Boolean {
    val today = Calendar.getInstance()
    val other = Calendar.getInstance().apply { time = date }
    return today.get(Calendar.YEAR) == other.get(Calendar.YEAR) &&
            today.get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR)
}