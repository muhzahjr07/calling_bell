package com.example.callingbell

import android.content.Intent
import android.util.Log
import com.example.callingbell.data.SharedPrefsManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.json.JSONObject

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCM", "Message received from: ${remoteMessage.from}")

        // FCM payloads can be in "data" or "notification"
        // We prefer "data" so it handles everything silently in background
        if (remoteMessage.data.isNotEmpty()) {
            val type = remoteMessage.data["type"]
            val sender = remoteMessage.data["sender"]
            val roomCode = remoteMessage.data["roomCode"]

            if (type == "RING") {
                val prefs = SharedPrefsManager(this)
                val settings = prefs.getSettings()
                
                // Show Full Screen Intent Notification
                val serviceIntent = Intent(this, RingListenerService::class.java).apply {
                    action = "SHOW_RING_SCREEN"
                    putExtra("sender", sender)
                    putExtra("roomCode", roomCode)
                }
                startService(serviceIntent)
                
                // Also play sound
                playToneAndHaptics(this, "fcm_ring", settings.defaultChimeId, settings)
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New Token: $token")
        // No need to send to server since we use topics
    }
}
