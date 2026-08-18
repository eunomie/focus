package dev.eunomie.focus.domain

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log
import dev.eunomie.focus.data.Effects

private const val DALTONIZER_ENABLED = "accessibility_display_daltonizer_enabled"
private const val DALTONIZER = "accessibility_display_daltonizer"
private const val MONOCHROMACY = 0
private const val DOZE_ALWAYS_ON = "doze_always_on"
private const val LOCK_SCREEN_NOTIFICATIONS = "lock_screen_show_notifications"

private const val TAG = "FocusDeviceState"

/**
 * The device-wide effects: greyscale, always-on display, and hiding lock-screen
 * notifications.
 *
 * All three are **global device state, not app state** — if this process dies mid-session
 * they stay applied, which is why [FocusController] reconciles them rather than only
 * undoing them on a clean exit.
 *
 * Leaving focus mode restores the values that were in place when it started, captured by
 * [snapshot]. It used to write fixed defaults instead, which quietly clobbered anyone who
 * ran with always-on display on: the first exit turned it off and nothing turned it back.
 *
 * All of it needs `WRITE_SECURE_SETTINGS`, granted once over adb. Without it every method
 * here is a no-op and focus mode simply has no device-wide effects.
 */
class DeviceState(private val context: Context) {

    val granted: Boolean
        get() = context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) ==
            PackageManager.PERMISSION_GRANTED

    /** Current values of everything focus mode touches, for restoring later. */
    fun snapshot(): Map<String, Int> {
        if (!granted) return emptyMap()
        return KEYS.associateWith { read(it) }
    }

    fun apply(effects: Effects) {
        if (!granted) return
        if (effects.greyscale) {
            write(DALTONIZER_ENABLED, 1)
            write(DALTONIZER, MONOCHROMACY)
        }
        if (effects.alwaysOn) write(DOZE_ALWAYS_ON, 1)
        if (effects.hideLockNotifications) write(LOCK_SCREEN_NOTIFICATIONS, 0)
    }

    /** Put back exactly what [snapshot] recorded. */
    fun restore(previous: Map<String, Int>) {
        if (!granted) return
        if (previous.isEmpty()) {
            Log.w(TAG, "no snapshot to restore from, falling back to platform defaults")
            clearAll()
            return
        }
        previous.forEach { (key, value) -> write(key, value) }
    }

    /**
     * Last resort when no snapshot survived — used only by reconciliation after a crash
     * that lost the record. Fixed defaults are wrong for anyone who deviates from them,
     * but leaving the device grey with the display always on is worse.
     */
    fun clearAll() {
        if (!granted) return
        write(DALTONIZER_ENABLED, 0)
        write(DALTONIZER, -1)
        write(DOZE_ALWAYS_ON, 0)
        write(LOCK_SCREEN_NOTIFICATIONS, 1)
    }

    val greyscaleOn: Boolean get() = read(DALTONIZER_ENABLED) == 1

    private fun read(key: String): Int =
        Settings.Secure.getInt(context.contentResolver, key, DEFAULTS.getValue(key))

    private fun write(key: String, value: Int) {
        runCatching { Settings.Secure.putInt(context.contentResolver, key, value) }
            .onFailure { Log.w(TAG, "could not write $key", it) }
    }

    private companion object {
        val DEFAULTS = mapOf(
            DALTONIZER_ENABLED to 0,
            DALTONIZER to -1,
            DOZE_ALWAYS_ON to 0,
            LOCK_SCREEN_NOTIFICATIONS to 1,
        )
        val KEYS = DEFAULTS.keys.toList()
    }
}
