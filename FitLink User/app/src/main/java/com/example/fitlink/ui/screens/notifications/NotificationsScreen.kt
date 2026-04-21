package com.example.fitlink.ui.screens.notifications

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.fitlink.R
import com.example.fitlink.ui.screens.dashboard.PurpleGradientStart
import com.example.fitlink.ui.screens.dashboard.PurpleGradientEnd
import com.example.fitlink.ui.screens.dashboard.CardWhite
import com.example.fitlink.ui.screens.dashboard.TextDark
import com.example.fitlink.ui.screens.dashboard.TextMedium
import com.example.fitlink.ui.screens.dashboard.LightGreenAccent
import com.example.fitlink.ui.screens.dashboard.TextLight
import com.example.fitlink.ui.viewmodels.NotificationsViewModel
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.draw.shadow
import com.example.fitlink.data.models.Notification
import com.example.fitlink.ui.screens.dashboard.BackgroundSoft
import com.example.fitlink.ui.screens.dashboard.PrimaryGreen


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    userId: String,
    onBack: () -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    LaunchedEffect(userId) {
        viewModel.initialize(userId)
    }

    val notifications by viewModel.filteredNotifications.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()

    var selectedNotification by remember { mutableStateOf<Notification?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundSoft)
    ) {

        Column {

            // ===== HEADER =====
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(PurpleGradientStart, PurpleGradientEnd)
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Row(verticalAlignment = Alignment.CenterVertically) {

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onBack() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_back),
                                contentDescription = null,
                                tint = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = "Notifications",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            AnimatedContent(
                                targetState = unreadCount,
                                label = ""
                            ) { count ->
                                Text(
                                    text = "$count unread",
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    if (unreadCount > 0) {
                        Text(
                            text = "Mark all",
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { viewModel.markAllAsRead() }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ===== FILTER =====
            ModernFilterRow(
                selectedType = selectedType,
                onTypeSelected = { viewModel.filterByType(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ===== CONTENT =====
            when {
                isLoading -> ShimmerNotificationList()

                notifications.isEmpty() -> EmptyNotificationsPlaceholder()

                else -> {
                    AnimatedContent(
                        targetState = notifications,
                        label = ""
                    ) { list ->

                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(list, key = { it.id }) { notification ->
                                SwipeableNotificationItem(
                                    notification = notification,
                                    onMarkRead = {
                                        viewModel.markAsRead(notification.id)
                                    },
                                    onDelete = {
                                        viewModel.deleteNotification(notification.id)
                                    },
                                    onClick = {
                                        selectedNotification = notification
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // ===== BOTTOM SHEET =====
        if (selectedNotification != null) {
            ModalBottomSheet(
                onDismissRequest = { selectedNotification = null }
            ) {
                Column(Modifier.padding(24.dp)) {

                    Text(
                        text = selectedNotification!!.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(selectedNotification!!.message)

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        formatTimeAgo(selectedNotification!!.timestamp),
                        color = TextMedium
                    )
                }
            }
        }
    }
}
@Composable
fun ModernFilterRow(
    selectedType: String?,
    onTypeSelected: (String?) -> Unit
) {
    val types = listOf(
        "All" to null,
        "Workout" to "WORKOUT_REMINDER",
        "Achievement" to "ACHIEVEMENT_UNLOCKED",
        "Message" to "TRAINER_MESSAGE",
        "Streak" to "STREAK_ALERT"
    )

    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        types.forEach { (label, type) ->

            val isSelected = selectedType == type

            val backgroundColor by animateColorAsState(
                if (isSelected) PurpleGradientStart else LightGreenAccent
            )

            val textColor by animateColorAsState(
                if (isSelected) Color.White else TextDark
            )

            Box(
                modifier = Modifier
                    .shadow(
                        elevation = if (isSelected) 6.dp else 0.dp,
                        shape = RoundedCornerShape(30.dp)
                    )
                    .clip(RoundedCornerShape(30.dp))
                    .background(backgroundColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onTypeSelected(type) }
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            ) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableNotificationItem(
    notification: Notification,
    onMarkRead: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            when (it) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onMarkRead()
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    false
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color = when (dismissState.targetValue) {
                SwipeToDismissBoxValue.StartToEnd -> PrimaryGreen
                SwipeToDismissBoxValue.EndToStart -> Color.Red
                else -> Color.Transparent
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = if (dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd)
                        "Mark as Read"
                    else
                        "Delete",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    ) {
        ModernNotificationItem(notification, onClick)
    }
}
@Composable
fun ModernNotificationItem(
    notification: com.example.fitlink.data.models.Notification,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Gradient accent icon container
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(PurpleGradientStart, PurpleGradientEnd)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_bell),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {

                Row(verticalAlignment = Alignment.CenterVertically) {

                    Text(
                        text = notification.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextDark
                    )

                    if (!notification.isRead) {
                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(PurpleGradientStart)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = notification.message,
                    fontSize = 14.sp,
                    color = TextMedium
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = formatTimeAgo(notification.timestamp),
                    fontSize = 12.sp,
                    color = TextLight
                )
            }
        }
    }
}

@Composable
fun EmptyNotificationsPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_bell),
                contentDescription = null,
                tint = TextLight,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No notifications yet",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = TextDark
            )
            Text(
                text = "We'll notify you when something arrives",
                fontSize = 14.sp,
                color = TextMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
@Composable
fun ShimmerNotificationList() {
    repeat(5) {
        Box(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .height(90.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.LightGray.copy(alpha = 0.3f))
        )
    }
}
fun formatTimeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60000 -> "Just now"
        diff < 3600000 -> "${diff / 60000}m ago"
        diff < 86400000 -> "${diff / 3600000}h ago"
        diff < 604800000 -> "${diff / 86400000}d ago"
        else -> java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
    }
}