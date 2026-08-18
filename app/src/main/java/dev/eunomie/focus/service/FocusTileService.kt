package dev.eunomie.focus.service

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import dev.eunomie.focus.FocusApp
import dev.eunomie.focus.ui.ToggleActivity

/**
 * The primary trigger: reachable from inside any app, not just the home screen, and the
 * same control that turns focus back off.
 */
class FocusTileService : TileService() {

    override fun onStartListening() {
        val active = (application as FocusApp).controller.isActive()
        qsTile?.apply {
            state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            subtitle = if (active) "On" else "Off"
            updateTile()
        }
    }

    override fun onClick() {
        val intent = Intent(this, ToggleActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivityAndCollapse(
            android.app.PendingIntent.getActivity(
                this,
                0,
                intent,
                android.app.PendingIntent.FLAG_IMMUTABLE,
            ),
        )
    }
}
