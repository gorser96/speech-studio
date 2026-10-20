package com.logoped_plus.domain.model

enum class TagColorKey(val displayName: String) {
    RED("Красный"),
    ORANGE("Оранжевый"),
    YELLOW("Жёлтый"),
    GREEN("Зелёный"),
    TEAL("Бирюзовый"),
    BLUE("Синий"),
    PURPLE("Фиолетовый"),
    GRAY("Серый")
}

data class Tag(
    val id: String,
    val name: String,
    val color: TagColorKey
)
