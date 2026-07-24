package com.example.callingbell

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

data class HeartbeatEvent(val roomCode: String, val userName: String, val timestamp: Long)

object HeartbeatEventBus {
    private val _events = MutableSharedFlow<HeartbeatEvent>(extraBufferCapacity = 10)
    val events: SharedFlow<HeartbeatEvent> = _events

    fun emitEvent(event: HeartbeatEvent) {
        _events.tryEmit(event)
    }
}
