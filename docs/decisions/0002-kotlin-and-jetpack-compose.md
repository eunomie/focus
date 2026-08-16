# 2. Build the launcher in Kotlin with Jetpack Compose

Date: 2026-08-16
Status: Accepted

## Context

The target is a single device — a Pixel 6 running stock Android. The app is a
launcher: a handful of bespoke screens sitting directly on top of platform APIs
that have no cross-platform equivalent (`LauncherApps`, `RoleManager`,
`TileService`, `AutomaticZenRule`, optionally `NotificationListenerService`).

The language was not specified, so it is being chosen here rather than assumed
silently.

## Decision

**Kotlin, with Jetpack Compose for the UI.** A single Gradle module (`:app`)
until there is a reason for more.

## Consequences

Kotlin is the default for Android and the only one Google treats as
first-class; the platform APIs above are documented Kotlin-first. Fairphone's
own Moments launcher — the closest real precedent — is Kotlin and Compose
throughout, which is a useful sanity check that the stack handles this exact
job.

Compose specifically: the focus home screen is a short list of text and a clock,
not a virtualised icon grid, so Compose's weak spot does not apply. Its
`@Preview` system doubles as a mockup pipeline, so the design mockups and the
real screens converge in one place instead of drifting apart.

Rejected:

- **Java** — no Compose ergonomics, and much more ceremony for the same result.
  Kept only as the fallback language for *build tooling* modules, where the
  Dagger Java SDK is a genuine option (see ADR 3).
- **Flutter / React Native** — a cross-platform layer is pure overhead for a
  single-Android-device target, and every interesting API here would need a
  hand-written platform channel. Cost with no matching benefit.
- **Kotlin Multiplatform** — nothing to share with; there is no second target.
