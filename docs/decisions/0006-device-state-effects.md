# 6. Focus mode changes device state, not just the home screen

Date: 2026-08-16
Status: Accepted

Extends [ADR 5](0005-v1-scope.md), which scoped v1 before these two ideas
existed.

## Context

Two additions surfaced after the spike, both from the same instinct: focus mode
should change what the *phone* is, not just what the home screen looks like.

1. **Starred contacts should ring through.** A focus mode you cannot be reached
   in is one you will not leave switched on. If a partner calls during a work
   session it is probably worth interrupting for.
2. **The lock screen should show it too.** Glancing at the phone should say
   "focus mode" without unlocking it — closer to a device that always behaves
   the same way, where the lock is a commodity rather than a gate.

## Decision

### Starred contacts break through

`ZenPolicy` already does exactly this, and it is how Android's own Bedtime mode
behaves:

```kotlin
ZenPolicy.Builder()
    .allowCalls(ZenPolicy.PEOPLE_TYPE_STARRED)
    .allowMessages(ZenPolicy.PEOPLE_TYPE_STARRED)
    .allowRepeatCallers(true)
    .build()
```

Moved from the v2 "allowed contacts" nice-to-have into **v1 core**.

### Device-state effects, all reverted on exit

| Effect | Mechanism | Permission |
|---|---|---|
| Greyscale | `Settings.Secure.accessibility_display_daltonizer{,_enabled}` | `WRITE_SECURE_SETTINGS` (adb) |
| Always-on display | `Settings.Secure.doze_always_on` | `WRITE_SECURE_SETTINGS` (adb) |
| No lock-screen notifications | `Settings.Secure.lock_screen_show_notifications` | `WRITE_SECURE_SETTINGS` (adb) |
| Lock + home wallpaper | `WallpaperManager.setBitmap(…, FLAG_LOCK / FLAG_SYSTEM)` | `SET_WALLPAPER` (normal) |

All four verified writable on the target device except the wallpaper, which uses
a documented, permissionless-in-practice API.

**Not attempted: replacing the lock screen with our own UI.** Android has no API
for it. Apps that appear to do this draw an activity over the keyguard with
`showWhenLocked` — fragile, security-degrading, and broken by most releases.
This is the same wall that made Fairphone ship Moments as a privileged system
app, and it is not worth climbing on a daily driver.

What replaces it: the lock wallpaper is rendered from the same Compose code as
the focus home screen, laid out so the *system* clock occupies the top band and
the app list sits in the dead space below. The result reads as the focus screen
with a real clock rather than one frozen into a bitmap. No fake controls —
anything button-shaped invites taps that cannot land.

### The wallpaper also fixes the transition flicker

Swiping up to home briefly reveals the previous launcher's wallpaper before the
focus screen draws (see [ADR 4](0004-focus-holds-the-home-role-while-active.md)).
Setting the **home** wallpaper as well as the lock one removes it: what the
transition reveals already looks like the focus screen.

Taken to its conclusion, the home activity's window draws over the wallpaper and
renders only the live parts — clock and exit control — while the static app list
*is* the wallpaper. The launcher and the wallpaper become the same pixels, so
the transition cannot show a seam.

## Consequences

**The restore safety net stops being a one-liner.** ADR 5 called for restoring
greyscale on every path out. There are now four pieces of global device state,
and a crash could strand all of them at once — grey screen, always-on display
burning battery, notifications hidden from the lock screen, and the wrong
wallpaper. Restoration becomes a real component: persisted intended state,
reconciled on app start and on `BOOT_COMPLETED`, not a line in an `onDestroy`.

**Never read the system wallpaper.** Restoring the user's original would mean
reading it, and `getWallpaperFile(FLAG_LOCK)` needs `READ_WALLPAPER_INTERNAL` —
signature-level, so unlike `WRITE_SECURE_SETTINGS` it cannot be granted over
`adb`. Fairphone hit this exact wall; their manifest carries the permission as
protected plus a note that Google rejected a Play update over
`MANAGE_EXTERNAL_STORAGE`. Instead the user picks their normal wallpapers once
during setup, the app keeps its own copies, and restores those. No extra
permission at all.

**Everything here degrades cleanly.** Without the `adb` grant, greyscale,
always-on and lock-screen notifications simply do nothing; the wallpaper and the
contact breakthrough still work. Nothing core depends on a privileged grant.

**The always-on display is drawn by SystemUI**, so it shows the system clock and
notification icons on black — never the wallpaper. Focus mode gets a clock on
black when asleep and the mimicked layout when woken to the lock screen.
