package com.example.worktimer.widget

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.worktimer.R
import com.example.worktimer.data.TimeTrackerRepository
import com.example.worktimer.data.TimerState
import kotlinx.coroutines.*

class TimeTrackerWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val repository = TimeTrackerRepository(context)
        val session = repository.liveSession.value

        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, session)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE) {
            val pendingResult = goAsync()
            val repository = TimeTrackerRepository(context)
            
            @OptIn(DelicateCoroutinesApi::class)
            GlobalScope.launch {
                try {
                    val current = repository.liveSession.value
                    if (current.state == TimerState.WORKING) {
                        repository.startBreak()
                    } else {
                        repository.startWorking()
                    }

                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val appWidgetIds = appWidgetManager.getAppWidgetIds(
                        android.content.ComponentName(context, TimeTrackerWidgetProvider::class.java)
                    )
                    
                    val newSession = repository.liveSession.value
                    for (id in appWidgetIds) {
                        updateAppWidget(context, appWidgetManager, id, newSession)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        const val ACTION_TOGGLE = "com.example.worktimer.ACTION_TOGGLE"

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
            
            views.setTextViewText(R.id.tv_widget_time_left, formatTimeLeft(timeLeftMs))

            // Toggle pending intent
            val intent = Intent(context, TimeTrackerWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_toggle, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun formatTimeLeft(millis: Long): String {
            val totalMinutes = millis / (1000 * 60)
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            return "${hours}h ${minutes}m left"
        }
    }
}
