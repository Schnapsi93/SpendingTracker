package com.aljaz.spendingtracker.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.aljaz.spendingtracker.data.local.dao.CategoryDao
import com.aljaz.spendingtracker.data.local.dao.CategoryRuleDao
import com.aljaz.spendingtracker.data.local.dao.TransactionDao
import com.aljaz.spendingtracker.data.local.entity.CategoryEntity
import com.aljaz.spendingtracker.data.local.entity.CategoryRuleEntity
import com.aljaz.spendingtracker.data.local.entity.TransactionEntity

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
 */
@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        CategoryRuleEntity::class
    ],
    version = 3,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3)
    ]
)
abstract class AppDatabase : RoomDatabase() {

    /** Gives access to transaction queries. */
    abstract fun transactionDao(): TransactionDao

    /** Gives access to category queries. */
    abstract fun categoryDao(): CategoryDao

    /** Gives access to keyword rule queries. */
    abstract fun categoryRuleDao(): CategoryRuleDao
}