package com.example.nexu

data class UserProfile(
    val id: String,
    val name: String,
    val career: String?,
    val bio: String?,
    val date_of_birth: String?,
    val gender: String?,
    val tags: List<String>?,
    val avatar_url: String?
)
