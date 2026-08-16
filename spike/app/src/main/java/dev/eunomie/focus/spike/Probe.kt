package dev.eunomie.focus.spike

import android.app.AutomaticZenRule
import android.app.NotificationManager
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.service.notification.Condition
import android.service.notification.ZenPolicy

private const val DALTONIZER_ENABLED = "accessibility_display_daltonizer_enabled"
private const val DALTONIZER = "accessibility_display_daltonizer"
private const val MONOCHROMACY = 0
private const val DALTONIZER_OFF = -1

private val ZEN_RULE_ID: Uri = Uri.parse("dev.eunomie.focus.spike:zen")

data class ProbeState(
    val currentHomePackage: String,
    val roleHeld: Boolean,
    val roleAvailable: Boolean,
    val homeComponentEnabled: Boolean,
    val canWriteSecureSettings: Boolean,
    val greyscaleOn: Boolean,
    val notificationAccessGranted: Boolean,
    val notificationCount: Int,
    val dndAccessGranted: Boolean,
    val zenRuleActive: Boolean,
)

class Probe(private val context: Context) {

    private val roleManager get() = context.getSystemService(RoleManager::class.java)
    private val notificationManager get() = context.getSystemService(NotificationManager::class.java)
    private val homeComponent = ComponentName(context, HomeActivity::class.java)

    fun read() = ProbeState(
        currentHomePackage = currentHomePackage(),
        roleHeld = roleManager.isRoleHeld(RoleManager.ROLE_HOME),
        roleAvailable = roleManager.isRoleAvailable(RoleManager.ROLE_HOME),
        homeComponentEnabled = homeComponentEnabled(),
        canWriteSecureSettings = canWriteSecureSettings(),
        greyscaleOn = greyscaleOn(),
        notificationAccessGranted = notificationAccessGranted(),
        notificationCount = SpikeNotificationListener.activeCount,
        dndAccessGranted = notificationManager.isNotificationPolicyAccessGranted,
        zenRuleActive = zenRuleActive(),
    )

    /** Experiment 1: does requesting ROLE_HOME actually show a one-tap dialog? */
    fun requestHomeRoleIntent(): Intent =
        roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)

    /** The documented way back: the system Home-app picker. */
    fun homeSettingsIntent(): Intent = Intent(Settings.ACTION_HOME_SETTINGS)

    /**
     * Experiment 2, the one that could make exit zero-tap: disabling our own HOME
     * activity should stop us satisfying the role's <required-components>. If the
     * platform then falls back to Pixel Launcher on its own, exiting focus mode
     * needs no user interaction at all.
     */
    fun setHomeComponentEnabled(enabled: Boolean) {
        val state = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        context.packageManager.setComponentEnabledSetting(
            homeComponent, state, PackageManager.DONT_KILL_APP,
        )
    }

    private fun homeComponentEnabled(): Boolean =
        when (context.packageManager.getComponentEnabledSetting(homeComponent)) {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED -> false
            else -> true
        }

    private fun currentHomePackage(): String {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved = context.packageManager
            .resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolved?.activityInfo?.packageName ?: "(none)"
    }

    /** Experiment 3: greyscale via the accessibility daltonizer. */
    fun setGreyscale(on: Boolean) {
        val resolver = context.contentResolver
        Settings.Secure.putInt(resolver, DALTONIZER_ENABLED, if (on) 1 else 0)
        Settings.Secure.putInt(resolver, DALTONIZER, if (on) MONOCHROMACY else DALTONIZER_OFF)
    }

    private fun greyscaleOn(): Boolean =
        Settings.Secure.getInt(context.contentResolver, DALTONIZER_ENABLED, 0) == 1

    private fun canWriteSecureSettings(): Boolean =
        context.checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Greyscale is global device state, not app state: if this process dies mid-session
     * the phone stays grey. Every entry point calls this so there is no path that leaves
     * it stuck.
     */
    fun restoreGreyscaleIfStranded(focusActive: Boolean) {
        if (!focusActive && greyscaleOn() && canWriteSecureSettings()) setGreyscale(false)
    }

    private fun notificationAccessGranted(): Boolean =
        NotificationManagerCompatShim.enabledListenerPackages(context)
            .contains(context.packageName)

    fun notificationAccessSettingsIntent(): Intent =
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)

    fun dndAccessSettingsIntent(): Intent =
        Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)

    /**
     * Experiment 4: with a zen rule active, does the listener still see notifications?
     * DND should suppress alerting, not posting — the count line in v1 depends on that
     * being true.
     */
    fun setZenRule(active: Boolean): String = runCatching {
        val nm = notificationManager
        if (!nm.isNotificationPolicyAccessGranted) return "DND access not granted"

        val existing = nm.automaticZenRules.entries.firstOrNull { it.value.conditionId == ZEN_RULE_ID }
        val id = existing?.key ?: nm.addAutomaticZenRule(
            AutomaticZenRule.Builder("Focus Spike", ZEN_RULE_ID)
                .setZenPolicy(ZenPolicy.Builder().disallowAllSounds().build())
                .setConfigurationActivity(ComponentName(context, MainActivity::class.java))
                .build(),
        )
        nm.setAutomaticZenRuleState(
            id,
            Condition(
                ZEN_RULE_ID,
                if (active) "focus on" else "focus off",
                if (active) Condition.STATE_TRUE else Condition.STATE_FALSE,
            ),
        )
        if (active) "zen rule on" else "zen rule off"
    }.getOrElse { "FAILED: ${it.javaClass.simpleName}: ${it.message}" }

    private fun zenRuleActive(): Boolean =
        notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
}

private object NotificationManagerCompatShim {
    fun enabledListenerPackages(context: Context): Set<String> =
        Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            ?.split(":")
            ?.mapNotNull { ComponentName.unflattenFromString(it)?.packageName }
            ?.toSet()
            ?: emptySet()
}
