package com.taskplanner.app.data.repository

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

data class AuthSession(
    val uid: String,
    val email: String?,
    val refreshToken: String,
    val idToken: String?,
    val expiresAtMillis: Long,
)

interface SessionStore {
    fun read(): AuthSession?
    fun write(session: AuthSession)
    fun clear()
}

@Singleton
class EncryptedSessionStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : SessionStore {
    private val preferences = context.getSharedPreferences("auth_session", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val alias = "task_planner_auth_session"

    override fun read(): AuthSession? {
        val saved = preferences.getString("session", null) ?: return null
        return runCatching {
            val bytes = Base64.decode(saved, Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
            cipher.updateAAD(context.packageName.toByteArray())
            gson.fromJson(String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8), AuthSession::class.java)
                ?.takeIf { it.uid.isNotBlank() && it.refreshToken.isNotBlank() }
        }.getOrNull().also { if (it == null) clear() }
    }

    override fun write(session: AuthSession) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        cipher.updateAAD(context.packageName.toByteArray())
        val plain = gson.toJson(session).toByteArray(Charsets.UTF_8)
        val saved = Base64.encodeToString(cipher.iv + cipher.doFinal(plain), Base64.NO_WRAP)
        preferences.edit().putString("session", saved).apply()
    }

    override fun clear() { preferences.edit().remove("session").apply() }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }
}
