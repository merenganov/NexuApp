package com.example.nexu.sockets

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

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