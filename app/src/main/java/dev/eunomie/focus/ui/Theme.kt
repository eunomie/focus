package dev.eunomie.focus.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

val FocusBackground = Color(0xFF08090B)
val FocusInk = Color(0xFFF2F4F7)
val FocusApp = Color(0xFFD8DCE3)
val FocusDim = Color(0xFF6B7382)
val FocusEdge = Color(0xFF23272E)
val FocusFill = Color(0xFF2C4B3B)
val FocusCheck = Color(0xFF7DD3A0)

@Composable
fun FocusTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = FocusBackground,
            surface = FocusBackground,
            onBackground = FocusInk,
            onSurface = FocusInk,
        ),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = FocusBackground) { content() }
    }
}
