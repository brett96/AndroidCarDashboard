package com.example.cardashboardtest.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.cardashboardtest.model.BluetoothLog
import com.example.cardashboardtest.model.LogType
import java.util.Date

@Database(entities = [BluetoothLog::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bluetoothLogDao(): BluetoothLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class Converters {
    @androidx.room.TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @androidx.room.TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @androidx.room.TypeConverter
    fun fromLogType(value: LogType): String {
        return value.name
    }

    @androidx.room.TypeConverter
    fun toLogType(value: String): LogType {
        return LogType.valueOf(value)
    }
} 