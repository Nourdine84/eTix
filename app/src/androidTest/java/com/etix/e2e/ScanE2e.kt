package com.etix.e2e

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.platform.app.InstrumentationRegistry
import com.etix.data.AppDatabase
import com.etix.e2e.E2e.ctx
import com.etix.model.Ticket
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * Outils des tests de scan sur émulateur (lot 9). Données fictives uniquement ; images générées ou fournies
 * par le test (assets/scan, produites par .github/scripts/generer_images_scan.py).
 */
object ScanE2e {

    val LINES = listOf("ESSO", "12/01/2026", "TOTAL TTC 23,45 EUR", "CB VISA")

    fun tickets(): List<Ticket> = runBlocking { AppDatabase.getInstance(ctx).ticketDao().getAllFlow().first() }.sortedBy { it.id }

    fun dir(): File = File(ctx.cacheDir, "e2e").apply { mkdirs() }

    /** Ticket de test dessiné (texte net) ; [blank] = image blanche. */
    fun ticketImage(name: String, blank: Boolean = false, lines: List<String> = LINES): File {
        val bmp = Bitmap.createBitmap(1080, 1500, Bitmap.Config.ARGB_8888)
        Canvas(bmp).apply {
            drawColor(Color.WHITE)
            if (!blank) {
                val p = Paint().apply { color = Color.BLACK; textSize = 84f; isAntiAlias = true; typeface = Typeface.DEFAULT_BOLD }
                lines.forEachIndexed { i, l -> drawText(l, 90f, 260f + i * 170f, p) }
            }
        }
        val f = File(dir(), name)
        f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return f
    }

    /** Image fournie par le test (assets/scan/[name]) copiée dans le cache de l'app. */
    fun asset(name: String): File {
        val f = File(dir(), name)
        InstrumentationRegistry.getInstrumentation().context.assets.open("scan/$name").use { i ->
            f.outputStream().use { i.copyTo(it) }
        }
        return f
    }

    fun uriOf(f: File): Uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f)

    /** Sélecteur d'image simulé (Espresso-Intents) : renvoie [file]. */
    fun stubPicker(file: File) {
        intending(hasAction(Intent.ACTION_GET_CONTENT))
            .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, Intent().setData(uriOf(file))))
    }

    /** Appareil photo simulé : écrit [file] à l'emplacement demandé par l'app (EXTRA_OUTPUT), comme une vraie prise. */
    fun stubCamera(file: File) {
        intending(hasAction(MediaStore.ACTION_IMAGE_CAPTURE)).respondWithFunction { intent ->
            @Suppress("DEPRECATION")
            val out = intent.getParcelableExtra<Uri>(MediaStore.EXTRA_OUTPUT)!!
            ctx.contentResolver.openOutputStream(out)!!.use { o -> file.inputStream().use { it.copyTo(o) } }
            Instrumentation.ActivityResult(Activity.RESULT_OK, null)
        }
    }

    /** Journal du test, publié par la CI (files/shots/<name>.txt → annotation). */
    fun log(name: String, line: String) {
        File(File(ctx.filesDir, "shots").apply { mkdirs() }, "$name.txt")
            .appendText("API ${android.os.Build.VERSION.SDK_INT} : $line\n")
    }
}
