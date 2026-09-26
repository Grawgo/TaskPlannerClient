package com.taskplanner.app.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.http.Query

data class EmailPasswordRequest(
    val email: String,
    val password: String,
    val returnSecureToken: Boolean = true,
)

data class PasswordResetRequest(val requestType: String = "PASSWORD_RESET", val email: String)

data class FirebaseSignInResponse(
    val idToken: String,
    val refreshToken: String,
    val expiresIn: String,
    val localId: String,
    val email: String?,
)

data class FirebaseRefreshResponse(
    @SerializedName("access_token") val idToken: String,
    @SerializedName("refresh_token") val refreshToken: String,
    @SerializedName("expires_in") val expiresIn: String,
    @SerializedName("user_id") val uid: String,
)

interface FirebaseIdentityApi {
    @POST("v1/accounts:signInWithPassword")
    suspend fun signIn(@Query("key") apiKey: String, @Body input: EmailPasswordRequest): FirebaseSignInResponse

    @POST("v1/accounts:signUp")
    suspend fun register(@Query("key") apiKey: String, @Body input: EmailPasswordRequest): FirebaseSignInResponse

    @POST("v1/accounts:sendOobCode")
    suspend fun sendPasswordReset(@Query("key") apiKey: String, @Body input: PasswordResetRequest)
}

interface FirebaseSecureTokenApi {
    @FormUrlEncoded
    @POST("v1/token")
    suspend fun refresh(
        @Query("key") apiKey: String,
        @Field("grant_type") grantType: String = "refresh_token",
        @Field("refresh_token") refreshToken: String,
    ): FirebaseRefreshResponse
}
