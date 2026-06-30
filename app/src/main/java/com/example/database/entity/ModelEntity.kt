package com.example.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "models")
data class ModelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val isDefault: Boolean = false,
    val addedAt: Long = System.currentTimeMillis()
)
