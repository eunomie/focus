package dev.eunomie.focus.spike

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Stand-in for the real focus home screen. Deliberately ugly — it exists to be the
 * thing the HOME intent resolves to while the gesture-navigation tax (Q8) is being
 * lived with, not to look like the mockups.
 */
class HomeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    HomeScreen { startActivity(Intent(this, MainActivity::class.java)) }
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(onOpenProbe: () -> Unit) {
    val clock by produceState(currentTime()) {
        while (true) {
            value = currentTime()
            delay(1000)
        }
    }
    val count by produceState(0) {
        while (true) {
            value = SpikeNotificationListener.activeCount
            delay(1000)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(clock, fontSize = 44.sp)
        Text("focus spike — this is the HOME activity", fontSize = 13.sp, fontFamily = FontFamily.Monospace)
        Text("$count waiting", fontSize = 13.sp, fontFamily = FontFamily.Monospace)
        TextButton(onClick = onOpenProbe) { Text("open probe") }
    }
}

private fun currentTime(): String =
    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
