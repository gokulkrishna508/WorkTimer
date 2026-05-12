package com.example.worktimer.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class TimerState {
    STOPPED, WORKING, BREAK
}

data class LiveTimerSession(
    val state: TimerState = TimerState.STOPPED,
    val startTime: Long = 0L,
    val lastStateChangeTime: Long = 0L,
    val totalWorkTime: Long = 0L,
    val totalBreakTime: Long = 0L,
    val targetHours: Float = 8f,
    val lastActiveDate: String = ""
)

class TimeTrackerRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("time_tracker_prefs", Context.MODE_PRIVATE)
    private val db = WorkTimerDatabase.getInstance(context)
    private val dao = db.workSessionDao()

    private val _liveSession = MutableStateFlow(loadLiveSession())
    val liveSession: StateFlow<LiveTimerSession> = _liveSession.asStateFlow()

    private fun todayDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun loadLiveSession(): LiveTimerSession {
        return LiveTimerSession(
            state = TimerState.valueOf(
                prefs.getString("state", TimerState.STOPPED.name) ?: TimerState.STOPPED.name
            ),
            startTime = prefs.getLong("startTime", 0L),
            lastStateChangeTime = prefs.getLong("lastStateChangeTime", 0L),
            totalWorkTime = prefs.getLong("totalWorkTime", 0L),
            totalBreakTime = prefs.getLong("totalBreakTime", 0L),
            targetHours = prefs.getFloat("targetHours", 8f),
            lastActiveDate = prefs.getString("lastActiveDate", todayDate()) ?: todayDate()
        )
    }

    private fun saveLiveSession(session: LiveTimerSession) {
        prefs.edit()
            .putString("state", session.state.name)
            .putLong("startTime", session.startTime)
            .putLong("lastStateChangeTime", session.lastStateChangeTime)
            .putLong("totalWorkTime", session.totalWorkTime)
            .putLong("totalBreakTime", session.totalBreakTime)
            .putFloat("targetHours", session.targetHours)
            .putString("lastActiveDate", session.lastActiveDate)
            .apply()
        _liveSession.value = session
    }

    private suspend fun checkAndRolloverIfNeeded() {
        val current = _liveSession.value
        val today = todayDate()
        if (current.lastActiveDate.isNotEmpty() && current.lastActiveDate != today) {
            // Save prev day and reset
            stopAndSaveSession(current.lastActiveDate)
        }
    }

    suspend fun startWorking() {
        checkAndRolloverIfNeeded()
        val current = _liveSession.value
        val now = System.currentTimeMillis()
        when (current.state) {
            TimerState.STOPPED -> {
                saveLiveSession(
                    current.copy(
                        state = TimerState.WORKING,
                        startTime = now,
                        lastStateChangeTime = now,
                        totalWorkTime = 0L,
                        totalBreakTime = 0L,
                        lastActiveDate = todayDate()
                    )
                )
            }
            TimerState.BREAK -> {
                val breakDuration = now - current.lastStateChangeTime
                saveLiveSession(
                    current.copy(
                        state = TimerState.WORKING,
                        lastStateChangeTime = now,
                        totalBreakTime = current.totalBreakTime + breakDuration,
                        lastActiveDate = todayDate()
                    )
                )
            }
            TimerState.WORKING -> { /* already working */ }
        }
    }

    suspend fun startBreak() {
        checkAndRolloverIfNeeded()
        val current = _liveSession.value
        val now = System.currentTimeMillis()
        when (current.state) {
            TimerState.WORKING -> {
                val workDuration = now - current.lastStateChangeTime
                saveLiveSession(
                    current.copy(
                        state = TimerState.BREAK,
                        lastStateChangeTime = now,
                        totalWorkTime = current.totalWorkTime + workDuration,
                        lastActiveDate = todayDate()
                    )
                )
            }
            TimerState.STOPPED -> {
                saveLiveSession(
                    current.copy(
                        state = TimerState.BREAK,
                        startTime = now,
                        lastStateChangeTime = now,
                        totalWorkTime = 0L,
                        totalBreakTime = 0L,
                        lastActiveDate = todayDate()
                    )
                )
            }
            TimerState.BREAK -> { /* already on break */ }
        }
    }

    suspend fun stopAndSaveSession(date: String = todayDate()) {
        val current = _liveSession.value
        val now = System.currentTimeMillis()
        
        // Finalize current segment
        val finalWork = if (current.state == TimerState.WORKING) {
            current.totalWorkTime + (now - current.lastStateChangeTime)
        } else current.totalWorkTime
        
        val finalBreak = if (current.state == TimerState.BREAK) {
            current.totalBreakTime + (now - current.lastStateChangeTime)
        } else current.totalBreakTime

        // Save to Room
        val existing = dao.getSessionByDateOnce(date)
        val session = WorkSessionEntity(
            id = existing?.id ?: 0,
            date = date,
            targetHours = current.targetHours,
            totalWorkMillis = (existing?.totalWorkMillis ?: 0L) + finalWork,
            totalBreakMillis = (existing?.totalBreakMillis ?: 0L) + finalBreak,
            sessionCount = (existing?.sessionCount ?: 0) + 1
        )
        dao.insertOrUpdate(session)

        // Reset live state
        saveLiveSession(LiveTimerSession(targetHours = current.targetHours, lastActiveDate = todayDate()))
    }

    suspend fun updateTargetHours(hours: Float) {
        checkAndRolloverIfNeeded()
        val current = _liveSession.value
        saveLiveSession(current.copy(targetHours = hours, lastActiveDate = todayDate()))
    }

    fun getTodaySession(): Flow<WorkSessionEntity?> {
        return dao.getSessionByDate(todayDate())
    }

    fun getAllSessions(): Flow<List<WorkSessionEntity>> {
        return dao.getAllSessions()
    }

    fun getWeekSessions(): Flow<List<WorkSessionEntity>> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        // Go back to Monday of this week
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val dates = (0..6).map { offset ->
            val c = cal.clone() as Calendar
            c.add(Calendar.DAY_OF_YEAR, offset)
            sdf.format(c.time)
        }
        return dao.getSessionsByDates(dates)
    }

    fun getWeekDates(): List<String> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        return (0..6).map { offset ->
            val c = cal.clone() as Calendar
            c.add(Calendar.DAY_OF_YEAR, offset)
            sdf.format(c.time)
        }
    }
}
