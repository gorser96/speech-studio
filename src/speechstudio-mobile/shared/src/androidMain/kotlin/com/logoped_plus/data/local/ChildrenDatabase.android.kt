package com.logoped_plus.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.Dispatchers

/**
 * Android сохраняет прежний системный драйвер SQLite и тот же файл `children.db`,
 * поэтому существующие установки открываются без конвертации.
 */
fun createChildrenDatabase(context: Context, name: String = "children.db"): ChildrenDatabase {
    val appContext = context.applicationContext
    return Room.databaseBuilder<ChildrenDatabase>(
        context = appContext,
        name = appContext.getDatabasePath(name).absolutePath
    )
        .setQueryCoroutineContext(Dispatchers.IO)
        .addMigrations(*androidMigrations())
        .build()
}

internal fun androidMigrations(): Array<Migration> = arrayOf(
    object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            migrateToV2 { db.execSQL(it) }
        }
    },
    object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            migrateToV3 { db.execSQL(it) }
        }
    },
    object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            migrateToV4 { db.execSQL(it) }
        }
    },
    object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            migrateToV5 { db.execSQL(it) }
        }
    }
)
