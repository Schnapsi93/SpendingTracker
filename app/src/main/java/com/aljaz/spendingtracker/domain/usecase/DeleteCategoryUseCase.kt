package com.aljaz.spendingtracker.domain.usecase

import com.aljaz.spendingtracker.domain.model.Category
import com.aljaz.spendingtracker.domain.repository.CategoryRepository
import javax.inject.Inject

/**
 * Deletes a category. Its transactions and recurring transactions are
 * kept and become uncategorized.
 */
class DeleteCategoryUseCase @Inject constructor(
    private val repository: CategoryRepository
) {

    suspend operator fun invoke(category: Category) = repository.delete(category)
}