package com.samai.assistant.engine

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.samai.assistant.MainActivity
import com.samai.assistant.R
import com.samai.assistant.overlay.SAMOverlayManager
import com.samai.assistant.voice.VoiceManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SAMEngineService : LifecycleService() {

    @Inject lateinit var voiceManager: VoiceManager
    @Inject lateinit var overlayManager: SAMOverlayManager

    companion object {
        const val CHANNEL_ID = "sam_engine_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.samai.assistant.engine.START"
        const val ACTION_STOP = "com.samai.assistant.engine.STOP"

        @Volatile
        var isRunning = false
            private set
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                startForegroundService()
                isRunning = true
                startVoiceEngine()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        overlayManager.hide()
    }

    private fun startForegroundService() {
        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, SAMEngineService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val openIntent = Intent(this, MainActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(
            this, 1, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SAM Engine Active")
            .setContentText("Listening for wake word...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(openPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Stop", stopPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "SAM Engine",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "SAM background voice engine service"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun startVoiceEngine() {
        lifecycleScope.launch {
            voiceManager.isListening.collectLatest { listening ->
                if (listening) {
                    overlayManager.show(
                        com.samai.assistant.overlay.OverlayState.LISTENING,
                        "Listening..."
                    )
                }
            }
        }

        lifecycleScope.launch {
            voiceManager.transcriptionFlow.collectLatest { text ->
                if (text.isNotBlank()) {
                    val wakeWords = listOf("sam", "hey sam", "ok sam")
                    val lowerText = text.lowercase()

                    if (wakeWords.any { lowerText.contains(it) }) {
                        val command = lowerText
                            .replace("hey sam", "").replace("ok sam", "").replace("sam", "").trim()

                        if (command.isNotEmpty()) {
                            handleCommand(command)
                        } else {
                            overlayManager.show(
                                com.samai.assistant.overlay.OverlayState.LISTENING,
                                "Yes? I'm listening."
                            )
                            voiceManager.startListening()
                        }
                    }
                }
            }
        }
    }

    private fun handleCommand(command: String) {
        lifecycleScope.launch {
            overlayManager.show(com.samai.assistant.overlay.OverlayState.THINKING, "Processing...")

            val taskEngine = SAMTaskEngine(applicationContext, overlayManager)
            val result = taskEngine.executeCommand(command)

            if (result.success) {
                overlayManager.showResult(true, result.message)
                voiceManager.speak(result.message)
            } else {
                overlayManager.showResult(false, result.message)
                voiceManager.speak("Sorry, ${result.message}")
            }
        }
    }
}
