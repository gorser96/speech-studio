package com.logoped_plus.data.local

import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import kotlinx.coroutines.Dispatchers

/**
 * iOS использует bundled SQLite, поэтому версия движка не зависит от системы.
 * Путь к файлу передаёт платформенная оболочка (Application Support).
 */
fun createChildrenDatabase(path: String): ChildrenDatabase =
    Room.databaseBuilder<ChildrenDatabase>(name = path)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.Default)
        .addMigrations(*iosMigrations())
        .build()

internal fun iosMigrations(): Array<Migration> = arrayOf(
    object : Migration(1, 2) {
        override fun migrate(connection: SQLiteConnection) {
            migrateToV2 { connection.execSQL(it) }
        }
    },
    object : Migration(2, 3) {
        override fun migrate(connection: SQLiteConnection) {
            migrateToV3 { connection.execSQL(it) }
        }
    },
    object : Migration(3, 4) {
        override fun migrate(connection: SQLiteConnection) {
            migrateToV4 { connection.execSQL(it) }
        }
    }
)
