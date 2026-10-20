package com.logoped_plus.ui.screen.tags

import androidx.compose.ui.graphics.Color
import com.logoped_plus.domain.model.TagColorKey

val tagPalette: Map<TagColorKey, Color> = mapOf(
    TagColorKey.RED to Color(0xFFC62828),
    TagColorKey.ORANGE to Color(0xFFEF6C00),
    TagColorKey.YELLOW to Color(0xFFF9A825),
    TagColorKey.GREEN to Color(0xFF2E7D32),
    TagColorKey.TEAL to Color(0xFF00695C),
    TagColorKey.BLUE to Color(0xFF1565C0),
    TagColorKey.PURPLE to Color(0xFF6A1B9A),
    TagColorKey.GRAY to Color(0xFF546E7A)
)

fun tagLabelColor(key: TagColorKey): Color =
    if (key == TagColorKey.YELLOW) Color.Black else Color.White
