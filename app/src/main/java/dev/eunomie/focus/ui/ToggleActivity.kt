package dev.eunomie.focus.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import dev.eunomie.focus.FocusApp
import dev.eunomie.focus.domain.FocusController
import kotlinx.coroutines.launch

private const val TAG = "FocusToggle"

/**
 * The single entry point for flipping focus mode, shared by the tile, the launcher icon
 * and Quick Tap. Transparent and short-lived — it decides, applies, and finishes.
 *
 * Deliberately *not* `singleInstance`: such an activity cannot receive an activity result,
 * so the role request would come back RESULT_CANCELED the instant it was launched.
 */
class ToggleActivity : ComponentActivity() {

    private lateinit var controller: FocusController

    private val roleRequest =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Log.i(TAG, "role request result=${result.resultCode} held=${controller.homeRole.held}")
            if (result.resultCode == Activity.RESULT_OK || controller.homeRole.held) {
                enterAndShow()
            } else {
                // Don't leave the app as a home candidate after a refused request.
                controller.homeRole.releaseRole()
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        controller = (application as FocusApp).controller

        lifecycleScope.launch {
            val active = controller.isActive()
            val held = controller.homeRole.held
            Log.i(
                TAG,
                "toggle: active=$active held=$held " +
                    "requestable=${controller.homeRole.requestable}",
            )

            if (active) {
                controller.exitAsync().invokeOnCompletion {
                    runOnUiThread {
                        goToSystemHome()
                        finish()
                    }
                }
                return@launch
            }
            if (held) {
                enterAndShow()
                return@launch
            }
            // Must precede the request: a disabled HOME activity means the app does not
            // satisfy the role's required-components and the request is refused.
            controller.prepareForRoleRequest()
            runCatching { roleRequest.launch(controller.homeRole.requestIntent()) }
                .onFailure {
                    Log.e(TAG, "role request failed to launch", it)
                    controller.homeRole.releaseRole()
                    finish()
                }
        }
    }

    /**
     * Entering focus mode should *show* focus mode, not just arm it silently.
     *
     * Applying is left to the focus screen's own self-healing path rather than started
     * here as well — doing both meant two concurrent enters on every single entry, which
     * could mint duplicate zen rules and race the wallpaper backup into capturing focus
     * mode's own wallpaper as the thing to restore.
     */
    private fun enterAndShow() {
        startActivity(
            Intent(this, FocusHomeActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
        )
        finish()
    }

    private fun goToSystemHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
