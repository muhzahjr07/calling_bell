package com.example.callingbell

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.callingbell.data.SharedPrefsManager
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress

class RingListenerService : Service() {

    private val CHANNEL_ID = "RingListenerServiceChannel"
    private val HIGH_PRIORITY_CHANNEL_ID = "RingAlertChannel"
    private var udpSocket: DatagramSocket? = null
    private var listenJob: Job? = null
    private var heartbeatJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var currentRoomCode = ""
    private var multicastLock: WifiManager.MulticastLock? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "SHOW_RING_SCREEN") {
            val sender = intent.getStringExtra("sender") ?: "Partner"
            val roomCode = intent.getStringExtra("roomCode") ?: ""
            showFullScreenNotification(sender, "Ring", "🔔")
            return START_REDELIVER_INTENT
        }
        
        currentRoomCode = intent?.getStringExtra("ROOM_CODE") ?: ""
        
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (multicastLock == null) {
            multicastLock = wifiManager.createMulticastLock("CallingBellLock")
            multicastLock?.setReferenceCounted(true)
            multicastLock?.acquire()
        }

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Calling Bell")
            .setContentText("Listening for rings on Wi-Fi...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)

        startListening()
        startHeartbeat()

        return START_REDELIVER_INTENT
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = serviceScope.launch {
            while (isActive) {
                if (currentRoomCode.isNotEmpty()) {
                    val prefs = SharedPrefsManager(this@RingListenerService)
                    if (prefs.userName.isNotEmpty()) {
                        val payload = JSONObject().apply {
                            put("type", "heartbeat")
                            put("roomCode", currentRoomCode)
                            put("userName", prefs.userName)
                        }
                        sendUdpBroadcast(payload)
                    }
                }
                delay(3000)
            }
        }
    }

    private fun startListening() {
        listenJob?.cancel()
        listenJob = serviceScope.launch {
            try {
                if (udpSocket == null || udpSocket?.isClosed == true) {
                    udpSocket = DatagramSocket(null)
                    udpSocket?.reuseAddress = true
                    udpSocket?.bind(InetSocketAddress(3001))
                    udpSocket?.broadcast = true
                }

                val buffer = ByteArray(2048)
                val packet = DatagramPacket(buffer, buffer.size)

                while (isActive) {
                    udpSocket?.receive(packet)
                    val message = String(packet.data, 0, packet.length)
                    try {
                        val json = JSONObject(message)
                        val roomCode = json.optString("roomCode")
                        
                        if (roomCode == currentRoomCode) {
                            val prefs = SharedPrefsManager(this@RingListenerService)
                            val type = json.optString("type", "ring")
                            
                            if (type == "heartbeat") {
                                val hbUser = json.optString("userName")
                                if (hbUser.isNotEmpty() && hbUser != prefs.userName) {
                                    HeartbeatEventBus.emitEvent(HeartbeatEvent(roomCode, hbUser, System.currentTimeMillis()))
                                }
                                continue
                            }

                            val targetUserName = json.optString("targetUserName", "")
                            
                            // Check if this ring is for us
                            if (targetUserName.isNotEmpty() && targetUserName != prefs.userName) {
                                continue
                            }

                            val sender = json.optString("senderName", "Partner")
                            // Don't ring ourselves if we are the sender
                            if (sender == prefs.userName && targetUserName.isEmpty()) {
                                continue 
                            }

                            val intentObj = json.optJSONObject("intention")
                            val soundId = json.optString("sound", prefs.getSettings().defaultChimeId)
                            val intentLabel = intentObj?.optString("label") ?: "Ring"
                            val emoji = intentObj?.optString("emoji") ?: "🔔"
                            val intentId = intentObj?.optString("id") ?: "general"
                            val timestamp = json.optString("timestamp", "")

                            // Emit to UI
                            RingEventBus.emitEvent(RingEvent(sender, intentLabel, emoji, soundId, intentId, timestamp))
                            
                            // Play sound and haptics if we are the receiver
                            if (sender != prefs.userName) {
                                // Launch in coroutine to prevent blocking the UDP receiver loop!
                                serviceScope.launch {
                                    playToneAndHaptics(this@RingListenerService, intentId, soundId, prefs.getSettings())
                                }
                                showFullScreenNotification(sender, intentLabel, emoji)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showFullScreenNotification(sender: String, intentLabel: String, emoji: String) {
        val fullScreenIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, 0,
            fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, HIGH_PRIORITY_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("$emoji Incoming Ring from $sender")
            .setContentText("Reason: $intentLabel")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    override fun onDestroy() {
        super.onDestroy()
        listenJob?.cancel()
        heartbeatJob?.cancel()
        udpSocket?.close()
        udpSocket = null
        if (multicastLock?.isHeld == true) {
            multicastLock?.release()
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            
            // Low priority channel for background service
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Ring Listener Service",
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(serviceChannel)

            // High priority channel for full-screen alerts
            val highPriorityChannel = NotificationChannel(
                HIGH_PRIORITY_CHANNEL_ID,
                "Ring Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows full-screen alerts for incoming rings"
            }
            manager.createNotificationChannel(highPriorityChannel)
        }
    }
}
