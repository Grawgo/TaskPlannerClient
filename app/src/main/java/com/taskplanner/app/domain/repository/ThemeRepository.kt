package com.taskplanner.app.domain.repository

import kotlinx.coroutines.flow.Flow

interface ThemeRepository {
    val darkMode: Flow<Boolean>
    suspend fun setDarkMode(enabled: Boolean)
}
