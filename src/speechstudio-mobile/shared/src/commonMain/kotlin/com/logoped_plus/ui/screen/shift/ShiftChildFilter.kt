package com.logoped_plus.ui.screen.shift

import com.logoped_plus.domain.model.Child

/**
 * Чистый фильтр списка детей для выбора в смене.
 *
 * Поиск по имени нечувствителен к регистру и игнорирует пробелы по краям.
 * Фильтр по тэгам объединяет выбранные тэги: подходит ребёнок, у которого есть
 * хотя бы один из них. Пустой набор тэгов и пустой запрос не ограничивают список.
 */
fun filterShiftChildren(
    children: List<Child>,
    query: String,
    tagIds: Set<String>,
    excludeChildIds: Set<String> = emptySet()
): List<Child> {
    val trimmed = query.trim()
    return children
        .filter { it.id !in excludeChildIds }
        .filter { trimmed.isEmpty() || it.name.contains(trimmed, ignoreCase = true) }
        .filter { child -> tagIds.isEmpty() || child.tagIds.any { it in tagIds } }
}
