package dev.eunomie.focus.domain

import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.util.Log
import android.view.WindowManager
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import java.io.File
import java.io.FileOutputStream

/**
 * Painting the wallpaper to match the focus screen, and putting the original back.
 *
 * This is not decoration. Swiping up to home briefly reveals the wallpaper before the
 * focus screen draws — Quickstep will not coordinate that animation for a third-party
 * launcher — so making the wallpaper look like the focus screen removes the seam.
 *
 * The governing rule: **never change what cannot be put back.** Every entry point is
 * gated on a successful backup, so a missing `READ_MEDIA_IMAGES` grant means the
 * wallpaper is left alone rather than replaced with something unrecoverable.
 */
class Wallpapers(private val context: Context) {

    private companion object {
        const val TAG = "FocusWallpapers"
    }

    private val manager get() = WallpaperManager.getInstance(context)
    private val backupFile get() = File(context.filesDir, "wallpaper-backup.png")

    val hasBackup: Boolean get() = backupFile.exists()

    fun apply(appNames: List<String>): Boolean {
        if (!backupCurrent()) return false
        return runCatching {
            // Screen size, not desiredMinimumWidth: Android asks for a double-width
            // canvas (4800 on this device) so the home screen can pan, and rendering into
            // that put the text at the canvas centre — outside the window the lock screen
            // actually shows — at a text size scaled to 4800 rather than to the display.
            val bounds = context.getSystemService(WindowManager::class.java)
                .maximumWindowMetrics.bounds
            val bitmap = render(bounds.width(), bounds.height(), appNames)
            Log.i(
                TAG,
                "rendering wallpaper at ${bounds.width()}x${bounds.height()} " +
                    "for ${appNames.size} apps",
            )
            manager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
            manager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
            true
        }.onFailure { Log.w(TAG, "could not set the wallpaper", it) }.getOrDefault(false)
    }

    fun restore(): Boolean = runCatching {
        val bitmap = BitmapFactory.decodeFile(backupFile.path) ?: run {
            // Keeping an undecodable backup would wedge things permanently: apply() skips
            // while a backup exists, and restore() would retry the same bad file forever.
            Log.w(TAG, "backup could not be decoded, discarding it")
            backupFile.delete()
            return false
        }
        manager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
        manager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
        backupFile.delete()
        true
    }.onFailure { Log.w(TAG, "could not restore the wallpaper", it) }.getOrDefault(false)

    /**
     * Only backs up once per focus session — re-running mid-session would capture the
     * focus wallpaper itself and lose the original for good.
     */
    // Lint believes this needs MANAGE_EXTERNAL_STORAGE or READ_WALLPAPER_INTERNAL. On
    // Android 17 the actual gate is READ_MEDIA_IMAGES, verified on the device, and that
    // is adb-grantable where the other two are not. The call is wrapped anyway, and a
    // failed read simply means the wallpaper is left alone.
    @SuppressLint("MissingPermission")
    private fun backupCurrent(): Boolean {
        if (hasBackup) return true
        val bitmap = runCatching {
            manager.getWallpaperFile(WallpaperManager.FLAG_SYSTEM)?.use { descriptor ->
                BitmapFactory.decodeFileDescriptor(descriptor.fileDescriptor)
            }
        }.onFailure {
            // Never swallow this: without the reason, "the wallpaper did not change" is
            // indistinguishable from a missing permission, and the effect is gated on
            // the backup succeeding.
            Log.w(TAG, "cannot read the current wallpaper, leaving it alone", it)
        }.getOrNull() ?: run {
            Log.w(TAG, "no current wallpaper to back up, leaving it alone")
            return false
        }
        return runCatching {
            FileOutputStream(backupFile).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            true
        }.getOrDefault(false)
    }

    /**
     * The top band is left empty so the lock screen's own clock lands there — a live clock
     * instead of one frozen into a bitmap.
     */
    private fun render(width: Int, height: Int, appNames: List<String>): Bitmap {
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)
        canvas.drawColor("#08090B".toColorInt())

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#5F6874".toColorInt()
            textSize = width * 0.052f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        var y = height * 0.42f
        appNames.forEach { name ->
            canvas.drawText(name, width / 2f, y, paint)
            y += width * 0.115f
        }
        return bitmap
    }
}
