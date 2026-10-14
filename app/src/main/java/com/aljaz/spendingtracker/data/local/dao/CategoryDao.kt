package com.aljaz.spendingtracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aljaz.spendingtracker.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

/** All the ways the app reads and writes categories. */
@Dao
interface CategoryDao {

    /** @return the new row id, or -1 if a category with that name and type exists */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(category: CategoryEntity): Long

    /** Inserts several categories, skipping any that already exist. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    /**
     * Saves changes to a category.
     *
     * @return number of rows changed: 1 on success, 0 if the new name
     *         clashes with another category of the same type
     */
    @Update(onConflict = OnConflictStrategy.IGNORE)
    suspend fun update(category: CategoryEntity): Int

    @Delete
    suspend fun delete(category: CategoryEntity)

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): CategoryEntity?

    /** Streams all categories, grouped by type, alphabetical (case-insensitive). */
    @Query("SELECT * FROM categories ORDER BY type, name COLLATE NOCASE")
    fun observeAll(): Flow<List<CategoryEntity>>

    /** Number of categories, used to decide whether defaults are needed. */
    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int
}