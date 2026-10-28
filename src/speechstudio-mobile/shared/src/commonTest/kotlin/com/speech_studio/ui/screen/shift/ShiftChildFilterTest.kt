package com.speech_studio.ui.screen.shift

import com.speech_studio.domain.model.Child
import kotlin.test.Test
import kotlin.test.assertEquals

class ShiftChildFilterTest {

    private val child1 = Child(id = "c1", name = "Серёжа", tagIds = listOf("t1", "t2"))
    private val child2 = Child(id = "c2", name = "Настя", tagIds = listOf("t2", "t3"))
    private val child3 = Child(id = "c3", name = "Ваня", tagIds = emptyList())
    private val children = listOf(child1, child2, child3)

    @Test
    fun filter_emptyQueryAndTags_returnsAll() {
        val result = filterShiftChildren(children, "", emptySet())
        assertEquals(children, result)
    }

    @Test
    fun filter_excludeSet_removesMatching() {
        val result = filterShiftChildren(children, "", emptySet(), excludeChildIds = setOf("c1", "c3"))
        assertEquals(listOf(child2), result)
    }

    @Test
    fun filter_query_matchesCaseInsensitive() {
        val result = filterShiftChildren(children, "сЕр", emptySet())
        assertEquals(listOf(child1), result)
    }

    @Test
    fun filter_query_trimsWhitespace() {
        val result = filterShiftChildren(children, "  Настя  ", emptySet())
        assertEquals(listOf(child2), result)
    }

    @Test
    fun filter_onlyTagIds_unionSemantics() {
        val result = filterShiftChildren(children, "", setOf("t1", "t3"))
        assertEquals(listOf(child1, child2), result)
    }

    @Test
    fun filter_queryAndTags_bothApplied() {
        val result = filterShiftChildren(children, "ст", setOf("t2"))
        assertEquals(listOf(child2), result)
    }

    @Test
    fun filter_tagWithoutMatches_emptyResult() {
        val result = filterShiftChildren(children, "", setOf("t1"))
        assertEquals(listOf(child1), result)
    }
}
