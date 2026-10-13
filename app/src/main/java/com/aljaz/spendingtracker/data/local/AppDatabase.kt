package com.aljaz.spendingtracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.aljaz.spendingtracker.data.local.dao.TransactionDao
import com.aljaz.spendingtracker.data.local.entity.TransactionEntity

/**
 * The app's local SQLite database.
 *
 * Whenever you change an entity (add/remove/rename a column), increase
 * [version] and provide a migration so users keep their data.
 */
@Database(
    entities = [TransactionEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    /** Gives access to transaction queries. */
    abstract fun transactionDao(): TransactionDao
}