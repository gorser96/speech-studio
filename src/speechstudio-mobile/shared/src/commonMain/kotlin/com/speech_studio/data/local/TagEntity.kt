package com.speech_studio.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "tags", indices = [Index(value = ["id"], unique = true), Index(value = ["nameKey"], unique = true)])
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val position: Long = 0,
    val id: String,
    val name: String,
    val nameKey: String,
    val colorKey: String
)
