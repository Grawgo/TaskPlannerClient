package com.taskplanner.app.data.remote

import com.taskplanner.app.domain.model.TaskPriority
import com.taskplanner.app.domain.model.TaskStatus
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

data class TaskDto(
    val id: String,
    val title: String,
    val description: String,
    val dueAt: String?,
    val priority: TaskPriority,
    val status: TaskStatus,
    val completedAt: String?,
    val createdAt: String,
    val updatedAt: String,
)

data class TaskPageDto(val items: List<TaskDto>, val total: Long, val limit: Int, val offset: Int)

data class TaskInputDto(
    val title: String,
    val description: String,
    val dueAt: String?,
    val priority: TaskPriority,
    val status: TaskStatus,
)

interface TaskApi {
    @GET("v1/tasks")
    suspend fun list(
        @Header("Authorization") authorization: String,
        @Query("query") query: String?,
        @Query("status") status: String?,
        @Query("priority") priority: String?,
        @Query("sort") sort: String,
        @Query("order") order: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
    ): TaskPageDto

    @GET("v1/tasks/{id}")
    suspend fun get(@Header("Authorization") authorization: String, @Path("id") id: String): TaskDto

    @POST("v1/tasks")
    suspend fun create(@Header("Authorization") authorization: String, @Body input: TaskInputDto): TaskDto

    @PUT("v1/tasks/{id}")
    suspend fun update(
        @Header("Authorization") authorization: String,
        @Path("id") id: String,
        @Body input: TaskInputDto,
    ): TaskDto

    @DELETE("v1/tasks/{id}")
    suspend fun delete(@Header("Authorization") authorization: String, @Path("id") id: String): Response<Unit>
}
