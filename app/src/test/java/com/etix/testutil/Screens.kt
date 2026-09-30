package com.etix.testutil

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import org.robolectric.Shadows.shadowOf
import java.io.File
import java.io.FileOutputStream

/** Outils de capture Robolectric (rendu natif) — aperçu, pas une validation visuelle. */
object Screens {

    fun idle() = shadowOf(Looper.getMainLooper()).idle()

    /** Room émet sur un thread d'arrière-plan : on laisse tourner le looper jusqu'à la condition. */
    fun waitFor(cond: () -> Boolean) {
        repeat(100) {
            idle()
            if (cond()) return
            Thread.sleep(50)
        }
        idle()
    }

    fun capture(activity: Activity, name: String) {
        idle()
        val root = activity.window.decorView
        val bmp = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(bmp))
        val dir = File("build/screenshots").apply { mkdirs() }
        FileOutputStream(File(dir, "$name.png")).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
