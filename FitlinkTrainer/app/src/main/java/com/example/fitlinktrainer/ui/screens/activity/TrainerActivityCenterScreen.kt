package com.example.fitlinktrainer.ui.screens.activity

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.fitlinktrainer.R
import com.example.fitlinktrainer.data.model.User
import com.example.fitlinktrainer.viewmodel.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainerActivityCenterScreen(
    viewModel: TrainerActivityCenterViewModel = hiltViewModel(),
    onAssignWorkout: (String) -> Unit = {}
) {

    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Activity Center",
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(20.dp)
        ) {

            item {
                DashboardStats(state)
            }

            item {
                QuickActions(
                    onReminder = {},
                    onMotivation = {},
                    onAssignWorkout = {}
                )
            }

            if (state.atRiskClients.isNotEmpty()) {
                item {
                    RiskCard(state.atRiskClients.size)
                }
            }

            item {
                ActivityCharts(
                    state.weeklyActivityData,
                    state.weeklyCaloriesData
                )
            }

            item {
                Text(
                    "Client Activity",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            items(state.clients) { client ->

                ClientCard(
                    client = client,
                    isAtRisk = state.atRiskClients.any { it.id == client.id },
                    onReminderClick = {
                        viewModel.sendReminder(client.id)
                    },
                    onMotivationClick = {
                        viewModel.sendMotivation(client.id)
                    }
                )

            }
        }
    }
}






@Composable
fun DashboardStats(state: TrainerActivityState) {

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {

            GlassStatCard(
                "Active",
                state.activeClientsCount.toString(),
                Color(0xFF4CAF50)
            )

            GlassStatCard(
                "Inactive",
                state.inactiveClientsCount.toString(),
                Color(0xFFF44336)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {

            GlassStatCard(
                "Missed",
                state.missedWorkoutsCount.toString(),
                Color(0xFFFF9800)
            )

            GlassStatCard(
                "Weekly %",
                "${state.weeklyCompletionPercent}%",
                Color(0xFF2196F3)
            )
        }
    }
}



@Composable
fun RowScope.GlassStatCard(
    title: String,
    value: String,
    color: Color
) {

    val infiniteTransition = rememberInfiniteTransition()

    val glow by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(2000),
            RepeatMode.Reverse
        )
    )

    Card(
        modifier = Modifier
            .weight(1f)
            .height(100.dp)
            .shadow(10.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp)
    ) {

        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            color.copy(alpha = glow),
                            color.copy(alpha = 0.5f)
                        )
                    )
                )
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {

            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                Text(
                    title,
                    color = Color.White
                )

                Text(
                    value,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                )
            }
        }
    }
}





@Composable
fun QuickActions(
    onReminder: () -> Unit,
    onMotivation: () -> Unit,
    onAssignWorkout: () -> Unit
) {

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        ActionButton("Remind", R.drawable.ic_bell, onReminder)

        ActionButton("Motivate", R.drawable.ic_favorite, onMotivation)

        ActionButton("Assign", R.drawable.ic_add, onAssignWorkout)
    }
}



@Composable
fun RowScope.ActionButton(
    text: String,
    icon: Int,
    onClick: () -> Unit
) {

    ElevatedCard(
        modifier = Modifier
            .weight(1f)
            .height(80.dp),
        onClick = onClick
    ) {

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Icon(
                painterResource(icon),
                null
            )

            Spacer(Modifier.height(4.dp))

            Text(text)
        }
    }
}





@Composable
fun RiskCard(count: Int) {

    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFEBEE)
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {

            Icon(
                painterResource(R.drawable.ic_warning),
                null,
                tint = Color.Red
            )

            Spacer(Modifier.width(8.dp))

            Text(
                "$count clients inactive for 48 hours",
                color = Color.Red,
                fontWeight = FontWeight.Bold
            )
        }
    }
}






@Composable
fun ActivityCharts(
    activityData: Map<String, Int>,
    caloriesData: Map<String, Int>
) {

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

        Text("Weekly Activity")

        AnimatedBarChart(activityData, Color(0xFF2196F3))

        Text("Calories Burned")

        AnimatedBarChart(caloriesData, Color(0xFF4CAF50))
    }
}





@Composable
fun AnimatedBarChart(
    data: Map<String, Int>,
    color: Color
) {

    val maxValue = data.values.maxOrNull() ?: 1

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {

        data.forEach { (day, value) ->

            // SAFE DIVISION
            val target = if (maxValue == 0) 0f else (value.toFloat() / maxValue.toFloat()).coerceIn(0f, 1f)

            val animatedHeight by animateFloatAsState(
                targetValue = target,
                animationSpec = tween(900),
                label = "barAnimation"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.fillMaxHeight()
            ) {

                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .fillMaxHeight(animatedHeight)
                        .clip(RoundedCornerShape(6.dp))
                        .background(color)
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = day,
                    fontSize = 11.sp
                )
            }
        }
    }
}





@Composable
fun ClientCard(
    client: User,
    isAtRisk: Boolean,
    onReminderClick: () -> Unit,
    onMotivationClick: () -> Unit
) {

    val scale by animateFloatAsState(
        if (isAtRisk) 1.02f else 1f
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(20.dp)
    ) {

        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(client.profileImageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {

                Text(client.name, fontWeight = FontWeight.Bold)

                Text(
                    "Last active: ${formatTimestamp(client.lastActive)}",
                    fontSize = 12.sp
                )

                Text(
                    "🔥 ${client.streak} day streak"
                )
            }

            if (isAtRisk) {
                IconButton(onClick = onReminderClick) {
                    Icon(
                        painterResource(R.drawable.ic_bell),
                        contentDescription = "Send Reminder",
                        tint = Color.Red
                    )
                }
            }

            IconButton(onClick = onMotivationClick) {
                Icon(painterResource(R.drawable.ic_favorite), null)
            }
        }
    }
}





private fun formatTimestamp(timestamp: Long): String {

    val sdf = SimpleDateFormat(
        "MMM dd HH:mm",
        Locale.getDefault()
    )

    return sdf.format(timestamp)
}