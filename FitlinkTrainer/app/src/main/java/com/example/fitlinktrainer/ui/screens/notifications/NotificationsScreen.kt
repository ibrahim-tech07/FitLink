package com.example.fitlinktrainer.ui.screens.notifications



import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitlinktrainer.R
import com.example.fitlinktrainer.ui.screens.dashboard.formatTimeAgo
import com.example.fitlinktrainer.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit
) {
    val notifications = remember { generateMockNotifications() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painter = painterResource(R.drawable.ic_arrow_back), contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Background,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = Background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(notifications) { notification ->
                NotificationItem(notification)
            }
        }
    }
}

@Composable
fun NotificationItem(notification: Notification) {
    val interactionSource = remember { MutableInteractionSource() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null  // no ripple
            ) {
                // handle notification click
            },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .animateContentSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = when (notification.type) {
                    "message" -> PrimaryBlue.copy(alpha = 0.1f)
                    "streak" -> Success.copy(alpha = 0.1f)
                    "payment" -> Warning.copy(alpha = 0.1f)
                    else -> Surface.copy(alpha = 0.1f)
                },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    painter = painterResource(
                        when (notification.type) {
                            "message" -> R.drawable.ic_chat
                            "streak" -> R.drawable.ic_fitness_center
                            "payment" -> R.drawable.ic_attach_money
                            else -> R.drawable.ic_bell
                        }
                    ),
                    contentDescription = null,
                    tint = when (notification.type) {
                        "message" -> PrimaryBlue
                        "streak" -> Success
                        "payment" -> Warning
                        else -> TextSecondary
                    },
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.CenterVertically)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = notification.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2
                )
            }
            Text(
                text = formatTimeAgo(notification.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}

data class Notification(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    val timestamp: Long
)

fun generateMockNotifications(): List<Notification> {
    val now = System.currentTimeMillis()
    return listOf(
        Notification("1", "message", "New message from John", "Hey coach, need help with my form", now - 15 * 60 * 1000),
        Notification("2", "streak", "Streak alert", "Sara hasn't logged a workout today", now - 2 * 60 * 60 * 1000),
        Notification("3", "payment", "Payment received", "Mike paid for August package", now - 24 * 60 * 60 * 1000),
        Notification("4", "message", "New message from Lisa", "Can we reschedule?", now - 2 * 24 * 60 * 60 * 1000)
    )
}