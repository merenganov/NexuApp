package com.example.nexu

data class Post(
    val id: String,
    val description: String,
    val timestamp: String,
    val user: UserPostData,
    val tag: TagPostData
)

data class UserPostData(
    val id: String,
    val name: String,
    val career: String,
    val avatar_url: String?
)

data class TagPostData(
    val id: String,
    val name: String,
    val icon: String
)


// Respuestas de la API
data class PostsResponse(
    val data: List<Post>
)

data class CreatePostRequest(
    val tag_id: String,
    val description: String
)

data class CreatePostResponse(
    val data: Post // si no te regresa enriquecido, luego recargamos la lista
)

data class DeletePostResponse(
    val message: String
)

data class TagResponse(
    val data: List<TagPostData>
)
