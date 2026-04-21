package com.example.fitlinktrainer.ui.screens.dashboard

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.fitlinktrainer.R
import com.example.fitlinktrainer.data.model.Activity
import com.example.fitlinktrainer.data.model.DashboardStats
import com.example.fitlinktrainer.data.model.Trainer
import com.example.fitlinktrainer.ui.screens.components.ChartSeries
import com.example.fitlinktrainer.ui.screens.components.MiniLineChart
import com.example.fitlinktrainer.ui.screens.components.ShimmerCard
import com.example.fitlinktrainer.ui.theme.*
import com.example.fitlinktrainer.viewmodel.AdminDashboardViewModel
import com.example.fitlinktrainer.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onLogout: () -> Unit,
    onNavigateToPending: () -> Unit = {},
    onNavigateToUser: (String) -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    viewModel: AdminDashboardViewModel = hiltViewModel()
) {
    val stats by viewModel.dashboardStats.collectAsStateWithLifecycle()
    val pendingTrainers by viewModel.pendingTrainers.collectAsStateWithLifecycle()

    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Background,
        contentColor = TextPrimary,
        topBar = {
            DashboardTopBar(
                onNotificationsClick = onNavigateToNotifications,
                onLogout = onLogout
            )
        }
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .padding(paddingValues)
                .padding(horizontal = 20.dp),

            verticalArrangement = Arrangement.spacedBy(20.dp),

            // ADD THIS LINE
            contentPadding = PaddingValues(bottom = 40.dp)

        )
            {
                item { HeaderSection() }
                item { KpiRow(stats = stats, isLoading = isLoading) }
                item {
                    ChartsSection(
                        registrationsData = stats.registrationsOverTime,
                        workoutsData = stats.workoutsPerDay,
                        isLoading = isLoading
                    )
                }
                item {
                    PendingTrainersSection(
                        trainers = pendingTrainers,
                        isLoading = isLoading,
                        onViewAll = onNavigateToPending,
                        onApprove = { trainerId -> viewModel.approveTrainer(trainerId) },
                        onReject = { trainerId -> viewModel.rejectTrainer(trainerId) },
                        onCardClick = { trainerId -> onNavigateToUser(trainerId) }
                    )
                }
                item {

                }
            }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardTopBar(
    onNotificationsClick: () -> Unit,
    onLogout: () -> Unit
) {
    TopAppBar(
        title = {

        },
        navigationIcon = {
            IconButton(onClick = { /* Open drawer or profile */ }) {
                Icon(
                    painter = painterResource(R.drawable.ic_menu),
                    contentDescription = "Menu",
                    tint = TextPrimary
                )
            }
        },
        actions = {


            IconButton(onClick = onNotificationsClick) {
                BadgedBox(badge = { Badge { Text("3") } }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_bell),
                        contentDescription = "Notifications",
                        tint = TextPrimary
                    )
                }
            }
            IconButton(onClick = onLogout) {
                Icon(
                    painter = painterResource(R.drawable.ic_logout),
                    contentDescription = "Logout",
                    tint = TextPrimary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Background,
            titleContentColor = TextPrimary,
            actionIconContentColor = TextPrimary
        )
    )
}

@Composable
private fun HeaderSection() {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Spacer(modifier = Modifier.height(26.dp))
        Text(
            text = "Welcome back, Admin",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Here's what's happening with FitLink today",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(12.dp))

        AssistChip(
            onClick = { },
            label = { Text("Last 30 days") },
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_calendar),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = Surface
            ),
            border = BorderStroke(1.dp, Border),
            shape = RoundedCornerShape(10.dp)
        )
    }
}

@Composable
private fun KpiRow(
    stats: DashboardStats,
    isLoading: Boolean
) {

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {

            KpiItem("Total Users", stats.totalUsers.toString(), stats.userChange, isLoading, Modifier.weight(1f))
            KpiItem("Total Trainers", stats.totalTrainers.toString(), stats.trainerChange, isLoading, Modifier.weight(1f))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {

            KpiItem("Active Workouts", stats.activeWorkouts.toString(), stats.workoutChange, isLoading, Modifier.weight(1f))
            KpiItem("MRR", "$${stats.mrr}", stats.mrrChange, isLoading, Modifier.weight(1f))
        }
    }
}
@Composable
private fun KpiItem(
    title: String,
    value: String,
    change: Float,
    isLoading: Boolean,
    modifier: Modifier
) {

    if (isLoading) {
        ShimmerCard(
            modifier = modifier.height(120.dp)
        )
    } else {
        KpiCard(
            title = title,
            value = value,
            change = change,
            modifier = modifier
        )
    }
}
@Composable
private fun KpiCard(
    title: String,
    value: String,
    change: Float,
    gradientAccent: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(16.dp), clip = false)
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        // Use Box to allow a top accent and column content below it
        Box {
            if (gradientAccent) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .background(brush = Brush.horizontalGradient(GradientBrush))
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // add top padding when accent present so content doesn't overlap
                    .padding(top = if (gradientAccent) 12.dp else 16.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val changeIcon = if (change >= 0) R.drawable.ic_trend_up else R.drawable.ic_trend_down
                    val changeColor = if (change >= 0) Success else Error
                    Icon(
                        painter = painterResource(id = changeIcon),
                        contentDescription = null,
                        tint = changeColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${if (change >= 0) "+" else ""}${change}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = changeColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun ChartsSection(
    registrationsData: List<Float>,
    workoutsData: List<Float>,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Analytics Overview",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (isLoading) {
                ShimmerCard(modifier = Modifier.height(180.dp))
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ){
                    ChartItem(
                        title = "New Registrations",
                        data = registrationsData,
                        modifier = Modifier.weight(1f)
                    )
                    ChartItem(
                        title = "Workouts Per Day",
                        data = workoutsData,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChartItem(
    title: String,
    data: List<Float>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(8.dp))
        MiniLineChart(
            series = listOf(
                ChartSeries(
                    label = title,
                    data = data,
                    color = PrimaryBlue
                )
            ),
            modifier = Modifier
                .height(140.dp)
                .fillMaxWidth(),
            gradient = Brush.horizontalGradient(GradientBrush),
            showArea = true,
            showLegend = false
        )
    }
}

@Composable
private fun PendingTrainersSection(
    trainers: List<Trainer>,
    isLoading: Boolean,
    onViewAll: () -> Unit,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit,
    onCardClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pending Trainer Approvals",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(
                    onClick = onViewAll,
                    colors = ButtonDefaults.textButtonColors(contentColor = PrimaryBlue)
                ) {
                    Text("View All")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (isLoading) {
                repeat(3) {
                    ShimmerCard(
                        modifier = Modifier
                            .height(100.dp)
                            .fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else if (trainers.isEmpty()) {
                EmptyState(
                    icon = R.drawable.ic_pending,
                    message = "No pending trainers"
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(trainers.take(5)) { trainer ->
                        PendingTrainerMiniCard(
                            trainer = trainer,
                            onApprove = { onApprove(trainer.id) },
                            onReject = { onReject(trainer.id) },
                            onClick = { onCardClick(trainer.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingTrainerMiniCard(
    trainer: Trainer,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(260.dp)
            .clickable { onClick() }
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = trainer.profileImageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = trainer.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = trainer.specialties?.joinToString(", ") ?: "Trainer",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onApprove,
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Approve", fontSize = 14.sp, color = Color.White)
                }
                Button(
                    onClick = onReject,
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Error),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Reject", fontSize = 14.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun RecentActivitySection(
    activities: List<Activity>,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Recent Activity",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (isLoading) {
                repeat(4) {
                    ShimmerCard(modifier = Modifier
                        .height(60.dp)
                        .fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else if (activities.isEmpty()) {
                EmptyState(
                    icon = R.drawable.ic_activity,
                    message = "No recent activity"
                )
            } else {
                activities.forEachIndexed { index, activity ->
                    ActivityRow(activity)
                    if (index < activities.lastIndex) {
                        Divider(
                            thickness = 1.dp,
                            color = Border,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityRow(activity: Activity) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = PrimaryBlue.copy(alpha = 0.1f),
            modifier = Modifier.size(40.dp)
        ) {
            // Use Box to center the icon inside Surface
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    painter = painterResource(
                        id = when (activity.type) {
                            "user_registered" -> R.drawable.ic_person_add
                            "trainer_approved" -> R.drawable.ic_check_circle
                            "workout_completed" -> R.drawable.ic_fitness_center
                            "subscription" -> R.drawable.ic_payment
                            else -> R.drawable.ic_bell
                        }
                    ),
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = activity.description,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = formatTimeAgo(activity.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun EmptyState(icon: Int, message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            tint = TextSecondary.copy(alpha = 0.5f),
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary
        )
    }
}

// Placeholder functions (replace with actual implementation)
fun formatTimeAgo(timestamp: Long): String = "2 hours ago"