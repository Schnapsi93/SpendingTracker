package com.aljaz.spendingtracker.domain.repository

import com.aljaz.spendingtracker.domain.model.UnrecognizedMessage
import kotlinx.coroutines.flow.Flow

/** Stores bank messages that could not be parsed. */
interface UnrecognizedMessageRepository {

    /** Saves a message; the same sender and text is stored only once. */
    suspend fun add(message: UnrecognizedMessage)

    suspend fun delete(id: Long)

    /** All stored messages, newest first; updates automatically. */
    fun observeAll(): Flow<List<UnrecognizedMessage>>
}