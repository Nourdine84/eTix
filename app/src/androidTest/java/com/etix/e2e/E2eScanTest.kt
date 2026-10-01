package com.etix.e2e

import android.Manifest
import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.etix.R
import com.etix.data.AppDatabase
import com.etix.e2e.E2e.ctx
import com.etix.e2e.E2e.shot
import com.etix.e2e.E2e.waitFor
import com.etix.features.ocr.scan.MlKitTextReader
import com.etix.model.Ticket
import com.etix.ui.main.MainActivityV2
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.io.File
import java.util.Calendar

/**
 * Lot 9 — parcours de scan sur émulateur, reconnaissance ML Kit RÉELLE sur une image de ticket générée.
 * Seules les applications externes (appareil photo, sélecteur d'image) sont simulées (Espresso-Intents) :
 * elles renvoient l'image comme le ferait l'utilisateur. Données fictives ; un seul ticket créé (s02, après
 * validation explicite) ; les tickets existants ne sont jamais modifiés.
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class E2eScanTest {

    private val dao get() = AppDatabase.getInstance(ctx).ticketDao()
    private fun tickets(): List<Ticket> = runBlocking { dao.getAllFlow().first() }.sortedBy { it.id }

    private val lines = listOf("ESSO", "12/01/2026", "TOTAL TTC 23,45 EUR", "CB VISA")

    private fun ticketImage(name: String, blank: Boolean = false): File {
        val bmp = Bitmap.createBitmap(1080, 1500, Bitmap.Config.ARGB_8888)
        Canvas(bmp).apply {
            drawColor(Color.WHITE)
            if (!blank) {
                val p = Paint().apply { color = Color.BLACK; textSize = 84f; isAntiAlias = true; typeface = Typeface.DEFAULT_BOLD }
                lines.forEachIndexed { i, l -> drawText(l, 90f, 260f + i * 170f, p) }
            }
        }
        val f = File(File(ctx.cacheDir, "e2e").apply { mkdirs() }, name)
        f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return f
    }

    private fun uriOf(f: File): Uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f)

    /** Sélecteur d'image simulé : renvoie [file]. */
    private fun stubPicker(file: File) {
        intending(hasAction(Intent.ACTION_GET_CONTENT))
            .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, Intent().setData(uriOf(file))))
    }

    /** Appareil photo simulé : écrit [file] à l'emplacement demandé par l'app (EXTRA_OUTPUT), comme une vraie prise. */
    private fun stubCamera(file: File) {
        intending(hasAction(MediaStore.ACTION_IMAGE_CAPTURE)).respondWithFunction { intent ->
            @Suppress("DEPRECATION")
            val out = intent.getParcelableExtra<Uri>(MediaStore.EXTRA_OUTPUT)!!
            ctx.contentResolver.openOutputStream(out)!!.use { o -> file.inputStream().use { it.copyTo(o) } }
            Instrumentation.ActivityResult(Activity.RESULT_OK, null)
        }
    }

    private fun startMain() {
        ctx.startActivity(Intent(ctx, MainActivityV2::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        E2e.waitForAppReady("E2eScanTest"); waitFor(withId(R.id.bottomNav))
    }

    private fun openScanFromAdd() {
        onView(withId(R.id.menu_add)).perform(click())
        waitFor(withId(R.id.inputStore))
        onView(allOf(withId(R.id.btnScanTicket), isDisplayed())).perform(click())
        waitFor(withId(R.id.btnPickImage))
    }

    /** Formulaire « Ajouter » prérempli par le scan (ML Kit réel), en 60 s maximum (1re initialisation ML Kit). */
    private fun waitForPrefill() {
        waitFor(allOf(withId(R.id.scanBanner), isDisplayed()), 60_000)
        // Le formulaire n'existe que sur la page « Ajouter » : identifiants uniques, sans exiger l'affichage
        // (la catégorie peut être sous le bord de l'écran)
        onView(withId(R.id.inputStore)).check(matches(withText("ESSO")))
        onView(withId(R.id.inputAmount)).check(matches(withText("23,45")))
        onView(withId(R.id.tvCategoryValue)).check(matches(withText("Carburant")))
        onView(withId(R.id.badgeAmount)).check(matches(withText("Vérifié")))
    }

    @Before fun setUp() {
        Intents.init()
        if (Build.VERSION.SDK_INT >= 23) InstrumentationRegistry.getInstrumentation().uiAutomation
            .grantRuntimePermission(ctx.packageName, Manifest.permission.CAMERA)
    }

    @After fun tearDown() = Intents.release()

    /** Texte lu par ML Kit sur l'image de test, consigné pour le diagnostic (shots/scan_ocr.txt). */
    @Test fun s00_mlkit_lit_l_image_de_test() {
        val bmp = android.graphics.BitmapFactory.decodeFile(ticketImage("diag.png").path)
        val text = runBlocking { MlKitTextReader.read(bmp) }
        File(File(ctx.filesDir, "shots").apply { mkdirs() }, "scan_ocr.txt")
            .appendText("API ${Build.VERSION.SDK_INT} — ML Kit :\n$text\n")
        assertEquals(true, text.contains("ESSO") && text.contains("23,45"))
    }

    @Test fun s01_image_choisie_formulaire_prerempli_puis_annuler_sans_ticket() {
        val before = tickets()
        startMain()
        openScanFromAdd()
        shot("70_scan_intro")
        stubPicker(ticketImage("galerie.png"))
        onView(withId(R.id.btnPickImage)).perform(click())
        waitForPrefill()
        shot("71_scan_formulaire_prerempli")
        assertEquals(before, tickets())
        onView(withId(R.id.btnDiscardScan)).perform(click())
        waitFor(allOf(withId(R.id.inputStore), withText("")))
        assertEquals(before, tickets())
    }

    @Test fun s02_photo_depuis_l_accueil_puis_enregistrement_apres_validation() {
        val before = tickets()
        startMain()
        onView(withId(R.id.menu_home)).perform(click())
        onView(allOf(withId(R.id.btnScanTicket), androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA(withId(R.id.homeScroll))))
            .perform(E2e.nestedScrollTo(), click())
        waitFor(withId(R.id.btnTakePhoto))
        stubCamera(ticketImage("photo.png"))
        onView(withId(R.id.btnTakePhoto)).perform(click())
        waitForPrefill()
        assertEquals("Aucun ticket avant « Enregistrer »", before, tickets())
        onView(allOf(withId(R.id.inputAmount), isDisplayed())).perform(replaceText("23,40"))
        E2e.closeKeyboard()
        onView(withId(R.id.btnSaveTicket)).perform(scrollTo(), click())
        val end = System.currentTimeMillis() + 10_000
        while (tickets().size == before.size && System.currentTimeMillis() < end) Thread.sleep(200)
        val after = tickets()
        assertEquals(before, after.take(before.size))           // tickets existants inchangés
        val t = after.last()
        assertEquals("ESSO", t.store); assertEquals(23.40, t.amount, 0.001); assertEquals("Carburant", t.category)
        val c = Calendar.getInstance().apply { timeInMillis = t.dateMillis }
        assertEquals(12, c.get(Calendar.DAY_OF_MONTH)); assertEquals(Calendar.JANUARY, c.get(Calendar.MONTH))
        assertEquals(2026, c.get(Calendar.YEAR))
        shot("72_scan_enregistre")
    }

    @Test fun s03_rien_detecte_reessayer_puis_retour_sans_ticket() {
        val before = tickets()
        startMain()
        openScanFromAdd()
        stubPicker(ticketImage("vide.png", blank = true))
        onView(withId(R.id.btnPickImage)).perform(click())
        waitFor(withText("Aucune information détectée"), 60_000)
        shot("73_scan_rien_detecte")
        onView(withId(R.id.btnRetry)).perform(click())
        waitFor(withId(R.id.btnPickImage))
        pressBack()
        waitFor(withId(R.id.inputStore))
        assertEquals(before, tickets())
    }
}
