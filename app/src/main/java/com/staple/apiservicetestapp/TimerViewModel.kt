package com.staple.apiservicetestapp

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TimerViewModel : ViewModel() {
    private val _remainingTime = MutableStateFlow(0L)
    val remainingTime: StateFlow<Long> = _remainingTime

    fun startTimer(context: Context, minutes: Int, seconds: Int) {
        val intent = Intent(context, TimerService::class.java)
        val duration = (minutes * 60 + seconds) * 1000L
        intent.action = TimerService.ACTION_START
        intent.putExtra(TimerService.EXTRA_DURATION, duration)
        context.startForegroundService(intent)
    }

    fun stopTimer(context: Context) {
        val intent = Intent(context, TimerService::class.java)
        intent.action = TimerService.ACTION_STOP
        context.startService(intent)
    }

    fun resumeIfNeeded(context: Context) {
        val prefs = context.getSharedPreferences("TimerPrefs", Context.MODE_PRIVATE)
        val remainingTime = prefs.getLong("remainingTime", 0L)
        val startTime = prefs.getLong("startTime", 0L)

        if (remainingTime > 0 && startTime > 0) {
            val elapsed = System.currentTimeMillis() - startTime
            val adjusted = (remainingTime - elapsed).coerceAtLeast(0L)

            if (adjusted > 0) {
                val intent = Intent(context, TimerService::class.java).apply {
                    action = TimerService.ACTION_START
                    putExtra(TimerService.EXTRA_DURATION, adjusted)
                    putExtra(TimerService.EXTRA_RESUMED, true)
                }
                context.startForegroundService(intent)

                _remainingTime.value = adjusted
            } else {
                prefs.edit().clear().apply()
            }
        }
    }

    fun resetTimer(context: Context) {
        val intent = Intent(context, TimerService::class.java)
        intent.action = TimerService.ACTION_RESET
        context.startService(intent)
        _remainingTime.value = 0L
    }

    fun updateTime(millis: Long) {
        viewModelScope.launch {
            _remainingTime.emit(millis)
        }
    }
}