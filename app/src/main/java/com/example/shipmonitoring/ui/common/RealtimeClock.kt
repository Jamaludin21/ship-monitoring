package com.example.shipmonitoring.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

@Composable
fun rememberCurrentTimeMillis(tickMs: Long = 1_000L): Long {
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(tickMs) {
        while (true) {
            delay(tickMs)
            nowMillis = System.currentTimeMillis()
        }
    }

    return nowMillis
}
