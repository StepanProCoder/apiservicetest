package com.staple.apiservicetestapp

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.widget.NumberPicker
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.forEach { (perm, granted) ->
            if (!granted) {
                println("Permission NOT granted: $perm")
            }
        }
    }

    private val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_UPDATE_TIME -> {
                    val remainingMillis = msg.arg1.toLong()
                    viewModel.updateTime(remainingMillis)
                }
            }
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val binder = binder as TimerService.LocalBinder
            val timerService = binder.getService()
            timerService.setHandler(handler)  // Передаем Handler в сервис
        }

        override fun onServiceDisconnected(name: ComponentName?) {

        }
    }

    val viewModel = TimerViewModel()

    companion object {
        const val MSG_UPDATE_TIME = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestAllNecessaryPermissions()

        viewModel.resumeIfNeeded(this)

        setContent {
            TimerPickerScreen(viewModel)
        }
    }

    override fun onStart() {
        super.onStart()
        bindService(
            Intent(this, TimerService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onStop() {
        super.onStop()
        unbindService(serviceConnection)
    }

    private fun requestAllNecessaryPermissions() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        permissions.add(Manifest.permission.FOREGROUND_SERVICE)

        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }
}

@Composable
fun TimerPickerScreen(viewModel: TimerViewModel) {
    val context = LocalContext.current
    var minutes by remember { mutableStateOf(1) }
    var seconds by remember { mutableStateOf(0) }
    val remainingTime by viewModel.remainingTime.collectAsStateWithLifecycle()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Text("Таймер обратного отсчета", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(32.dp))

        // Display current remaining time
        TimeDisplay(remainingTime)

        Spacer(modifier = Modifier.height(32.dp))

        Row(horizontalArrangement = Arrangement.Center) {
            AndroidView(
                factory = { context ->
                    NumberPicker(context).apply {
                        minValue = 0
                        maxValue = 59
                        value = minutes
                        setOnValueChangedListener { _, _, newVal -> minutes = newVal }
                    }
                },
                modifier = Modifier.size(100.dp, 150.dp)
            )

            Text(":", style = MaterialTheme.typography.displayMedium)

            AndroidView(
                factory = { context ->
                    NumberPicker(context).apply {
                        minValue = 0
                        maxValue = 59
                        value = seconds
                        setOnValueChangedListener { _, _, newVal -> seconds = newVal }
                    }
                },
                modifier = Modifier.size(100.dp, 150.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(horizontalArrangement = Arrangement.Center) {
            Button(
                onClick = { viewModel.startTimer(context, minutes, seconds) },
                modifier = Modifier.padding(8.dp)
            ) {
                Text("Старт")
            }

            Button(
                onClick = { viewModel.stopTimer(context) },
                modifier = Modifier.padding(8.dp)
            ) {
                Text("Стоп")
            }

            Button(
                onClick = { viewModel.resetTimer(context) },
                modifier = Modifier.padding(8.dp)
            ) {
                Text("Сброс")
            }
        }
    }
}

@Composable
fun TimeDisplay(remainingMillis: Long) {
    val minutes = (remainingMillis / 1000) / 60
    val seconds = (remainingMillis / 1000) % 60
    Text(
        text = String.format("%02d:%02d", minutes, seconds),
        style = MaterialTheme.typography.displayLarge,
        modifier = Modifier.padding(16.dp)
    )
}