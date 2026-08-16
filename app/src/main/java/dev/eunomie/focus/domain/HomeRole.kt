package dev.eunomie.focus.domain

import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import dev.eunomie.focus.ui.FocusHomeActivity

/**
 * Taking the HOME role and giving it back.
 *
 * Giving it back is not symmetric with taking it — there is no API to release a role.
 * Instead the app's own HOME activity is disabled, which stops it satisfying the role's
 * `<required-components>` so the platform hands the role to the fallback holder. Verified
 * on Pixel 6 / Android 17; see ADR 4.
 */
class HomeRole(private val context: Context) {

    private val roleManager get() = context.getSystemService(RoleManager::class.java)
    private val homeActivity = ComponentName(context, FocusHomeActivity::class.java)

    val held: Boolean get() = roleManager.isRoleHeld(RoleManager.ROLE_HOME)

    val requestable: Boolean get() = roleManager.isRoleAvailable(RoleManager.ROLE_HOME)

    fun requestIntent(): Intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)

    fun homeSettingsIntent(): Intent = Intent(Settings.ACTION_HOME_SETTINGS)

    /**
     * Must run *before* requesting the role: while the component is disabled the app does
     * not qualify and the request is refused outright.
     */
    fun enableHomeActivity() = setHomeActivity(enabled = true)

    fun releaseRole() = setHomeActivity(enabled = false)

    private fun setHomeActivity(enabled: Boolean) {
        val state = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        context.packageManager
            .setComponentEnabledSetting(homeActivity, state, PackageManager.DONT_KILL_APP)
    }
}
