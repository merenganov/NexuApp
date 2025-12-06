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
import retrofit2.Call
import retrofit2.http.DELETE
import retrofit2.http.Multipart
import retrofit2.http.PUT
import retrofit2.http.Part

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
    val email: String,
    val career: String?,
    val bio: String?,
    val date_of_birth: String?,
    val gender: String?,
    val tags: List<String>?,
    val avatar_url: String?
)
data class UserProfileResponse(
    val data: UserProfile
)

data class SignupResponse(
    val data: LoginData   // Usa la misma estructura que login
)
data class Tag(
    val id: String,
    val name: String,
    val icon: String?,
    val description: String?
)

data class TagsResponse(
    val data: List<Tag>
)

// Request para actualizar perfil
data class UpdateProfileRequest(
    val career: String?,
    val date_of_birth: String?,
    val bio: String?,
    val gender: String?,
    val tag_ids: List<String>?
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


    @PUT("users/me")
    suspend fun updateUserProfile(
        @Header("Authorization") token: String,
        @Body request: UpdateProfileRequest
    ): Response<UserProfileResponse>

    @GET("tags/")
    suspend fun getTags(
        @Header("Authorization") auth: String
    ): Response<TagResponse>


    @Multipart
    @POST("users/upload_avatar")
    suspend fun uploadAvatar(
        @Header("Authorization") token: String,
        @Part avatar: okhttp3.MultipartBody.Part
    ): Response<UserProfileResponse>

    @GET("posts/")
    suspend fun getPosts(
        @Header("Authorization") auth: String
    ): Response<PostsResponse>


    @POST("posts/")
    suspend fun createPost(
        @Header("Authorization") auth: String,
        @Body body: CreatePostRequest
    ): Response<CreatePostResponse>


    @DELETE("posts/{id}")
    suspend fun deletePost(
        @Header("Authorization") auth: String,
        @Path("id") postId: String
    ): Response<DeletePostResponse>






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
