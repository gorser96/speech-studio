package com.speech_studio.domain.model

data class Child(
    val id: String,
    val name: String,
    val tagIds: List<String> = emptyList(),
    val schedule: ChildSchedule = ChildSchedule()
)
