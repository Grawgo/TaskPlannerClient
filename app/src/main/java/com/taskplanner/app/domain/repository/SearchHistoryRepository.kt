package com.taskplanner.app.domain.repository

import kotlinx.coroutines.flow.Flow

interface SearchHistoryRepository {
    fun observe(): Flow<List<String>>
    suspend fun remember(query: String)
    suspend fun clear()
}
