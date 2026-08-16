# Platform spike — runbook

Throwaway code in `spike/`. It exists to answer the four questions that could
still change the architecture, **before** any real app is written. Everything it
proves or disproves feeds back into
[ADR 4](decisions/0004-focus-holds-the-home-role-while-active.md) and the design
doc.

Expect to throw the whole directory away afterwards.

## Build and install

```sh
dagger call focus spike-apk export --path=/tmp/focus-spike.apk
adb install -r /tmp/focus-spike.apk
adb shell pm grant dev.eunomie.focus.spike android.permission.WRITE_SECURE_SETTINGS
```

The `adb` grant is what makes greyscale testable; without it the app still runs
and the greyscale buttons simply do nothing (the probe screen shows
`WRITE_SECURE_SETTINGS: NOT granted`). Sideloading the APK from a file manager
works for installing, but not for the grant — that needs `adb` either way.

### No cable needed

Android 11+ supports wireless debugging, so none of the above needs USB — not
even the first pairing.

On the phone: Settings → System → Developer options → **Wireless debugging** →
on → **Pair device with pairing code**. Then, on the same network:

```sh
adb pair 192.168.1.42:41234      # pairing dialog's port, plus the 6-digit code
adb connect 192.168.1.42:37103   # port from the main Wireless debugging screen
```

The pairing port and the connect port are **different**. The pairing port is
only shown inside the pairing dialog and changes each time it is opened; the
connect port is on the main Wireless debugging screen.

Don't use `adb -d` — that flag means "the device on USB" and fails over
wireless. With one device connected, no flag is needed; otherwise
`adb -s 192.168.1.42:37103 …`.

### Rescue commands

Experiment 3 deliberately strands the device in greyscale, and experiment 2
deliberately messes with which app is home. Both are recoverable from `adb`
without touching the app:

```sh
# undo a stranded greyscale
adb shell settings put secure accessibility_display_daltonizer_enabled 0
adb shell settings put secure accessibility_display_daltonizer -1

# re-enable the spike's HOME activity if it was left disabled
adb shell pm enable dev.eunomie.focus.spike/.HomeActivity

# last resort: remove the app entirely, which drops the HOME role with it
adb uninstall dev.eunomie.focus.spike
```

Worth having wireless debugging connected *before* starting experiment 3, so the
rescue path is already open if the phone goes grey.

Then open **Focus Spike** and grant notification access and DND access from the
buttons in section 4.

## The four experiments

Each maps to a section on the probe screen. The `state` block at the top updates
once a second, so the answer to most of these is "watch that block".

### 1 — Can the HOME role be taken in one tap?

Tap **Request ROLE_HOME**. Expected: a system dialog, one tap, and then
`ROLE_HOME held: true` with `home app: dev.eunomie.focus.spike`.

Then tap **Open Home settings** and pick Pixel Launcher. That is the documented
way back, and the fallback if experiment 2 fails.

> **Record:** how many taps each direction actually took, and whether the dialog
> appeared at all.

### 2 — Does the role fall back on its own? *(the interesting one)*

With the role held, tap **Disable HOME component**, then watch the `home app`
line **without touching Settings**.

- If it flips to `com.google.android.apps.nexuslauncher` by itself, exiting focus
  mode can be **zero-tap** and the design gets noticeably better.
- If it stays put or goes to `(none)`, the Settings picker is the shipped path
  and nothing else changes.

Press the home gesture too — the question is not just what the API reports but
what actually appears.

Tap **Re-enable HOME component** afterwards to restore the app.

> **Record:** what `home app` became, and what pressing home actually showed.

### 3 — Greyscale

Tap **Greyscale ON**. The whole device should desaturate. Tap **Greyscale OFF**.

Then the failure mode that matters: turn greyscale on and **force-stop the app**
from Settings. The phone should stay grey — confirming greyscale is global device
state, not app state, and that the restore-on-start safety net in
[ADR 5](decisions/0005-v1-scope.md) is genuinely necessary rather than
theoretical. Re-open the app to check it recovers.

> **Record:** whether the daltonizer keys work on this Android version, and
> whether reopening the app cleared a stranded greyscale.

### 4 — Do notifications survive DND?

Grant notification access and DND access, then tap **Zen rule ON**. Confirm
`zen rule: ACTIVE`.

Now send yourself a message from another device. The phone should stay silent —
but `notifications seen` should still increment.

> **Record:** whether the count moved while the zen rule was active. The v1
> notification count line depends on this being true.

### 5 — Live with it (Q8)

Take the role and leave it for a few hours of normal use. The point is the
subjective one: how much the loss of Pixel Launcher's gesture-navigation
animations and At a Glance actually grates.

Also bind **Settings → System → Gestures → Quick tap → Open app → Focus Spike**
and confirm a double-tap on the back of the phone launches it. That is the
proposed stand-in for Fairphone's hardware switch.

> **Record:** whether Quick Tap works, and whether the gesture tax is a
> non-issue, an irritation, or a dealbreaker.

## After

Update ADR 4 with what was learned, mark it Accepted or supersede it, and delete
`spike/`.
