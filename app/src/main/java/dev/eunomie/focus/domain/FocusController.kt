package dev.eunomie.focus.domain

import android.content.Context
import android.util.Log
import dev.eunomie.focus.data.Effect
import dev.eunomie.focus.data.FocusSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val TAG = "FocusController"

/**
 * The whole app is this state machine. Nothing else decides anything.
 *
 * Order matters in both directions, and both orders were learned the hard way:
 *
 * - Entering re-enables the HOME activity *before* the role is requested. While the
 *   component is disabled the app does not satisfy the role's `<required-components>`
 *   and the request is refused outright.
 * - Both directions persist the intended state *first*, so a process death mid-transition
 *   still leaves a record for [reconcile] to work from.
 */
class FocusController(context: Context) {

    private val app = context.applicationContext

    /**
     * Application-scoped on purpose. Applying focus mode outlives the activity that
     * triggered it — granting the HOME role tears down the task ToggleActivity was
     * launched into, so anything running in its lifecycleScope is cancelled mid-flight.
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Every transition runs under this. A boolean flag was not enough: it was read once at
     * the top of the entry path and the rest of the work then ran unguarded, so an exit
     * could complete while an in-flight entry was still writing, leaving the role released
     * but the device still grey and silenced.
     */
    private val transition = Mutex()
    private val settings = FocusSettings(app)
    private val installedApps = InstalledApps(app)

    val homeRole = HomeRole(app)
    val zen = ZenRules(app)
    val deviceState = DeviceState(app)
    val wallpapers = Wallpapers(app)

    val allowedApps = settings.allowedApps
    val effects = settings.effects

    /**
     * Holding the HOME role *is* focus mode — that is the one piece of state the platform
     * keeps for us, and it survives our process being killed. The persisted flag only
     * records whether the effects have been applied yet.
     */
    fun isActive(): Boolean = homeRole.held

    /** Call before requesting the role — see the class comment. */
    fun prepareForRoleRequest() = homeRole.enableHomeActivity()

    /**
     * Applying focus mode has to survive the transition that triggers it: granting the
     * HOME role tears down the calling task and can take the process with it, killing any
     * background work half-done. The focus home screen appearing is proof the role landed,
     * so it re-asserts the effects itself rather than trusting whatever started it.
     */
    fun ensureEnteredAsync(): Job = scope.launch { enter() }

    fun exitAsync(): Job = scope.launch { exit() }

    /**
     * Idempotent on purpose, and never gated on a "did we already do this?" flag — such a
     * flag can be stale-true after a transition dies half-done, which silently skips the
     * effects forever. Re-asserting is cheap; the wallpaper is the only expensive part and
     * it is skipped when a backup already exists, which means it is already ours.
     */
    suspend fun enter() = transition.withLock {
        // Re-checked inside the lock: the role can be released by an exit that was waiting
        // on it, and re-applying afterwards is exactly the strand this guards against.
        if (!homeRole.held) return@withLock

        val effects = settings.effectsNow()
        // Only snapshot when nothing is applied yet, or a re-assert would capture focus
        // mode's own values as the thing to restore.
        if (!settings.isApplied()) {
            settings.setDeviceSnapshot(deviceState.snapshot())
        }
        settings.setActive(true)
        if (effects.zen) zen.setActive(true)
        deviceState.apply(effects)
        val wallpaperApplied = effects.wallpaper &&
            !wallpapers.hasBackup &&
            wallpapers.apply(allowedAppLabels())
        settings.setApplied(true)
        Log.i(
            TAG,
            "enter: effects=$effects zenAccess=${zen.accessGranted} " +
                "secureSettings=${deviceState.granted} wallpaperApplied=$wallpaperApplied",
        )
    }

    /**
     * Releases the role *first*. While it is still held, anything that sends a HOME intent
     * lands back on our own focus screen, which re-asserts the effects being undone.
     */
    suspend fun exit() = transition.withLock {
        homeRole.releaseRole()
        revert()
        Log.i(TAG, "exit: held=${homeRole.held}")
    }

    /**
     * Undo whatever is currently applied. The persisted records are cleared last, so a
     * death part-way through leaves them set and the next reconcile finishes the job.
     */
    private suspend fun revert() {
        zen.setActive(false)
        deviceState.restore(settings.deviceSnapshot())
        val restored = wallpapers.restore()
        settings.setDeviceSnapshot(emptyMap())
        settings.setApplied(false)
        settings.setActive(false)
        Log.i(TAG, "revert: wallpaperRestored=$restored")
    }

    /**
     * Put the device back if the intended state and the actual state disagree.
     *
     * The role is the source of truth for whether focus mode is on; the persisted
     * applied-record is the source of truth for whether anything still needs undoing.
     * Probing one setting (greyscale) instead meant that switching the greyscale effect
     * off and crashing left the always-on display and hidden lock-screen notifications
     * stranded with nothing to detect them.
     */
    suspend fun reconcile() = transition.withLock {
        if (homeRole.held) return@withLock
        val stranded = settings.isApplied() ||
            settings.deviceSnapshot().isNotEmpty() ||
            wallpapers.hasBackup ||
            deviceState.greyscaleOn
        if (!stranded) return@withLock
        Log.i(TAG, "reconcile: focus is off but effects are still applied, reverting")
        revert()
    }

    suspend fun allowedAppLabels(): List<String> =
        installedApps.labelsFor(settings.allowedAppsNow()).map { it.label }

    suspend fun allowedAppEntries(): List<AppEntry> =
        installedApps.labelsFor(settings.allowedAppsNow())

    fun availableApps(): List<AppEntry> =
        installedApps.all().also { Log.i(TAG, "availableApps=${it.size}") }

    fun launchIntent(packageName: String) = installedApps.launchIntent(packageName)

    suspend fun toggleApp(packageName: String) = settings.toggleApp(packageName)

    suspend fun moveApp(packageName: String, delta: Int) = settings.moveApp(packageName, delta)

    suspend fun toggleEffect(effect: Effect) = settings.toggleEffect(effect)
}
