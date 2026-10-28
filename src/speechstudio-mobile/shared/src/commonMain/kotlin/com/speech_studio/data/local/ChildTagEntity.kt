package com.speech_studio.data.local

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "child_tags", primaryKeys = ["childId", "tagId"],
    foreignKeys = [
        ForeignKey(entity = ChildEntity::class, parentColumns = ["id"], childColumns = ["childId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TagEntity::class, parentColumns = ["id"], childColumns = ["tagId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class ChildTagEntity(val childId: String, val tagId: String)
