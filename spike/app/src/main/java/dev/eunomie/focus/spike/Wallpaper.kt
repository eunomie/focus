package dev.eunomie.focus.spike

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import java.io.File
import java.io.FileOutputStream

/**
 * Experiment 6: does painting the wallpaper remove the swipe-up-to-home flicker?
 *
 * ADR 6 claims it does — what the transition reveals would already look like the focus
 * screen. That claim is reasoning, not measurement, and it is the last one in the design
 * that has not been put on a device.
 *
 * It also tests the question underneath ADR 6's workaround: whether the current wallpaper
 * can be read back at all. If it can, the real app does not need to hoard its own copy.
 */
class Wallpaper(private val context: Context) {

    private val wm get() = WallpaperManager.getInstance(context)
    private val backup get() = File(context.filesDir, "wallpaper-backup.png")

    val hasBackup: Boolean get() = backup.exists()

    /**
     * Try every documented way to read the current wallpaper and report exactly what each
     * one did. "Didn't work" is not a useful design input; "threw SecurityException" and
     * "returned null" point at different fixes.
     */
    fun backupCurrent(): String {
        val notes = mutableListOf<String>()

        attempt("getWallpaperFile(FLAG_SYSTEM)", notes) {
            wm.getWallpaperFile(WallpaperManager.FLAG_SYSTEM)?.use { pfd ->
                BitmapFactory.decodeFileDescriptor(pfd.fileDescriptor)
            }
        }?.let { return save(it, "getWallpaperFile(FLAG_SYSTEM)") }

        attempt("getWallpaperFile(FLAG_LOCK)", notes) {
            wm.getWallpaperFile(WallpaperManager.FLAG_LOCK)?.use { pfd ->
                BitmapFactory.decodeFileDescriptor(pfd.fileDescriptor)
            }
        }?.let { return save(it, "getWallpaperFile(FLAG_LOCK)") }

        attempt("drawable", notes) {
            wm.drawable?.let { d ->
                Bitmap.createBitmap(
                    d.intrinsicWidth.coerceAtLeast(1),
                    d.intrinsicHeight.coerceAtLeast(1),
                    Bitmap.Config.ARGB_8888,
                ).also { bmp ->
                    Canvas(bmp).let { c -> d.setBounds(0, 0, c.width, c.height); d.draw(c) }
                }
            }
        }?.let { return save(it, "drawable") }

        return "NO BACKUP — " + notes.joinToString(" | ")
    }

    private inline fun attempt(
        label: String,
        notes: MutableList<String>,
        block: () -> Bitmap?,
    ): Bitmap? = try {
        block().also { if (it == null) notes += "$label: null" }
    } catch (t: Throwable) {
        notes += "$label: ${t.javaClass.simpleName}: ${t.message}"
        null
    }

    private fun save(bmp: Bitmap, how: String): String {
        FileOutputStream(backup).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return "backed up (${bmp.width}x${bmp.height}) — $how"
    }

    /** Paint both wallpapers with something that looks like the focus screen. */
    fun applyFocus(apps: List<String>): String = runCatching {
        val w = wm.desiredMinimumWidth.takeIf { it > 0 } ?: 1080
        val h = wm.desiredMinimumHeight.takeIf { it > 0 } ?: 2400
        val bmp = render(w, h, apps)
        wm.setBitmap(bmp, null, true, WallpaperManager.FLAG_SYSTEM)
        wm.setBitmap(bmp, null, true, WallpaperManager.FLAG_LOCK)
        "focus wallpaper applied to home + lock (${w}x$h)"
    }.getOrElse { "FAILED: ${it.javaClass.simpleName}: ${it.message}" }

    fun restore(): String = runCatching {
        if (!hasBackup) return "no backup to restore from"
        val bmp = BitmapFactory.decodeFile(backup.path) ?: return "backup unreadable"
        wm.setBitmap(bmp, null, true, WallpaperManager.FLAG_SYSTEM)
        wm.setBitmap(bmp, null, true, WallpaperManager.FLAG_LOCK)
        "wallpaper restored from backup"
    }.getOrElse { "FAILED: ${it.javaClass.simpleName}: ${it.message}" }

    /**
     * The top band is left empty on purpose: the lock screen draws the system clock there,
     * so the mimic gets a live clock instead of one frozen into a bitmap.
     */
    private fun render(w: Int, h: Int, apps: List<String>): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.parseColor("#08090B"))

        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#5F6874")
            textSize = w * 0.052f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        var y = h * 0.42f
        apps.forEach { name ->
            c.drawText(name, w / 2f, y, text)
            y += w * 0.115f
        }
        return bmp
    }
}
