# 5. v1 scope

Date: 2026-08-16
Status: Accepted

## Context

The design doc closed with eight open questions that were product taste rather
than engineering, and so were Yves's to answer rather than mine. All eight were
answered on 2026-08-16. This records the answers, because the design doc will be
rewritten and these should not be re-litigated when it is.

## Decision

| # | Question | Answer |
|---|----------|--------|
| 1 | How many apps, and which? | Cap of **5**. Contents editable, the cap is not. |
| 2 | Escape hatch to all apps from inside focus mode? | **No drawer.** "If I need an app I'm not in focus mode anymore." |
| 3 | Schedule or automatic trigger? | **No.** Manual only. |
| 4 | Notification filtering depth? | **DND + a bare count line**, in v1. |
| 5 | Device-wide greyscale? | **Yes**, in v1. |
| 6 | Exit friction? | **Hold ~1s**, no lockout. |
| 7 | One focus mode or several? | **One.** |
| 8 | Accept losing Pixel gesture polish? | **Yes** — measure it in the spike. |

Six confirmed the recommendation. Q4 and Q5 moved the notification count line
and greyscale from v2 into v1.

## Consequences

Two details under Q4 that were mine to settle, both trivially reversible:

- The count includes **allowed apps only**. A count that includes everything is
  noise, and by the definition of the allowlist the rest doesn't matter.
- The line is **not tappable**. The moment it expands it is the notification
  shade again, which is the thing focus mode exists to get away from.

Q4 adds `BIND_NOTIFICATION_LISTENER_SERVICE` to v1. DND suppresses *alerting*,
not *posting*, so a listener still sees what arrives during focus mode — worth
confirming in the spike before it is depended on.

Q5 adds a one-time `adb -d shell pm grant … WRITE_SECURE_SETTINGS`, then
`Settings.Secure.accessibility_display_daltonizer_enabled` = 1/0 with
`accessibility_display_daltonizer` = 0 (monochromacy) / −1. The app degrades to
colour when the grant is absent, so this never becomes a hard dependency.

Q5 also introduces the one sharp edge in the design: greyscale is **global
device state, not app state**. If the app dies mid-session the phone stays grey.
Greyscale must therefore be restored on every path out — including app start and
`BOOT_COMPLETED` when persisted state says focus is off.

Q7 is a one-way door if ignored: storage keys settings under a profile id from
the start so a second mode stays an additive change, while nothing in the UI
suggests modes exist.
