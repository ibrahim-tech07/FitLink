

package com.example.fitlink.ui.screens.settings

import android.app.Activity
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.fitlink.R
import com.example.fitlink.ui.viewmodels.NotificationSettingsViewModel
import com.example.fitlink.utlis.NotificationHelper
import com.example.fitlink.utlis.NotificationPermissionHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    userId: String,
    onBack: () -> Unit,
    viewModel: NotificationSettingsViewModel = hiltViewModel()
) {

    LaunchedEffect(userId) {
        viewModel.initialize(userId)
    }

    val settings by viewModel.settings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val context = LocalContext.current
    val hasPermission =
        NotificationPermissionHandler.hasNotificationPermission(context)

    var showTimePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painterResource(id = R.drawable.ic_arrow_back),
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                item {

                    Card(Modifier.fillMaxWidth()) {

                        Column(Modifier.padding(16.dp)) {

                            if (!hasPermission) {
                                PermissionWarning(
                                    context = context
                                )
                                Spacer(Modifier.height(16.dp))
                            }

                            NotificationToggleItem(
                                "Workout Reminders",
                                "Daily workout reminder",
                                settings.workoutReminders
                            ) {
                                viewModel.updateSettings(
                                    settings.copy(workoutReminders = it)
                                )
                            }

                            if (settings.workoutReminders) {
                                ReminderTimeSection(
                                    hour = settings.reminderHour,
                                    minute = settings.reminderMinute
                                ) {
                                    showTimePicker = true
                                }
                            }

                            DividerSection()

                            NotificationToggleItem(
                                "Achievement Alerts",
                                "Unlock achievement alerts",
                                settings.achievementAlerts
                            ) {
                                viewModel.updateSettings(
                                    settings.copy(achievementAlerts = it)
                                )
                            }

                            DividerSection()

                            NotificationToggleItem(
                                "Message Alerts",
                                "Trainer message notifications",
                                settings.messageAlerts
                            ) {
                                viewModel.updateSettings(
                                    settings.copy(messageAlerts = it)
                                )
                            }

                            DividerSection()

                            NotificationToggleItem(
                                "Streak Alerts",
                                "Workout streak updates",
                                settings.streakAlerts
                            ) {
                                viewModel.updateSettings(
                                    settings.copy(streakAlerts = it)
                                )
                            }

                            DividerSection()

                            NotificationToggleItem(
                                "Goal Completion Alerts",
                                "Goal achievement notifications",
                                settings.goalAlerts
                            ) {
                                viewModel.updateSettings(
                                    settings.copy(goalAlerts = it)
                                )
                            }

                            DividerSection()

                            NotificationToggleItem(
                                "Marketing & Promotions",
                                "App updates & offers",
                                settings.marketingAlerts
                            ) {
                                viewModel.updateSettings(
                                    settings.copy(marketingAlerts = it)
                                )
                            }

                            Spacer(Modifier.height(24.dp))

                            Button(
                                onClick = {
                                    viewModel.saveSettings()

                                    if (settings.workoutReminders) {
                                        NotificationHelper.scheduleDailyWorkoutReminder(
                                            context,
                                            settings.reminderHour,
                                            settings.reminderMinute
                                        )
                                    } else {
                                        NotificationHelper.cancelWorkoutReminder(context)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = hasPermission
                            ) {
                                Text("Save Preferences")
                            }

                            error?.let {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            context,
            { _, hour, minute ->
                viewModel.updateSettings(
                    settings.copy(
                        reminderHour = hour,
                        reminderMinute = minute
                    )
                )
                showTimePicker = false
            },
            settings.reminderHour,
            settings.reminderMinute,
            true
        ).show()
    }
}
@Composable
fun NotificationToggleItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}@Composable
private fun DividerSection() {
    HorizontalDivider(Modifier.padding(vertical = 12.dp))
}

@Composable
private fun ReminderTimeSection(
    hour: Int,
    minute: Int,
    onClick: () -> Unit
) {
    Surface(
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Reminder Time")

            Button(onClick = onClick) {
                Text(String.format("%02d:%02d", hour, minute))
            }
        }
    }
}

@Composable
private fun PermissionWarning(context: android.content.Context) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Notifications are disabled",
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Button(
                onClick = {
                    (context as? Activity)?.let {
                        NotificationPermissionHandler
                            .requestNotificationPermission(it)
                    }
                }
            ) {
                Text("Enable")
            }
        }
    }
}