package com.aljaz.spendingtracker.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.aljaz.spendingtracker.data.local.dao.CategoryDao
import com.aljaz.spendingtracker.data.local.dao.CategoryRuleDao
import com.aljaz.spendingtracker.data.local.dao.TransactionDao
import com.aljaz.spendingtracker.data.local.dao.UnrecognizedMessageDao
import com.aljaz.spendingtracker.data.local.entity.CategoryEntity
import com.aljaz.spendingtracker.data.local.entity.CategoryRuleEntity
import com.aljaz.spendingtracker.data.local.entity.TransactionEntity
import com.aljaz.spendingtracker.data.local.entity.UnrecognizedMessageEntity
import com.aljaz.spendingtracker.data.local.dao.RecurringTransactionDao
import com.aljaz.spendingtracker.data.local.entity.RecurringTransactionEntity

/**
 * The app's local SQLite database.
 *
 * Whenever you change an entity (add/remove/rename a column or table),
 * increase [version] and add a migration so users keep their data.
 *
 * Version history:
 * - 1: transactions table
 * - 2: categories table (auto-migration, no data changes)
 * - 3: category_rules table (auto-migration, no data changes)
 * - 4: unrecognized_messages table (auto-migration, no data changes)
 * - 5: recurring_transactions table (auto-migration, no data changes)
 */
@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        CategoryRuleEntity::class,
        UnrecognizedMessageEntity::class,
        RecurringTransactionEntity::class
    ],
    version = 5,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5)
    ]
)
abstract class AppDatabase : RoomDatabase() {

    /** Gives access to transaction queries. */
    abstract fun transactionDao(): TransactionDao

    /** Gives access to category queries. */
    abstract fun categoryDao(): CategoryDao

    /** Gives access to keyword rule queries. */
    abstract fun categoryRuleDao(): CategoryRuleDao


    /** Gives access to unrecognized bank messages. */
    abstract fun unrecognizedMessageDao(): UnrecognizedMessageDao

    /** Gives access to recurring transaction templates. */
    abstract fun recurringTransactionDao(): RecurringTransactionDao
}