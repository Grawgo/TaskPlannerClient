package com.taskplanner.app.di

import android.content.Context
import android.os.Build
import androidx.room.Room
import com.taskplanner.app.BuildConfig
import com.taskplanner.app.data.local.SearchHistoryDao
import com.taskplanner.app.data.local.SearchHistoryDatabase
import com.taskplanner.app.data.remote.TaskApi
import com.taskplanner.app.data.remote.FirebaseIdentityApi
import com.taskplanner.app.data.remote.FirebaseSecureTokenApi
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
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryBindings {
    @Binds abstract fun tasks(implementation: RemoteTaskRepository): TaskRepository
    @Binds abstract fun history(implementation: RoomSearchHistoryRepository): SearchHistoryRepository
    @Binds abstract fun theme(implementation: DataStoreThemeRepository): ThemeRepository
    @Binds abstract fun session(implementation: FirebaseAuthRepository): SessionProvider
    @Binds abstract fun auth(implementation: FirebaseAuthRepository): AuthRepository
    @Binds abstract fun sessionStore(implementation: EncryptedSessionStore): SessionStore
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Named("firebaseApiKey")
    fun firebaseApiKey(): String = BuildConfig.FIREBASE_API_KEY

    @Provides @Singleton
    fun retrofit(): Retrofit = Retrofit.Builder()
        .baseUrl(debugApiBaseUrl())
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private fun debugApiBaseUrl(): String {
        if (!BuildConfig.DEBUG) return BuildConfig.API_BASE_URL
        val emulator = Build.HARDWARE.contains("ranchu", ignoreCase = true) ||
            Build.HARDWARE.contains("goldfish", ignoreCase = true) ||
            Build.FINGERPRINT.startsWith("generic", ignoreCase = true)
        return if (emulator) BuildConfig.API_BASE_URL else "http://127.0.0.1:8080/"
    }

    @Provides @Singleton fun taskApi(retrofit: Retrofit): TaskApi = retrofit.create(TaskApi::class.java)

    @Provides @Singleton
    fun firebaseIdentityApi(): FirebaseIdentityApi = Retrofit.Builder()
        .baseUrl("https://identitytoolkit.googleapis.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(FirebaseIdentityApi::class.java)

    @Provides @Singleton
    fun firebaseSecureTokenApi(): FirebaseSecureTokenApi = Retrofit.Builder()
        .baseUrl("https://securetoken.googleapis.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(FirebaseSecureTokenApi::class.java)

    @Provides @Singleton
    fun searchDatabase(@ApplicationContext context: Context): SearchHistoryDatabase =
        Room.databaseBuilder(context, SearchHistoryDatabase::class.java, "task_planner_search.db").build()

    @Provides fun searchDao(database: SearchHistoryDatabase): SearchHistoryDao = database.searchHistoryDao()
}
