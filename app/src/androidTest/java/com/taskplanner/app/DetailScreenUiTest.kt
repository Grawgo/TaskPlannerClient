package com.taskplanner.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.taskplanner.app.domain.model.*
import com.taskplanner.app.domain.repository.TaskRepository
import com.taskplanner.app.domain.usecase.TaskActions
import com.taskplanner.app.presentation.DetailScreen
import com.taskplanner.app.presentation.DetailViewModel
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DetailScreenUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun deleteRemainsReachableWithLongDescriptionOnShortScreen() {
        val repository = DetailRepository()
        lateinit var viewModel: DetailViewModel
        var returned = false
        compose.runOnUiThread {
            viewModel = DetailViewModel(SavedStateHandle(mapOf("id" to "task")), TaskActions(repository))
        }
        compose.setContent {
            MaterialTheme {
                Box(Modifier.height(240.dp)) {
                    DetailScreen(viewModel, onBack = { returned = true }, onEdit = {})
                }
            }
        }

        compose.onNodeWithText("  Удалить").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertTrue(repository.deleted)
            assertTrue(returned)
        }
    }

    private class DetailRepository : TaskRepository {
        var deleted = false
        private val task = Task("task", "Длинная задача", "Описание задачи. ".repeat(120), null,
            TaskPriority.HIGH, TaskStatus.TODO, null, "2026-10-03T10:00:00Z", "2026-10-03T10:00:00Z")
        override suspend fun get(id: String) = task
        override suspend fun delete(id: String) { deleted = true }
        override suspend fun list(query: TaskQuery) = error("Not used")
        override suspend fun create(draft: TaskDraft) = error("Not used")
        override suspend fun update(id: String, draft: TaskDraft) = error("Not used")
    }
}
