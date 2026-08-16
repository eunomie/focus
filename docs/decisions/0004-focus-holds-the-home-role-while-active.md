# 4. Focus holds the HOME role only while focus mode is active

Date: 2026-08-16
Status: Accepted — verified on device 2026-08-16

## Context

The locked-in requirement is a two-way swap: normal launcher → focus launcher →
normal launcher.

Fairphone Moments achieves this seamlessly, but reading its
[source](https://github.com/fairphone/fairphone-moments) shows why it can:
`Android.bp` installs it as a **presigned, privileged system app**
(`privileged: true`), holding protected permissions such as
`WRITE_SECURE_SETTINGS` and `INTERACT_ACROSS_USERS_FULL`, on an OS Fairphone
controls. Its switch is a plain activity lifecycle toggle —
`startActivity(NEW_TASK | CLEAR_TASK)` on, `finish()` off — which only produces
a real launcher swap because the surrounding framework is theirs.

None of that is available to a third-party app on a stock Pixel 6. On stock
Android, `android.app.role.HOME` is `exclusive`, and there is **no public API
for an app to release a role it holds**. Taking the home screen is easy; handing
it back is not symmetric.

## Decision

Focus takes the HOME role when focus mode starts and gives it up when focus mode
ends, rather than holding it permanently.

- **In:** `RoleManager.createRequestRoleIntent(ROLE_HOME)` — one system dialog,
  one tap.
- **Out:** disable the app's own HOME activity with
  `setComponentEnabledSetting`. The role's `<required-components>` is "an
  activity with MAIN + HOME"; a disabled one no longer qualifies, so the
  platform hands the role back on its own. **Zero taps.**
- **Fallback:** `Settings.ACTION_HOME_SETTINGS` if the above ever stops working.

## Verification

Measured on the actual target — Pixel 6, **Android 17 (SDK 37)** — with a
throwaway spike, since deleted. The working probe code is worth cribbing from
when the real app implements these mechanisms:

```sh
git show ae87bcf271eb:spike/app/src/main/java/dev/eunomie/focus/spike/Probe.kt
git show ae87bcf271eb:spike/app/src/main/java/dev/eunomie/focus/spike/Wallpaper.kt
```

| Claim | Result |
|---|---|
| `ROLE_HOME` is requestable in one tap | **True.** System dialog, one tap, role granted. |
| Disabling the HOME component returns the role automatically | **True.** Holder went back to `com.google.android.apps.nexuslauncher` with no user interaction. |
| `ACTION_HOME_SETTINGS` works as a manual fallback | **True.** |
| Greyscale via the accessibility daltonizer | **True**, after a one-time `adb` grant. |
| A notification listener still sees notifications under DND | **True.** DND suppresses alerting, not posting. |
| Quick Tap can launch the app | **True.** Visible in logcat as `Columbus/Service`. |
| The gesture-navigation tax is tolerable | **True.** See below. |

The automatic return was the open question and it came out better than assumed,
so exit costs nothing rather than a trip through Settings.

Two behaviours that look like faults and are not, both worth remembering:

- Disabling the HOME component while the focus home screen is on screen makes
  that screen vanish. That is the component switching off, not a crash.
- While the component is disabled, requesting `ROLE_HOME` is refused — the app
  no longer satisfies `<required-components>`. **Re-enable, then request.**
  Ordering matters.

## Consequences

Rejected: **Focus holds HOME permanently and forwards to Pixel Launcher when
inactive.** Pixel Launcher is bound to Quickstep as the role holder, so driving
it while it does not hold the role risks flicker, broken animations and a
configuration Google never tests.

Accepted costs, now measured rather than guessed:

- **One tap in, zero out.**
- **The gesture-navigation tax is real but minor.** Living with it produced one
  visible artefact: swiping up to go home briefly reveals the previous
  launcher's wallpaper before the focus screen draws. Quickstep coordinates that
  animation with Pixel Launcher and will not do so for a third-party home app.
  Judged "not a big deal" in practice.
- That artefact is **fixed, and verified on device**: focus mode sets the
  **home** wallpaper as well as the lock wallpaper, so what the transition
  reveals already looks like the focus screen. See
  [ADR 6](0006-device-state-effects.md).
- No privileged permissions, no root, no custom ROM.

The target device runs Android 17, so these results describe the current
platform rather than one about to be superseded.
