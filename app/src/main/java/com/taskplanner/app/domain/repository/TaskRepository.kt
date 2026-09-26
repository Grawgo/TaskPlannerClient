package com.taskplanner.app.domain.repository

import com.taskplanner.app.domain.model.*

interface TaskRepository {
    suspend fun list(query: TaskQuery): TaskPage
    suspend fun get(id: String): Task
    suspend fun create(draft: TaskDraft): Task
    suspend fun update(id: String, draft: TaskDraft): Task
    suspend fun delete(id: String)
}
