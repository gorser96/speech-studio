package com.logoped_plus.domain.text

/**
 * Locale-independent comparison key for Russian names.
 *
 * Mirrors the previous PRIMARY-strength Russian `Collator` closely enough for the
 * application's lists: case is ignored, `ё` is compared as `е`, and the order is
 * defined by Unicode code points rather than by a platform locale.
 */
object RussianNameSort {
    fun key(name: String): String = name.lowercase().replace('ё', 'е')

    fun compare(left: String, right: String): Int = key(left).compareTo(key(right))

    /**
     * Sorts by [name] while keeping the original order for equal keys.
     * The stable fallback is the item's index in [items].
     */
    fun <T> sortedBy(items: List<T>, name: (T) -> String): List<T> =
        items.withIndex()
            .sortedWith(
                compareBy(
                    { key(name(it.value)) },
                    { it.index }
                )
            )
            .map { it.value }
}
