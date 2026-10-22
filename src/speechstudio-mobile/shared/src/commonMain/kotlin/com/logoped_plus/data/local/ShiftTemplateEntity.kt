package com.logoped_plus.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Шаблон смены: только сетка времени и дни повторения, без состава детей.
 * Дни недели хранятся битовой маской ISO (бит 0 — понедельник).
 */
@Entity(tableName = "shift_templates", indices = [Index(value = ["id"], unique = true)])
data class ShiftTemplateEntity(
    @PrimaryKey(autoGenerate = true) val position: Long = 0,
    val id: String,
    val name: String,
    val startNanoOfDay: Long,
    val endNanoOfDay: Long,
    val baseDurationMinutes: Int,
    val breakMinutes: Int,
    val daysMask: Int,
    val endEpochDay: Long?
)