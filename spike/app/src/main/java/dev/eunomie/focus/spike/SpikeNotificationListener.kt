package dev.eunomie.focus.spike

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class SpikeNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() = refresh()
    override fun onNotificationPosted(sbn: StatusBarNotification?) = refresh()
    override fun onNotificationRemoved(sbn: StatusBarNotification?) = refresh()

    private fun refresh() {
        activeCount = runCatching { activeNotifications?.size ?: 0 }.getOrDefault(0)
    }

    companion object {
        @Volatile
        var activeCount: Int = 0
            private set
    }
}
