package dev.eunomie.focus.spike

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val probe = Probe(this)

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ProbeScreen(probe, ::startActivity)
                }
            }
        }
    }
}

@Composable
private fun ProbeScreen(probe: Probe, launch: (Intent) -> Unit) {
    var state by remember { mutableStateOf(probe.read()) }

    LaunchedEffect(Unit) {
        while (true) {
            state = probe.read()
            delay(1000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("focus — platform spike", fontSize = 22.sp)
        Text(
            "Throwaway. Answers the questions that could still change the design.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Section("state")
        Row("home app", state.currentHomePackage)
        Row("ROLE_HOME held", state.roleHeld.toString())
        Row("HOME component", if (state.homeComponentEnabled) "enabled" else "DISABLED")
        Row("WRITE_SECURE_SETTINGS", if (state.canWriteSecureSettings) "granted" else "NOT granted")
        Row("greyscale", if (state.greyscaleOn) "ON" else "off")
        Row("notification access", if (state.notificationAccessGranted) "granted" else "NOT granted")
        Row("notifications seen", state.notificationCount.toString())
        Row("DND access", if (state.dndAccessGranted) "granted" else "NOT granted")
        Row("zen rule", if (state.zenRuleActive) "ACTIVE" else "off")

        Section("1 · taking the home role")
        Act("Request ROLE_HOME") { launch(probe.requestHomeRoleIntent()) }
        Act("Open Home settings (the documented way back)") { launch(probe.homeSettingsIntent()) }

        Section("2 · does the role fall back on its own?")
        Text(
            "Take the role, then disable the HOME component and check the 'home app' line " +
                "above without touching Settings. If it flips to Pixel Launcher by itself, " +
                "exiting focus mode can be zero-tap.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Act("Disable HOME component") { probe.setHomeComponentEnabled(false) }
        Act("Re-enable HOME component") { probe.setHomeComponentEnabled(true) }

        Section("3 · greyscale")
        Act("Greyscale ON") { probe.setGreyscale(true) }
        Act("Greyscale OFF") { probe.setGreyscale(false) }

        Section("4 · notifications under DND")
        Text(
            "Turn the zen rule on, then send yourself a message. If 'notifications seen' " +
                "still increments, the v1 count line works.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Act("Grant notification access") { launch(probe.notificationAccessSettingsIntent()) }
        Act("Grant DND access") { launch(probe.dndAccessSettingsIntent()) }
        Act("Zen rule ON") { probe.setZenRule(true) }
        Act("Zen rule OFF") { probe.setZenRule(false) }
    }
}

@Composable
private fun Section(title: String) {
    HorizontalDivider(modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))
    Text(title.uppercase(), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
}

@Composable
private fun Row(label: String, value: String) {
    Text("$label: $value", fontSize = 13.sp, fontFamily = FontFamily.Monospace)
}

@Composable
private fun Act(label: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(label) }
}
