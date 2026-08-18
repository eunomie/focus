package dev.eunomie.focus.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import dev.eunomie.focus.data.FocusSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Feeds the "N waiting" line, and nothing else.
 *
 * Counts only allowed apps: a count that includes everything is noise, and by the
 * definition of the allowlist the rest does not matter. No senders, no previews, nothing
 * tappable — the moment it expands it is the notification shade again.
 *
 * DND suppresses *alerting*, not *posting*, so this still sees what arrives during focus
 * mode. Verified on device.
 */
class FocusNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onListenerConnected() = refresh()

    override fun onListenerDisconnected() {
        // Otherwise the last count sticks on screen after the listener drops.
        waitingCount = 0
    }
    override fun onNotificationPosted(sbn: StatusBarNotification?) = refresh()
    override fun onNotificationRemoved(sbn: StatusBarNotification?) = refresh()

    private fun refresh() {
        scope.launch {
            val allowed = FocusSettings(applicationContext).allowedAppsNow().toSet()
            waitingCount = runCatching {
                activeNotifications.orEmpty()
                    .count { it.packageName in allowed && !it.isOngoing }
            }.getOrDefault(0)
        }
    }

    companion object {
        @Volatile
        var waitingCount: Int = 0
            private set
    }
}
