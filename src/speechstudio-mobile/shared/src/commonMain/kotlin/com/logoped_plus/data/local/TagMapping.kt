package com.logoped_plus.data.local

import com.logoped_plus.domain.model.Tag
import com.logoped_plus.domain.model.TagColorKey

fun TagEntity.toDomain() = Tag(id, name, TagColorKey.valueOf(colorKey))

fun Tag.toEntity(position: Long = 0) = TagEntity(
    position, id, name, name.lowercase(), color.name
)
