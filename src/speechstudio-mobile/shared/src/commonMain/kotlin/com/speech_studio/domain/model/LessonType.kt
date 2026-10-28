package com.speech_studio.domain.model

/**
 * Сохранённый тип занятия.
 *
 * Тип фиксируется при создании и не выводится из числа участников: группа,
 * оставшаяся с одним участником после редактирования, остаётся групповой.
 */
enum class LessonType {
    INDIVIDUAL,
    GROUP;

    companion object {
        fun fromStorage(value: String?): LessonType =
            entries.firstOrNull { it.name == value } ?: INDIVIDUAL
    }
}