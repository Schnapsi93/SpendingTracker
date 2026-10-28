package com.aljaz.spendingtracker.domain.usecase

import com.aljaz.spendingtracker.core.AppResult
import com.aljaz.spendingtracker.domain.model.Category
import com.aljaz.spendingtracker.domain.model.TransactionType
import com.aljaz.spendingtracker.domain.repository.CategoryRepository
import javax.inject.Inject

/**
 * Validates and saves a category: adds it when its id is 0,
 * otherwise updates the existing one.
 */
class SaveCategoryUseCase @Inject constructor(
    private val repository: CategoryRepository
) {

    /**
     * @return [AppResult.Success] when saved, or [AppResult.Error] with
     *         a message the user can read
     */
    suspend operator fun invoke(category: Category): AppResult<Unit> {
        // Trim and collapse repeated spaces, so "  Car   Fuel " becomes "Car Fuel".
        val name = category.name.trim().replace(Regex("\\s+"), " ")

        if (name.isEmpty()) {
            return AppResult.Error("Enter a name.")
        }
        if (name.length > MAX_NAME_LENGTH) {
            return AppResult.Error("Name is too long (max $MAX_NAME_LENGTH characters).")
        }
        if (category.defaultSplitPercent !in 0..100) {
            return AppResult.Error("Split share must be between 0 and 100.")
        }

        // Only expenses are split; an income category is never shared.
        val cleaned = category.copy(
            name = name,
            isSplit = category.isSplit && category.type == TransactionType.EXPENSE
        )
        val saved = if (cleaned.id == 0L) {
            repository.add(cleaned) != null
        } else {
            repository.update(cleaned)
        }

        if (!saved) {
            val typeLabel = when (cleaned.type) {
                TransactionType.INCOME -> "an income"
                TransactionType.EXPENSE -> "an expense"
            }
            return AppResult.Error("There is already $typeLabel category called \"$name\".")
        }
        return AppResult.Success(Unit)
    }

    private companion object {
        const val MAX_NAME_LENGTH = 30
    }
}