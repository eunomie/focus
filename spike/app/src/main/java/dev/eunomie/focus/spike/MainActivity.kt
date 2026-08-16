package dev.eunomie.focus.spike

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val GOOD = Color(0xFF7DD3A0)
private val WARN = Color(0xFFF0B429)
private val NEUTRAL = Color(0xFF98A0AD)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val probe = Probe(this)

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ProbeScreen(probe)
                }
            }
        }
    }
}

@Composable
private fun ProbeScreen(probe: Probe) {
    var state by remember { mutableStateOf(probe.read()) }
    var last by remember { mutableStateOf("—") }

    val roleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        last = when (result.resultCode) {
            Activity.RESULT_OK -> "role request: RESULT_OK — role granted"
            Activity.RESULT_CANCELED -> "role request: RESULT_CANCELED — refused or dismissed"
            else -> "role request: code ${result.resultCode}"
        }
    }
    val plainLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { }
    val launch: (Intent) -> Unit = { plainLauncher.launch(it) }

    LaunchedEffect(Unit) {
        while (true) {
            state = probe.read()
            delay(1000)
        }
    }

    val isPixelLauncher = state.currentHomePackage.contains("nexuslauncher")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("focus — platform spike", fontSize = 22.sp)
        Text(
            "Throwaway. Buttons grey out when they can't do anything useful yet — " +
                "the reason why is printed underneath.",
            fontSize = 13.sp,
            color = NEUTRAL,
        )

        Section("state")
        Row("home app", state.currentHomePackage, if (isPixelLauncher) null else GOOD)
        Row("ROLE_HOME held", state.roleHeld.toString(), if (state.roleHeld) GOOD else null)
        Row("ROLE_HOME available", state.roleAvailable.toString(), if (state.roleAvailable) GOOD else WARN)
        Row(
            "HOME component",
            if (state.homeComponentEnabled) "enabled" else "DISABLED",
            if (state.homeComponentEnabled) GOOD else WARN,
        )
        Row("WRITE_SECURE_SETTINGS", grant(state.canWriteSecureSettings), tint(state.canWriteSecureSettings))
        Row("greyscale", if (state.greyscaleOn) "ON" else "off", if (state.greyscaleOn) GOOD else null)
        Row("notification access", grant(state.notificationAccessGranted), tint(state.notificationAccessGranted))
        Row("notifications seen", state.notificationCount.toString())
        Row("DND access", grant(state.dndAccessGranted), tint(state.dndAccessGranted))
        Row("zen rule", if (state.zenRuleActive) "ACTIVE" else "off", if (state.zenRuleActive) GOOD else null)
        Row("last result", last)

        Section("1 · taking the home role")
        Act(
            "Request ROLE_HOME",
            enabled = state.roleAvailable && state.homeComponentEnabled && !state.roleHeld,
            note = when {
                state.roleHeld -> "Already the home app — nothing to request."
                !state.homeComponentEnabled ->
                    "HOME component is disabled, so the app no longer satisfies the role's " +
                        "required-components and the request will be refused. Re-enable it first (§2)."
                !state.roleAvailable -> "The platform reports ROLE_HOME as unavailable."
                else -> null
            },
        ) {
            last = runCatching {
                roleLauncher.launch(probe.requestHomeRoleIntent()); "role request: dialog launched"
            }.getOrElse { "role request threw: ${it.javaClass.simpleName}: ${it.message}" }
        }
        Act("Open Home settings (pick a launcher by hand)") { launch(probe.homeSettingsIntent()) }

        Section("2 · does the role fall back on its own?")
        Text(
            "With the role held, disable the HOME component and watch 'home app' above. " +
                "Nothing else to press — the verdict appears here.",
            fontSize = 12.sp,
            color = NEUTRAL,
        )
        if (!state.homeComponentEnabled) {
            Text(
                if (isPixelLauncher) {
                    "VERDICT: the role fell back to Pixel Launcher on its own. Exit can be zero-tap."
                } else {
                    "… still ${state.currentHomePackage} — no automatic fallback yet."
                },
                fontSize = 13.sp,
                color = if (isPixelLauncher) GOOD else WARN,
                fontFamily = FontFamily.Monospace,
            )
        }
        Act(
            "Disable HOME component",
            enabled = state.homeComponentEnabled,
            note = if (!state.homeComponentEnabled) "Already disabled." else null,
        ) {
            probe.setHomeComponentEnabled(false)
            last = "HOME component disabled — if the focus home screen vanished, that is expected, not a crash"
        }
        Text(
            "Heads up: if you are looking at the spike's home screen when you press this, " +
                "it disappears. That is the component being switched off, not a crash.",
            fontSize = 11.sp,
            color = WARN,
        )
        Act(
            "Re-enable HOME component",
            enabled = !state.homeComponentEnabled,
            note = if (state.homeComponentEnabled) "Already enabled." else null,
        ) {
            probe.setHomeComponentEnabled(true)
            last = "HOME component re-enabled — this restores candidacy only, it does not retake the role"
        }

        Section("3 · greyscale")
        Act(
            "Greyscale ON",
            enabled = state.canWriteSecureSettings && !state.greyscaleOn,
            note = greyscaleNote(state.canWriteSecureSettings, state.greyscaleOn, wantOn = true),
        ) { probe.setGreyscale(true); last = "greyscale on" }
        Act(
            "Greyscale OFF",
            enabled = state.canWriteSecureSettings && state.greyscaleOn,
            note = greyscaleNote(state.canWriteSecureSettings, state.greyscaleOn, wantOn = false),
        ) { probe.setGreyscale(false); last = "greyscale off" }
        Act(
            "Restore if stranded (the v1 safety net)",
            enabled = state.canWriteSecureSettings,
            note = if (!state.canWriteSecureSettings) "Needs the adb grant." else null,
        ) {
            probe.restoreGreyscaleIfStranded(focusActive = false)
            last = "ran the stranded-greyscale restore"
        }

        Section("4 · notifications under DND")
        Text(
            "Zen rule on, then send yourself a message. If 'notifications seen' still " +
                "increments, the v1 count line works.",
            fontSize = 12.sp,
            color = NEUTRAL,
        )
        Act(
            "Grant notification access",
            enabled = !state.notificationAccessGranted,
            note = if (state.notificationAccessGranted) "Already granted." else null,
        ) { launch(probe.notificationAccessSettingsIntent()) }
        Act(
            "Grant DND access",
            enabled = !state.dndAccessGranted,
            note = if (state.dndAccessGranted) "Already granted." else null,
        ) { launch(probe.dndAccessSettingsIntent()) }
        Act(
            "Zen rule ON",
            enabled = state.dndAccessGranted && !state.zenRuleActive,
            note = zenNote(state.dndAccessGranted, state.zenRuleActive, wantOn = true),
        ) { last = probe.setZenRule(true) }
        Act(
            "Zen rule OFF",
            enabled = state.dndAccessGranted && state.zenRuleActive,
            note = zenNote(state.dndAccessGranted, state.zenRuleActive, wantOn = false),
        ) { last = probe.setZenRule(false) }
    }
}

private fun grant(granted: Boolean) = if (granted) "granted" else "NOT granted"
private fun tint(granted: Boolean) = if (granted) GOOD else WARN

private fun greyscaleNote(granted: Boolean, on: Boolean, wantOn: Boolean): String? = when {
    !granted -> "Needs: adb shell pm grant dev.eunomie.focus.spike android.permission.WRITE_SECURE_SETTINGS"
    wantOn && on -> "Already on."
    !wantOn && !on -> "Already off."
    else -> null
}

private fun zenNote(granted: Boolean, active: Boolean, wantOn: Boolean): String? = when {
    !granted -> "Grant DND access first."
    wantOn && active -> "Already active."
    !wantOn && !active -> "Already off."
    else -> null
}

@Composable
private fun Section(title: String) {
    HorizontalDivider(modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))
    Text(title.uppercase(), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
}

@Composable
private fun Row(label: String, value: String, valueColor: Color? = null) {
    Text(
        "$label: $value",
        fontSize = 13.sp,
        fontFamily = FontFamily.Monospace,
        color = valueColor ?: MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun Act(
    label: String,
    enabled: Boolean = true,
    note: String? = null,
    onClick: () -> Unit,
) {
    Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) { Text(label) }
    if (note != null) {
        Text(note, fontSize = 11.sp, color = NEUTRAL, modifier = Modifier.padding(bottom = 4.dp))
    }
}
