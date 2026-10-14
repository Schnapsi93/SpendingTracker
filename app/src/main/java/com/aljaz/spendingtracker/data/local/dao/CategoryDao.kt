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

    @Update
    suspend fun update(category: CategoryEntity)

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