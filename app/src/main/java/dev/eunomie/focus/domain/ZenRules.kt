package dev.eunomie.focus.domain

import android.app.AutomaticZenRule
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.service.notification.Condition
import android.service.notification.ZenPolicy
import android.util.Log
import androidx.core.net.toUri
import dev.eunomie.focus.ui.SettingsActivity

private val RULE_ID: Uri = "dev.eunomie.focus:zen".toUri()

/**
 * Silence, except for the people worth being interrupted by.
 *
 * Starred contacts come from the favourites already curated in Contacts, so there is no
 * second list to maintain and no `READ_CONTACTS` — the platform evaluates who is starred.
 * Repeat callers cover the emergency from someone who was never starred.
 */
class ZenRules(private val context: Context) {

    private val notifications get() = context.getSystemService(NotificationManager::class.java)

    val accessGranted: Boolean get() = notifications.isNotificationPolicyAccessGranted

    fun setActive(active: Boolean) {
        if (!accessGranted) {
            Log.w(TAG, "no DND policy access, focus mode will not silence anything")
            return
        }
        // Only create on the way in: reconciliation runs on every cold start, and creating
        // a rule purely to set it false added a stray "Focus" entry to the user's DND
        // settings on devices that had never entered focus mode.
        val id = existingRuleId() ?: (if (active) createRule() else null) ?: return
        notifications.setAutomaticZenRuleState(
            id,
            Condition(
                RULE_ID,
                if (active) "focus on" else "focus off",
                if (active) Condition.STATE_TRUE else Condition.STATE_FALSE,
            ),
        )
    }

    private fun existingRuleId(): String? =
        notifications.automaticZenRules.entries.firstOrNull { it.value.conditionId == RULE_ID }?.key

    // A rule is rejected unless it points at a real configuration activity, which is why
    // SettingsActivity carries an AUTOMATIC_ZEN_RULE intent-filter.
    private fun createRule(): String? = runCatching {
        notifications.addAutomaticZenRule(
            AutomaticZenRule.Builder("Focus", RULE_ID)
                .setZenPolicy(
                    ZenPolicy.Builder()
                        .disallowAllSounds()
                        .allowCalls(ZenPolicy.PEOPLE_TYPE_STARRED)
                        .allowMessages(ZenPolicy.PEOPLE_TYPE_STARRED)
                        .allowRepeatCallers(true)
                        .build(),
                )
                .setConfigurationActivity(ComponentName(context, SettingsActivity::class.java))
                .build(),
        )
    }.onFailure { Log.w(TAG, "could not create the zen rule", it) }.getOrNull()

    private companion object {
        const val TAG = "FocusZenRules"
    }
}
