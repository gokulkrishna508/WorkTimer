package com.example.worktimer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.worktimer.data.TimeTrackerRepository
import com.example.worktimer.data.TimerState
import com.example.worktimer.data.WorkSessionEntity
import com.example.worktimer.widget.TimeTrackerWidgetProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TimeTrackerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TimeTrackerRepository(application)
    private val appContext = application.applicationContext

    // Ticks every second to keep UI fresh
    private val _tick = MutableStateFlow(System.currentTimeMillis())

    // Today's saved session from Room
    private val _todayDbSession = repository.getTodaySession()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val targetReachedEventTime: StateFlow<Long> = repository.targetReachedEventTime

    init {
        viewModelScope.launch {
            checkDateRollover()
            while (isActive) {
                val now = System.currentTimeMillis()
                _tick.value = now
                
                // Check for midnight rollover every minute or so, or on every tick
                val currentDay = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(now))
                if (currentDay != repository.liveSession.value.lastActiveDate && repository.liveSession.value.lastActiveDate.isNotEmpty()) {
                    checkDateRollover()
                }

                if (repository.completeDailyTargetIfNeeded()) {
                    refreshWidgets()
                }
                
                delay(1000)
            }
        }
    }

    private suspend fun checkDateRollover() {
        val live = repository.liveSession.value
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        
        if (live.lastActiveDate.isNotEmpty() && live.lastActiveDate != today) {
            // Save the session to the PREVIOUS date and reset
            repository.stopAndSaveSession(live.lastActiveDate)
        }
    }

    // ── Focus tab state ──
    val uiState: StateFlow<TimeTrackerUiState> = combine(
        repository.liveSession,
        _tick,
        _todayDbSession
    ) { live, now, dbSession ->
        val currentWorkDuration = if (live.state == TimerState.WORKING) {
            now - live.lastStateChangeTime
        } else 0L

        val currentBreakDuration = if (live.state == TimerState.BREAK) {
            now - live.lastStateChangeTime
        } else 0L

        val liveWork = live.totalWorkTime + currentWorkDuration
        val liveBreak = live.totalBreakTime + currentBreakDuration

        // Total today = saved DB sessions + current live session
        val totalWorkToday = (dbSession?.totalWorkMillis ?: 0L) + liveWork
        val totalBreakToday = (dbSession?.totalBreakMillis ?: 0L) + liveBreak

        val currentSessionMillis = when (live.state) {
            TimerState.WORKING -> currentWorkDuration
            TimerState.BREAK -> currentBreakDuration
            TimerState.STOPPED -> 0L
        }

        // Progress is total work hours vs target (8h default)
        val targetMs = (live.targetHours * 3600 * 1000).toLong()
        val progress = if (targetMs > 0) totalWorkToday.toFloat() / targetMs else 0f

        TimeTrackerUiState(
            state = live.state,
            currentSessionMillis = currentSessionMillis,
            elapsedWorkMillis = liveWork,
            elapsedBreakMillis = liveBreak,
            totalWorkTodayMillis = totalWorkToday,
            totalBreakTodayMillis = totalBreakToday,
            targetHours = live.targetHours,
            progress = progress.coerceIn(0f, 1f),
            todaySessionCount = (dbSession?.sessionCount ?: 0) + if (live.state != TimerState.STOPPED) 1 else 0
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TimeTrackerUiState())

    // ── Weekly tab state ──
    private val weekDates = repository.getWeekDates()

    val weeklyState: StateFlow<WeeklyUiState> = repository.getWeekSessions()
        .map { sessions ->
            val sessionMap = sessions.associateBy { it.date }
            val dailyData = weekDates.map { date ->
                val entity = sessionMap[date]
                DayData(
                    date = date,
                    workHours = (entity?.totalWorkMillis ?: 0L) / (1000f * 3600f),
                    breakHours = (entity?.totalBreakMillis ?: 0L) / (1000f * 3600f),
                    targetHours = entity?.targetHours ?: 8f
                )
            }
            val totalWorkHours = dailyData.sumOf { it.workHours.toDouble() }.toFloat()
            val totalBreakHours = dailyData.sumOf { it.breakHours.toDouble() }.toFloat()
            val totalH = totalWorkHours.toInt()
            val totalM = ((totalWorkHours - totalH) * 60).toInt()

            WeeklyUiState(
                days = dailyData,
                totalWorkHours = totalWorkHours,
                totalBreakHours = totalBreakHours,
                formattedTotalH = totalH,
                formattedTotalM = totalM
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WeeklyUiState())

    // ── Actions ──
    fun onWorkingClicked() {
        viewModelScope.launch {
            repository.startWorking()
            refreshWidgets()
        }
    }

    fun onBreakClicked() {
        viewModelScope.launch {
            repository.startBreak()
            refreshWidgets()
        }
    }

    fun onStopClicked() {
        viewModelScope.launch {
            repository.stopAndSaveSession()
            refreshWidgets()
        }
    }

    fun onTargetHoursChanged(hours: Float) {
        viewModelScope.launch {
            repository.updateTargetHours(hours)
            refreshWidgets()
        }
    }

    fun onTargetReachedDialogDismissed(eventTime: Long) {
        repository.acknowledgeTargetReachedEvent(eventTime)
    }

    private fun refreshWidgets() {
        TimeTrackerWidgetProvider.updateAllWidgets(appContext, repository.liveSession.value)
    }
}

// ── UI State models ──

data class TimeTrackerUiState(
    val state: TimerState = TimerState.STOPPED,
    val currentSessionMillis: Long = 0L,
    val elapsedWorkMillis: Long = 0L,
    val elapsedBreakMillis: Long = 0L,
    val totalWorkTodayMillis: Long = 0L,
    val totalBreakTodayMillis: Long = 0L,
    val targetHours: Float = 8f,
    val progress: Float = 0f,
    val todaySessionCount: Int = 0
)

data class DayData(
    val date: String = "",
    val workHours: Float = 0f,
    val breakHours: Float = 0f,
    val targetHours: Float = 8f
)

data class WeeklyUiState(
    val days: List<DayData> = emptyList(),
    val totalWorkHours: Float = 0f,
    val totalBreakHours: Float = 0f,
    val formattedTotalH: Int = 0,
    val formattedTotalM: Int = 0
)
