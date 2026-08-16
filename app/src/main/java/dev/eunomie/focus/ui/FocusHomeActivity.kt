package dev.eunomie.focus.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeGestures
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import dev.eunomie.focus.domain.AppEntry
import dev.eunomie.focus.FocusApp
import dev.eunomie.focus.domain.FocusController
import dev.eunomie.focus.service.FocusNotificationListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val HOLD_TO_EXIT_MILLIS = 1000L

/**
 * The focus home screen: clock, allowed apps, one count of what is waiting, one way out.
 *
 * No drawer, no search, no widgets, no swipe to anything. Text rather than icons —
 * nothing here is meant to catch the eye.
 */
class FocusHomeActivity : ComponentActivity() {

    private lateinit var controller: FocusController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        controller = (application as FocusApp).controller
        controller.ensureEnteredAsync()

        setContent {
            FocusTheme {
                FocusHomeScreen(
                    controller = controller,
                    onLaunch = { pkg -> controller.launchIntent(pkg)?.let(::startActivity) },
                    onExit = {
                        controller.exitAsync().invokeOnCompletion {
                            runOnUiThread {
                                startActivity(
                                    Intent(Intent.ACTION_MAIN)
                                        .addCategory(Intent.CATEGORY_HOME)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                )
                                finish()
                            }
                        }
                    },
                    onOpenSettings = {
                        startActivity(Intent(this, SettingsActivity::class.java))
                    },
                )
            }
        }
    }
}

@Composable
private fun FocusHomeScreen(
    controller: FocusController,
    onLaunch: (String) -> Unit,
    onExit: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var apps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    var clock by remember { mutableStateOf(now("HH:mm")) }
    var date by remember { mutableStateOf(now("EEEE, d MMMM")) }
    var waiting by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            apps = controller.allowedAppEntries()
            clock = now("HH:mm")
            date = now("EEEE, d MMMM")
            waiting = FocusNotificationListener.waitingCount
            delay(1000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeGestures)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .pointerInput(Unit) { detectTapGestures(onLongPress = { onOpenSettings() }) },
        ) {
            Text(
                clock,
                fontSize = 56.sp,
                fontWeight = FontWeight.Light,
                color = FocusInk,
                textAlign = TextAlign.Center,
            )
            Text(
                date,
                fontSize = 13.sp,
                color = FocusDim,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp),
            )
        }

        Column(
            modifier = Modifier.padding(top = 52.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            apps.forEach { app ->
                Text(
                    app.label,
                    fontSize = 21.sp,
                    color = FocusApp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.pointerInput(app.packageName) {
                        detectTapGestures { onLaunch(app.packageName) }
                    },
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        if (waiting > 0) {
            Text(
                "$waiting waiting",
                fontSize = 12.sp,
                color = FocusDim,
                textAlign = TextAlign.Center,
            )
        }

        HoldToExit(onExit = onExit, modifier = Modifier.padding(top = 14.dp, bottom = 26.dp))
    }
}

/**
 * Press-and-hold rather than tap: long enough that it never fires in a pocket, short
 * enough that it is never an obstacle. Explicitly not a lockout — being unable to leave
 * your own phone during a real interruption is a failure, not a feature.
 */
@Composable
private fun HoldToExit(onExit: () -> Unit, modifier: Modifier = Modifier) {
    var holding by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }

    LaunchedEffect(holding) {
        if (!holding) {
            progress = 0f
            return@LaunchedEffect
        }
        val step = 16L
        var elapsed = 0L
        while (elapsed < HOLD_TO_EXIT_MILLIS) {
            delay(step)
            elapsed += step
            progress = elapsed.toFloat() / HOLD_TO_EXIT_MILLIS
        }
        holding = false
        onExit()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            // Without this the press is stolen by the navigation handle's long-press
            // (Circle to Search / Gemini), which owns the bottom-centre of the screen.
            .systemGestureExclusion()
            .border(1.dp, if (holding) FocusFill else FocusEdge, RoundedCornerShape(20.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        holding = true
                        tryAwaitRelease()
                        holding = false
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .fillMaxSize()
                .background(FocusFill),
        )
        Text(
            if (holding) "KEEP HOLDING…" else "HOLD TO EXIT",
            fontSize = 11.sp,
            color = if (holding) FocusApp else FocusDim,
        )
    }
}

private fun now(pattern: String): String =
    SimpleDateFormat(pattern, Locale.getDefault()).format(Date())
