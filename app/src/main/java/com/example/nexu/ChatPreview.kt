package com.example.nexu

data class ChatPreview(
    val nombre: String,
    val id: String,
    var ultimoMensaje: String,
    val timestamp: Long,
    val fotoPerfilUrl: String = ""
)
