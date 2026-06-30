package com.example.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.database.dao.ChatDao
import com.example.database.dao.ModelDao
import com.example.database.entity.ChatMessageEntity
import com.example.database.entity.ChatSessionEntity
import com.example.database.entity.ModelEntity

@Database(
    entities = [ChatSessionEntity::class, ChatMessageEntity::class, ModelEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun modelDao(): ModelDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ai_engine_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
