package com.example.fitlink.ui.screens.settings

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.fitlink.R
import com.example.fitlink.ui.screens.dashboard.BackgroundSoft
import com.example.fitlink.ui.screens.dashboard.*;
import com.example.fitlink.ui.screens.dashboard.TextDark
import com.example.fitlink.ui.viewmodels.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userId: String,
    onClose: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    var notificationsEnabled by remember { mutableStateOf(true) }
    var darkMode by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundSoft)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                interactionSource = remember { MutableInteractionSource() },

            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = "Close",
                    tint = TextDark
                )
            }
            Text(
                text = "Settings",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Box(modifier = Modifier.size(40.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingSwitchItem(
                    icon = R.drawable.ic_bell,
                    title = "Push Notifications",
                    checked = notificationsEnabled,
                    onCheckedChange = { notificationsEnabled = it }
                )
                HorizontalDivider(color = LightGreenAccent, thickness = 1.dp)
                SettingSwitchItem(
                    icon = R.drawable.ic_moon,
                    title = "Dark Mode",
                    checked = darkMode,
                    onCheckedChange = { darkMode = it }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingItem(
                    icon = R.drawable.ic_info,
                    title = "About",
                    onClick = { /* show about dialog */ }
                )
                HorizontalDivider(color = LightGreenAccent, thickness = 1.dp)
                SettingItem(
                    icon = R.drawable.ic_logout,
                    title = "Logout",
                    isDestructive = true,
                    onClick = { showLogoutDialog = true }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "Version 1.0.0",
            fontSize = 12.sp,
            color = TextLight,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            textAlign = TextAlign.Center
        )
    }

    // Logout dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    Text("Logout", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false },
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    Text("Cancel")
                }
            },
            title = { Text("Are you sure?", fontWeight = FontWeight.Bold) },
            text = { Text("You will be logged out from your account.") },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun SettingSwitchItem(
    icon: Int,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onCheckedChange(!checked) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = PurpleGradientStart,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = TextDark,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = PurpleGradientStart,
                checkedTrackColor = PurpleGradientStart.copy(alpha = 0.5f),
                uncheckedThumbColor = TextMedium,
                uncheckedTrackColor = LightGreenAccent
            ),
            interactionSource = remember { MutableInteractionSource() }
        )
    }
}

@Composable
fun SettingItem(
    icon: Int,
    title: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = if (isDestructive) Color.Red else PurpleGradientStart,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = if (isDestructive) Color.Red else TextDark
        )
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            painter = painterResource(R.drawable.ic_arrow_right),
            contentDescription = null,
            tint = TextLight,
            modifier = Modifier.size(16.dp)
        )
    }
}