package com.logoped_plus.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "children", indices = [Index(value = ["id"], unique = true)])
data class ChildEntity(
    @PrimaryKey(autoGenerate = true) val position: Long = 0,
    val id: String,
    val name: String
)
