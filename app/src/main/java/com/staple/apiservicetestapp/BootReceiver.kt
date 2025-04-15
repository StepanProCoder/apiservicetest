package com.staple.apiservicetestapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == android.content.Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Устройство перезагружено")

            // Получаем данные о продолжении таймера, если они есть
            val sharedPreferences = context.getSharedPreferences("TimerPrefs", Context.MODE_PRIVATE)
            val remainingTime = sharedPreferences.getLong("remainingTime", 0L)

            if (remainingTime > 0) {
                val serviceIntent = Intent(context, TimerService::class.java).apply {
                    putExtra("duration", remainingTime)
                    action = "START_TIMER"
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
        }
    }
}
