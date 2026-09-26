package com.taskplanner.app

import com.taskplanner.app.data.remote.EmailPasswordRequest
import com.taskplanner.app.data.remote.FirebaseIdentityApi
import com.taskplanner.app.data.remote.FirebaseRefreshResponse
import com.taskplanner.app.data.remote.FirebaseSecureTokenApi
import com.taskplanner.app.data.remote.FirebaseSignInResponse
import com.taskplanner.app.data.remote.PasswordResetRequest
import com.taskplanner.app.data.repository.AuthSession
import com.taskplanner.app.data.repository.FirebaseAuthRepository
import com.taskplanner.app.data.repository.SessionStore
import com.taskplanner.app.domain.model.AuthOperationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class FirebaseAuthRepositoryTest {
    @Test fun signInPersistsSessionAndExposesIdToken() = runBlocking {
        val store = MemorySessionStore()
        val repository = FirebaseAuthRepository("test-api-key", FakeIdentityApi(), FakeSecureTokenApi(), store)

        repository.signIn("person@example.com", "password123")

        assertEquals("user-1", repository.currentUid())
        assertEquals("person@example.com", repository.authState.first()?.email)
        assertEquals("initial-token", repository.idToken())
        assertEquals("refresh-1", store.session?.refreshToken)
    }

    @Test fun restoresSessionAndRefreshesExpiredToken() = runBlocking {
        val store = MemorySessionStore(AuthSession("user-1", "person@example.com", "refresh-1", null, 0))
        val repository = FirebaseAuthRepository("test-api-key", FakeIdentityApi(), FakeSecureTokenApi(), store)

        assertEquals("user-1", repository.authState.first()?.uid)
        assertEquals("renewed-token", repository.idToken())
        assertEquals("refresh-2", store.session?.refreshToken)

        repository.signOut()
        assertNull(repository.currentUid())
        assertNull(repository.idToken())
        assertNull(store.session)
    }

    @Test fun missingConfigurationDoesNotSendCredentials() {
        val repository = FirebaseAuthRepository("", FakeIdentityApi(), FakeSecureTokenApi(), MemorySessionStore())
        assertThrows(AuthOperationException::class.java) {
            runBlocking { repository.signIn("person@example.com", "password123") }
        }
    }
}

private class MemorySessionStore(var session: AuthSession? = null) : SessionStore {
    override fun read(): AuthSession? = session
    override fun write(session: AuthSession) { this.session = session }
    override fun clear() { session = null }
}

private class FakeIdentityApi : FirebaseIdentityApi {
    override suspend fun signIn(apiKey: String, input: EmailPasswordRequest): FirebaseSignInResponse =
        FirebaseSignInResponse("initial-token", "refresh-1", "3600", "user-1", input.email)

    override suspend fun register(apiKey: String, input: EmailPasswordRequest): FirebaseSignInResponse =
        signIn(apiKey, input)

    override suspend fun sendPasswordReset(apiKey: String, input: PasswordResetRequest) = Unit
}

private class FakeSecureTokenApi : FirebaseSecureTokenApi {
    override suspend fun refresh(apiKey: String, grantType: String, refreshToken: String): FirebaseRefreshResponse =
        FirebaseRefreshResponse("renewed-token", "refresh-2", "3600", "user-1")
}
