package com.hivid.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [HistoryEntity::class, LogEntity::class], version = 2, exportSchema = false)
abstract class HividDatabase : RoomDatabase() {

    abstract fun historyDao(): HistoryDao
    abstract fun logDao(): LogDao

    companion object {
        @Volatile
        private var INSTANCE: HividDatabase? = null

        fun getDatabase(context: Context): HividDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HividDatabase::class.java,
                    "hivid_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
