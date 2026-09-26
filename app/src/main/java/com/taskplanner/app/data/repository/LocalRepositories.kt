package com.taskplanner.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.taskplanner.app.data.local.SearchHistoryDao
import com.taskplanner.app.domain.repository.AuthRepository
import com.taskplanner.app.domain.repository.SearchHistoryRepository
import com.taskplanner.app.domain.repository.SessionProvider
import com.taskplanner.app.domain.repository.ThemeRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.settingsStore by preferencesDataStore(name = "task_planner_settings")

class RoomSearchHistoryRepository @Inject constructor(
    private val dao: SearchHistoryDao,
    private val session: SessionProvider,
    private val auth: AuthRepository,
) : SearchHistoryRepository {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observe(): Flow<List<String>> = auth.authState.flatMapLatest { user ->
        user?.uid?.let(dao::observe) ?: flowOf(emptyList())
    }
    override suspend fun remember(query: String) {
        val uid = session.currentUid() ?: return
        val clean = query.trim().take(120)
        if (clean.isNotEmpty()) dao.remember(uid, clean, System.currentTimeMillis())
    }
    override suspend fun clear() { session.currentUid()?.let { dao.clear(it) } }
}

class DataStoreThemeRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ThemeRepository {
    private val darkModeKey = booleanPreferencesKey("dark_mode")
    override val darkMode: Flow<Boolean> = context.settingsStore.data.map { it[darkModeKey] ?: false }
    override suspend fun setDarkMode(enabled: Boolean) {
        context.settingsStore.edit { it[darkModeKey] = enabled }
    }
}
