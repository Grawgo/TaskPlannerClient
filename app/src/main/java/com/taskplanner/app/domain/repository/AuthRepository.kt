package com.taskplanner.app.domain.repository

import com.taskplanner.app.domain.model.AuthUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val authState: Flow<AuthUser?>
    val isConfigured: Boolean
    suspend fun signIn(email: String, password: String)
    suspend fun register(email: String, password: String)
    suspend fun sendPasswordReset(email: String)
    fun signOut()
}
