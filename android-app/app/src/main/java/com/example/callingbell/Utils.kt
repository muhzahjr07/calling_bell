package com.example.callingbell

import android.content.Context
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.callingbell.data.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

fun playToneAndHaptics(context: Context, intentId: String, soundId: String, settings: AppSettings) {
    try {
        // 1. Haptics Waveform
        if (settings.isVibrationEnabled) {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 150, 100, 150, 100, 300)
                val maxAmp = (255 * settings.hapticsIntensity).toInt().coerceIn(1, 255)
                val amplitudes = intArrayOf(0, maxAmp, 0, maxAmp, 0, maxAmp)
                val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 150, 100, 150, 100, 300), -1)
            }
        }

        // 2. Play Sound with Volume
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val originalVol = am.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
        val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION)
        val targetVol = (maxVol * settings.volume).toInt()
        am.setStreamVolume(AudioManager.STREAM_NOTIFICATION, targetVol, 0)

        when (soundId) {
            "classic" -> {
                val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
                toneGen.startTone(ToneGenerator.TONE_CDMA_CALL_SIGNAL_ISDN_NORMAL, 600)
                runBlocking { delay(700) }
                toneGen.release()
            }
            "hotel" -> {
                val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
                runBlocking { delay(250) }
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
                runBlocking { delay(300) }
                toneGen.release()
            }
            "beep" -> {
                val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
                toneGen.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 200)
                runBlocking { delay(300) }
                toneGen.release()
            }
            "chirp" -> {
                val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
                toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE, 500)
                runBlocking { delay(600) }
                toneGen.release()
            }
            "alarm" -> {
                val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
                toneGen.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 800)
                runBlocking { delay(900) }
                toneGen.release()
            }
            else -> {
                // Default notification sound
                val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val r = RingtoneManager.getRingtone(context, notification)
                r.play()
            }
        }
        
        // Restore volume
        am.setStreamVolume(AudioManager.STREAM_NOTIFICATION, originalVol, 0)

    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun sendUdpBroadcast(payload: JSONObject) {
    Thread {
        try {
            val socket = DatagramSocket()
            socket.broadcast = true
            val data = payload.toString().toByteArray()
            val packet = DatagramPacket(data, data.size, InetAddress.getByName("255.255.255.255"), 3001)
            socket.send(packet)
            socket.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }.start()
}
