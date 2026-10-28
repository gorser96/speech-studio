package com.speech_studio.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Смена на дату со снимком окна, базовой длительности и перерыва.
 * Ссылка на шаблон обнуляется при удалении шаблона.
 */
@Entity(
    tableName = "work_shifts",
    indices = [Index(value = ["id"], unique = true), Index("templateId")],
    foreignKeys = [
        ForeignKey(
            entity = ShiftTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class WorkShiftEntity(
    @PrimaryKey(autoGenerate = true) val position: Long = 0,
    val id: String,
    val epochDay: Long,
    val startNanoOfDay: Long,
    val endNanoOfDay: Long,
    val baseDurationMinutes: Int,
    val breakMinutes: Int,
    val templateId: String? = null
)