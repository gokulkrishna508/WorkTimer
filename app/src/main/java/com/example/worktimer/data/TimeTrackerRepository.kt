package com.example.worktimer.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.SharedPreferences
import android.content.Intent
import com.example.worktimer.widget.TimeTrackerWidgetProvider
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

private val LIVE_SESSION_PREF_KEYS = setOf(
    "state",
    "startTime",
    "lastStateChangeTime",
    "totalWorkTime",
    "totalBreakTime",
    "targetHours",
    "lastActiveDate",
    "targetReachedEventTime",
    "targetReachedAcknowledgedTime"
)

class TimeTrackerRepository(context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("time_tracker_prefs", Context.MODE_PRIVATE)
    private val db = WorkTimerDatabase.getInstance(appContext)
    private val dao = db.workSessionDao()

    private val _liveSession = MutableStateFlow(loadLiveSession())
    val liveSession: StateFlow<LiveTimerSession> = _liveSession.asStateFlow()

    private val _targetReachedEventTime = MutableStateFlow(loadPendingTargetReachedEventTime())
    val targetReachedEventTime: StateFlow<Long> = _targetReachedEventTime.asStateFlow()

    private val preferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key in LIVE_SESSION_PREF_KEYS) {
                _liveSession.value = loadLiveSession()
                _targetReachedEventTime.value = loadPendingTargetReachedEventTime()
            }
        }

    init {
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

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

    private fun loadPendingTargetReachedEventTime(): Long {
        val eventTime = prefs.getLong("targetReachedEventTime", 0L)
        val acknowledgedTime = prefs.getLong("targetReachedAcknowledgedTime", 0L)
        return if (eventTime > acknowledgedTime) eventTime else 0L
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

    private suspend fun getTotalWorkTodayMillis(session: LiveTimerSession): Long {
        val savedWork = dao.getSessionByDateOnce(todayDate())?.totalWorkMillis ?: 0L
        val liveWork = if (session.state == TimerState.WORKING) {
            session.totalWorkTime + (System.currentTimeMillis() - session.lastStateChangeTime)
        } else {
            session.totalWorkTime
        }
        return savedWork + liveWork
    }

    private suspend fun hasReachedDailyTarget(session: LiveTimerSession): Boolean {
        val targetMs = (session.targetHours * 3600 * 1000).toLong()
        return targetMs > 0L && getTotalWorkTodayMillis(session) >= targetMs
    }

    private fun notifyTargetReached() {
        val eventTime = System.currentTimeMillis()
        prefs.edit().putLong("targetReachedEventTime", eventTime).apply()
        _targetReachedEventTime.value = eventTime
    }

    fun acknowledgeTargetReachedEvent(eventTime: Long) {
        prefs.edit().putLong("targetReachedAcknowledgedTime", eventTime).apply()
        _targetReachedEventTime.value = loadPendingTargetReachedEventTime()
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
        if (hasReachedDailyTarget(current)) {
            notifyTargetReached()
            cancelDailyLimitAlarm()
            return
        }
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
                scheduleDailyLimitAlarm()
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
                scheduleDailyLimitAlarm()
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
                cancelDailyLimitAlarm()
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
                cancelDailyLimitAlarm()
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
        cancelDailyLimitAlarm()
    }

    suspend fun updateTargetHours(hours: Float) {
        checkAndRolloverIfNeeded()
        val current = _liveSession.value
        saveLiveSession(current.copy(targetHours = hours, lastActiveDate = todayDate()))
        if (hasReachedDailyTarget(_liveSession.value)) {
            completeDailyTargetIfNeeded()
        } else if (_liveSession.value.state == TimerState.WORKING) {
            scheduleDailyLimitAlarm()
        }
    }

    suspend fun completeDailyTargetIfNeeded(): Boolean {
        val current = _liveSession.value
        if (current.state != TimerState.WORKING || !hasReachedDailyTarget(current)) {
            return false
        }

        stopAndSaveSession()
        notifyTargetReached()
        return true
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

    private suspend fun scheduleDailyLimitAlarm() {
        val current = _liveSession.value
        if (current.state != TimerState.WORKING) return

        val targetMs = (current.targetHours * 3600 * 1000).toLong()
        val workedToday = getTotalWorkTodayMillis(current)
        val remainingMs = targetMs - workedToday
        if (remainingMs <= 0L) {
            completeDailyTargetIfNeeded()
            return
        }

        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAtMillis = System.currentTimeMillis() + remainingMs
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            dailyLimitPendingIntent()
        )
    }

    private fun cancelDailyLimitAlarm() {
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(dailyLimitPendingIntent())
    }

    private fun dailyLimitPendingIntent(): PendingIntent {
        val intent = Intent(appContext, TimeTrackerWidgetProvider::class.java).apply {
            action = TimeTrackerWidgetProvider.ACTION_DAILY_LIMIT_REACHED
        }
        return PendingIntent.getBroadcast(
            appContext,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
