package com.logoped_plus.domain.text

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RussianNameSortTest {

    @Test
    fun sortsInRussianAlphabeticalOrder() {
        val names = listOf("Яна", "Борис", "аня", "Виктор", "Аня", "Глеб")
        // "Аня" and "аня" have the same case-insensitive key, so the stable
        // fallback keeps their original order: "аня" appears first in the input.
        assertEquals(
            listOf("аня", "Аня", "Борис", "Виктор", "Глеб", "Яна"),
            RussianNameSort.sortedBy(names) { it }
        )
    }

    @Test
    fun yoIsComparedAsYe() {
        val names = listOf("ёлка", "Егор", "ёж", "Елена")
        assertEquals(
            listOf("Егор", "ёж", "Елена", "ёлка"),
            RussianNameSort.sortedBy(names) { it }
        )
        assertEquals(RussianNameSort.key("Ёлка"), RussianNameSort.key("елка"))
    }

    @Test
    fun caseIsIgnored() {
        assertEquals(RussianNameSort.key("ИВАН"), RussianNameSort.key("иван"))
        assertEquals(RussianNameSort.key("Пётр"), RussianNameSort.key("петр"))
        assertTrue(RussianNameSort.compare("иван", "Иван") == 0)
    }

    @Test
    fun namesakesKeepOriginalOrder() {
        data class Item(val id: String, val name: String)
        val items = listOf(
            Item("first", "Иван"),
            Item("second", "иван"),
            Item("third", "Иван")
        )
        assertEquals(
            listOf("first", "second", "third"),
            RussianNameSort.sortedBy(items) { it.name }.map { it.id }
        )
    }

    @Test
    fun emptyAndSingleItemListsAreStable() {
        assertEquals(emptyList(), RussianNameSort.sortedBy(emptyList<String>()) { it })
        assertEquals(listOf("Аня"), RussianNameSort.sortedBy(listOf("Аня")) { it })
    }
}
