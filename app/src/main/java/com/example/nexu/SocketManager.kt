package com.example.nexu.network

import android.content.Context
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.json.JSONObject
import java.net.URISyntaxException

/**
 * SocketManager (SharedFlow migration)
 *
 * - Centralized singleton that manages a single Socket.IO connection for the whole app.
 * - Emits incoming socket events through a global SharedFlow (SocketEventBus.events).
 * - Activities/Fragments/ViewModels can collect from SocketEventBus.events using lifecycleScope
 *   and "repeatOnLifecycle" to react to events without dealing with BroadcastReceivers.
 * - Keeps helpers to emit events and to add/remove ad-hoc socket listeners when strictly needed.
 *
 * Usage (high level):
 *  - SocketManager.initialize(appContext, jwt, "http://10.0.2.2:5000")
 *  - SocketManager.emit("dm", jsonObject)
 *  - Observe events:
 *      lifecycleScope.launch {
 *          repeatOnLifecycle(Lifecycle.State.STARTED) {
 *              SocketEventBus.events.collect { event ->
 *                  // handle event.name and event.data
 *              }
 *          }
 *      }
 */

/** Simple event container used by the SharedFlow bus. */
data class SocketEvent(
    val name: String,
    val data: String
)

/** Global event bus backed by a MutableSharedFlow */
object SocketEventBus {
    // No replay, buffered to avoid drops for short bursts; overflow drops oldest
    private val _events = MutableSharedFlow<SocketEvent>(
        replay = 0,
        extraBufferCapacity = 64
    )

    val events = _events.asSharedFlow()

    internal fun tryEmit(event: SocketEvent) {
        _events.tryEmit(event)
    }
}

object SocketManager {
    private var socket: Socket? = null
    private var initialized = false
    private lateinit var appContext: Context

    // Coroutine scope used for emitting events to the SharedFlow and for internal ops
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Protect initialize from races when called multiple times concurrently
    private val initMutex = Mutex()

    /** Initialize the socket manager once per app session. Safe to call multiple times. */
    suspend fun initialize(context: Context, jwt: String, baseUrl: String) {
        initMutex.withLock {
            if (initialized) return
            appContext = context.applicationContext

            val opts = IO.Options().apply {
                reconnection = true
                extraHeaders = mapOf("Authorization" to listOf("Bearer $jwt"))
            }

            try {
                socket = IO.socket(baseUrl, opts)
            } catch (e: URISyntaxException) {
                e.printStackTrace()
                return
            }

            socket?.apply {
                on(Socket.EVENT_CONNECT) { _ ->
                    broadcastEvent("connect", JSONObject().put("msg", "connected").put("id", id()))
                }

                on(Socket.EVENT_DISCONNECT) { _ ->
                    broadcastEvent("disconnect", JSONObject().put("msg", "disconnected"))
                }

                on(Socket.EVENT_CONNECT_ERROR) { args ->
                    val o = JSONObject()
                    if (args.isNotEmpty()) o.put("err", args[0].toString())
                    broadcastEvent("connect_error", o)
                }

                // App-level events from server
                on("message") { args ->
                    if (args.isNotEmpty()) broadcastSafe("message", args[0])
                }

                on("new_notification") { args ->
                    if (args.isNotEmpty()) broadcastSafe("new_notification", args[0])
                }

                on("client_error") { args ->
                    if (args.isNotEmpty()) broadcastSafe("client_error", args[0])
                }

                on("server_error") { args ->
                    if (args.isNotEmpty()) broadcastSafe("server_error", args[0])
                }

                connect()
            }

            initialized = true
        }
    }

    /** Emit an event to the server with a JSONObject payload. */
    fun emit(event: String, payload: JSONObject) {
        socket?.emit(event, payload)
    }


    /** Disconnect the socket (e.g. on logout). */
    fun disconnect() {
        socket?.disconnect()
        socket?.off()
        socket = null
        initialized = false
    }

    /** Internal: broadcast raw event + data (JSONObject or String) to the SharedFlow. */
    private fun broadcastSafe(event: String, dataObj: Any) {
        val jsonString = when (dataObj) {
            is JSONObject -> dataObj.toString()
            is String -> dataObj
            else -> dataObj.toString()
        }
        broadcastEvent(event, JSONObject().put("payload", jsonString))
    }

    private fun broadcastEvent(event: String, payloadObj: JSONObject) {
        val json = payloadObj.toString()
        // Use scope to avoid blocking socket callbacks and to safely emit to the flow
        scope.launch {
            SocketEventBus.tryEmit(SocketEvent(event, json))
        }
    }

    /** Helper: is the socket connected? */
    fun isConnected(): Boolean = socket?.connected() == true
}
