package com.example.fitlink.ui.screens.trainers

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.fitlink.R
import com.example.fitlink.data.models.Trainer
import com.example.fitlink.ui.screens.dashboard.*
import com.example.fitlink.ui.viewmodels.TrainersViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainersScreen(
    userId: String,
    viewModel: TrainersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(userId) {
        viewModel.initialize(userId)
    }

    val trainers = uiState.filteredTrainers
    val searchQuery = uiState.searchQuery
    val selectedSpecialty = uiState.selectedSpecialty
    val isLoading = uiState.isLoading
    val error = uiState.error
    val lazyListState = rememberLazyListState()
    var isRefreshing by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.events.collect { message ->
            snackbarHostState.showSnackbar(
                message = message,
                withDismissAction = true
            )
        }
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundSoft,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Find Your Trainer",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Text(
                            "Connect with experts to achieve your goals",
                            fontSize = 13.sp,
                            color = TextMedium
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundSoft
                )
            )
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Main content
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 🔍 Premium Search Bar
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.search(it) },
                    modifier = Modifier.padding(16.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 🔘 Filter Chips
                SpecialtyFilterRow(
                    selectedSpecialty = selectedSpecialty,
                    onSpecialtySelected = { viewModel.filterBySpecialty(it) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Content based on state
                when {
                    isLoading -> LoadingShimmer()
                    error != null -> ErrorMessage(
                        error = error!!,
                        onRetry = { viewModel.refresh() }
                    )
                    trainers.isEmpty() -> EmptyTrainersMessage(searchQuery)
                    else -> {
                        LazyColumn(
                            state = lazyListState,
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 8.dp,
                                bottom = 100.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(trainers, key = { it.id }) { trainer ->
                                AnimatedTrainerCard(
                                    trainer = trainer,
                                    currentUserId = userId,
                                    connectedTrainerId = uiState.connectedTrainerId,
                                    onConnect = {
                                        viewModel.connectWithTrainer(trainer.id, trainer.name)
                                    },
                                    onDisconnect = {
                                        viewModel.disconnectTrainer(trainer.id)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Pull-to-refresh indicator (optional)
            if (isRefreshing) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    color = PurpleGradientStart
                )
            }
        }
    }
}

// ==================== SEARCH BAR ====================
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_search),
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
                cursorBrush = SolidColor(PurpleGradientStart),
                decorationBox = { innerTextField ->
                    Box {
                        if (query.isEmpty()) {
                            Text(
                                text = "Search by name or specialty...",
                                fontSize = 16.sp,
                                color = TextLight
                            )
                        }
                        innerTextField()
                    }
                }
            )
            if (query.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { onQueryChange("") },
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = "Clear",
                        tint = TextMedium,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ==================== SPECIALTY FILTER ROW ====================
@Composable
fun SpecialtyFilterRow(
    selectedSpecialty: String,
    onSpecialtySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val specialties = listOf(
        "All" to R.drawable.ic_dumbbell,
        "Strength" to R.drawable.ic_dumbbell,
        "Cardio" to R.drawable.ic_flame,
        "Yoga" to R.drawable.ic_user,
        "Nutrition" to R.drawable.ic_flame
    )

    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        specialties.forEach { (specialty, iconRes) ->
            val isSelected = selectedSpecialty == specialty
            val backgroundColor by animateColorAsState(
                targetValue = if (isSelected) PurpleGradientStart else CardWhite,
                animationSpec = tween(300)
            )
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else TextDark,
                animationSpec = tween(300)
            )

            Surface(
                modifier = Modifier
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSpecialtySelected(specialty) },
                color = backgroundColor,
                shadowElevation = if (isSelected) 4.dp else 0.dp,
                tonalElevation = if (isSelected) 4.dp else 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = specialty,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = contentColor
                    )
                }
            }
        }
    }
}

// ==================== ANIMATED TRAINER CARD ====================
@Composable
fun AnimatedTrainerCard(
    trainer: Trainer,
    currentUserId: String,
    connectedTrainerId: String?,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val elevation by animateDpAsState(
        targetValue = if (isPressed) 12.dp else 6.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMedium)
    )

    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + slideInVertically { it / 4 }
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = elevation),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) { }
        ) {

            Column(
                modifier = Modifier.padding(20.dp)
            ) {

                // ================= HEADER ROW =================
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    // ===== PROFILE IMAGE =====
                    Box {

                        if (trainer.profileImageUrl.isNotEmpty()) {

                            AsyncImage(
                                model = trainer.profileImageUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                            )

                        } else {
                            // Default avatar
                            Surface(
                                shape = CircleShape,
                                color = LightPurpleBg,
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_user),
                                        contentDescription = null,
                                        tint = PurpleGradientStart,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {

                        Text(
                            text = trainer.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )

                        if (trainer.isVerified) {
                            Text(
                                text = "✓ Verified Trainer",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = PrimaryGreen
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = trainer.specialties.joinToString(" • "),
                            fontSize = 13.sp,
                            color = TextMedium
                        )
                    }

                    Text(
                        text = "₹${trainer.hourlyRate}/hr",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ===== BIO =====
                Text(
                    text = trainer.bio,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = TextMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ===== STATS =====
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatChip(R.drawable.ic_star, String.format("%.1f", trainer.rating))
                    StatChip(R.drawable.ic_user, "${trainer.reviewCount} reviews")
                    StatChip(R.drawable.ic_users, "${trainer.clientIds.size} clients")
                    StatChip(R.drawable.ic_clock, "${trainer.experience} yrs")
                }

                Spacer(modifier = Modifier.height(20.dp))

                val isThisTrainerConnected = connectedTrainerId == trainer.id
                val isAnotherTrainerConnected = connectedTrainerId != null && connectedTrainerId != trainer.id
                Button(
                    onClick = {
                        if (isThisTrainerConnected) onDisconnect() else onConnect()
                    },
                    enabled = !isAnotherTrainerConnected || isThisTrainerConnected,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when {
                            isThisTrainerConnected -> Color.Red
                            isAnotherTrainerConnected -> Color.Gray
                            else -> PurpleGradientStart
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = when {
                            isThisTrainerConnected -> "Disconnect"
                            isAnotherTrainerConnected -> "Already Connected"
                            else -> "Connect"
                        },
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun StatChip(icon: Int, value: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = LightPurpleBg,
        modifier = Modifier.wrapContentSize()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = PurpleGradientStart,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextDark
            )
        }
    }
}

// ==================== LOADING SHIMMER ====================
@Composable
fun LoadingShimmer() {
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
            animation = tween(durationMillis = 900, easing = LinearEasing)
        )
    )

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(4) {
            ShimmerTrainerCard(translateAnim.value, shimmerColors)
        }
    }
}

@Composable
fun ShimmerTrainerCard(
    translateX: Float,
    colors: List<Color>
) {
    val shimmerBrush = Brush.horizontalGradient(
        colors = colors,
        startX = translateX,
        endX = translateX + 300f
    )

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {

            // ================= HEADER =================
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                // Profile Image Shimmer
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(shimmerBrush)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    // Name shimmer
                    Box(
                        modifier = Modifier
                            .width(140.dp)
                            .height(18.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(shimmerBrush)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Verified text shimmer
                    Box(
                        modifier = Modifier
                            .width(100.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(shimmerBrush)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Rate shimmer
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(shimmerBrush)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Specialties shimmer
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(shimmerBrush)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Bio line 1
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(shimmerBrush)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Bio line 2
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(shimmerBrush)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // ================= STATS =================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                repeat(4) {
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(24.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(shimmerBrush)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Connect Button Shimmer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(shimmerBrush)
            )
        }
    }
}

// ==================== ERROR MESSAGE ====================
@Composable
fun ErrorMessage(error: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
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
                    painter = painterResource(R.drawable.ic_info),
                    contentDescription = null,
                    tint = PurpleGradientStart,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Unable to load trainers",
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
            Text("Try Again", color = Color.White)
        }
    }
}

// ==================== EMPTY STATE ====================
@Composable
fun EmptyTrainersMessage(searchQuery: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = LightPurpleBg,
            modifier = Modifier.size(120.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_user),
                    contentDescription = null,
                    tint = PurpleGradientStart,
                    modifier = Modifier.size(56.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = if (searchQuery.isNotEmpty()) "No trainers found" else "No trainers available",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (searchQuery.isNotEmpty())
                "No trainers match \"$searchQuery\". Try a different search."
            else
                "Check back soon for available trainers in your area.",
            fontSize = 16.sp,
            color = TextMedium,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
    }
}