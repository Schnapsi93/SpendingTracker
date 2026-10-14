package com.aljaz.spendingtracker.domain.usecase

import com.aljaz.spendingtracker.domain.model.Category
import com.aljaz.spendingtracker.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Streams all categories; emits again whenever they change. */
class ObserveCategoriesUseCase @Inject constructor(
    private val repository: CategoryRepository
) {

    operator fun invoke(): Flow<List<Category>> = repository.observeAll()
}