package com.example.worktimer.widget

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.worktimer.R
import com.example.worktimer.data.TimeTrackerRepository
import com.example.worktimer.data.TimerState
import kotlinx.coroutines.*
import java.text.SimpleDateFormat

class TimeTrackerWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        val repository = TimeTrackerRepository.getInstance(context)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val session = repository.liveSession.value
                val today = SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                
                if (session.lastActiveDate.isNotEmpty() && session.lastActiveDate != today) {
                    repository.stopAndSaveSession(session.lastActiveDate)
                }

                val finalSession = repository.liveSession.value
                for (appWidgetId in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId, finalSession)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE) {
            val pendingResult = goAsync()
            val repository = TimeTrackerRepository.getInstance(context)
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val current = repository.liveSession.value
                    if (current.state == TimerState.WORKING) {
                        repository.startBreak()
                    } else {
                        repository.startWorking()
                    }

                    updateAllWidgets(context, repository.liveSession.value)
                } finally {
                    pendingResult.finish()
                }
            }
        } else if (intent.action == ACTION_DAILY_LIMIT_REACHED) {
            val pendingResult = goAsync()
            val repository = TimeTrackerRepository.getInstance(context)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    repository.completeDailyTargetIfNeeded()
                    updateAllWidgets(context, repository.liveSession.value)
                } finally {
                    pendingResult.finish()
                }
            }
        } else if (intent.action == ACTION_MIDNIGHT_ROLLOVER) {
            val pendingResult = goAsync()
            val repository = TimeTrackerRepository.getInstance(context)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val session = repository.liveSession.value
                    val today = SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                    if (session.lastActiveDate.isNotEmpty() && session.lastActiveDate != today) {
                        repository.stopAndSaveSession(session.lastActiveDate)
                    }

                    repository.scheduleMidnightRollover()
                    
                    updateAllWidgets(context, repository.liveSession.value)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        const val ACTION_TOGGLE = "com.example.worktimer.ACTION_TOGGLE"
        const val ACTION_DAILY_LIMIT_REACHED = "com.example.worktimer.ACTION_DAILY_LIMIT_REACHED"
        const val ACTION_MIDNIGHT_ROLLOVER = "com.example.worktimer.ACTION_MIDNIGHT_ROLLOVER"

        fun updateAllWidgets(
            context: Context,
            session: com.example.worktimer.data.LiveTimerSession
        ) {
            val appContext = context.applicationContext
            val appWidgetManager = AppWidgetManager.getInstance(appContext)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(appContext, TimeTrackerWidgetProvider::class.java)
            )

            for (id in appWidgetIds) {
                updateAppWidget(appContext, appWidgetManager, id, session)
            }
        }

        @SuppressLint("RemoteViewLayout")
        internal fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            session: com.example.worktimer.data.LiveTimerSession
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_time_tracker)

            val isWorking = session.state == TimerState.WORKING
            
            // Calculate time left
            val now = System.currentTimeMillis()
            val elapsedToday = session.totalWorkTime + if (isWorking) (now - session.lastStateChangeTime) else 0L
            val targetMs = (session.targetHours * 3600 * 1000).toLong()
            val timeLeftMs = (targetMs - elapsedToday).coerceAtLeast(0L)
            val hasReachedDailyLimit = hasReachedDailyLimitToday(context)

            // UI State
            if (isWorking) {
                views.setViewVisibility(R.id.v_indicator_top, View.VISIBLE)
                views.setViewVisibility(R.id.v_indicator_bottom, View.GONE)
                views.setImageViewResource(R.id.iv_play_icon, R.drawable.ic_play) // White
                views.setImageViewResource(R.id.iv_pause_icon, R.drawable.ic_pause) // Brownish
                views.setTextViewText(R.id.tv_widget_status, "TRACKING")
                views.setTextColor(R.id.tv_widget_status, 0xFF2962FF.toInt())
            } else {
                views.setViewVisibility(R.id.v_indicator_top, View.GONE)
                views.setViewVisibility(R.id.v_indicator_bottom, View.VISIBLE)
                views.setImageViewResource(R.id.iv_play_icon, R.drawable.ic_play_blue)
                views.setImageViewResource(R.id.iv_pause_icon, R.drawable.ic_pause_white)
                views.setTextViewText(R.id.tv_widget_status, "PAUSED")
                views.setTextColor(R.id.tv_widget_status, 0xFF6B7280.toInt())
            }
            
            if (hasReachedDailyLimit) {
                views.setViewVisibility(R.id.tv_widget_time_left, View.GONE)
            } else {
                views.setViewVisibility(R.id.tv_widget_time_left, View.VISIBLE)
                views.setTextViewText(R.id.tv_widget_time_left, formatTimeLeft(timeLeftMs))
            }

            // Toggle pending intent (Pill only)
            val intent = Intent(context, TimeTrackerWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.fl_toggle_container, pendingIntent)

            // Open App intent (Root background)
            val openAppIntent = Intent(context, com.example.worktimer.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context, 100, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun formatTimeLeft(millis: Long): String {
            val totalMinutes = millis / (1000 * 60)
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            return "${hours}h ${minutes}m left"
        }

        private fun hasReachedDailyLimitToday(context: Context): Boolean {
            val today = SimpleDateFormat(
                "yyyy-MM-dd",
                java.util.Locale.getDefault()
            ).format(java.util.Date())
            val prefs = context.applicationContext.getSharedPreferences(
                "time_tracker_prefs",
                Context.MODE_PRIVATE
            )
            return prefs.getString("targetReachedDate", "") == today
        }
    }
}
