// File: com/example/fitlink/ui/viewmodels/CalendarViewModel.kt
package com.example.fitlink.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlink.data.models.DailyStats
import com.example.fitlink.data.models.Workout
import com.example.fitlink.data.models.WorkoutStatus
import com.example.fitlink.data.repositories.DailyStatsRepository
import com.example.fitlink.data.repositories.WorkoutRepository
import com.example.fitlink.data.repositories.UserRepository
import com.example.fitlink.data.service.FirebaseService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.*
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.math.max

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val dailyStatsRepository: DailyStatsRepository,
    private val userRepository: UserRepository,
    private val firebaseService: FirebaseService
) : ViewModel() {

    // ---------------------------
    // Helper data classes
    // ---------------------------
    data class DayInfo(
        val dateStartMillis: Long, // start of day millis (device timezone)
        val dayOfMonth: Int,
        val isCompleted: Boolean = false,
        val workoutsCompleted: Int = 0,
        val caloriesBurned: Int = 0,
        val isFuture: Boolean = false
    )

    data class StreakSegment(
        val startMillis: Long,
        val endMillis: Long,
        val lengthDays: Int
    )

    // =========================
    // UI STATE
    // =========================
    data class CalendarUiState(
        val currentMonth: YearMonth = YearMonth.now(),
        val monthWorkouts: List<Workout> = emptyList(),
        val monthStats: List<DailyStats> = emptyList(),
        val selectedDate: Long? = null,
        val dateWorkouts: List<Workout> = emptyList(),
        val isLoading: Boolean = false,
        val isRefreshing: Boolean = false,
        val error: String? = null,
        // dayMap for UI rendering — key = startOfDayMillis
        val dayMap: Map<Long, DayInfo> = emptyMap(),
        // streak segments within the fetched range (month)
        val streakSegments: List<StreakSegment> = emptyList(),
        val currentStreak: Int = 0,
        val longestStreak: Int = 0,
        val largestGapDays: Int = 0,
        val nextSuggestedDayMillis: Long? = null,
        val lastStreakDateMillis: Long? = null // optional friendly date to show last active
    )

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    // -------------------------
    // internals
    // -------------------------
    private var currentUserId: String = ""
    private var loadJob: Job? = null

    private val zone: ZoneId = ZoneId.systemDefault()

    // LISTEN to user doc to pick up authoritative streak/last active info
    fun initialize(userId: String) {
        if (userId.isBlank()) return
        if (currentUserId == userId) return
        currentUserId = userId

        viewModelScope.launch {
            firebaseService.listenToUser(currentUserId)
                .catch { /* log if you have logger */ }
                .collect { user ->
                    user?.let {
                        _uiState.update { s ->
                            s.copy(
                                currentStreak = max(s.currentStreak, it.streak),
                                longestStreak = max(s.longestStreak, it.longestStreak),
                                lastStreakDateMillis = it.lastStreakDate
                            )
                        }
                    }
                }
        }
        viewModelScope.launch {
            firebaseService.listenToUser(currentUserId).collect { }
        }

        viewModelScope.launch {
            workoutRepository.refreshEvents.collect {
                loadMonthData(true)
            }
        }

        loadMonthData()
    }

    // Month navigation
    fun nextMonth() { updateMonth(_uiState.value.currentMonth.plusMonths(1)) }
    fun previousMonth() { updateMonth(_uiState.value.currentMonth.minusMonths(1)) }

    private fun updateMonth(newMonth: YearMonth) {
        _uiState.update { it.copy(currentMonth = newMonth) }
        loadMonthData()
    }

    /**
     * Load workouts + daily stats for the month (and builds derived dayMap & streaks).
     * isRefresh toggles UI loading flags.
     */
    fun loadMonthData(isRefresh: Boolean = false) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = !isRefresh, isRefreshing = isRefresh, error = null) }

            val month = _uiState.value.currentMonth
            val year = month.year
            val monthValue = month.monthValue

            try {
                // Repository methods should be efficient; they return Result<List<Workout>>
                val workoutsResult = workoutRepository.getWorkoutsByMonth(currentUserId, year, monthValue)
                val statsResult = dailyStatsRepository.getMonthlyStats(currentUserId, year, monthValue)

                val workouts = workoutsResult.getOrElse { throw it }
                val stats = statsResult.getOrElse { throw it }

                // Build maps keyed by start-of-day millis (device timezone)
                val statsMap = stats.associateBy { normalizeToStartOfDay(it.date) }

                // Completed workouts grouping: include both scheduled/completed date fields for robustness
                val completedWorkoutsByDay = workouts
                    .filter { it.status == WorkoutStatus.COMPLETED }
                    .groupBy { normalizeToStartOfDay( if (it.completedDate != null && it.completedDate > 0L) it.completedDate else it.scheduledDate ) }

                // Build day map for UI
                val dayMap = buildDayMapForMonth(month, statsMap, completedWorkoutsByDay, workouts)

                // Compute streaks + gaps
                val streakSegments = computeStreakSegments(dayMap)
                val computedCurrentStreak = computeCurrentStreak(dayMap)
                val longestStreakComputed = streakSegments.maxOfOrNull { it.lengthDays } ?: 0
                val longestStreakFromUser = _uiState.value.longestStreak // authoritative if user doc says more
                val longestStreak = max(longestStreakFromUser, longestStreakComputed)

                val largestGap = computeLargestGap(dayMap)
                val nextSuggested = computeNextSuggestedDay(dayMap)

                _uiState.update {
                    it.copy(
                        monthWorkouts = workouts,
                        monthStats = stats,
                        dayMap = dayMap,
                        streakSegments = streakSegments,
                        currentStreak = max(it.currentStreak, computedCurrentStreak),
                        longestStreak = longestStreak,
                        largestGapDays = largestGap,
                        nextSuggestedDayMillis = nextSuggested,
                        isLoading = false,
                        isRefreshing = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, isRefreshing = false, error = e.message ?: "Failed to load month data")
                }
            }
        }
    }

    fun refresh() { loadMonthData(isRefresh = true) }
    fun retry() { loadMonthData() }

    // date selection
    fun selectDate(timestamp: Long) {
        _uiState.update { it.copy(selectedDate = timestamp) }
        viewModelScope.launch {
            try {
                val result = workoutRepository.getWorkoutsByDate(currentUserId, timestamp)
                _uiState.update { it.copy(dateWorkouts = result.getOrDefault(emptyList())) }
            } catch (_: Exception) { /* swallow */ }
        }
    }

    fun clearSelectedDate() {
        _uiState.update { it.copy(selectedDate = null, dateWorkouts = emptyList()) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    // Core helpers
    private fun buildDayMapForMonth(
        month: YearMonth,
        statsMap: Map<Long, DailyStats>,
        completedWorkoutsByDay: Map<Long, List<Workout>>,
        allWorkoutsInMonth: List<Workout>
    ): Map<Long, DayInfo> {
        val map = mutableMapOf<Long, DayInfo>()
        val first = month.atDay(1)
        val zone = this.zone
        val todayStart = normalizeToStartOfDay(System.currentTimeMillis())

        for (d in 1..month.lengthOfMonth()) {
            val localDate = month.atDay(d)
            val dayStart = localDate.atStartOfDay(zone).toInstant().toEpochMilli()

            val stat = statsMap[dayStart]
            val completedList = completedWorkoutsByDay[dayStart].orEmpty()

            // Some days may not have DailyStats available — fallback to workouts sums
            val workoutsCompleted = stat?.workoutsCompleted ?: completedList.size
            val calories = stat?.caloriesBurned ?: completedList.sumOf { it.caloriesBurnEstimate ?: 0 }

            val isFuture = dayStart > todayStart
            val isCompleted = workoutsCompleted > 0 || calories > 0

            map[dayStart] = DayInfo(
                dateStartMillis = dayStart,
                dayOfMonth = d,
                isCompleted = isCompleted,
                workoutsCompleted = workoutsCompleted,
                caloriesBurned = calories,
                isFuture = isFuture
            )
        }

        return map.toSortedMap()
    }

    private fun computeStreakSegments(dayMap: Map<Long, DayInfo>): List<StreakSegment> {
        if (dayMap.isEmpty()) return emptyList()
        val keys = dayMap.keys.sorted()
        val segments = mutableListOf<StreakSegment>()

        var segStart: Long? = null
        var prev: Long? = null

        for (k in keys) {
            val info = dayMap[k] ?: continue
            if (info.isCompleted) {
                if (segStart == null) segStart = k
                else {
                    if (prev != null) {
                        val expected = prev + MILLIS_IN_DAY
                        if (k != expected) {
                            // end previous
                            val length = (ChronoUnit.DAYS.between(Instant.ofEpochMilli(segStart).atZone(zone).toLocalDate(), Instant.ofEpochMilli(prev).atZone(
                                zone).toLocalDate()) + 1).toInt()
                            segments.add(StreakSegment(segStart, prev, length))
                            segStart = k
                        }
                    }
                }
            } else {
                if (segStart != null && prev != null) {
                    val length = (ChronoUnit.DAYS.between(Instant.ofEpochMilli(segStart).atZone(zone).toLocalDate(), Instant.ofEpochMilli(prev).atZone(zone).toLocalDate()) + 1).toInt()
                    segments.add(StreakSegment(segStart, prev, length))
                    segStart = null
                }
            }
            prev = k
        }

        if (segStart != null && prev != null) {
            val length = (ChronoUnit.DAYS.between(Instant.ofEpochMilli(segStart).atZone(zone).toLocalDate(), Instant.ofEpochMilli(prev).atZone(zone).toLocalDate()) + 1).toInt()
            segments.add(StreakSegment(segStart, prev, length))
        }

        return segments
    }

    /**
     * Compute current streak by counting back from today while dayMap reports completed.
     * If dayMap doesn't include days prior to month, rely on authoritative user streak value (already listened).
     */
    private fun computeCurrentStreak(dayMap: Map<Long, DayInfo>): Int {
        if (dayMap.isEmpty()) return 0
        var count = 0
        val todayStart = normalizeToStartOfDay(System.currentTimeMillis())
        var dayStart = todayStart

        // Use dayMap if dayStart present; else stop (we do not attempt remote backfill here).
        while (true) {
            val info = dayMap[dayStart]
            if (info != null && info.isCompleted) count++ else break
            dayStart -= MILLIS_IN_DAY
            val earliest = dayMap.keys.minOrNull() ?: break
            if (dayStart < earliest) break
        }
        return count
    }

    private fun computeLargestGap(dayMap: Map<Long, DayInfo>): Int {
        if (dayMap.isEmpty()) return 0
        val keys = dayMap.keys.sorted()
        var lastCompleted: Long? = null
        var maxGap = 0
        for (k in keys) {
            val info = dayMap[k] ?: continue
            if (info.isCompleted) {
                if (lastCompleted != null) {
                    val gap = ((k - lastCompleted) / MILLIS_IN_DAY).toInt() - 1
                    if (gap > maxGap) maxGap = gap
                }
                lastCompleted = k
            }
        }
        return maxGap.coerceAtLeast(0)
    }

    private fun computeNextSuggestedDay(dayMap: Map<Long, DayInfo>): Long? {
        if (dayMap.isEmpty()) return null
        val todayStart = normalizeToStartOfDay(System.currentTimeMillis())
        // last completed day (not future)
        val lastCompleted = dayMap.keys.filter { !dayMap[it]!!.isFuture && dayMap[it]!!.isCompleted }.maxOrNull()
        val candidate = (lastCompleted ?: todayStart) + MILLIS_IN_DAY
        val monthEnd = dayMap.keys.maxOrNull() ?: return null
        return if (candidate <= monthEnd && candidate <= todayStart + MILLIS_IN_DAY) candidate else null
    }

    private fun normalizeToStartOfDay(ts: Long?): Long {
        val z = zone
        val epoch = ts ?: System.currentTimeMillis()
        val local = Instant.ofEpochMilli(epoch).atZone(z).toLocalDate()
        return local.atStartOfDay(z).toInstant().toEpochMilli()
    }

    companion object {
        const val MILLIS_IN_DAY = 24L * 60L * 60L * 1000L
    }
}