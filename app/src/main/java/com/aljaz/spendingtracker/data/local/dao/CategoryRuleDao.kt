package com.aljaz.spendingtracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aljaz.spendingtracker.data.local.entity.CategoryRuleEntity
import kotlinx.coroutines.flow.Flow

/** All the ways the app reads and writes category rules. */
@Dao
interface CategoryRuleDao {

    /** Inserts a rule, replacing an existing one with the same keyword and field. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: CategoryRuleEntity): Long

    @Delete
    suspend fun delete(rule: CategoryRuleEntity)

    @Query("SELECT * FROM category_rules")
    suspend fun getAll(): List<CategoryRuleEntity>

    @Query("SELECT * FROM category_rules ORDER BY keyword")
    fun observeAll(): Flow<List<CategoryRuleEntity>>

    @Query("SELECT COUNT(*) FROM category_rules")
    suspend fun count(): Int

    /** Removes every rule pointing to a category; used when the category is deleted. */
    @Query("DELETE FROM category_rules WHERE categoryId = :categoryId")
    suspend fun deleteForCategory(categoryId: Long)
}