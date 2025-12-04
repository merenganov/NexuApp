package com.example.nexu

data class Mensaje(
    val texto: String,
    val autor: String,      // email de quien envió
    val receptor: String,   // email de quien recibe
    val timestamp: Long     // para ordenar mensajes
)
