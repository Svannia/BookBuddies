package com.example.bookbuddies.datastore

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// added a "bookshelf" field
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE books ADD COLUMN bookshelf TEXT NOT NULL DEFAULT ''")
    }
}

// changed the field "location" to "source", and added a new boolean field "isGift"
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE books RENAME COLUMN boughtAt TO source")
        db.execSQL("ALTER TABLE books ADD COLUMN isGift INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS calendar_events (
                uid TEXT PRIMARY KEY NOT NULL,
                title TEXT NOT NULL,
                allDay INTEGER NOT NULL DEFAULT 0,
                dateStart INTEGER NOT NULL,
                dateEnd INTEGER NOT NULL,
                minuteStart INTEGER NOT NULL DEFAULT 0,
                minuteEnd INTEGER NOT NULL DEFAULT 0,
                location TEXT NOT NULL DEFAULT '',
                notes TEXT NOT NULL DEFAULT '',
                tags TEXT NOT NULL DEFAULT '[]',
                reminder INTEGER NOT NULL DEFAULT 0,
                reminderTime INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS event_tags (
                uid TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL
            )
            """.trimIndent()
        )
    }
}