package com.appbuddies.bookbuddies.datastore

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
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

// added two new tables for CalendarEvent and EventTag
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
                colour INTEGER NOT NULL DEFAULT ${0xFF808080}
            )
            """.trimIndent()
        )
    }
}

// added colour buckets for the cover in books table
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE books ADD COLUMN coverColours TEXT NOT NULL DEFAULT '[]'")
        db.execSQL("ALTER TABLE books ADD COLUMN chosenCoverColour INTEGER NOT NULL DEFAULT 0")
    }
}

// change event data. again.
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS calendar_events")
        db.execSQL("""
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
                tag TEXT NOT NULL DEFAULT '',
                reminder INTEGER NOT NULL DEFAULT 0,
                reminderTime INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())
    }
}

// add timezone field for events
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE calendar_events ADD COLUMN timezone TEXT NOT NULL DEFAULT ''")
    }
}

// remove notifications
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE calendar_events DROP COLUMN reminder")
        db.execSQL("ALTER TABLE calendar_events DROP COLUMN reminderTime")
    }
}
