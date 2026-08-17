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
| Reading it back to restore | `WallpaperManager.getWallpaperFile(FLAG_SYSTEM)` | `READ_MEDIA_IMAGES` **and** `READ_EXTERNAL_STORAGE` (adb) |

Every row verified on the target device, Pixel 6 / Android 17.

**Not attempted: replacing the lock screen with our own UI.** Android has no API
for it. Apps that appear to do this draw an activity over the keyguard with
`showWhenLocked` — fragile, security-degrading, and broken by most releases.
This is the same wall that made Fairphone ship Moments as a privileged system
app, and it is not worth climbing on a daily driver.

The wallpaper must be rendered at the **screen** size, not
`WallpaperManager.desiredMinimumWidth` — Android asks for a double-width canvas
(4800px on a Pixel 6) so the home screen can pan, and rendering into that puts
the content at the centre of a canvas the lock screen never shows.

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

**Verified on device.** With the focus wallpaper applied, the flicker is gone.
This was the last claim in the design resting on reasoning rather than
measurement.

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

**The wallpaper can be read back, so the app restores the real one.** An earlier
draft of this ADR claimed the blocker was `READ_WALLPAPER_INTERNAL` — signature
level, therefore impossible — and designed around it with a setup step where the
user nominates a "normal" wallpaper for the app to hoard a copy of. That was
wrong, and the spike found it.

The actual gate on Android 17 needs **both** `READ_MEDIA_IMAGES` and the legacy
`READ_EXTERNAL_STORAGE`, which `WallpaperManager` still checks on the read path.
Both are runtime permissions and both grant cleanly over `adb`, a channel the
app already uses. Declaring only `READ_MEDIA_IMAGES` — the one the spike's final
experiment identified — is not enough, and was how the real app shipped with a
silently failing backup:

```sh
adb shell pm grant <pkg> android.permission.READ_MEDIA_IMAGES
adb shell pm grant <pkg> android.permission.READ_EXTERNAL_STORAGE
```

Verified on device: `getWallpaperFile(FLAG_SYSTEM)` returns the live wallpaper
once granted.

So there is no setup step and no nominated wallpaper. Focus mode backs up
whatever is actually set, paints its own, and puts the original back — which
also means it cannot go stale when the wallpaper changes.

**The rule that falls out: never change what cannot be put back.** The wallpaper
effect is gated on a successful backup. If the grant is missing, focus mode
leaves the wallpaper alone entirely rather than replacing it with something it
cannot undo. The spike enforced exactly this and it is the right behaviour for
the real app — a destructive action is only offered once its inverse is proven
to work.

**Everything here degrades cleanly.** Without the `adb` grants, greyscale,
always-on, lock-screen notifications *and* the wallpaper all simply do nothing —
the wallpaper because of the rule above, not because it is impossible. The
starred-contact breakthrough and the focus launcher itself need no grant at all,
so the core of the app works on a plain install and the device-state effects are
a strict enhancement.

**The always-on display is drawn by SystemUI**, so it shows the system clock and
notification icons on black — never the wallpaper. Focus mode gets a clock on
black when asleep and the mimicked layout when woken to the lock screen.
