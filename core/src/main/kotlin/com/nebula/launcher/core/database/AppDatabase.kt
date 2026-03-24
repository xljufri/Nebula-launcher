package com.nebula.launcher.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nebula.launcher.core.model.AppNode

@Database(entities = [AppNode::class], version = 1, exportSchema = false)
@TypeConverters(ColorConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appNodeDao(): AppNodeDao
}
