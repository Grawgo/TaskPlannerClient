package com.taskplanner.app

import android.content.pm.ActivityInfo
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.taskplanner.app.presentation.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SearchScreenUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun querySurvivesRotationAndClearRestoresEmptyState() {
        compose.onNodeWithText("Найти задачу").performClick()
        compose.onNodeWithText("Поиск задач").assertExists()
        compose.onNode(hasSetTextAction()).performTextInput("kursovaya")
        compose.onNodeWithContentDescription("Очистить").assertExists()

        try {
            compose.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            compose.waitForIdle()
            compose.onNodeWithText("kursovaya").assertExists()
            compose.onNodeWithContentDescription("Очистить").performClick()
            compose.onNodeWithText("Поиск задач").assertExists()
            compose.onNodeWithContentDescription("Очистить").assertDoesNotExist()
        } finally {
            compose.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }
}
