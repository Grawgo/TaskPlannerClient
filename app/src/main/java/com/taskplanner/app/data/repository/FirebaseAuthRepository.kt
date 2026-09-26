package com.taskplanner.app.data.repository

import com.google.gson.JsonParser
import com.taskplanner.app.data.remote.EmailPasswordRequest
import com.taskplanner.app.data.remote.FirebaseIdentityApi
import com.taskplanner.app.data.remote.FirebaseSecureTokenApi
import com.taskplanner.app.data.remote.PasswordResetRequest
import com.taskplanner.app.domain.model.AuthOperationException
import com.taskplanner.app.domain.model.AuthUser
import com.taskplanner.app.domain.repository.AuthRepository
import com.taskplanner.app.domain.repository.SessionProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import java.io.IOException
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import javax.inject.Named

@Singleton
class FirebaseAuthRepository @Inject constructor(
    @param:Named("firebaseApiKey") private val apiKey: String,
    private val identity: FirebaseIdentityApi,
    private val secureToken: FirebaseSecureTokenApi,
    private val store: SessionStore,
) : AuthRepository, SessionProvider {
    override val isConfigured: Boolean get() = apiKey.isNotBlank()
    private val session = MutableStateFlow(if (isConfigured) store.read() else null)
    private val refreshMutex = Mutex()
    private val generation = AtomicLong(0)

    override val authState: Flow<AuthUser?> = session.map { current ->
        current?.let { AuthUser(it.uid, it.email) }
    }.distinctUntilChanged()

    override fun currentUid(): String? = session.value?.uid

    override suspend fun idToken(forceRefresh: Boolean): String? = refreshMutex.withLock {
        val current = session.value ?: return@withLock null
        if (!forceRefresh && current.idToken != null && current.expiresAtMillis > System.currentTimeMillis() + 60_000) {
            return@withLock current.idToken
        }
        val startedAt = generation.get()
        try {
            val result = secureToken.refresh(apiKey = apiKey, refreshToken = current.refreshToken)
            if (startedAt != generation.get() || session.value?.uid != current.uid) return@withLock null
            if (result.uid != current.uid) {
                signOut()
                throw AuthOperationException("Сессия истекла. Войдите повторно")
            }
            val updated = current.copy(
                refreshToken = result.refreshToken,
                idToken = result.idToken,
                expiresAtMillis = expiry(result.expiresIn),
            )
            store.write(updated)
            session.value = updated
            updated.idToken
        } catch (error: HttpException) {
            if (error.code() == 400 || error.code() == 401) {
                signOut()
                throw AuthOperationException("Сессия истекла. Войдите повторно")
            }
            throw AuthOperationException(error.authMessage())
        } catch (_: IOException) {
            throw AuthOperationException("Нет соединения с Firebase. Проверьте сеть")
        }
    }

    override suspend fun signIn(email: String, password: String) {
        requireConfigured()
        try {
            val result = identity.signIn(apiKey, EmailPasswordRequest(email, password))
            saveSession(AuthSession(
                uid = result.localId, email = result.email ?: email,
                refreshToken = result.refreshToken, idToken = result.idToken,
                expiresAtMillis = expiry(result.expiresIn),
            ))
        } catch (error: HttpException) {
            throw AuthOperationException(error.authMessage())
        } catch (_: IOException) {
            throw AuthOperationException("Нет соединения с Firebase. Проверьте сеть")
        }
    }

    override suspend fun register(email: String, password: String) {
        requireConfigured()
        try {
            val result = identity.register(apiKey, EmailPasswordRequest(email, password))
            saveSession(AuthSession(
                uid = result.localId, email = result.email ?: email,
                refreshToken = result.refreshToken, idToken = result.idToken,
                expiresAtMillis = expiry(result.expiresIn),
            ))
        } catch (error: HttpException) {
            throw AuthOperationException(error.authMessage())
        } catch (_: IOException) {
            throw AuthOperationException("Нет соединения с Firebase. Проверьте сеть")
        }
    }

    override suspend fun sendPasswordReset(email: String) {
        requireConfigured()
        try { identity.sendPasswordReset(apiKey, PasswordResetRequest(email = email)) }
        catch (error: HttpException) { throw AuthOperationException(error.authMessage()) }
        catch (_: IOException) { throw AuthOperationException("Нет соединения с Firebase. Проверьте сеть") }
    }

    override fun signOut() {
        generation.incrementAndGet()
        store.clear()
        session.value = null
    }

    private fun saveSession(value: AuthSession) {
        generation.incrementAndGet()
        store.write(value)
        session.value = value
    }

    private fun requireConfigured() {
        if (!isConfigured) throw AuthOperationException("Firebase не настроен для этого приложения")
    }
}

private fun expiry(seconds: String): Long = System.currentTimeMillis() +
    (seconds.toLongOrNull() ?: 3600L).coerceIn(1L, 86_400L) * 1000

private fun HttpException.authMessage(): String {
    val code = runCatching {
        JsonParser.parseString(response()?.errorBody()?.string())
            .asJsonObject.getAsJsonObject("error").get("message").asString
    }.getOrNull()?.substringBefore(' ')
    return when (code) {
        "EMAIL_EXISTS" -> "Этот адрес уже зарегистрирован"
        "EMAIL_NOT_FOUND", "INVALID_PASSWORD", "INVALID_LOGIN_CREDENTIALS" -> "Неверная почта или пароль"
        "INVALID_EMAIL" -> "Введите корректный адрес электронной почты"
        "WEAK_PASSWORD" -> "Слишком простой пароль"
        "USER_DISABLED" -> "Аккаунт отключён"
        "TOO_MANY_ATTEMPTS_TRY_LATER" -> "Слишком много попыток. Повторите позже"
        "INVALID_API_KEY", "PROJECT_NOT_FOUND" -> "Проверьте настройки Firebase"
        else -> "Не удалось выполнить запрос авторизации (${code()})"
    }
}
