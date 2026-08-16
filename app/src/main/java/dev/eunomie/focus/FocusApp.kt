package dev.eunomie.focus

import android.app.Application
import dev.eunomie.focus.domain.FocusController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FocusApp : Application() {

    val controller: FocusController by lazy { FocusController(this) }

    override fun onCreate() {
        super.onCreate()
        // The other half of the safety net, alongside BootReceiver: if the process died
        // mid-session the device is still wearing focus mode's effects.
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            controller.reconcile()
        }
    }
}
