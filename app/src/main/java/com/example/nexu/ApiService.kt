package com.example.nexu

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

// ---------- MODELOS ----------

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginData(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String
)

data class LoginResponse(
    val data: LoginData
)

// (si más adelante vas a usar chats, puedes dejar esto también)
data class ChatSummary(
    val id: String,
    @SerializedName("other_user") val otherUser: OtherUser,
    @SerializedName("last_message") val lastMessage: LastMessage?,
    @SerializedName("unread_messages") val unreadMessages: Int
)

data class OtherUser(
    val id: String,
    val name: String,
    @SerializedName("avatar_url") val avatarUrl: String?
)

data class LastMessage(
    val content: String,
    val timestamp: String
)

data class ChatsResponse(
    val data: List<ChatSummary>
)

data class Message(
    val id: String,
    @SerializedName("sender_id") val senderId: String,
    val content: String,
    val timestamp: String,
    val delivered: Boolean,
    @SerializedName("conversation_id") val conversationId: String
)

data class MessagesResponse(
    val data: List<Message>
)
data class SignupRequest(
    val name: String,
    val email: String,
    val password: String,
    val gender: String
)
data class UserProfile(
    val id: String,
    val name: String,
    val email: String
)
data class UserProfileResponse(
    val data: UserProfile
)

data class SignupResponse(
    val data: LoginData   // Usa la misma estructura que login
)


// ---------- INTERFAZ API ----------

interface ApiService {

    @POST("users/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("chats/")
    suspend fun getChats(@Header("Authorization") token: String): Response<ChatsResponse>

    @GET("chats/{chatId}")
    suspend fun getMessages(
        @Header("Authorization") token: String,
        @Path("chatId") chatId: String
    ): Response<MessagesResponse>
    @POST("users/signup")
    suspend fun signup(@Body request: SignupRequest): Response<SignupResponse>

    @GET("users/me")
    suspend fun getUserProfile(
        @Header("Authorization") token: String
    ): Response<UserProfileResponse>


}

// ---------- RETROFIT CLIENT ----------

object RetrofitClient {
    // Cambia esto por la URL de tu backend
    private const val BASE_URL = "http://192.168.1.15:5000/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
            .create(ApiService::class.java)
    }
}
