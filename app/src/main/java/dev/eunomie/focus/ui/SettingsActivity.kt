package dev.eunomie.focus.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dev.eunomie.focus.FocusApp
import dev.eunomie.focus.data.Effect
import dev.eunomie.focus.data.Effects
import dev.eunomie.focus.data.MAX_ALLOWED_APPS
import dev.eunomie.focus.domain.AppEntry
import dev.eunomie.focus.domain.FocusController
import kotlinx.coroutines.launch

/**
 * Everything configurable, in one scrolling list: what focus mode does to the device, and
 * which apps survive it.
 *
 * One `LazyColumn` for the whole screen rather than a header plus a nested list — a
 * scrollable inside a `Column` measures to its content height and stops scrolling, which
 * is the bug this screen shipped with twice.
 *
 * Also the zen rule's configuration activity, which the platform requires to exist before
 * it will accept the rule at all.
 */
class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val controller = (application as FocusApp).controller

        setContent {
            FocusTheme {
                SettingsScreen(
                    controller = controller,
                    onToggleApp = { pkg -> lifecycleScope.launch { controller.toggleApp(pkg) } },
                    onToggleEffect = { fx -> lifecycleScope.launch { controller.toggleEffect(fx) } },
                    onMoveApp = { pkg, d -> lifecycleScope.launch { controller.moveApp(pkg, d) } },
                )
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    controller: FocusController,
    onToggleApp: (String) -> Unit,
    onToggleEffect: (Effect) -> Unit,
    onMoveApp: (String, Int) -> Unit,
) {
    var available by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    val allowed by controller.allowedApps.collectAsStateWithLifecycle(emptyList())
    val effects by controller.effects.collectAsStateWithLifecycle(Effects())

    LaunchedEffect(Unit) { available = controller.availableApps() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        item {
            Text(
                "Focus settings",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = FocusInk,
                modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 28.dp, bottom = 18.dp),
            )
        }

        item { SectionHeader("What focus mode does") }
        items(Effect.entries.toList(), key = { it.name }) { effect ->
            EffectRow(effect, effects.isOn(effect)) { onToggleEffect(effect) }
        }

        item {
            HorizontalDivider(color = FocusEdge, modifier = Modifier.padding(vertical = 14.dp))
            SectionHeader(
                if (allowed.size >= MAX_ALLOWED_APPS) {
                    "Allowed apps — $MAX_ALLOWED_APPS of $MAX_ALLOWED_APPS, deselect one to swap"
                } else {
                    "Allowed apps — ${allowed.size} of $MAX_ALLOWED_APPS"
                },
            )
        }
        // Chosen apps first, in the order they appear on the focus screen.
        val byPackage = available.associateBy { it.packageName }
        val chosen = allowed.mapNotNull { byPackage[it] }
        itemsIndexed(chosen, key = { _, app -> "chosen-${app.packageName}" }) { index, app ->
            AppRow(
                app = app,
                checked = true,
                atCap = false,
                onToggle = { onToggleApp(app.packageName) },
                canMoveUp = index > 0,
                canMoveDown = index < chosen.size - 1,
                onMove = { delta -> onMoveApp(app.packageName, delta) },
            )
        }

        if (chosen.isNotEmpty()) {
            item { SectionHeader("Everything else") }
        }
        items(available.filterNot { it.packageName in allowed }, key = { it.packageName }) { app ->
            AppRow(
                app = app,
                checked = false,
                atCap = allowed.size >= MAX_ALLOWED_APPS,
                onToggle = { onToggleApp(app.packageName) },
            )
        }
    }
}

private fun Effects.isOn(effect: Effect): Boolean = when (effect) {
    Effect.ZEN -> zen
    Effect.GREYSCALE -> greyscale
    Effect.ALWAYS_ON -> alwaysOn
    Effect.HIDE_LOCK_NOTIFICATIONS -> hideLockNotifications
    Effect.WALLPAPER -> wallpaper
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        fontSize = 11.sp,
        color = FocusDim,
        modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 6.dp, bottom = 8.dp),
    )
}

@Composable
private fun EffectRow(effect: Effect, on: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 22.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(effect.label, fontSize = 15.sp, color = if (on) FocusInk else FocusApp)
            Text(effect.detail, fontSize = 11.sp, color = FocusDim)
        }
        Switch(
            checked = on,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = FocusBackground,
                checkedTrackColor = FocusCheck,
                uncheckedThumbColor = FocusDim,
                uncheckedTrackColor = FocusBackground,
                uncheckedBorderColor = FocusEdge,
            ),
        )
    }
}

@Composable
private fun AppRow(
    app: AppEntry,
    checked: Boolean,
    atCap: Boolean,
    onToggle: () -> Unit,
    canMoveUp: Boolean = false,
    canMoveDown: Boolean = false,
    onMove: (Int) -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !atCap, onClick = onToggle)
            .padding(horizontal = 22.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            app.label,
            fontSize = 15.sp,
            color = when {
                checked -> FocusInk
                atCap -> FocusDim
                else -> FocusApp
            },
            modifier = Modifier.weight(1f),
        )
        if (checked) {
            MoveButton("\u25B2", canMoveUp) { onMove(-1) }
            MoveButton("\u25BC", canMoveDown) { onMove(1) }
        }
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(if (checked) FocusCheck else Color.Transparent),
        )
    }
}

@Composable
private fun MoveButton(glyph: String, enabled: Boolean, onClick: () -> Unit) {
    Text(
        glyph,
        fontSize = 13.sp,
        color = if (enabled) FocusApp else FocusEdge,
        modifier = Modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}
