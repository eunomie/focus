package dev.eunomie.focus.domain

import android.content.Context
import android.util.Log
import dev.eunomie.focus.data.Effect
import dev.eunomie.focus.data.Effects
import dev.eunomie.focus.data.FocusSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

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

    /** Stops the self-healing entry path from undoing an exit that is still in progress. */
    private val exiting = AtomicBoolean(false)
    private val settings = FocusSettings(app)
    private val installedApps = InstalledApps(app)

    val homeRole = HomeRole(app)
    val zen = ZenRules(app)
    val deviceState = DeviceState(app)
    val wallpapers = Wallpapers(app)

    val active = settings.active
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

    fun enterAsync(): Job = scope.launch { enter() }

    /**
     * Applying focus mode has to survive the transition that triggers it: granting the
     * HOME role tears down the calling task and can take the process with it, killing any
     * background work half-done. The focus home screen appearing is proof the role landed,
     * so it re-asserts the effects itself rather than trusting whatever started it.
     */
    fun ensureEnteredAsync(): Job = scope.launch {
        if (!exiting.get() && homeRole.held) enter()
    }

    fun exitAsync(): Job = scope.launch { exit() }

    /**
     * Idempotent on purpose, and never gated on a "did we already do this?" flag — such a
     * flag can be stale-true after a transition dies half-done, which silently skips the
     * effects forever. Re-asserting is cheap; the wallpaper is the only expensive part and
     * it is skipped when a backup already exists, which means it is already ours.
     */
    suspend fun enter() {
        val effects = settings.effectsNow()
        settings.setActive(true)
        if (effects.zen) zen.setActive(true)
        deviceState.apply(focusActive = true, effects = effects)
        val wallpaperApplied = effects.wallpaper &&
            !wallpapers.hasBackup &&
            wallpapers.apply(allowedAppLabels())
        Log.i(
            TAG,
            "enter: effects=$effects zenAccess=${zen.accessGranted} " +
                "secureSettings=${deviceState.granted} wallpaperApplied=$wallpaperApplied",
        )
    }

    /**
     * Releases the role *first*. While it is still held, anything that sends a HOME intent
     * lands back on our own focus screen, which re-asserts the effects that are being
     * undone — the exit and the self-healing entry path fight, and entry wins.
     */
    suspend fun exit() {
        exiting.set(true)
        try {
            val effects = settings.effectsNow()
            settings.setActive(false)
            homeRole.releaseRole()
            if (effects.zen) zen.setActive(false)
            deviceState.apply(focusActive = false, effects = effects)
            val restored = wallpapers.restore()
            Log.i(TAG, "exit: wallpaperRestored=$restored held=${homeRole.held}")
        } finally {
            exiting.set(false)
        }
    }

    suspend fun toggleEffect(effect: Effect) = settings.toggleEffect(effect)

    /**
     * Put the device back if the intended state and the actual state disagree.
     *
     * Focus mode mutates four pieces of global device state, so a crash can strand all of
     * them at once — a grey screen, an always-on display eating battery, hidden
     * lock-screen notifications and the wrong wallpaper. Run on every app start and on
     * boot, which are the two moments the app gets to notice.
     */
    suspend fun reconcile() {
        // The role is the source of truth. Checking the persisted flag instead meant that
        // a process restart between the role grant and the effects being applied looked
        // like "focus is off", so reconciliation helpfully undid focus mode.
        if (homeRole.held) return
        if (settings.isActive()) settings.setActive(false)
        // Unconditional, and it only touches our own rule: a stranded Do Not Disturb is
        // the worst of the four to leave behind, since it silently eats calls.
        zen.setActive(false)
        if (deviceState.greyscaleOn) deviceState.clearAll()
        if (wallpapers.hasBackup) wallpapers.restore()
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
}
