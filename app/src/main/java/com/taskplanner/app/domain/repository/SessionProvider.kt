package com.taskplanner.app.domain.repository

interface SessionProvider {
    suspend fun idToken(forceRefresh: Boolean = false): String?
    fun currentUid(): String?
}
