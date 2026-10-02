package com.etix.e2e

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.etix.R
import com.etix.e2e.E2e.ctx
import com.etix.e2e.E2e.shot
import com.etix.e2e.E2e.waitFor
import com.etix.e2e.ScanE2e.tickets
import com.etix.ui.main.MainActivityV2
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.io.File
import java.util.regex.Pattern

/**
 * Lot 9 — scan avec les VRAIES applications du système de l'émulateur (rien n'est simulé côté système) :
 * - p01 : vrai sélecteur d'image (DocumentsUI ou sélecteur de photos, selon l'image système) pilotant une image
 *   de ticket déposée dans la galerie de l'émulateur → lecture ML Kit → formulaire prérempli ;
 * - p02 : vraie application appareil photo de l'émulateur (caméra arrière « emulated », image de synthèse,
 *   sans texte) : 1) l'utilisateur refuse l'accès caméra à l'application appareil photo → retour à l'intro
 *   d'eTix, aucun ticket ; 2) accès rétabli (équivalent Paramètres : pm grant sur l'application appareil photo),
 *   photo prise et validée → photo lue par eTix (aucune
 *   information détectée attendue sur l'image de synthèse), photo temporaire supprimée, aucun ticket.
 * eTix ne demande aucune autorisation (pas de permission CAMERA) : le refus porte sur l'application appareil
 * photo elle-même, seul refus possible sur ce parcours. Étapes et écrans consignés dans shots/scan_systeme.txt.
 * Données fictives ; réglages de l'émulateur jetable uniquement.
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@androidx.annotation.RequiresApi(29)
class E2eScanSystemeTest {

    private val instr get() = InstrumentationRegistry.getInstrumentation()
    private val device: UiDevice get() = UiDevice.getInstance(instr)
    private var inserted: Uri? = null

    private fun log(line: String) = ScanE2e.log("scan_systeme", line)

    private fun shell(cmd: String): String {
        val pfd = instr.uiAutomation.executeShellCommand(cmd)
        return ParcelFileDescriptor.AutoCloseInputStream(pfd).bufferedReader().use { it.readText() }.trim()
    }

    /** Textes visibles à l'écran (diagnostic), sans le contenu d'aucun ticket réel (émulateur jetable). */
    private fun screen(): String {
        val texts = device.findObjects(By.textContains("")).mapNotNull { it.text?.takeIf { t -> t.isNotBlank() } }.take(25)
        return "${device.currentPackageName} : ${texts.joinToString(" | ").take(600)}"
    }

    private fun dump(tag: String) {
        runCatching {
            val f = File(File(ctx.filesDir, "shots").apply { mkdirs() }, "systeme_$tag.xml")
            device.dumpWindowHierarchy(f)
        }
        shot("zz_systeme_$tag")
        log("[$tag] ${screen()}")
    }

    private fun startMain() {
        ctx.startActivity(Intent(ctx, MainActivityV2::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        E2e.waitForAppReady("E2eScanSystemeTest"); waitFor(withId(R.id.bottomNav))
    }

    private fun openScan() {
        onView(withId(R.id.menu_add)).perform(click())
        waitFor(withId(R.id.inputStore))
        onView(allOf(withId(R.id.btnScanTicket), isDisplayed())).perform(click())
        waitFor(withId(R.id.btnPickImage))
    }

    private fun waitOtherApp(timeoutMs: Long = 15_000): String {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) {
            val p = device.currentPackageName
            if (p != null && p != ctx.packageName) return p
            Thread.sleep(250)
        }
        dump("autre_app_absente")
        throw AssertionError("aucune application du système ouverte (${device.currentPackageName})")
    }

    private fun waitBackInEtix(timeoutMs: Long = 20_000): Boolean =
        device.wait(Until.hasObject(By.pkg(ctx.packageName).depth(0)), timeoutMs) == true

    private fun first(timeoutMs: Long, vararg selectors: BySelector): UiObject2? {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) {
            for (s in selectors) device.findObject(s)?.let { return it }
            Thread.sleep(250)
        }
        return null
    }

    private val res = { id: String -> By.res(Pattern.compile(".*:id/$id")) }

    @Before fun setUp() {
        assertTrue("Parcours système vérifié sur API 29+ (galerie MediaStore)", Build.VERSION.SDK_INT >= 29)
        device.wakeUp()
    }

    @After fun tearDown() {
        inserted?.let { runCatching { ctx.contentResolver.delete(it, null, null) } }
    }

    @Test fun p01_vrai_selecteur_d_image() {
        // Image de ticket déposée dans la galerie de l'émulateur (dossier Pictures/eTixE2E)
        val name = "etix_e2e_ticket_${System.currentTimeMillis()}"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$name.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/eTixE2E")
        }
        val uri = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
        inserted = uri
        val src = ScanE2e.ticketImage("galerie_systeme.png")
        ctx.contentResolver.openOutputStream(uri)!!.use { o -> src.inputStream().use { it.copyTo(o) } }
        log("p01 : image déposée dans la galerie ($name.png)")

        val before = tickets()
        startMain()
        openScan()
        onView(withId(R.id.btnPickImage)).perform(click())
        val pkg = waitOtherApp()
        log("p01 : sélecteur ouvert par le système = $pkg")
        Thread.sleep(1_500)
        shot("80_systeme_selecteur")
        val item = if (pkg.contains("documentsui")) {
            first(8_000, By.textContains(name), By.descContains(name)) ?: run {
                // Vue « Récents » sans le nom : recherche par nom dans DocumentsUI
                first(3_000, res("option_menu_search"), By.desc(Pattern.compile("(?i)(search|rechercher)")))?.click()
                first(3_000, res("search_src_text"))?.text = name
                device.pressEnter()
                first(8_000, By.textContains(name))
            }
        } else {
            // Sélecteur de photos : la plus récente (celle déposée par le test) en premier
            first(8_000, res("icon_thumbnail"), By.descStartsWith("Photo"), By.descStartsWith("Image"))
        }
        if (item == null) { dump("p01_image_introuvable"); throw AssertionError("image du test introuvable dans $pkg") }
        log("p01 : élément choisi « ${item.text ?: item.contentDescription} »")
        item.click()
        // Certains sélecteurs demandent une confirmation (« Ajouter », « Sélectionner »)
        if (!waitBackInEtix(4_000)) {
            first(3_000, By.text(Pattern.compile("(?i)(add|ajouter|select|sélectionner|done|ok|ouvrir|open)")))?.let {
                log("p01 : confirmation « ${it.text} »"); it.click()
            }
        }
        if (!waitBackInEtix()) { dump("p01_pas_de_retour"); throw AssertionError("pas de retour dans eTix après le choix") }
        waitFor(allOf(withId(R.id.scanBanner), isDisplayed()), 60_000)
        onView(withId(R.id.inputStore)).check(androidx.test.espresso.assertion.ViewAssertions.matches(
            androidx.test.espresso.matcher.ViewMatchers.withText("ESSO")))
        onView(withId(R.id.inputAmount)).check(androidx.test.espresso.assertion.ViewAssertions.matches(
            androidx.test.espresso.matcher.ViewMatchers.withText("23,45")))
        shot("81_systeme_selecteur_formulaire_prerempli")
        log("p01 : formulaire prérempli (ESSO, 23,45) après le vrai sélecteur $pkg ; aucun ticket créé")
        assertEquals(before, tickets())
        onView(withId(R.id.btnDiscardScan)).perform(click())
    }

    @Test fun p02_vraie_application_appareil_photo_refus_puis_photo() {
        val resolved = shell("cmd package resolve-activity --brief -a android.media.action.IMAGE_CAPTURE")
        val camPkg = resolved.lines().lastOrNull { it.contains("/") }?.substringBefore("/")
        log("p02 : application appareil photo du système = ${camPkg ?: "aucune"} ($resolved)")
        assertTrue("aucune application appareil photo sur l'émulateur", camPkg != null)
        // État de départ connu pour l'application appareil photo (pas eTix) : accès caméra non accordé
        shell("pm revoke $camPkg android.permission.CAMERA")
        val etixPerms = shell("dumpsys package ${ctx.packageName}").lines()
            .filter { it.contains("android.permission.CAMERA") }.map { it.trim() }
        log("p02 : permission CAMERA dans eTix : ${if (etixPerms.isEmpty()) "aucune (ni demandée ni déclarée)" else etixPerms}")
        assertTrue("eTix ne doit pas déclarer CAMERA : $etixPerms", etixPerms.isEmpty())

        val before = tickets()
        val scans = File(ctx.cacheDir, "scans")
        startMain()
        openScan()

        // 1) Refus de l'accès caméra dans l'application appareil photo
        onView(withId(R.id.btnTakePhoto)).perform(click())
        val pkg1 = waitOtherApp()
        log("p02 : ouvert sans demande d'autorisation par eTix → $pkg1")
        val endDeny = System.currentTimeMillis() + 25_000
        var denied = 0
        while (System.currentTimeMillis() < endDeny && device.currentPackageName != ctx.packageName) {
            val deny = first(2_000, res("permission_deny_button"), res("permission_deny_and_dont_ask_again_button"),
                By.text(Pattern.compile("(?i)(don.t allow|deny|refuser|ne pas autoriser)")))
            if (deny != null) { denied++; log("p02 : refus « ${deny.text} » (${device.currentPackageName})"); deny.click(); Thread.sleep(800); continue }
            val close = first(1_000, By.text(Pattern.compile("(?i)(dismiss|ok|close|fermer|quit|quitter)")))
            if (close != null) { log("p02 : fenêtre de l'appareil photo « ${close.text} »"); close.click(); Thread.sleep(800); continue }
            if (denied > 0) { log("p02 : retour système"); device.pressBack(); Thread.sleep(800) }
        }
        if (!waitBackInEtix(5_000)) { dump("p02_refus_pas_de_retour"); throw AssertionError("pas de retour dans eTix après le refus") }
        assertTrue("au moins un refus effectué", denied > 0)
        waitFor(withId(R.id.btnTakePhoto))
        shot("82_systeme_camera_refusee_retour_intro")
        log("p02 : après refus → intro d'eTix (« Prendre une photo » visible), aucun ticket")
        assertEquals(before, tickets())

        // 2) Accès rétabli puis photo prise et validée. Après un refus, l'application appareil photo de l'émulateur
        //    ne redemande plus l'accès (« Camera error… critical permissions », constaté au run 36987368919) :
        //    l'utilisateur doit le rétablir dans les Paramètres de CETTE application. Équivalent ici : pm grant sur
        //    l'application appareil photo (jamais sur eTix, qui ne déclare pas CAMERA).
        for (perm in listOf("android.permission.CAMERA", "android.permission.RECORD_AUDIO")) {
            val r = shell("pm grant $camPkg $perm")
            log("p02 : accès rétabli pour $camPkg : $perm ${if (r.isBlank()) "accordé" else "→ $r"}")
        }
        onView(withId(R.id.btnTakePhoto)).perform(click())
        val pkg2 = waitOtherApp()
        val endAllow = System.currentTimeMillis() + 30_000
        var shutter: UiObject2? = null
        while (System.currentTimeMillis() < endAllow && shutter == null) {
            first(1_500, res("permission_allow_foreground_only_button"), res("permission_allow_button"),
                res("permission_allow_one_time_button"),
                By.text(Pattern.compile("(?i)(while using the app|only this time|allow|autoriser.*|lors de l.utilisation.*)")))
                ?.let { log("p02 : accès accordé « ${it.text} »"); it.click(); Thread.sleep(800); return@let }
            first(1_000, By.text(Pattern.compile("(?i)^(no thanks|no|non|skip|next|got it|ok)$")))
                ?.let { log("p02 : fenêtre de l'appareil photo « ${it.text} »"); it.click(); Thread.sleep(800) }
            shutter = first(1_500, res("shutter_button"), By.desc(Pattern.compile("(?i).*(shutter|prendre une photo|take photo).*")))
        }
        if (shutter == null) { dump("p02_declencheur_introuvable"); throw AssertionError("déclencheur introuvable dans $pkg2") }
        Thread.sleep(1_500)
        shot("83_systeme_appareil_photo")
        shutter.click()
        log("p02 : photo prise dans $pkg2")
        val done = first(15_000, res("done_button"), res("btn_done"),
            By.desc(Pattern.compile("(?i)(done|ok|valider|terminé)")), By.text(Pattern.compile("(?i)^(ok|done|valider)$")))
        if (done == null) { dump("p02_validation_introuvable"); throw AssertionError("bouton de validation introuvable dans $pkg2") }
        done.click()
        if (!waitBackInEtix()) { dump("p02_photo_pas_de_retour"); throw AssertionError("pas de retour dans eTix après la photo") }

        // Photo lue par eTix : image de synthèse sans ticket → « Aucune information détectée » (ou formulaire si du
        // texte était reconnu) ; jamais d'erreur d'accès au fichier écrit par l'application appareil photo
        val end = System.currentTimeMillis() + 60_000
        var outcome = ""
        while (System.currentTimeMillis() < end && outcome.isEmpty()) {
            when {
                device.hasObject(By.text("Aucune information détectée")) -> outcome = "aucune information détectée"
                device.hasObject(By.textContains("Informations détectées")) -> outcome = "formulaire prérempli"
                device.hasObject(By.text("Lecture impossible")) -> outcome = "lecture impossible"
            }
            Thread.sleep(300)
        }
        shot("84_systeme_photo_lue")
        log("p02 : photo de l'appareil photo lue par eTix → ${outcome.ifEmpty { "aucun résultat en 60 s" }}")
        assertTrue("photo réellement lue (résultat : « $outcome »)", outcome == "aucune information détectée" || outcome == "formulaire prérempli")
        val endDel = System.currentTimeMillis() + 5_000
        while (scans.listFiles().orEmpty().isNotEmpty() && System.currentTimeMillis() < endDel) Thread.sleep(200)
        val left = scans.listFiles()?.map { it.name }.orEmpty()
        log("p02 : photos temporaires restantes dans le cache : ${left.size}")
        assertTrue("photo temporaire supprimée après lecture : $left", left.isEmpty())
        assertEquals(before, tickets())
    }
}
