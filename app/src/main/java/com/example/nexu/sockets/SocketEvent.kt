package com.example.nexu.sockets

/** Simple event container used by the SharedFlow bus. */
data class SocketEvent(
    val name: String,
    val data: String
)

