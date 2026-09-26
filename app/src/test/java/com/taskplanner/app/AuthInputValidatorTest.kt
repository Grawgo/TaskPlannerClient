package com.taskplanner.app

import com.taskplanner.app.domain.model.AuthValidationException
import com.taskplanner.app.domain.usecase.AuthInputValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AuthInputValidatorTest {
    @Test fun trimsEmailButPreservesPassword() {
        val input = AuthInputValidator.login("  person@example.com  ", " secret ")
        assertEquals("person@example.com", input.email)
        assertEquals(" secret ", input.password)
    }

    @Test fun rejectsInvalidRegistration() {
        assertThrows(AuthValidationException::class.java) {
            AuthInputValidator.register("person@example.com", "short", "short")
        }
        assertThrows(AuthValidationException::class.java) {
            AuthInputValidator.register("person@example.com", "secret123", "different")
        }
    }

    @Test fun rejectsInvalidResetEmail() {
        assertThrows(AuthValidationException::class.java) { AuthInputValidator.email("not-an-email") }
    }
}
