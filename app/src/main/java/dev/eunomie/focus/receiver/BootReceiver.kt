package dev.eunomie.focus.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.eunomie.focus.FocusApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Half of the safety net. Focus mode mutates global device state, so if the app died
 * while focus was on, the phone comes back grey with an always-on display and the wrong
 * wallpaper. Boot is one of only two moments the app reliably gets to notice; app start
 * is the other.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        // The shared controller, so this cannot race FocusApp.onCreate's own reconcile.
        val controller = (context.applicationContext as FocusApp).controller
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                controller.reconcile()
            } finally {
                pending.finish()
            }
        }
    }
}
