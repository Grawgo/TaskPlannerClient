package com.taskplanner.app.domain.usecase

import com.taskplanner.app.domain.model.AuthValidationException

data class AuthCredentials(val email: String, val password: String)

object AuthInputValidator {
    private val emailPattern = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

    fun login(email: String, password: String): AuthCredentials {
        val cleanEmail = email(email)
        if (password.isBlank()) throw AuthValidationException("Введите пароль")
        return AuthCredentials(cleanEmail, password)
    }

    fun register(email: String, password: String, confirmation: String): AuthCredentials {
        val credentials = login(email, password)
        if (password.length < 6) throw AuthValidationException("Пароль должен содержать не менее 6 символов")
        if (password != confirmation) throw AuthValidationException("Пароли не совпадают")
        return credentials
    }

    fun email(value: String): String {
        val clean = value.trim()
        if (!emailPattern.matches(clean)) throw AuthValidationException("Введите корректный адрес электронной почты")
        return clean
    }
}
