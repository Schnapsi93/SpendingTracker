package com.aljaz.spendingtracker.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aljaz.spendingtracker.data.local.dao.CategoryDao
import com.aljaz.spendingtracker.data.local.dao.CategoryRuleDao
import com.aljaz.spendingtracker.data.local.dao.RecurringTransactionDao
import com.aljaz.spendingtracker.data.local.dao.TransactionDao
import com.aljaz.spendingtracker.data.local.dao.UnrecognizedMessageDao
import com.aljaz.spendingtracker.data.local.entity.CategoryEntity
import com.aljaz.spendingtracker.data.local.entity.CategoryRuleEntity
import com.aljaz.spendingtracker.data.local.entity.RecurringTransactionEntity
import com.aljaz.spendingtracker.data.local.entity.TransactionEntity
import com.aljaz.spendingtracker.data.local.entity.UnrecognizedMessageEntity

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
 * - 6: transactions.recurringId column (manual migration MIGRATION_5_6)
 * - 7: split/sync columns on transactions and categories (manual migration MIGRATION_6_7)
 */
@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        CategoryRuleEntity::class,
        UnrecognizedMessageEntity::class,
        RecurringTransactionEntity::class
    ],
    version = 7,
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

    companion object {

        /**
         * SQL expression that produces a random version-4 UUID, e.g.
         * "3f2b8c1e-9a4d-4e7f-b2c1-0d9e8f7a6b5c". SQLite has no UUID function,
         * so it is assembled from random bytes. It runs once per row.
         */
        private const val NEW_UUID_SQL =
            "lower(hex(randomblob(4))) || '-' || " +
                    "lower(hex(randomblob(2))) || '-4' || " +
                    "substr(lower(hex(randomblob(2))), 2) || '-' || " +
                    "substr('89ab', 1 + (abs(random()) % 4), 1) || " +
                    "substr(lower(hex(randomblob(2))), 2) || '-' || " +
                    "lower(hex(randomblob(6)))"

        /**
         * v5 → v6, written by hand: adds the recurringId column, then links
         * entries a repeat created earlier. Those carry the fingerprint
         * "recurring:<seriesId>:<n>", so the series id is read from there.
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN recurringId INTEGER")
                db.execSQL(
                    "UPDATE transactions SET recurringId = CAST(" +
                            "substr(substr(dedupeHash, 11), 1, instr(substr(dedupeHash, 11), ':') - 1) " +
                            "AS INTEGER) WHERE dedupeHash LIKE 'recurring:%'"
                )
            }
        }

        /**
         * v6 → v7, written by hand: prepares for split expenses and sync.
         *
         * Transactions get a global uuid (each existing row gets its own random one),
         * the split fields, a sync state and an edit time (existing rows use their
         * transaction time). Categories get the split switch and default share.
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // --- transactions ---
                // Added with a temporary empty default, then filled row by row.
                db.execSQL("ALTER TABLE transactions ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
                db.execSQL("UPDATE transactions SET uuid = $NEW_UUID_SQL")
                // Name must match what Room generates for Index(value = ["uuid"]).
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_transactions_uuid ON transactions (uuid)"
                )

                db.execSQL("ALTER TABLE transactions ADD COLUMN isShared INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE transactions ADD COLUMN payerSharePercent INTEGER")
                db.execSQL(
                    "ALTER TABLE transactions ADD COLUMN syncState TEXT NOT NULL DEFAULT 'LOCAL_ONLY'"
                )
                db.execSQL("ALTER TABLE transactions ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE transactions SET updatedAt = timestamp")

                // --- categories ---
                db.execSQL("ALTER TABLE categories ADD COLUMN isSplit INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    "ALTER TABLE categories ADD COLUMN defaultSplitPercent INTEGER NOT NULL DEFAULT 50"
                )
            }
        }
    }
}