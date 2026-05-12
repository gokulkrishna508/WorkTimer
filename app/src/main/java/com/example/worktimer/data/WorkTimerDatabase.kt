package com.example.worktimer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [WorkSessionEntity::class], version = 1, exportSchema = false)
abstract class WorkTimerDatabase : RoomDatabase() {
    abstract fun workSessionDao(): WorkSessionDao

    companion object {
        @Volatile
        private var INSTANCE: WorkTimerDatabase? = null

        fun getInstance(context: Context): WorkTimerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WorkTimerDatabase::class.java,
                    "work_timer_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
