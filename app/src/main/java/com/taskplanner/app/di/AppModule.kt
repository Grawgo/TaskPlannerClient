package com.taskplanner.app.di

import android.content.Context
import androidx.room.Room
import com.taskplanner.app.BuildConfig
import com.taskplanner.app.data.local.SearchHistoryDao
import com.taskplanner.app.data.local.SearchHistoryDatabase
import com.taskplanner.app.data.remote.TaskApi
import com.taskplanner.app.data.repository.*
import com.taskplanner.app.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryBindings {
    @Binds abstract fun tasks(implementation: RemoteTaskRepository): TaskRepository
    @Binds abstract fun history(implementation: RoomSearchHistoryRepository): SearchHistoryRepository
    @Binds abstract fun theme(implementation: DataStoreThemeRepository): ThemeRepository
    @Binds abstract fun session(implementation: NoSessionProvider): SessionProvider
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun retrofit(): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides @Singleton fun taskApi(retrofit: Retrofit): TaskApi = retrofit.create(TaskApi::class.java)

    @Provides @Singleton
    fun searchDatabase(@ApplicationContext context: Context): SearchHistoryDatabase =
        Room.databaseBuilder(context, SearchHistoryDatabase::class.java, "task_planner_search.db").build()

    @Provides fun searchDao(database: SearchHistoryDatabase): SearchHistoryDao = database.searchHistoryDao()
}
