package com.staple.apiservicetestapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Message
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class TimerService : Service() {
    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_RESET = "ACTION_RESET"
        const val EXTRA_DURATION = "EXTRA_DURATION"
        const val EXTRA_RESUMED = "EXTRA_RESUMED"
        const val NOTIFICATION_CHANNEL_ID = "timer_channel"
        const val NOTIFICATION_ID = 1
    }

    private var notificationCounter = 0
    private var durationMillis: Long = 0L
    private var remainingMillis: Long = 0L
    private var timerJob: Job? = null
    private lateinit var notificationManager: NotificationManager

    private val binder = LocalBinder()
    var handler: Handler? = null

    inner class LocalBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    @JvmName("setCustomHandler")
    fun setHandler(handler: Handler) {
        this.handler = handler
    }

    private fun updateTime(remainingMillis: Long) {
        handler?.sendMessage(
            Message.obtain().apply {
                what = MainActivity.MSG_UPDATE_TIME
                arg1 = remainingMillis.toInt()
            }
        )
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val resumed = intent.getBooleanExtra(EXTRA_RESUMED, false)
                durationMillis = intent.getLongExtra(EXTRA_DURATION, 0L)
                if (durationMillis > 0) {
                    startTimer(durationMillis, resumed)
                }
            }
            ACTION_STOP -> {
                stopTimer()
                stopSelf()
            }
            ACTION_RESET -> {
                resetTimer()
                stopSelf()
            }
        }

        return START_STICKY
    }

    private fun startTimer(duration: Long, resumed: Boolean) {
        timerJob?.cancel()
        remainingMillis = duration

        if (!resumed) {
            persistTimerState()
        }

        startForeground(NOTIFICATION_ID, buildNotification(formatTime(remainingMillis)))

        timerJob = CoroutineScope(Dispatchers.Default).launch {
            var lastMinute = TimeUnit.MILLISECONDS.toMinutes(remainingMillis)

            while (remainingMillis > 0) {
                delay(1000)
                remainingMillis -= 1000
                persistTimerState()
                updateTime(remainingMillis)
                updateNotification(formatTime(remainingMillis))

                val currentMinute = TimeUnit.MILLISECONDS.toMinutes(remainingMillis)
                if (currentMinute < lastMinute) {
                    lastMinute = currentMinute
                    sendNewMinuteNotification(currentMinute + 1)
                }
            }

            showFinishedNotification()
            clearPersistedState()
            stopSelf()
        }
    }

    private fun sendNewMinuteNotification(minutesLeft: Long) {
        notificationCounter++ // Увеличиваем счетчик для уникального ID

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Таймер")
            .setContentText("Осталось $minutesLeft ${getMinuteDeclension(minutesLeft)}")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false) // Разрешаем звук каждый раз
            .build()

        // Используем уникальный ID для каждого уведомления
        notificationManager.notify(NOTIFICATION_ID + notificationCounter, notification)
    }

    private fun getMinuteDeclension(minutes: Long): String {
        return when {
            minutes % 10 == 1L && minutes % 100 != 11L -> "минута"
            minutes % 10 in 2..4 && minutes % 100 !in 12..14 -> "минуты"
            else -> "минут"
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        clearPersistedState()
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun resetTimer() {
        timerJob?.cancel()
        clearPersistedState()
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun persistTimerState() {
        val prefs = getSharedPreferences("TimerPrefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putLong("remainingTime", remainingMillis)
            putLong("startTime", System.currentTimeMillis())
            apply()
        }
    }

    private fun clearPersistedState() {
        val prefs = getSharedPreferences("TimerPrefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    private fun updateNotification(text: String) {
        val notification = buildNotification(text)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun showFinishedNotification() {
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Таймер завершён")
            .setContentText("Время вышло!")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setVibrate(longArrayOf(1000, 1000, 1000))
            .build()

        notificationManager.notify(NOTIFICATION_ID + 1, notification)
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
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Уведомления таймера",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Канал для уведомлений таймера"
            enableVibration(true)
            vibrationPattern = longArrayOf(1000, 1000, 1000)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun formatTime(millis: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
    }
}