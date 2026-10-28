package com.speech_studio.data.local

import com.speech_studio.domain.model.Tag
import com.speech_studio.domain.model.TagColorKey

fun TagEntity.toDomain() = Tag(id, name, TagColorKey.valueOf(colorKey))

fun Tag.toEntity(position: Long = 0) = TagEntity(
    position, id, name, name.lowercase(), color.name
)
