package com.staple.apiservicetestapp

import android.content.Intent
import android.widget.NumberPicker
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun TimerPickerScreen(viewModel: TimerViewModel) {
    val context = LocalContext.current
    var minutes by remember { mutableStateOf(1) }
    var seconds by remember { mutableStateOf(0) }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(32.dp))
        Text("Выберите время", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(16.dp))

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

            Spacer(modifier = Modifier.width(16.dp))

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
            Button(onClick = {
                viewModel.startTimer(context, minutes, seconds)
            }) {
                Text("Старт")
            }

            Spacer(modifier = Modifier.width(16.dp))

            Button(onClick = {
                viewModel.stopTimer(context)
            }) {
                Text("Стоп")
            }
        }
    }
}
