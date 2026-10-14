package com.aljaz.spendingtracker.domain.usecase

import com.aljaz.spendingtracker.domain.model.DefaultCategories
import com.aljaz.spendingtracker.domain.repository.CategoryRepository
import javax.inject.Inject

/**
 * Creates the default categories if there are none yet.
 *
 * Safe to run on every app start: it does nothing once categories exist.
 */
class EnsureDefaultCategoriesUseCase @Inject constructor(
    private val repository: CategoryRepository
) {

    suspend operator fun invoke() {
        if (repository.count() == 0) {
            repository.addAll(DefaultCategories.all)
        }
    }
}