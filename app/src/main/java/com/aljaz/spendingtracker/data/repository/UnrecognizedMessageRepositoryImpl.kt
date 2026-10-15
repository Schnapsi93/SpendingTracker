package com.aljaz.spendingtracker.data.repository

import com.aljaz.spendingtracker.data.local.dao.UnrecognizedMessageDao
import com.aljaz.spendingtracker.data.mapper.toDomain
import com.aljaz.spendingtracker.data.mapper.toEntity
import com.aljaz.spendingtracker.domain.model.UnrecognizedMessage
import com.aljaz.spendingtracker.domain.repository.UnrecognizedMessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Room-backed implementation of [UnrecognizedMessageRepository]. */
class UnrecognizedMessageRepositoryImpl @Inject constructor(
    private val dao: UnrecognizedMessageDao
) : UnrecognizedMessageRepository {

    override suspend fun add(message: UnrecognizedMessage) =
        dao.insert(message.toEntity())

    override suspend fun delete(id: Long) =
        dao.deleteById(id)

    override fun observeAll(): Flow<List<UnrecognizedMessage>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }
}