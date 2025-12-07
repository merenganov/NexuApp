package com.example.nexu.sockets

/** Wrapper del backend */
data class SocketPayloadWrapper(
    val payload: String
)

/** Payload del evento new_notification */
data class NewNotificationPayload(
    val chat_id: String,
    val sender_id: String,
    val sender_name: String,
    val message: String,
    val message_id: String,
    val timestamp: String
)
