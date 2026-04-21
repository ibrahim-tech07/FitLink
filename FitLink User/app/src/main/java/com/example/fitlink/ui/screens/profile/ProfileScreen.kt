package com.example.fitlink.ui.screens.profile

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.fitlink.R
import com.example.fitlink.data.models.AchievementModel
import com.example.fitlink.data.models.User
import com.example.fitlink.ui.screens.dashboard.*
import com.example.fitlink.ui.viewmodels.ProfileViewModel

import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userId: String,
    onClose: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val user by viewModel.user.collectAsState()
    val achievements by viewModel.achievements.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val uploadProgress by viewModel.uploadProgress.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var isEditing by remember { mutableStateOf(false) }
    var editedUser by remember { mutableStateOf<User?>(null) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Image picker
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.uploadProfileImage(userId, it)
        }
    }

    LaunchedEffect(userId) {
        viewModel.loadUserData(userId)
    }

    // When user data changes, reset editedUser
    LaunchedEffect(user) {
        editedUser = user
    }

    // Show success/failure messages
    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            Toast.makeText(context, "Profile updated", Toast.LENGTH_SHORT).show()
            viewModel.resetMessages()
        }
    }
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.resetMessages()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundSoft)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Close button
                    IconButton(
                        onClick = {
                            if (isEditing && editedUser != user) {
                                showDiscardDialog = true
                            } else {
                                onClose()
                            }
                        },
                        interactionSource = remember { MutableInteractionSource() },

                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = "Close",
                            tint = TextDark
                        )
                    }

                    Text(
                        text = "Profile",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )

                    // Edit / Save button
                    if (isEditing) {
                        TextButton(
                            onClick = {
                                if (editedUser != null) {
                                    viewModel.updateUser(userId, editedUser!!)
                                    isEditing = false
                                }
                            },
                            interactionSource = remember { MutableInteractionSource() },

                        ) {
                            Text("Save", color = PurpleGradientStart)
                        }
                    } else {
                        IconButton(
                            onClick = { isEditing = true },
                            interactionSource = remember { MutableInteractionSource() },

                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_edit),
                                contentDescription = "Edit",
                                tint = PurpleGradientStart
                            )
                        }
                    }
                }
            }

            // Profile picture card
            item {
                ProfilePictureCard(
                    user = editedUser ?: user,
                    isEditing = isEditing,
                    onImageClick = { imagePicker.launch("image/*") },
                    uploadProgress = uploadProgress
                )
            }

            // Personal info card
            item {
                PersonalInfoCard(
                    user = editedUser ?: user,
                    isEditing = isEditing,
                    onUserChange = { updated -> editedUser = updated }
                )
            }

            // Physical stats card
            item {
                PhysicalStatsCard(
                    user = editedUser ?: user,
                    isEditing = isEditing,
                    onUserChange = { updated -> editedUser = updated }
                )
            }

            // Fitness goal card
            item {
                FitnessGoalCard(
                    user = editedUser ?: user,
                    isEditing = isEditing,
                    onUserChange = { updated -> editedUser = updated }
                )
            }

            // Streak card (reuse from Dashboard)
            item {
                StreakCard(streakDays = user?.streak ?: 0)
            }

            // Achievements section
            item {
                AchievementsSection(achievements = achievements)
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // Loading overlay
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryGreen)
            }
        }
    }

    // Discard changes dialog
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard changes?") },
            text = { Text("You have unsaved changes. Are you sure you want to leave?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        editedUser = user
                        isEditing = false
                        onClose()
                    },
                    interactionSource = remember { MutableInteractionSource() },

                ) {
                    Text("Discard", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDiscardDialog = false },
                    interactionSource = remember { MutableInteractionSource() },

                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// ==================== Profile Picture Card ====================
@Composable
fun ProfilePictureCard(
    user: User?,
    isEditing: Boolean,
    onImageClick: () -> Unit,
    uploadProgress: Float
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(LightPurpleBg)
                    .then(
                        if (isEditing) {
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onImageClick() }
                        } else Modifier
                    )
                    .border(3.dp, PurpleGradientStart, CircleShape)
            ) {
                if (!user?.profileImageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(user?.profileImageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Profile",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_user),
                        contentDescription = null,
                        tint = PurpleGradientStart,
                        modifier = Modifier
                            .size(60.dp)
                            .align(Alignment.Center)
                    )
                }

                if (isEditing) {
                    // Camera overlay
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.Black.copy(alpha = 0.3f))
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_camera),
                            contentDescription = "Change photo",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }

            if (uploadProgress > 0 && uploadProgress < 1f) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = uploadProgress,
                    color = PurpleGradientStart,
                    trackColor = LightGreenAccent,
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )
            }

            if (isEditing) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap to change photo",
                    fontSize = 12.sp,
                    color = TextMedium
                )
            }
        }
    }
}

// ==================== Personal Info Card ====================
@Composable
fun PersonalInfoCard(
    user: User?,
    isEditing: Boolean,
    onUserChange: (User) -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Personal Information",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Name
            InfoRow(
                icon = R.drawable.ic_user,
                label = "Name",
                value = user?.name ?: "",
                isEditing = isEditing,
                onValueChange = { newName ->
                    user?.let { onUserChange(it.copy(name = newName)) }
                },
                keyboardType = KeyboardType.Text
            )

            HorizontalDivider(color = LightGreenAccent, thickness = 1.dp)

            // Email (read-only)
            InfoRow(
                icon = R.drawable.ic_mail,
                label = "Email",
                value = user?.email ?: "",
                isEditing = false,
                onValueChange = {},
                keyboardType = KeyboardType.Email
            )
        }
    }
}

// ==================== Physical Stats Card ====================
@Composable
fun PhysicalStatsCard(
    user: User?,
    isEditing: Boolean,
    onUserChange: (User) -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Physical Stats",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Age
            InfoRow(
                icon = R.drawable.ic_cake,
                label = "Age",
                value = user?.age?.toString() ?: "0",
                isEditing = isEditing,
                onValueChange = { newAgeStr ->
                    val age = newAgeStr.toIntOrNull() ?: 0
                    user?.let { onUserChange(it.copy(age = age)) }
                },
                keyboardType = KeyboardType.Number
            )

            HorizontalDivider(color = LightGreenAccent, thickness = 1.dp)

            // Weight (kg)
            InfoRow(
                icon = R.drawable.ic_upper_body,
                label = "Weight (kg)",
                value = user?.weight?.toString() ?: "0.0",
                isEditing = isEditing,
                onValueChange = { newWeightStr ->
                    val weight = newWeightStr.toDoubleOrNull() ?: 0.0
                    user?.let { onUserChange(it.copy(weight = weight)) }
                },
                keyboardType = KeyboardType.Decimal
            )

            HorizontalDivider(color = LightGreenAccent, thickness = 1.dp)

            // Height (cm)
            InfoRow(
                icon = R.drawable.ic_height,
                label = "Height (cm)",
                value = user?.height?.toString() ?: "0.0",
                isEditing = isEditing,
                onValueChange = { newHeightStr ->
                    val height = newHeightStr.toDoubleOrNull() ?: 0.0
                    user?.let { onUserChange(it.copy(height = height)) }
                },
                keyboardType = KeyboardType.Decimal
            )
        }
    }
}

// ==================== Fitness Goal Card ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FitnessGoalCard(
    user: User?,
    isEditing: Boolean,
    onUserChange: (User) -> Unit
) {
    val goals = listOf("Lose weight", "Maintain weight", "Gain muscle", "Improve endurance")

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Fitness Goal",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isEditing) {
                var expanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    TextField(
                        value = user?.fitnessGoal ?: goals[0],
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = CardWhite,
                            unfocusedContainerColor = CardWhite,
                            focusedIndicatorColor = PurpleGradientStart,
                            unfocusedIndicatorColor = LightGreenAccent
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        goals.forEach { goal ->
                            DropdownMenuItem(
                                text = { Text(goal) },
                                onClick = {
                                    user?.let { onUserChange(it.copy(fitnessGoal = goal)) }
                                    expanded = false
                                },
                                interactionSource = remember { MutableInteractionSource() },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_flag),
                        contentDescription = null,
                        tint = PurpleGradientStart,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = user?.fitnessGoal ?: "Not set",
                        fontSize = 16.sp,
                        color = TextDark,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ==================== Info Row Helper ====================
@Composable
fun InfoRow(
    icon: Int,
    label: String,
    value: String,
    isEditing: Boolean,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            tint = PurpleGradientStart,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))

        if (isEditing) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(label) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = CardWhite,
                    unfocusedContainerColor = CardWhite,
                    focusedIndicatorColor = PurpleGradientStart,
                    unfocusedIndicatorColor = LightGreenAccent
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        } else {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = TextMedium
                )
                Text(
                    text = value,
                    fontSize = 16.sp,
                    color = TextDark,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ==================== Achievements Section ====================
@Composable
fun AchievementsSection(achievements: List<AchievementModel>) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Achievements",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (achievements.isEmpty()) {
                Text(
                    text = "No achievements yet. Keep going!",
                    fontSize = 14.sp,
                    color = TextMedium,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.height(280.dp)
                ) {
                    items(achievements) { achievement ->
                        AchievementItem(achievement)
                    }
                }
            }
        }
    }
}

@Composable
fun AchievementItem(achievement: AchievementModel) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LightPurpleBg),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(
                painter = painterResource(
                    when (achievement.icon) {
                        "ic_trending_up" -> R.drawable.ic_trending_up
                        "ic_award" -> R.drawable.ic_award
                        else -> R.drawable.ic_star
                    }
                ),
                contentDescription = null,
                tint = PurpleGradientStart,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = achievement.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = achievement.description,
                fontSize = 12.sp,
                color = TextMedium,
                maxLines = 2,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}