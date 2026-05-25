package com.example.shipmonitoring.ui.screens

import android.view.animation.OvershootInterpolator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.shipmonitoring.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit
) {
    val logoSize = (LocalConfiguration.current.screenWidthDp * 0.45f).dp.coerceIn(180.dp, 260.dp)

    // State untuk animasi ukuran (scale) dari 0 (tidak terlihat) ke 1 (ukuran asli)
    val scale = remember { Animatable(0f) }

    // Efek yang berjalan sekali saat komponen dimuat
    LaunchedEffect(key1 = true) {
        // 1. Jalankan Animasi
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 1000, // Durasi animasi 1 detik
                easing = { OvershootInterpolator(2f).getInterpolation(it) } // Efek memantul
            )
        )

        // 2. Tahan Splash Screen selama 2 detik
        delay(2000L)

        // 3. Pindah ke halaman Login
        onNavigateToLogin()
    }

    // Antarmuka Splash Screen
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Gunakan asset logo custom dari resource lokal (bukan default Android Studio).
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                contentDescription = "Logo Splash Screen",
                modifier = Modifier
                    .size(logoSize)
                    .scale(scale.value) // Menerapkan animasi membesar
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Teks Judul
            Text(
                text = "SIMOKAL",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.scale(scale.value) // Menerapkan animasi yang sama ke teks
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Sistem Informasi Monitoring Kapal",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.scale(scale.value)
            )
        }
    }
}
