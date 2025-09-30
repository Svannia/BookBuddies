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