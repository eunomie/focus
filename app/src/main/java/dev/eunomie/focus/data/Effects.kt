package dev.eunomie.focus.data

/**
 * Which device-wide effects focus mode applies. Configured once, then focus mode just
 * does what it was told.
 *
 * An effect that is switched off is never touched at all — not applied on entry, not
 * reset on exit — so Focus leaves settings it does not own alone.
 */
data class Effects(
    val zen: Boolean = true,
    val greyscale: Boolean = true,
    val alwaysOn: Boolean = true,
    val hideLockNotifications: Boolean = true,
    val wallpaper: Boolean = true,
)

enum class Effect(val label: String, val detail: String) {
    ZEN("Silence notifications", "Starred contacts and repeat callers still ring"),
    GREYSCALE("Greyscale", "Needs the one-time adb grant"),
    ALWAYS_ON("Always-on display", "Needs the one-time adb grant"),
    HIDE_LOCK_NOTIFICATIONS("Hide lock-screen notifications", "Needs the one-time adb grant"),
    WALLPAPER("Focus wallpaper", "Also removes the swipe-up flicker"),
}
