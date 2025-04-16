package com.staple.apiservicetestapp

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Устройство перезагружено")

            val prefs = context.getSharedPreferences("TimerPrefs", Context.MODE_PRIVATE)
            val remainingTime = prefs.getLong("remainingTime", 0L)
            val startTime = prefs.getLong("startTime", 0L)

            if (remainingTime > 0 && startTime > 0) {
                val elapsed = System.currentTimeMillis() - startTime
                val adjustedTime = maxOf(0, remainingTime - elapsed)

                if (adjustedTime > 0) {
                    val activityIntent = Intent(context, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    }
                    context.startActivity(activityIntent)
                }
            }
        }
    }
}