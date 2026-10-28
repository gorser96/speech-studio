package com.speech_studio.data.local

import androidx.room.Embedded
import androidx.room.Relation
import com.speech_studio.domain.model.Child

data class ChildWithTags(
    @Embedded val child: ChildEntity,
    @Relation(parentColumn = "id", entityColumn = "childId") val tags: List<ChildTagEntity>
)

fun ChildWithTags.toDomain() = Child(
    id = child.id,
    name = child.name,
    tagIds = tags.map { it.tagId }
)
