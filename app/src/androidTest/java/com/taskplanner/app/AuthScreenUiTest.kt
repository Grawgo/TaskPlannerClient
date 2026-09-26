package com.taskplanner.app

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.taskplanner.app.presentation.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthScreenUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun navigatesBetweenLoginRegistrationAndPasswordReset() {
        compose.onNodeWithText("Войти").performClick()
        compose.onNodeWithText("Вход").assertExists()

        compose.onNodeWithText("Создать аккаунт").performClick()
        compose.onNodeWithText("Регистрация").assertExists()
        compose.onNodeWithText("Повторите пароль").assertExists()

        compose.onNodeWithText("Вернуться ко входу").performClick()
        compose.onNodeWithText("Забыли пароль?").performClick()
        compose.onNodeWithText("Сброс пароля").assertExists()
        compose.onNodeWithText("Отправить письмо").assertExists()
    }
}
