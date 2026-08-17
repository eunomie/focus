package dev.eunomie.focus.domain

import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
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

    private val manager get() = WallpaperManager.getInstance(context)
    private val backupFile get() = File(context.filesDir, "wallpaper-backup.png")

    val hasBackup: Boolean get() = backupFile.exists()

    fun apply(appNames: List<String>): Boolean {
        if (!backupCurrent()) return false
        return runCatching {
            val width = manager.desiredMinimumWidth.takeIf { it > 0 } ?: 1080
            val height = manager.desiredMinimumHeight.takeIf { it > 0 } ?: 2400
            val bitmap = render(width, height, appNames)
            manager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
            manager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
            true
        }.getOrDefault(false)
    }

    fun restore(): Boolean = runCatching {
        val bitmap = BitmapFactory.decodeFile(backupFile.path) ?: return false
        manager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
        manager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
        backupFile.delete()
        true
    }.getOrDefault(false)

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
        }.getOrNull() ?: return false
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
