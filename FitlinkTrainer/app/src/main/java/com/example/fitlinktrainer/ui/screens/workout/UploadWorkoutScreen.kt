package com.example.fitlinktrainer.ui.screens.workout

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.fitlinktrainer.R
import com.example.fitlinktrainer.data.model.Exercise
import com.example.fitlinktrainer.data.model.User
import com.example.fitlinktrainer.data.templates.WorkoutTemplates
import com.example.fitlinktrainer.ui.theme.*
import com.example.fitlinktrainer.viewmodel.UploadWorkoutViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadWorkoutScreen(
    trainerId: String,
    onNavigateBack: () -> Unit,
    viewModel: UploadWorkoutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val clients by viewModel.clients.collectAsStateWithLifecycle()
    val uploadingMedia = uiState.uploadingMedia
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    var showExerciseDialog by remember { mutableStateOf(false) }
    var editingExercise by remember { mutableStateOf<Exercise?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.setImage(it) } }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.setVideo(it) } }

    LaunchedEffect(trainerId) {
        viewModel.initialize(trainerId)
    }

    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            listState.animateScrollToItem(0)
        }
    }

    val totalCalories = remember(uiState.exercises) {
        uiState.exercises.sumOf { it.caloriesBurn }
    }
    val totalDuration = remember(uiState.exercises) {
        uiState.exercises.sumOf { (it.durationSeconds.takeIf { it > 0 } ?: 60) / 60 }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Create Workout",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .padding(paddingValues)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                // Workout Details Card
                item {
                    WorkoutDetailsCard(
                        title = uiState.title,
                        onTitleChange = viewModel::updateTitle,
                        description = uiState.description,
                        onDescriptionChange = viewModel::updateDescription,
                        difficulty = uiState.difficulty,
                        onDifficultyChange = viewModel::updateDifficulty,
                        totalCalories = totalCalories,
                        totalDuration = totalDuration,
                        imageUri = uiState.imageUri,
                        videoUri = uiState.videoUri,
                        onImageClick = { imagePickerLauncher.launch("image/*") },
                        onVideoClick = { videoPickerLauncher.launch("video/*") },
                        onClearImage = { viewModel.setImage(null) },
                        onClearVideo = { viewModel.setVideo(null) }
                    )
                }

                // Client Assignment Card
                item {
                    ClientAssignmentCard(
                        clients = clients,
                        selectedUserId = uiState.selectedUserId,
                        onSelect = viewModel::selectUser
                    )
                }

                // Templates Section (with Warmup, Main, Cooldown)
                item {
                    TemplatesSection(
                        onTemplateSelected = viewModel::setTemplate
                    )
                }

                // Exercises Header with add button
                item {
                    ExercisesHeader(
                        onAddExercise = {
                            editingExercise = null
                            showExerciseDialog = true
                        }
                    )
                }

                // Exercises List
                itemsIndexed(
                    items = uiState.exercises,
                    key = { _, exercise -> exercise.id ?: exercise.hashCode() }
                ) { index, exercise ->
                    ExerciseItem(
                        exercise = exercise,
                        onEdit = {
                            editingExercise = exercise
                            showExerciseDialog = true
                        },
                        onDelete = { viewModel.removeExercise(exercise) }
                    )
                }

                // Upload Button
                item {
                    UploadButton(
                        isLoading = uiState.isLoading,
                        enabled = !uploadingMedia,
                        onClick = {
                            viewModel.uploadWorkout(context, trainerId) { success, message ->
                                if (success) onNavigateBack()
                            }
                        }
                    )
                }

                // Success/Error Message
                item {
                    AnimatedVisibility(
                        visible = uiState.error != null || uiState.success,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (uiState.success) Success.copy(alpha = 0.1f) else Error.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = uiState.error ?: "Workout uploaded successfully!",
                                color = if (uiState.success) Success else Error,
                                modifier = Modifier.padding(16.dp),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Loading overlay
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { }
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = PrimaryBlue
                    )
                }
            }
        }
    }

    // Add/Edit Exercise Dialog
    if (showExerciseDialog) {
        AddEditExerciseDialog(
            initialExercise = editingExercise,
            onDismiss = { showExerciseDialog = false },
            onSave = { exercise ->
                if (editingExercise == null) {
                    viewModel.addExercise(exercise)
                } else {
                    viewModel.updateExercise(exercise)
                }
                showExerciseDialog = false
            },
            viewModel = viewModel
        )
    }
}

// ==================== WORKOUT DETAILS CARD ====================
@Composable
private fun WorkoutDetailsCard(
    title: String,
    onTitleChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    difficulty: String,
    onDifficultyChange: (String) -> Unit,
    totalCalories: Int,
    totalDuration: Int,
    imageUri: Uri?,
    videoUri: Uri?,
    onImageClick: () -> Unit,
    onVideoClick: () -> Unit,
    onClearImage: () -> Unit,
    onClearVideo: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Workout Details",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                label = { Text("Workout title") },
                placeholder = { Text("e.g. Full Body Burn") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = PrimaryBlue,
                    unfocusedIndicatorColor = Border,
                    focusedLabelColor = PrimaryBlue,
                    unfocusedLabelColor = TextSecondary
                ),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                label = { Text("Description (optional)") },
                placeholder = { Text("Describe the workout...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = PrimaryBlue,
                    unfocusedIndicatorColor = Border,
                    focusedLabelColor = PrimaryBlue
                ),
                minLines = 3,
                maxLines = 5
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Difficulty",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            DifficultyChips(
                selected = difficulty,
                onSelected = onDifficultyChange
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatBox(
                    icon = R.drawable.ic_flame,
                    value = "$totalCalories",
                    label = "kcal"
                )
                StatBox(
                    icon = R.drawable.ic_clock,
                    value = "$totalDuration",
                    label = "min"
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Workout Media",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MediaUploadButton(
                    icon = painterResource(R.drawable.ic_image),
                    label = "Image",
                    tint = PrimaryBlue,
                    onClick = onImageClick,
                    modifier = Modifier.weight(1f)
                )
                MediaUploadButton(
                    icon = painterResource(R.drawable.ic_video),
                    label = "Video",
                    tint = Color(0xFFFF6B6B),
                    onClick = onVideoClick,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            imageUri?.let { uri ->
                Box(modifier = Modifier.fillMaxWidth()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(uri)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                    IconButton(
                        onClick = onClearImage,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = "Remove",
                            tint = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            videoUri?.let { uri ->
                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_play),
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    IconButton(
                        onClick = onClearVideo,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = "Remove",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

// ==================== DIFFICULTY CHIPS ====================
@Composable
private fun DifficultyChips(
    selected: String,
    onSelected: (String) -> Unit
) {
    val difficulties = listOf("Beginner", "Intermediate", "Advanced")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        difficulties.forEach { level ->
            val isSelected = selected == level
            Surface(
                shape = RoundedCornerShape(50),
                color = if (isSelected) PrimaryBlue else Surface,
                modifier = Modifier
                    .wrapContentWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelected(level) }
            ) {
                Text(
                    text = level,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else TextPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

// ==================== STAT BOX ====================
@Composable
private fun StatBox(icon: Int, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = CircleShape,
            color = PrimaryBlue.copy(alpha = 0.1f),
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(text = label, fontSize = 12.sp, color = TextSecondary)
    }
}

// ==================== MEDIA UPLOAD BUTTON ====================
@Composable
private fun MediaUploadButton(
    icon: Painter,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(80.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        color = Surface,
        shadowElevation = 2.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = label, fontSize = 12.sp, color = TextPrimary)
        }
    }
}

// ==================== CLIENT ASSIGNMENT CARD ====================
@Composable
private fun ClientAssignmentCard(
    clients: List<User>,
    selectedUserId: String?,
    onSelect: (String?) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Assign to Client",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (clients.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No clients connected yet",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(clients) { client ->
                        ClientChip(
                            client = client,
                            isSelected = selectedUserId == client.id,
                            onSelect = {
                                if (selectedUserId == client.id) onSelect(null) else onSelect(client.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClientChip(
    client: User,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        modifier = Modifier
            .wrapContentWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onSelect() },
        shape = RoundedCornerShape(50),
        color = if (isSelected) PrimaryBlue else Surface,
        shadowElevation = if (isSelected) 4.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = client.profileImageUrl ?: "",
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Surface),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = client.name,
                color = if (isSelected) Color.White else TextPrimary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ==================== TEMPLATES SECTION ====================
@Composable
private fun TemplatesSection(
    onTemplateSelected: (List<Exercise>) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Quick Templates",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Warmup
            TemplateRow(
                title = "🔥 Warmup",
                templates = listOf(
                    TemplateChipData("Dynamic Stretch", WorkoutTemplates.warmupDynamic),
                    TemplateChipData("Mobility", WorkoutTemplates.warmupMobility)
                ),
                onTemplateSelected = onTemplateSelected
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Main Workout
            TemplateRow(
                title = "💪 Main Workout",
                templates = listOf(
                    TemplateChipData("Beginner Full Body", WorkoutTemplates.beginnerFullBody),
                    TemplateChipData("Fat Burn", WorkoutTemplates.fatBurnCardio),
                    TemplateChipData("Strength", WorkoutTemplates.strengthWorkout),
                    TemplateChipData("HIIT Advanced", WorkoutTemplates.hiitAdvanced)
                ),
                onTemplateSelected = onTemplateSelected
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Cooldown
            TemplateRow(
                title = "🧘 Cooldown",
                templates = listOf(
                    TemplateChipData("Full Body Stretch", WorkoutTemplates.cooldownStretch),
                    TemplateChipData("Breathing", WorkoutTemplates.cooldownBreathing)
                ),
                onTemplateSelected = onTemplateSelected
            )
        }
    }
}

private data class TemplateChipData(val label: String, val exercises: List<Exercise>)

@Composable
private fun TemplateRow(
    title: String,
    templates: List<TemplateChipData>,
    onTemplateSelected: (List<Exercise>) -> Unit
) {
    Column {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(templates) { template ->
                TemplateChip(
                    label = template.label,
                    onClick = { onTemplateSelected(template.exercises) }
                )
            }
        }
    }
}

@Composable
private fun TemplateChip(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .wrapContentWidth()
            .height(44.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        shape = RoundedCornerShape(24.dp),
        color = PrimaryBlue.copy(alpha = 0.12f),
        contentColor = PrimaryBlue
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ==================== EXERCISES HEADER ====================
@Composable
private fun ExercisesHeader(
    onAddExercise: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Exercises",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        FloatingActionButton(
            onClick = onAddExercise,
            containerColor = PrimaryBlue,
            shape = CircleShape,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = "Add Exercise",
                tint = Color.White
            )
        }
    }
}

// ==================== EXERCISE ITEM ====================
@Composable
private fun ExerciseItem(
    exercise: Exercise,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail with play overlay if video
            Box(modifier = Modifier.size(100.dp)) {
                val thumbnailModel = exercise.gifUrl ?: exercise.imageUrl
                AsyncImage(
                    model = thumbnailModel,
                    contentDescription = null,
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(LightGrayBg),
                    contentScale = ContentScale.Crop
                )
                if (!exercise.videoUrl.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            tonalElevation = 4.dp,
                            modifier = Modifier.size(44.dp),
                            color = Color.White.copy(alpha = 0.15f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_play),
                                    contentDescription = "Play",
                                    modifier = Modifier.size(28.dp),
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = exercise.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${exercise.sets} sets × ${exercise.reps} reps",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    if (exercise.restSeconds > 0) {
                        Icon(
                            painter = painterResource(R.drawable.ic_clock),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = TextSecondary
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "${exercise.restSeconds}s rest",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        painter = painterResource(R.drawable.ic_flame),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = PrimaryBlue
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "${exercise.caloriesBurn} kcal",
                        fontSize = 12.sp,
                        color = PrimaryBlue
                    )
                }
            }

            // Actions (only edit and delete)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_edit),
                        contentDescription = "Edit",
                        tint = TextSecondary
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = "Delete",
                        tint = Error
                    )
                }
            }
        }
    }
}

@Composable
private fun AddEditExerciseDialog(
    initialExercise: Exercise?,
    onDismiss: () -> Unit,
    onSave: (Exercise) -> Unit,
    viewModel: UploadWorkoutViewModel
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initialExercise?.name ?: "") }
    var sets by remember { mutableStateOf(initialExercise?.sets?.toString() ?: "") }
    var reps by remember { mutableStateOf(initialExercise?.reps?.toString() ?: "") }
    var restSeconds by remember { mutableStateOf(initialExercise?.restSeconds?.toString() ?: "30") }
    var caloriesBurn by remember { mutableStateOf(initialExercise?.caloriesBurn?.toString() ?: "") }
    var minutes by remember { mutableStateOf("1") }
    var gifUrl by remember { mutableStateOf(initialExercise?.gifUrl) }
    var videoUrl by remember { mutableStateOf(initialExercise?.videoUrl) }
    var isUploading by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }

    val gifPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            isUploading = true
            viewModel.uploadExerciseGif(context, it) { url ->
                gifUrl = url
                isUploading = false
            }
        }
    }

    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            isUploading = true
            viewModel.uploadExerciseVideo(context, it) { url ->
                videoUrl = url
                isUploading = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialExercise == null) "Add Exercise" else "Edit Exercise",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                // Exercise name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = false },
                    label = { Text("Exercise name") },
                    placeholder = { Text("e.g. Push‑ups") },
                    isError = nameError,
                    supportingText = if (nameError) {
                        { Text("Name is required", color = Error, fontSize = 12.sp) }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = PrimaryBlue,
                        unfocusedIndicatorColor = Border,
                        focusedLabelColor = PrimaryBlue,
                        unfocusedLabelColor = TextSecondary
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Sets and Reps row
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sets,
                        onValueChange = { sets = it },
                        label = { Text("Sets") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = PrimaryBlue,
                            unfocusedIndicatorColor = Border
                        ),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = reps,
                        onValueChange = { reps = it },
                        label = { Text("Reps") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = PrimaryBlue,
                            unfocusedIndicatorColor = Border
                        ),
                        singleLine = true
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Rest seconds
                OutlinedTextField(
                    value = restSeconds,
                    onValueChange = { restSeconds = it },
                    label = { Text("Rest (seconds)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = PrimaryBlue,
                        unfocusedIndicatorColor = Border
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Calories estimate
                OutlinedTextField(
                    value = caloriesBurn,
                    onValueChange = { caloriesBurn = it },
                    label = { Text("Calories (estimate)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = PrimaryBlue,
                        unfocusedIndicatorColor = Border
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Duration input (commented out in original, uncommented for clarity but kept as optional)
                // You can enable this if duration-based exercises are needed
                /*
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Duration-based", modifier = Modifier.weight(1f))
                    Switch(
                        checked = isDuration,
                        onCheckedChange = { isDuration = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = PrimaryBlue)
                    )
                }
                if (isDuration) {
                    OutlinedTextField(
                        value = minutes,
                        onValueChange = { minutes = it },
                        label = { Text("Duration (minutes)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = PrimaryBlue,
                            unfocusedIndicatorColor = Border
                        ),
                        singleLine = true
                    )
                }
                */
                Spacer(modifier = Modifier.height(16.dp))

                // Media upload buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { gifPicker.launch("image/*") },
                        modifier = Modifier.weight(1f),
                        enabled = gifUrl == null && !isUploading,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (gifUrl != null) PrimaryBlue else TextPrimary
                        ),
                        border = if (gifUrl != null) {
                            ButtonDefaults.outlinedButtonBorder.copy(brush = SolidColor(PrimaryBlue))
                        } else null
                    ) {
                        if (gifUrl != null) {
                            Icon(
                                painter = painterResource(R.drawable.ic_check),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = PrimaryBlue
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("GIF uploaded", color = PrimaryBlue)
                        } else {
                            Text("Upload GIF")
                        }
                    }
                    OutlinedButton(
                        onClick = { videoPicker.launch("video/*") },
                        modifier = Modifier.weight(1f),
                        enabled = videoUrl == null && !isUploading,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (videoUrl != null) PrimaryBlue else TextPrimary
                        ),
                        border = if (videoUrl != null) {
                            ButtonDefaults.outlinedButtonBorder.copy(brush = SolidColor(PrimaryBlue))
                        } else null
                    ) {
                        if (videoUrl != null) {
                            Icon(
                                painter = painterResource(R.drawable.ic_check),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = PrimaryBlue
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Video uploaded", color = PrimaryBlue)
                        } else {
                            Text("Upload Video")
                        }
                    }
                }

                // Upload progress indicator
                if (isUploading) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = PrimaryBlue
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                        return@TextButton
                    }

                    val setsInt = sets.toIntOrNull() ?: 1
                    val repsInt = reps.toIntOrNull() ?: 1
                    val restInt = restSeconds.toIntOrNull() ?: 30
                    val caloriesInt = caloriesBurn.toIntOrNull() ?: 1
                    val minutesInt = minutes.toIntOrNull() ?: 1

                    val exercise = Exercise(
                        id = initialExercise?.id ?: System.currentTimeMillis().toString(),
                        name = name,
                        sets = setsInt,
                        reps = repsInt,
                        restSeconds = restInt,
                        caloriesBurn = caloriesInt,
                        duration = true,
                        durationSeconds = minutesInt * 60,
                        gifUrl = gifUrl,
                        videoUrl = videoUrl
                    )

                    onSave(exercise)
                },
                enabled = !isUploading
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ==================== UPLOAD BUTTON ====================
@Composable
private fun UploadButton(
    isLoading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = !isLoading && enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = PrimaryBlue,
            disabledContainerColor = PrimaryBlue.copy(alpha = 0.5f)
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Text(
                text = "Upload Workout",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ==================== COLORS ====================
private val PrimaryBlue = Color(0xFF3B82F6)
private val Background = Color(0xFFF8FAFC)
private val Surface = Color.White
private val TextPrimary = Color(0xFF1E293B)
private val TextSecondary = Color(0xFF64748B)
private val Border = Color(0xFFE2E8F0)
private val Success = Color(0xFF10B981)
private val Error = Color(0xFFEF4444)
private val LightGrayBg = Color(0xFFF1F5F9)