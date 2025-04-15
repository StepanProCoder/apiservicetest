package com.staple.apiservicetestapp

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

class TimerService : Service() {

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_RESET = "ACTION_RESET"
        const val EXTRA_DURATION = "EXTRA_DURATION"
        const val NOTIFICATION_CHANNEL_ID = "timer_channel"
        const val NOTIFICATION_ID = 1
    }

    private var durationMillis: Long = 0L
    private var remainingMillis: Long = 0L
    private var timerJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification("Таймер запускается..."))

        when (intent?.action) {
            ACTION_START -> {
                durationMillis = intent.getLongExtra(EXTRA_DURATION, 0L)
                if (durationMillis > 0) {
                    startTimer(durationMillis)
                }
            }

            ACTION_RESET -> {
                stopTimer()
                stopSelf()
            }
        }

        return START_STICKY
    }

    private fun startTimer(duration: Long) {
        timerJob?.cancel()
        remainingMillis = duration
        timerJob = CoroutineScope(Dispatchers.Default).launch {
            while (remainingMillis > 0) {
                delay(1000)
                remainingMillis -= 1000

                // Каждую минуту — обновляем уведомление
                if (remainingMillis % (60 * 1000) == 0L) {
                    val minutesLeft = TimeUnit.MILLISECONDS.toMinutes(remainingMillis)
                    updateNotification("Осталось $minutesLeft мин.")
                }
            }

            showFinishedNotification()
            stopSelf()
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
    }

    private fun updateNotification(text: String) {
        val notification = buildNotification(text)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun showFinishedNotification() {
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Таймер завершён")
            .setContentText("Время вышло!")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID + 1, notification)

        // Проигрываем звук
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val ringtone = RingtoneManager.getRingtone(applicationContext, alarmUri)
        ringtone.play()
    }

    private fun buildNotification(contentText: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Таймер")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Уведомления таймера",
            NotificationManager.IMPORTANCE_HIGH
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTimer()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
