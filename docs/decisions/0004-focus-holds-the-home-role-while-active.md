# 4. Focus holds the HOME role only while focus mode is active

Date: 2026-08-16
Status: Proposed — awaiting review

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
Android:

- `android.app.role.HOME` is `exclusive`, and per AOSP `roles.xml` it carries
  `requestTitle`/`requestDescription`, so it **is** requestable —
  `RoleManager.createRequestRoleIntent(ROLE_HOME)` shows a one-tap system
  dialog.
- There is **no public API for an app to release a role it holds.** Handing
  control back is not symmetric with taking it.

## Decision

Focus takes the HOME role when focus mode starts and gives it up when focus
mode ends, rather than holding it permanently.

- **In:** `RoleManager.createRequestRoleIntent(ROLE_HOME)` — one system dialog,
  one tap.
- **Out:** `Settings.ACTION_HOME_SETTINGS` — the system Home-app picker.

Before this is built, a time-boxed on-device spike tests whether disabling the
app's own HOME activity via `setComponentEnabledSetting` invalidates the role's
`<required-components>` and makes it fall back to Pixel Launcher automatically.
AOSP `roles.xml` references `HomeRoleBehavior.getFallbackHolder()`, which
suggests it might. If it works, the exit becomes zero-tap; if not, the Settings
picker is the shipped path.

## Consequences

Rejected: **Focus holds HOME permanently and forwards to Pixel Launcher when
inactive.** Zero taps, but it fights the platform — Pixel Launcher is bound to
Quickstep as the role holder, so forwarding to it while it does *not* hold the
role risks visible flicker, broken home/recents animations and a launcher in a
state Google never tests. Too fragile to be the daily path on a daily-driver
phone.

Accepted costs:

- One extra tap entering, one or two leaving. Entering a focus mode
  deliberately is arguably the right place for a beat of friction.
- While Focus holds HOME, Pixel Launcher's gesture-navigation polish and At a
  Glance are gone. That is unavoidable for *any* third-party launcher and is
  part of what the spike should measure in practice.
- No privileged permissions, no root, no custom ROM. The app stays a normal
  sideloaded APK.

Because the app is sideloaded rather than shipped through Play, one-time
`adb shell pm grant` of `WRITE_SECURE_SETTINGS` remains available as an
opt-in unlock for device-wide effects like grayscale. That is a personal-device
affordance, not a dependency.
