package dev.eunomie.focus.domain

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import dev.eunomie.focus.data.Effects

private const val DALTONIZER_ENABLED = "accessibility_display_daltonizer_enabled"
private const val DALTONIZER = "accessibility_display_daltonizer"
private const val MONOCHROMACY = 0
private const val DALTONIZER_OFF = -1

private const val DOZE_ALWAYS_ON = "doze_always_on"
private const val LOCK_SCREEN_NOTIFICATIONS = "lock_screen_show_notifications"

/**
 * The device-wide effects: greyscale, always-on display, and hiding lock-screen
 * notifications.
 *
 * All three are **global device state, not app state** — if this process dies mid-session
 * they stay applied, which is why [FocusController] reconciles them on every start rather
 * than only undoing them on a clean exit.
 *
 * All three need `WRITE_SECURE_SETTINGS`, granted once over adb. Without it every method
 * here is a no-op and focus mode simply has no device-wide effects.
 */
class DeviceState(private val context: Context) {

    val granted: Boolean
        get() = context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) ==
            PackageManager.PERMISSION_GRANTED

    fun apply(focusActive: Boolean, effects: Effects) {
        if (!granted) return
        val resolver = context.contentResolver
        if (effects.greyscale) {
            Settings.Secure.putInt(resolver, DALTONIZER_ENABLED, if (focusActive) 1 else 0)
            Settings.Secure.putInt(
                resolver,
                DALTONIZER,
                if (focusActive) MONOCHROMACY else DALTONIZER_OFF,
            )
        }
        if (effects.alwaysOn) {
            Settings.Secure.putInt(resolver, DOZE_ALWAYS_ON, if (focusActive) 1 else 0)
        }
        if (effects.hideLockNotifications) {
            Settings.Secure.putInt(resolver, LOCK_SCREEN_NOTIFICATIONS, if (focusActive) 0 else 1)
        }
    }

    /** Unconditional: reconciliation must clean up regardless of current preferences. */
    fun clearAll() {
        if (!granted) return
        val resolver = context.contentResolver
        Settings.Secure.putInt(resolver, DALTONIZER_ENABLED, 0)
        Settings.Secure.putInt(resolver, DALTONIZER, DALTONIZER_OFF)
        Settings.Secure.putInt(resolver, DOZE_ALWAYS_ON, 0)
        Settings.Secure.putInt(resolver, LOCK_SCREEN_NOTIFICATIONS, 1)
    }

    val greyscaleOn: Boolean
        get() = Settings.Secure.getInt(context.contentResolver, DALTONIZER_ENABLED, 0) == 1
}
