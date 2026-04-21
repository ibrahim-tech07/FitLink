package com.example.fitlinktrainer.ui.screens.dashboard


import androidx.compose.animation.animateContentSize
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.fitlinktrainer.R
import com.example.fitlinktrainer.data.model.User
import com.example.fitlinktrainer.data.model.Message

import com.example.fitlinktrainer.ui.screens.activity.TrainerActivityCenterScreen

import com.example.fitlinktrainer.ui.screens.components.ChartSeries
import com.example.fitlinktrainer.ui.screens.components.MiniLineChart
import com.example.fitlinktrainer.ui.screens.components.ShimmerCard
import com.example.fitlinktrainer.ui.screens.messages.MessagesScreen
import com.example.fitlinktrainer.ui.screens.notifications.NotificationsScreen
import com.example.fitlinktrainer.ui.screens.workout.UploadWorkoutScreen
import com.example.fitlinktrainer.ui.theme.*
import com.example.fitlinktrainer.viewmodel.TrainerDashboardViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable

fun Home(
    trainerId: String,
    onNavigateToDashboard: () -> Unit = {},
    onNavigateToClients: () -> Unit = {},
    onNavigateToMessages: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToRevenue: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToClient: (String) -> Unit = {},
    onLogout: () -> Unit = {},
    viewModel: TrainerDashboardViewModel = hiltViewModel()
) {

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(trainerId) {
        viewModel.initialize(trainerId)
    }

    val clients by viewModel.clients.collectAsStateWithLifecycle()
    val recentMessages by viewModel.recentMessages.collectAsStateWithLifecycle()
    val performanceData by viewModel.performanceData.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val trainer by viewModel.trainer.collectAsStateWithLifecycle()
    // Wrap performance data into a ChartSeries for the new chart
    val chartSeries = remember(performanceData) {
        listOf(
            ChartSeries(
                label = "All Clients",
                data = performanceData,
                color = PrimaryBlue
            )
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {

            DrawerContent(
                onNavigateToDashboard = {
                    onNavigateToDashboard()
                    scope.launch { drawerState.close() }
                },
                onNavigateToClients = {
                    onNavigateToClients()
                    scope.launch { drawerState.close() }
                },
                onNavigateToMessages = {
                    onNavigateToMessages()
                    scope.launch { drawerState.close() }
                },
                onNavigateToProfile = {
                    onNavigateToProfile()
                    scope.launch { drawerState.close() }
                },
                onLogout = onLogout
            )

        }
    ) {
        Scaffold(
            topBar = {
                DashboardTopBar(
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onNotificationsClick = { onNavigateToNotifications() }
                )
            },
            containerColor = Background
        ) { paddingValues ->

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {

                DashboardTabContent(
                    trainerId = trainerId,
                    trainerName = trainer?.name ?: "Trainer",
                    trainerImage = trainer?.profileImageUrl,
                    clients = clients,
                    recentMessages = recentMessages,
                    performanceData = performanceData,
                    chartSeries = chartSeries,
                    isLoading = isLoading,
                    onNavigateToClient = onNavigateToClient,
                    onNavigateToMessages = onNavigateToMessages
                )

            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardTopBar(
    onMenuClick: () -> Unit,
    onNotificationsClick: () -> Unit
) {
    TopAppBar(
        title = {

        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_menu),
                    contentDescription = "Menu",
                    tint = TextPrimary
                )
            }
        },
        actions = {
            IconButton(onClick = onNotificationsClick) {
                BadgedBox(badge = { Badge { Text("2") } }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_bell),
                        contentDescription = "Notifications",
                        tint = TextPrimary
                    )
                }
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
private fun DrawerContent(
    onNavigateToDashboard: () -> Unit,
    onNavigateToClients: () -> Unit,
    onNavigateToMessages: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onLogout: () -> Unit
) {

    ModalDrawerSheet(
        modifier = Modifier.width(280.dp)
    ) {

        Column(
            modifier = Modifier.fillMaxSize()
        ) {

            Text(
                "FitLink Trainer",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(20.dp),
                color = PrimaryBlue
            )
            DrawerMenuItem(
                R.drawable.ic_user,
                "Profile",
                onNavigateToProfile
            )
            DrawerMenuItem( R.drawable.ic_dashboard, "Dashboard",onNavigateToDashboard)
            DrawerMenuItem( R.drawable.ic_people, "Clients",onNavigateToClients)
            DrawerMenuItem( R.drawable.ic_chat,"Messages", onNavigateToMessages)

            Spacer(modifier = Modifier.weight(1f))

            DrawerMenuItem(R.drawable.ic_logout,"Logout",  onLogout)

        }
    }
}

@Composable
private fun DrawerMenuItem(
    icon: Int,
    label: String,
    onClick: () -> Unit,
    tint: Color = TextPrimary
) {
    NavigationDrawerItem(
        label = { Text(label, color = tint) },
        selected = false,
        onClick = onClick,
        icon = {
            Icon(
                painter = painterResource(icon),
                contentDescription = null
            )
        },
        colors = NavigationDrawerItemDefaults.colors(
            unselectedContainerColor = Color.Transparent,
            unselectedTextColor = tint,
            unselectedIconColor = tint
        ),
        modifier = Modifier.padding(horizontal = 12.dp)
    )
}

@Composable
private fun TrainerHeader(
    trainerName: String,
    profileImageUrl: String?,
    isLoading: Boolean
) {
    if (isLoading) {
        ShimmerCard(modifier = Modifier.height(80.dp))
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(profileImageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Surface),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Hello, $trainerName",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Let's check your progress",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}
@Composable
fun DashboardTabContent(
    trainerId: String,
    trainerName: String,
    trainerImage: String?,
    clients: List<User>,
    recentMessages: List<Message>,
    performanceData: List<Float>,
    chartSeries: List<ChartSeries>,
    isLoading: Boolean,
    onNavigateToClient: (String) -> Unit,
    onNavigateToMessages: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        state = rememberLazyListState()
    ) {
        item {
            Spacer(modifier = Modifier.height(48.dp))
            TrainerHeader(
                trainerName = trainerName,
                profileImageUrl = trainerImage,
                isLoading = isLoading
            )
        }
        item {
            MetricsRow(
                totalClients = clients.size,
                activeToday = performanceData.lastOrNull()?.toInt() ?: 0,
                workoutsThisWeek = performanceData.sum().toInt(),
                revenue = clients.size * 20,
                isLoading = isLoading
            )
        }
        item {
            PerformanceChartSection(
                series = chartSeries,
                isLoading = isLoading
            )
        }
        item {
            RecentMessagesSection(
                messages = recentMessages,
                isLoading = isLoading,
                clients = clients,
                onMessageClick = { message ->
                    onNavigateToClient(message.senderId)
                },
                onViewAllClick = onNavigateToMessages
            )
        }
        item {
            RecentClientsSection(
                clients = clients,
                isLoading = isLoading,
                onNavigateToMessages = onNavigateToMessages
            )
        }
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun MetricsRow(
    totalClients: Int,
    activeToday: Int,
    workoutsThisWeek: Int,
    revenue: Int,
    isLoading: Boolean
){
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val cardWidth = (screenWidth - 60.dp) / 2 // Two cards per row with spacing

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                repeat(2) {
                    ShimmerCard(modifier = Modifier.weight(1f).height(100.dp))
                }
            } else {
                StatCard(
                    title = "Total Clients",
                    value = totalClients.toString(),
                    icon = painterResource(R.drawable.ic_people),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Active Today",
                    value = activeToday.toString(),
                    icon = painterResource(R.drawable.ic_fitness_center),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                repeat(2) {
                    ShimmerCard(modifier = Modifier.weight(1f).height(100.dp))
                }
            } else {
                StatCard(
                    title = "Workouts/Week",
                    value = workoutsThisWeek.toString(),
                    icon = painterResource(R.drawable.ic_trend_up),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Revenue (MTD)",
                    value = "₹$revenue",
                    icon = painterResource(R.drawable.ic_attach_money),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: Painter,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            Surface(
                shape = CircleShape,
                color = PrimaryBlue.copy(alpha = 0.1f),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.CenterVertically)
                )
            }
        }
    }
}

@Composable
private fun PerformanceChartSection(
    series: List<ChartSeries>,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp)),
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
                    text = "Client Activity (Last 7 days)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    painter = painterResource(R.drawable.ic_info),
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (isLoading) {
                ShimmerCard(modifier = Modifier.height(120.dp))
            } else {
                MiniLineChart(
                    series = series,
                    modifier = Modifier
                        .height(120.dp)
                        .fillMaxWidth(),
                    gradient = Brush.horizontalGradient(GradientBrush),
                    showArea = true,
                    showLegend = false // single series, no legend needed
                )
            }
        }
    }
}


@Composable
private fun RecentMessagesSection(
    messages: List<Message>,
    clients: List<User>,
    isLoading: Boolean,
    onMessageClick: (Message) -> Unit,
    onViewAllClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp)),
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
                    text = "Recent Messages",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(
                    onClick = onViewAllClick,
                    colors = ButtonDefaults.textButtonColors(contentColor = PrimaryBlue)
                ) {
                    Text("View All")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (isLoading) {
                repeat(2) {
                    ShimmerCard(modifier = Modifier.height(56.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else if (messages.isEmpty()) {
                EmptyState(
                    icon = R.drawable.ic_chat,
                    message = "No recent messages"
                )
            } else {
                messages.take(3).forEach { message ->

                    val sender = clients.find { it.id == message.senderId }

                    MessageItem(
                        message = message,
                        sender = sender,
                        onClick = { onMessageClick(message) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageItem(
    message: Message,
    sender: User?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model =  sender?.profileImageUrl,
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Surface),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = sender?.name ?: "User",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = formatTimeAgo(message.timestamp),
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}

@Composable
private fun RecentClientsSection(
    clients: List<User>,
    isLoading: Boolean,
    onNavigateToMessages: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Recent Clients",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (isLoading) {
                repeat(2) {
                    ShimmerCard(modifier = Modifier.height(72.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else if (clients.isEmpty()) {
                EmptyState(
                    icon = R.drawable.ic_people,
                    message = "No clients yet"
                )
            } else {
                clients.take(3).forEach { client ->
                    ClientItem(
                        client = client,
                        onClick = {
                            onNavigateToMessages()
                        }
                    )
                    HorizontalDivider(color = Border, thickness = 1.dp)
                }
            }
        }
    }
}

@Composable
private fun ClientItem(
    client: User,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = client.profileImageUrl,
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Surface),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = client.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = "Streak: ${client.streak} days",
                style = MaterialTheme.typography.bodySmall,
                color = if (client.streak > 0) Success else TextSecondary
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = TextSecondary
        )
    }
}

@Composable
private fun EmptyState(icon: Int, message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
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

// Placeholder functions
