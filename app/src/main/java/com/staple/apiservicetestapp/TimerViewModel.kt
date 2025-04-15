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
        intent.putExtra("duration", duration)
        context.startForegroundService(intent)
    }

    fun stopTimer(context: Context) {
        context.stopService(Intent(context, TimerService::class.java))
    }

    fun resetTimer(context: Context) {
        val intent = Intent(context, TimerService::class.java)
        intent.action = TimerService.ACTION_RESET
        context.startService(intent)
    }

    fun updateTime(millis: Long) {
        viewModelScope.launch {
            _remainingTime.emit(millis)
        }
    }
}