package com.logoped_plus.data.local

/**
 * SQL миграций общий для Android и iOS.
 *
 * Платформенные адаптеры только передают `execSQL` соответствующего API:
 * на Android — `SupportSQLiteDatabase`, на iOS — `SQLiteConnection`.
 */
internal fun migrateToV2(execSQL: (String) -> Unit) {
    execSQL("CREATE TABLE IF NOT EXISTS `lessons` (`position` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `id` TEXT NOT NULL, `scheduledEpochDay` INTEGER NOT NULL, `scheduledNanoOfDay` INTEGER NOT NULL, `durationMinutes` INTEGER NOT NULL, `comment` TEXT NOT NULL)")
    execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_lessons_id` ON `lessons` (`id`)")
    execSQL("CREATE TABLE IF NOT EXISTS `lesson_participants` (`lessonId` TEXT NOT NULL, `childId` TEXT NOT NULL, `ordinal` INTEGER NOT NULL, PRIMARY KEY(`lessonId`, `childId`), FOREIGN KEY(`lessonId`) REFERENCES `lessons`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(`childId`) REFERENCES `children`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT)")
    execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_lesson_participants_lessonId_ordinal` ON `lesson_participants` (`lessonId`, `ordinal`)")
    execSQL("CREATE INDEX IF NOT EXISTS `index_lesson_participants_childId` ON `lesson_participants` (`childId`)")
    execSQL("CREATE TABLE IF NOT EXISTS `lesson_videos` (`lessonId` TEXT NOT NULL, `uri` TEXT NOT NULL, `ordinal` INTEGER NOT NULL, PRIMARY KEY(`lessonId`, `uri`), FOREIGN KEY(`lessonId`) REFERENCES `lessons`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
    execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_lesson_videos_lessonId_ordinal` ON `lesson_videos` (`lessonId`, `ordinal`)")
}

internal fun migrateToV3(execSQL: (String) -> Unit) {
    execSQL("ALTER TABLE `lesson_participants` RENAME TO `lesson_participants_old`")
    // Table rename does not rename its indexes; drop them so the new table can use the names.
    execSQL("DROP INDEX `index_lesson_participants_lessonId_ordinal`")
    execSQL("DROP INDEX `index_lesson_participants_childId`")
    execSQL("CREATE TABLE `lesson_participants` (`lessonId` TEXT NOT NULL, `childId` TEXT NOT NULL, `childName` TEXT, `ordinal` INTEGER NOT NULL, PRIMARY KEY(`lessonId`, `childId`), FOREIGN KEY(`lessonId`) REFERENCES `lessons`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
    execSQL("CREATE UNIQUE INDEX `index_lesson_participants_lessonId_ordinal` ON `lesson_participants` (`lessonId`, `ordinal`)")
    execSQL("CREATE INDEX `index_lesson_participants_childId` ON `lesson_participants` (`childId`)")
    execSQL("INSERT INTO `lesson_participants` (`lessonId`, `childId`, `childName`, `ordinal`) SELECT `lessonId`, `childId`, NULL, `ordinal` FROM `lesson_participants_old`")
    execSQL("DROP TABLE `lesson_participants_old`")
}

internal fun migrateToV4(execSQL: (String) -> Unit) {
    execSQL("CREATE TABLE IF NOT EXISTS `tags` (`position` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `id` TEXT NOT NULL, `name` TEXT NOT NULL, `nameKey` TEXT NOT NULL, `colorKey` TEXT NOT NULL)")
    execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tags_id` ON `tags` (`id`)")
    execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tags_nameKey` ON `tags` (`nameKey`)")
    execSQL("CREATE TABLE IF NOT EXISTS `child_tags` (`childId` TEXT NOT NULL, `tagId` TEXT NOT NULL, PRIMARY KEY(`childId`, `tagId`), FOREIGN KEY(`childId`) REFERENCES `children`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(`tagId`) REFERENCES `tags`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
    execSQL("CREATE TABLE IF NOT EXISTS `child_schedule` (`childId` TEXT NOT NULL, `endEpochDay` INTEGER NOT NULL, PRIMARY KEY(`childId`), FOREIGN KEY(`childId`) REFERENCES `children`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
    execSQL("CREATE TABLE IF NOT EXISTS `child_schedule_slots` (`childId` TEXT NOT NULL, `id` TEXT NOT NULL, `dayOfWeek` INTEGER NOT NULL, `startNanoOfDay` INTEGER NOT NULL, `durationMinutes` INTEGER NOT NULL, `ordinal` INTEGER NOT NULL, PRIMARY KEY(`childId`, `id`), FOREIGN KEY(`childId`) REFERENCES `children`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(`childId`) REFERENCES `child_schedule`(`childId`) ON UPDATE NO ACTION ON DELETE CASCADE)")
    execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_child_schedule_slots_childId_ordinal` ON `child_schedule_slots` (`childId`, `ordinal`)")
}

/**
 * Добавляет тип занятия и связь со сменой, таблицы шаблонов, смен и слотов.
 *
 * `lessons` пересоздаётся, чтобы получить внешний ключ на `work_shifts`; тип заполняется по
 * числу участников: группа при двух и более, иначе индивидуальное. Таблицы смен создаются
 * до пересоздания `lessons`. Порядок `_new_lessons` → `DROP` → `RENAME` сохраняет ссылки
 * других таблиц, а `sqlite_sequence` переносится вместе с таблицей.
 */
internal fun migrateToV5(execSQL: (String) -> Unit) {
    execSQL("CREATE TABLE IF NOT EXISTS `shift_templates` (`position` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `id` TEXT NOT NULL, `name` TEXT NOT NULL, `startNanoOfDay` INTEGER NOT NULL, `endNanoOfDay` INTEGER NOT NULL, `baseDurationMinutes` INTEGER NOT NULL, `breakMinutes` INTEGER NOT NULL, `daysMask` INTEGER NOT NULL, `endEpochDay` INTEGER)")
    execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_shift_templates_id` ON `shift_templates` (`id`)")
    execSQL("CREATE TABLE IF NOT EXISTS `work_shifts` (`position` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `id` TEXT NOT NULL, `epochDay` INTEGER NOT NULL, `startNanoOfDay` INTEGER NOT NULL, `endNanoOfDay` INTEGER NOT NULL, `baseDurationMinutes` INTEGER NOT NULL, `breakMinutes` INTEGER NOT NULL, `templateId` TEXT, FOREIGN KEY(`templateId`) REFERENCES `shift_templates`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )")
    execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_work_shifts_id` ON `work_shifts` (`id`)")
    execSQL("CREATE INDEX IF NOT EXISTS `index_work_shifts_templateId` ON `work_shifts` (`templateId`)")
    execSQL("CREATE TABLE IF NOT EXISTS `_new_lessons` (`position` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `id` TEXT NOT NULL, `scheduledEpochDay` INTEGER NOT NULL, `scheduledNanoOfDay` INTEGER NOT NULL, `durationMinutes` INTEGER NOT NULL, `comment` TEXT NOT NULL, `type` TEXT NOT NULL, `shiftId` TEXT, FOREIGN KEY(`shiftId`) REFERENCES `work_shifts`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )")
    execSQL(
        "INSERT INTO `_new_lessons` (`position`, `id`, `scheduledEpochDay`, `scheduledNanoOfDay`, `durationMinutes`, `comment`, `type`, `shiftId`) " +
            "SELECT `position`, `id`, `scheduledEpochDay`, `scheduledNanoOfDay`, `durationMinutes`, `comment`, " +
            "CASE WHEN (SELECT COUNT(*) FROM `lesson_participants` p WHERE p.`lessonId` = `lessons`.`id`) >= 2 THEN 'GROUP' ELSE 'INDIVIDUAL' END, " +
            "NULL FROM `lessons`"
    )
    execSQL("DROP TABLE `lessons`")
    execSQL("ALTER TABLE `_new_lessons` RENAME TO `lessons`")
    execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_lessons_id` ON `lessons` (`id`)")
    execSQL("CREATE INDEX IF NOT EXISTS `index_lessons_shiftId` ON `lessons` (`shiftId`)")
    execSQL("CREATE TABLE IF NOT EXISTS `shift_slots` (`id` TEXT NOT NULL, `shiftId` TEXT NOT NULL, `position` INTEGER NOT NULL, `startNanoOfDay` INTEGER NOT NULL, `durationMinutes` INTEGER NOT NULL, `childId` TEXT NOT NULL, `lessonId` TEXT, PRIMARY KEY(`id`), FOREIGN KEY(`shiftId`) REFERENCES `work_shifts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`childId`) REFERENCES `children`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`lessonId`) REFERENCES `lessons`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )")
    execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_shift_slots_shiftId_position` ON `shift_slots` (`shiftId`, `position`)")
    execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_shift_slots_lessonId` ON `shift_slots` (`lessonId`)")
    execSQL("CREATE INDEX IF NOT EXISTS `index_shift_slots_shiftId` ON `shift_slots` (`shiftId`)")
    execSQL("CREATE INDEX IF NOT EXISTS `index_shift_slots_childId` ON `shift_slots` (`childId`)")
}
