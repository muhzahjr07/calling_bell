package com.example.callingbell

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object RingEventBus {
    private val _events = MutableSharedFlow<RingEvent>(extraBufferCapacity = 10)
    val events = _events.asSharedFlow()

    fun emitEvent(event: RingEvent) {
        _events.tryEmit(event)
    }
}

data class RingEvent(
    val senderName: String,
    val intentLabel: String,
    val emoji: String,
    val soundId: String,
    val intentId: String,
    val timestamp: String
)
