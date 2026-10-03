package com.taskplanner.app

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.taskplanner.app.presentation.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthScreenUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun navigatesBetweenLoginRegistrationAndPasswordReset() {
        compose.onNodeWithText("Настройки").performClick()
        compose.onNodeWithText("Войти в аккаунт").performScrollTo().performClick()
        compose.onNodeWithText("Вход").assertExists()

        compose.onNodeWithText("Создать аккаунт").performScrollTo().performClick()
        compose.onNodeWithText("Регистрация").assertExists()
        compose.onNodeWithText("Повторите пароль").assertExists()

        compose.onNodeWithText("Вернуться ко входу").performScrollTo().performClick()
        compose.onNodeWithText("Забыли пароль?").performScrollTo().performClick()
        compose.onNodeWithText("Сброс пароля").assertExists()
        compose.onNodeWithText("Отправить письмо").assertExists()
    }
}
