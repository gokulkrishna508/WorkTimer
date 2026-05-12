package com.example.worktimer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "work_sessions")
data class WorkSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,                    // yyyy-MM-dd format
    val targetHours: Float = 8f,         // default 8 hours
    val totalWorkMillis: Long = 0L,
    val totalBreakMillis: Long = 0L,
    val sessionCount: Int = 0
)
