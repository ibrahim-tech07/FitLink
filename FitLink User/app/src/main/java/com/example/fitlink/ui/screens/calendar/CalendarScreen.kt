// File: com/example/fitlink/ui/screens/calendar/CalendarScreen.kt
package com.example.fitlink.ui.screens.calendar

import androidx.compose.animation.*
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.fitlink.R
import com.example.fitlink.data.models.Workout
import com.example.fitlink.data.models.WorkoutStatus
import com.example.fitlink.ui.screens.dashboard.*
import com.example.fitlink.ui.viewmodels.CalendarViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CalendarScreen(
    userId: String,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val lazyListState = rememberLazyListState()

    LaunchedEffect(Unit) { viewModel.initialize(userId) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundSoft)
    ) {
        LazyColumn(state = lazyListState, modifier = Modifier.fillMaxSize()) {
            item {
                CalendarHeader(
                    currentMonth = uiState.currentMonth,
                    onPrevious = { viewModel.previousMonth() },
                    onNext = { viewModel.nextMonth() }
                )
            }

            item {
                AnimatedContent(targetState = uiState.isLoading, transitionSpec = { fadeIn() with fadeOut() }) { loading ->
                    if (loading) StatsShimmer() else {
                        StatsRow(
                            monthWorkouts = uiState.monthWorkouts,
                            monthStats = uiState.monthStats,
                            currentStreak = uiState.currentStreak,
                            lastStreakDate = uiState.lastStreakDateMillis
                        )
                    }
                }
            }

            item {
                AnimatedContent(targetState = uiState.isLoading, transitionSpec = { fadeIn() with fadeOut() }) { loading ->
                    if (loading) CalendarGridShimmer() else {
                        CalendarGrid(
                            currentMonth = uiState.currentMonth,
                            dayMap = uiState.dayMap,
                            selectedDate = uiState.selectedDate,
                            onDateSelected = { viewModel.selectDate(it) }
                        )
                    }
                }
            }

            item { CalendarLegend() }

            item {
                UpcomingWorkouts(monthWorkouts = uiState.monthWorkouts, isLoading = uiState.isLoading)
            }
        }

        if (uiState.error != null && !uiState.isLoading) {
            ErrorOverlay(error = uiState.error!!, onRetry = {
                viewModel.clearError()
                viewModel.retry()
            })
        }
    }
}

// Header + StatsRow + StatCard identical to your previous version but StatsRow uses lastStreakDateMillis (nullable)
@Composable
fun CalendarHeader(
    currentMonth: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxWidth(), color = BackgroundSoft, shadowElevation = 0.dp) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(text = "Workout Calendar", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Text(text = "Track your fitness journey", fontSize = 14.sp, color = TextMedium, modifier = Modifier.padding(top = 4.dp))
            Spacer(modifier = Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrevious, interactionSource = remember { MutableInteractionSource() }) {
                    Icon(painter = painterResource(R.drawable.ic_chevron_left), contentDescription = "Previous month", tint = TextDark)
                }

                Text(text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextDark)

                IconButton(onClick = onNext, interactionSource = remember { MutableInteractionSource() }) {
                    Icon(painter = painterResource(R.drawable.ic_chevron_right), contentDescription = "Next month", tint = TextDark)
                }
            }
        }
    }
}

@Composable
fun StatsRow(monthWorkouts: List<Workout>, monthStats: List<com.example.fitlink.data.models.DailyStats>, currentStreak: Int, lastStreakDate: Long?) {
    val completed = monthWorkouts.count { it.status == WorkoutStatus.COMPLETED }
    val planned = monthWorkouts.count { it.status == WorkoutStatus.SCHEDULED }

    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard(icon = R.drawable.ic_check, value = "$completed", label = "Completed", iconColor = PrimaryGreen, backgroundColor = LightGreenAccent, modifier = Modifier.weight(1f))
        StatCard(icon = R.drawable.ic_flame, value = "$currentStreak", label = "Day Streak", iconColor = PurpleGradientStart, backgroundColor = LightPurpleBg, modifier = Modifier.weight(1f))
        StatCard(icon = R.drawable.ic_calendar, value = "$planned", label = "Planned", iconColor = Color(0xFF059669), backgroundColor = Color(0xFFD1FAE5), modifier = Modifier.weight(1f))
    }

    lastStreakDate?.let {
        val formatter = java.text.SimpleDateFormat("MMM dd", Locale.getDefault())
        Text(text = "Last active: ${formatter.format(java.util.Date(it))}", fontSize = 11.sp, color = TextLight, modifier = Modifier.padding(start = 20.dp, top = 4.dp))
    }
}

@Composable
fun StatCard(icon: Int, value: String, label: String, iconColor: Color, backgroundColor: Color, modifier: Modifier = Modifier) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = CardWhite), elevation = CardDefaults.cardElevation(2.dp), modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(shape = CircleShape, color = backgroundColor, modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(painter = painterResource(icon), contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Text(text = label, fontSize = 12.sp, color = TextMedium)
        }
    }
}

// ---------------- CALENDAR GRID using dayMap (source of truth)
enum class DayType { COMPLETED, PLANNED, REST }
data class DayData(val day: Int, val type: DayType, val isToday: Boolean, val date: Long)

@Composable
fun CalendarGrid(currentMonth: YearMonth, dayMap: Map<Long, com.example.fitlink.ui.viewmodels.CalendarViewModel.DayInfo>, selectedDate: Long?, onDateSelected: (Long) -> Unit) {
    val zone = java.time.ZoneId.systemDefault()
    val today = LocalDate.now()
    val days = remember(currentMonth, dayMap) {
        buildCalendarDaysFromDayMap(currentMonth, dayMap, today)
    }

    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = CardWhite), elevation = CardDefaults.cardElevation(4.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("S","M","T","W","T","F","S").forEach { d ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(text = d, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextLight)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            val firstDayOffset = currentMonth.atDay(1).dayOfWeek.value % 7
            val totalCells = (days.size + firstDayOffset + 6) / 7 * 7

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(totalCells / 7) { weekIndex ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        repeat(7) { dayIndex ->
                            val cellIndex = weekIndex * 7 + dayIndex
                            val dayData = if (cellIndex >= firstDayOffset && cellIndex < firstDayOffset + days.size) {
                                days[cellIndex - firstDayOffset]
                            } else null

                            Box(modifier = Modifier.weight(1f).aspectRatio(1f)) {
                                if (dayData != null) {
                                    DayCell(dayData = dayData, isSelected = selectedDate == dayData.date, onClick = { onDateSelected(dayData.date) })
                                } else {
                                    Spacer(modifier = Modifier.fillMaxSize())
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DayCell(dayData: DayData, isSelected: Boolean, onClick: () -> Unit) {
    val backgroundColor by animateColorAsState(targetValue = when {
        isSelected -> PurpleGradientStart
        dayData.isToday -> LightPurpleBg
        dayData.type == DayType.COMPLETED -> LightGreenAccent
        dayData.type == DayType.PLANNED -> Color(0xFFD1FAE5)
        else -> Color.Transparent
    }, animationSpec = tween(300))

    val borderColor = if (isSelected) PurpleGradientStart else Color.Transparent

    Box(modifier = Modifier.fillMaxSize().padding(2.dp).clip(RoundedCornerShape(8.dp)).background(backgroundColor).border(1.dp, borderColor, RoundedCornerShape(8.dp)).clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onClick() }, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = dayData.day.toString(), fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = when {
                isSelected -> Color.White
                dayData.isToday -> PurpleGradientStart
                dayData.type == DayType.COMPLETED -> PrimaryGreen
                dayData.type == DayType.PLANNED -> TextDark
                else -> TextLight
            })
            if (dayData.type == DayType.COMPLETED) {
                Icon(painter = painterResource(R.drawable.ic_check_circle), contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(10.dp))
            } else if (dayData.type == DayType.PLANNED) {
                Box(modifier = Modifier.size(6.dp).background(PrimaryGreen, CircleShape))
            }
        }
    }
}

fun buildCalendarDaysFromDayMap(currentMonth: YearMonth, dayMap: Map<Long, com.example.fitlink.ui.viewmodels.CalendarViewModel.DayInfo>, today: LocalDate): List<DayData> {
    val zone = java.time.ZoneId.systemDefault()
    val list = mutableListOf<DayData>()

    for (d in 1..currentMonth.lengthOfMonth()) {
        val localDate = currentMonth.atDay(d)
        val ts = localDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val info = dayMap[ts]
        val type = when {
            info?.isCompleted == true -> DayType.COMPLETED
            // planned if there are scheduled workouts for this day but no completion
            info?.workoutsCompleted == 0 && info?.caloriesBurned == 0 && info?.isFuture == false -> DayType.REST
            info?.isFuture == true -> DayType.REST
            else -> {
                // if stats say 0 but there are scheduled workouts — treat as planned
                if (info != null && info.workoutsCompleted == 0 && info.caloriesBurned == 0) DayType.REST else DayType.REST
            }
        }

        list.add(DayData(day = d, type = type, isToday = localDate == today, date = ts))
    }
    return list
}

// ==================== LEGEND ====================
@Composable
fun CalendarLegend() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        LegendItem(color = LightGreenAccent, label = "Completed")
        LegendItem(color = LightPurpleBg, label = "Today")
        LegendItem(color = Color(0xFFD1FAE5), label = "Planned")
        LegendItem(color = Color.Transparent, label = "Rest")
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (color == Color.Transparent) CardWhite else color)
                .border(1.dp, TextLight, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextMedium
        )
    }
}

// ==================== UPCOMING WORKOUTS ====================
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun UpcomingWorkouts(monthWorkouts: List<Workout>, isLoading: Boolean) {
    val upcoming = remember(monthWorkouts) {
        monthWorkouts
            .filter { it.scheduledDate >= System.currentTimeMillis() && it.status != WorkoutStatus.COMPLETED }
            .sortedBy { it.scheduledDate }
            .take(3)
    }

    AnimatedContent(
        targetState = isLoading,
        transitionSpec = { fadeIn() with fadeOut() }
    ) { loading ->
        if (loading) {
            UpcomingShimmer()
        } else {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                elevation = CardDefaults.cardElevation(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Upcoming Workouts",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )

                    if (upcoming.isEmpty()) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No upcoming workouts",
                                fontSize = 14.sp,
                                color = TextLight
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.height(12.dp))
                        upcoming.forEach { workout ->
                            UpcomingWorkoutItem(workout = workout)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UpcomingWorkoutItem(workout: Workout) {
    val date = remember(workout.scheduledDate) {
        val cal = Calendar.getInstance()
        cal.timeInMillis = workout.scheduledDate
        val formatter = java.text.SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
        formatter.format(cal.time)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = LightPurpleBg,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_dumbbell),
                    contentDescription = null,
                    tint = PurpleGradientStart,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = workout.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextDark
            )
            Text(
                text = date,
                fontSize = 12.sp,
                color = TextMedium
            )
        }
        Text(
            text = "${workout.durationMinutes} min",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = PurpleGradientStart
        )
    }
}

// ==================== SHIMMER LOADERS ====================
@Composable
fun StatsShimmer() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(3) {
            ShimmerStatCard()
        }
    }
}

@Composable
fun ShimmerStatCard(modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .shimmerEffect()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(20.dp)
                    .shimmerEffect()
            )
            Box(
                modifier = Modifier
                    .width(50.dp)
                    .height(12.dp)
                    .padding(top = 4.dp)
                    .shimmerEffect()
            )
        }
    }
}

@Composable
fun CalendarGridShimmer() {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                repeat(7) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(16.dp)
                            .padding(2.dp)
                            .shimmerEffect()
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 5 rows of cells
            repeat(5) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    repeat(7) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .shimmerEffect()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UpcomingShimmer() {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(20.dp)
                    .shimmerEffect()
            )
            Spacer(modifier = Modifier.height(16.dp))
            repeat(3) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .shimmerEffect()
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(14.dp)
                                .shimmerEffect()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.5f)
                                .height(12.dp)
                                .shimmerEffect()
                        )
                    }
                }
                if (it < 2) Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

// Simple shimmer modifier
fun Modifier.shimmerEffect(): Modifier = composed {
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
            animation = tween(durationMillis = 1200, easing = LinearEasing)
        )
    )
    this.background(
        brush = Brush.horizontalGradient(
            colors = shimmerColors,
            startX = translateAnim.value,
            endX = translateAnim.value + 300f
        )
    )
}

// ==================== ERROR OVERLAY ====================
@Composable
fun ErrorOverlay(error: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundSoft),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
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
                text = "Oops!",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Text(
                text = error,
                fontSize = 14.sp,
                color = TextMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = PurpleGradientStart),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Try Again")
            }
        }
    }
}

// ==================== DATA MODELS & HELPERS ====================

fun buildCalendarDays(
    month: YearMonth,
    workouts: List<Workout>,
    today: LocalDate
): List<DayData> {

    val zone = java.time.ZoneId.systemDefault()

    // 🔥 Step 1: Group workouts by LocalDate
    val workoutsByDate: Map<LocalDate, List<Workout>> =
        workouts.groupBy { workout ->
            java.time.Instant.ofEpochMilli(workout.scheduledDate)
                .atZone(zone)
                .toLocalDate()
        }

    val days = mutableListOf<DayData>()

    val daysInMonth = month.lengthOfMonth()

    for (day in 1..daysInMonth) {

        val date = month.atDay(day)

        val timestamp = date
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()

        val dayWorkouts = workoutsByDate[date].orEmpty()

        val hasCompleted = dayWorkouts.any { it.status == WorkoutStatus.COMPLETED }
        val hasPlanned = dayWorkouts.any { it.status == WorkoutStatus.SCHEDULED }

        val type = when {
            hasCompleted -> DayType.COMPLETED
            hasPlanned -> DayType.PLANNED
            else -> DayType.REST
        }

        days.add(
            DayData(
                day = day,
                type = type,
                isToday = date == today,
                date = timestamp
            )
        )
    }

    return days
}

fun calculateStreak(workouts: List<Workout>): Int {
    val completedDays = workouts
        .filter { it.status == WorkoutStatus.COMPLETED }
        .map {
            val cal = Calendar.getInstance()
            cal.timeInMillis = it.scheduledDate
            cal.get(Calendar.DAY_OF_YEAR) to cal.get(Calendar.YEAR)
        }
        .distinct()
        .sortedBy { it.first }

    if (completedDays.isEmpty()) return 0

    var maxStreak = 1
    var currentStreak = 1

    for (i in 1 until completedDays.size) {
        val prev = completedDays[i - 1]
        val curr = completedDays[i]
        if (curr.first == prev.first + 1 && curr.second == prev.second) {
            currentStreak++
            maxStreak = maxOf(maxStreak, currentStreak)
        } else {
            currentStreak = 1
        }
    }
    return maxStreak
}