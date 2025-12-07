package com.example.nexu.sockets

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import org.json.JSONObject

val gson = Gson()

/**
 * Extensión genérica para parsear un SocketEvent al payload deseado
 */
inline fun <reified T> SocketEvent.parsePayload(): T? {
    return try {
        val wrapper = gson.fromJson(this.data, SocketPayloadWrapper::class.java)
        gson.fromJson(wrapper.payload, T::class.java)
    } catch (e: JsonSyntaxException) {
        e.printStackTrace()
        null
    }
}

/**
 * Extensión específica para new_notification
 */
fun SocketEvent.toNewNotification(): NewNotificationPayload? {
    if (this.name != "new_notification") return null
    return this.parsePayload<NewNotificationPayload>()
}

fun SendMessagePayload.toJson(): JSONObject {
    return JSONObject().apply {
        put("target_id", target_id)
        put("content", content)
    }
}
