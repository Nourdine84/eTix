package com.etix.ui.scan

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.etix.R
import com.etix.data.AppDatabase
import com.etix.features.ocr.scan.MlKitTextReader
import com.etix.features.ocr.scan.ScanServices
import com.etix.features.ocr.scan.ScanTextReader
import com.etix.model.Ticket
import com.etix.testutil.Screens.capture
import com.etix.testutil.Screens.idle
import com.etix.testutil.Screens.waitFor
import com.etix.testutil.TestDb
import com.etix.ui.main.MainActivityV2
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.util.Calendar

/**
 * Lot 9 — parcours de scan (iOS ScannerFlowView + AddTicketViewModel.handleOCRResult) en Robolectric.
 * La reconnaissance ML Kit est remplacée par un lecteur simulé (texte de ticket fourni) ; l'image, le parcours,
 * l'analyse, le préremplissage et l'enregistrement sont réels. Exécution réelle d'ML Kit : E2eScanTest (émulateur).
 * Aucun ticket n'est créé sans « Enregistrer » ; les tickets existants ne sont jamais modifiés.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w360dp-h780dp-hdpi")
class Lot9ScanTest {

    private val ctx get() = RuntimeEnvironment.getApplication()
    private val dao get() = AppDatabase.getInstance(ctx).ticketDao()
    private val existing = listOf(
        Ticket(id = 1, store = "Lidl", amount = 10.0, category = "Alimentation", dateMillis = System.currentTimeMillis() - 86_400_000),
        Ticket(id = 2, store = "Carrefour", amount = 25.0, category = "Alimentation", dateMillis = System.currentTimeMillis() - 2 * 86_400_000),
    )

    private val ESSO = "ESSO\n12/01/2026\nTOTAL TTC 23,45 €\nCB VISA\nMERCI DE VOTRE VISITE"

    @Before fun setUp() { TestDb.reset(ctx); TestDb.seed(ctx, existing) }
    @After fun tearDown() { ScanServices.reader = MlKitTextReader }

    private fun tickets() = runBlocking { dao.getAllFlow().first() }.sortedBy { it.id }

    private fun image(name: String = "ticket.png"): Uri {
        val bmp = Bitmap.createBitmap(600, 800, Bitmap.Config.ARGB_8888)
        Canvas(bmp).apply {
            drawColor(Color.WHITE)
            val p = Paint().apply { color = Color.BLACK; textSize = 40f; isAntiAlias = true }
            ESSO.lines().forEachIndexed { i, l -> drawText(l, 40f, 80f + i * 60f, p) }
        }
        val f = File(ctx.cacheDir, name)
        f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return Uri.fromFile(f)
    }

    private fun launch(): MainActivityV2 = Robolectric.buildActivity(MainActivityV2::class.java).setup().get().also { idle() }

    private fun MainActivityV2.scan(): ScanFlowFragment = supportFragmentManager.findFragmentById(R.id.overlayContainer) as ScanFlowFragment
    private fun vm(f: Fragment) = ViewModelProvider(f)[ScanFlowViewModel::class.java]
    private fun MainActivityV2.add(): View = supportFragmentManager.findFragmentByTag("f1")!!.requireView()
    private fun MainActivityV2.text(id: Int) = add().findViewById<TextView>(id).text.toString()
    private fun MainActivityV2.visible(id: Int) = add().findViewById<View>(id).visibility == View.VISIBLE

    private fun openFromAdd(a: MainActivityV2) {
        a.goToPage(MainActivityV2.PAGE_ADD); idle()
        a.add().findViewById<View>(R.id.btnScanTicket).performClick(); idle()
    }

    /** Lecture simulée d'une image → retour au formulaire « Ajouter » prérempli. */
    private fun scanInto(a: MainActivityV2, text: String = ESSO) {
        ScanServices.reader = ScanTextReader { text }
        vm(a.scan()).process(image(), deleteAfter = false)
        waitFor { a.supportFragmentManager.findFragmentById(R.id.overlayContainer) == null && a.text(R.id.inputStore).isNotEmpty() }
    }

    /**
     * 02/10/2026 : plus d'étape d'autorisation. « Prendre une photo » ouvre directement l'application appareil
     * photo du système (ACTION_IMAGE_CAPTURE, photo écrite dans le cache d'eTix) ; aucune permission demandée,
     * et le manifeste ne déclare pas CAMERA.
     */
    @Test fun intro_puis_appareil_photo_du_systeme_sans_autorisation() {
        val a = launch()
        openFromAdd(a)
        assertEquals(View.VISIBLE, a.findViewById<View>(R.id.stepIntro).visibility)
        assertEquals("Barre d'onglets masquée pendant le scan (iOS fullScreenCover)",
            View.GONE, a.findViewById<View>(R.id.bottomNav).visibility)
        capture(a, "l9_01_scan_intro_light")
        a.findViewById<View>(R.id.btnTakePhoto).performClick(); idle()
        val started = org.robolectric.Shadows.shadowOf(a).nextStartedActivityForResult
        assertEquals(android.provider.MediaStore.ACTION_IMAGE_CAPTURE, started.intent.action)
        @Suppress("DEPRECATION")
        val out = started.intent.getParcelableExtra<Uri>(android.provider.MediaStore.EXTRA_OUTPUT)
        assertEquals("${ctx.packageName}.fileprovider", out?.authority)
        assertNull("Aucune autorisation demandée", org.robolectric.Shadows.shadowOf(a).lastRequestedPermission)
        val declared = ctx.packageManager.getPackageInfo(ctx.packageName, android.content.pm.PackageManager.GET_PERMISSIONS)
            .requestedPermissions.orEmpty().toList()
        assertTrue("CAMERA non déclarée : $declared", android.Manifest.permission.CAMERA !in declared)
        assertEquals(existing, tickets())
        // fermeture : barre d'onglets de retour
        a.onBackPressedDispatcher.onBackPressed(); idle()
        assertEquals(View.VISIBLE, a.findViewById<View>(R.id.bottomNav).visibility)
    }

    @Test @Config(qualifiers = "+night")
    fun intro_dark() {
        val a = launch()
        openFromAdd(a)
        capture(a, "l9_01_scan_intro_dark")
    }

    @Test fun lecture_en_cours_puis_formulaire_prerempli_sans_enregistrement() {
        val a = launch()
        openFromAdd(a)
        val gate = CompletableDeferred<String>()
        ScanServices.reader = ScanTextReader { gate.await() }
        vm(a.scan()).process(image(), deleteAfter = false)
        waitFor { a.findViewById<View>(R.id.stepProcessing)?.visibility == View.VISIBLE &&
            a.findViewById<TextView>(R.id.tvStep2).text.startsWith("›") }
        capture(a, "l9_03_scan_lecture_light")
        gate.complete(ESSO)
        waitFor { a.supportFragmentManager.findFragmentById(R.id.overlayContainer) == null && a.text(R.id.inputStore).isNotEmpty() }

        assertEquals("ESSO", a.text(R.id.inputStore))
        assertEquals("23,45", a.text(R.id.inputAmount))
        val c = Calendar.getInstance().apply { timeInMillis = (a.supportFragmentManager.findFragmentByTag("f1") as com.etix.ui.add.AddTicketFragmentV2).let {
            val f = it.javaClass.getDeclaredField("form").apply { isAccessible = true }
            (f.get(it) as com.etix.ui.ticket.TicketFormController).dateMillis } }
        assertEquals(12, c.get(Calendar.DAY_OF_MONTH)); assertEquals(Calendar.JANUARY, c.get(Calendar.MONTH)); assertEquals(2026, c.get(Calendar.YEAR))
        assertEquals("Carburant", a.text(R.id.tvCategoryValue))
        // 02/10/2026 : une lecture n'est jamais « Vérifié » (seul l'utilisateur vérifie, en enregistrant)
        assertEquals("À vérifier", a.text(R.id.badgeStore))      // iOS : enseigne = confiance moyenne
        assertEquals("Détecté", a.text(R.id.badgeAmount))        // montant sur l'unique ligne TOTAL TTC
        assertEquals("Détectée", a.text(R.id.badgeDate))
        assertTrue(a.visible(R.id.badgeStore) && a.visible(R.id.badgeAmount) && a.visible(R.id.badgeDate))
        assertTrue(a.visible(R.id.tvCategorySuggested))
        assertTrue(a.visible(R.id.scanBanner))
        assertEquals("Aucun ticket tant que l'utilisateur n'a pas validé", existing, tickets())
        capture(a, "l9_04_formulaire_prerempli_light")

        // « Annuler le scan » : formulaire vidé, aucun ticket
        a.add().findViewById<View>(R.id.btnDiscardScan).performClick(); idle()
        assertEquals("", a.text(R.id.inputStore))
        assertTrue(!a.visible(R.id.scanBanner) && !a.visible(R.id.badgeAmount) && !a.visible(R.id.tvCategorySuggested))
        assertEquals(existing, tickets())
    }

    @Test @Config(qualifiers = "+night")
    fun formulaire_prerempli_dark() {
        val a = launch()
        openFromAdd(a)
        scanInto(a)
        capture(a, "l9_04_formulaire_prerempli_dark")
    }

    @Test fun enregistrement_apres_validation_explicite() {
        val a = launch()
        openFromAdd(a)
        scanInto(a)
        // l'utilisateur corrige le montant avant de valider
        a.add().findViewById<EditText>(R.id.inputAmount).setText("23,40")
        a.add().findViewById<View>(R.id.btnSaveTicket).performClick()
        waitFor { tickets().size == 3 }
        val all = tickets()
        assertEquals(existing, all.take(2))                     // tickets existants inchangés
        val t = all.last()
        assertEquals("ESSO", t.store); assertEquals(23.40, t.amount, 0.001); assertEquals("Carburant", t.category)
        val c = Calendar.getInstance().apply { timeInMillis = t.dateMillis }
        assertEquals(12, c.get(Calendar.DAY_OF_MONTH)); assertEquals(Calendar.JANUARY, c.get(Calendar.MONTH))
        // le bandeau disparaît quand l'écran a repris la main après l'insertion (coroutine de l'écran)
        waitFor { !a.visible(R.id.scanBanner) && a.text(R.id.inputStore).isEmpty() }
    }

    /** iOS StoreCategoryMapper : l'historique du magasin passe avant le dictionnaire ; rien n'est reclassé. */
    @Test fun historique_du_magasin_prioritaire_sans_reclasser() {
        val esso = Ticket(id = 3, store = "Esso", amount = 50.0, category = "Transport", dateMillis = System.currentTimeMillis())
        TestDb.seed(ctx, listOf(esso))
        val a = launch()
        openFromAdd(a)
        scanInto(a)
        assertEquals("Transport", a.text(R.id.tvCategoryValue))
        assertTrue(a.visible(R.id.tvCategorySuggested))         // 1 seul ticket : suggestion signalée (iOS weakHistory)
        assertEquals(existing + esso, tickets())
    }

    @Test fun rien_detecte_puis_reessayer() {
        val a = launch()
        openFromAdd(a)
        ScanServices.reader = ScanTextReader { "" }
        vm(a.scan()).process(image(), deleteAfter = false)
        waitFor { a.findViewById<View>(R.id.stepFailure)?.visibility == View.VISIBLE }
        assertEquals("Aucune information détectée", a.findViewById<TextView>(R.id.tvFailureTitle).text.toString())
        capture(a, "l9_05_scan_rien_detecte_light")
        a.findViewById<View>(R.id.btnRetry).performClick(); idle()
        assertEquals(View.VISIBLE, a.findViewById<View>(R.id.stepIntro).visibility)
        assertEquals(existing, tickets())
    }

    @Test fun erreur_de_lecture_puis_saisie_manuelle() {
        val a = launch()
        openFromAdd(a)
        ScanServices.reader = ScanTextReader { throw IllegalStateException("échec simulé") }
        vm(a.scan()).process(image(), deleteAfter = false)
        waitFor { a.findViewById<View>(R.id.stepFailure)?.visibility == View.VISIBLE }
        assertEquals("Lecture impossible", a.findViewById<TextView>(R.id.tvFailureTitle).text.toString())
        capture(a, "l9_06_scan_erreur_light")
        a.findViewById<View>(R.id.btnManualEntry).performClick(); idle()
        assertNull(a.supportFragmentManager.findFragmentById(R.id.overlayContainer))
        assertEquals("", a.text(R.id.inputStore))
        assertEquals(existing, tickets())
    }

    @Test fun image_illisible() {
        val a = launch()
        openFromAdd(a)
        val bad = File(ctx.cacheDir, "pas_une_image.png").apply { writeText("pas une image") }
        vm(a.scan()).process(Uri.fromFile(bad), deleteAfter = false)
        waitFor { a.findViewById<View>(R.id.stepFailure)?.visibility == View.VISIBLE }
        assertTrue(a.findViewById<TextView>(R.id.tvFailureMessage).text.contains("n'a pas pu être ouverte"))
        assertEquals(existing, tickets())
    }

    @Test fun retour_systeme_ferme_le_parcours_sans_ticket() {
        val a = launch()
        openFromAdd(a)
        a.onBackPressedDispatcher.onBackPressed(); idle()
        assertNull(a.supportFragmentManager.findFragmentById(R.id.overlayContainer))
        assertEquals(existing, tickets())
    }

    @Test fun accueil_ouvre_ajouter_et_le_scanner() {
        val a = launch()
        a.supportFragmentManager.findFragmentByTag("f0")!!.requireView().findViewById<View>(R.id.btnScanTicket).performClick(); idle()
        assertTrue(a.supportFragmentManager.findFragmentById(R.id.overlayContainer) is ScanFlowFragment)
        assertEquals(MainActivityV2.PAGE_ADD, a.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPager).currentItem)
    }

    @Test @Config(qualifiers = "w320dp-h640dp-hdpi")
    fun petit_ecran_police_2() {
        RuntimeEnvironment.setFontScale(2.0f)
        val a = launch()
        openFromAdd(a)
        capture(a, "l9_07_scan_intro_320dp_police_2_light")
        scanInto(a)
        capture(a, "l9_08_formulaire_prerempli_320dp_police_2_light")
        assertEquals(existing, tickets())
    }

    // ---------- 02/10/2026 : valeurs absentes ou ambiguës, robustesse ----------

    private fun MainActivityV2.addFragment() = supportFragmentManager.findFragmentByTag("f1") as com.etix.ui.add.AddTicketFragmentV2
    private fun MainActivityV2.formDate(): Long = addFragment().let {
        val f = it.javaClass.getDeclaredField("form").apply { isAccessible = true }
        (f.get(it) as com.etix.ui.ticket.TicketFormController).dateMillis
    }

    /** Champs absents du ticket : « Non lu » / « Non lue » ; la date affichée (date du jour) ne passe pas pour lue. */
    @Test fun champs_absents_marques_non_lus() {
        val a = launch()
        openFromAdd(a)
        scanInto(a, "MAGASIN DUPONT\nMERCI DE VOTRE VISITE")
        assertEquals("À vérifier", a.text(R.id.badgeStore))
        assertEquals("", a.text(R.id.inputAmount))
        assertEquals("Non lu", a.text(R.id.badgeAmount))
        assertEquals("Non lue", a.text(R.id.badgeDate))
        assertTrue(a.visible(R.id.badgeAmount) && a.visible(R.id.badgeDate))
        assertEquals("Choisir une catégorie", a.text(R.id.tvCategoryValue))   // aucune catégorie affichée comme lue
        assertTrue(!a.visible(R.id.tvCategorySuggested))
        capture(a, "l9_09_champs_non_lus_light")
        assertEquals(existing, tickets())
    }

    /** Valeurs ambiguës : conservées mais « À vérifier » (deux totaux différents, deux dates). */
    @Test fun valeurs_ambigues_a_verifier() {
        val a = launch()
        openFromAdd(a)
        scanInto(a, "ESSO\n12/09/2026\nTOTAL 23,45\nTOTAL 32,45\nBON VALABLE JUSQU'AU 31/12/2026")
        assertEquals("À vérifier", a.text(R.id.badgeAmount))
        assertEquals("À vérifier", a.text(R.id.badgeDate))
        assertEquals(existing, tickets())
    }

    /** Un champ modifié par l'utilisateur n'est plus marqué comme détecté ; la marque revient s'il rétablit la valeur lue. */
    @Test fun modification_retire_la_marque() {
        val a = launch()
        openFromAdd(a)
        scanInto(a)
        val amount = a.add().findViewById<EditText>(R.id.inputAmount)
        amount.setText("23,40"); idle()
        assertTrue(!a.visible(R.id.badgeAmount))
        amount.setText("23,45"); idle()
        assertTrue(a.visible(R.id.badgeAmount))
        a.add().findViewById<EditText>(R.id.inputStore).setText("Esso Lyon"); idle()
        assertTrue(!a.visible(R.id.badgeStore))
    }

    /** Double appui sur « Enregistrer » : un seul ticket. */
    @Test fun double_appui_enregistrer_un_seul_ticket() {
        val a = launch()
        openFromAdd(a)
        scanInto(a)
        val save = a.add().findViewById<View>(R.id.btnSaveTicket)
        save.performClick(); save.performClick(); save.performClick()
        waitFor { tickets().size == 3 && a.text(R.id.inputStore).isEmpty() }
        Thread.sleep(300); idle()
        assertEquals(3, tickets().size)
        assertTrue("bouton réactivé après l'enregistrement", save.isEnabled)
    }

    /** Annulation (retour système) pendant la lecture : aucun formulaire rempli plus tard, aucun ticket. */
    @Test fun annulation_pendant_la_lecture() {
        val a = launch()
        openFromAdd(a)
        val gate = CompletableDeferred<String>()
        ScanServices.reader = ScanTextReader { gate.await() }
        vm(a.scan()).process(image(), deleteAfter = false)
        waitFor { a.findViewById<View>(R.id.stepProcessing)?.visibility == View.VISIBLE &&
            a.findViewById<TextView>(R.id.tvStep2).text.startsWith("›") }
        a.onBackPressedDispatcher.onBackPressed(); idle()
        assertNull(a.supportFragmentManager.findFragmentById(R.id.overlayContainer))
        gate.complete(ESSO)            // la reconnaissance se termine après l'annulation
        repeat(10) { idle(); Thread.sleep(50) }
        assertEquals("", a.text(R.id.inputStore))
        assertEquals("", a.text(R.id.inputAmount))
        assertTrue(!a.visible(R.id.scanBanner))
        assertNull(a.supportFragmentManager.findFragmentById(R.id.overlayContainer))
        assertEquals(existing, tickets())
    }

    /** Recréation (rotation) pendant la lecture : la lecture continue, un seul préremplissage, aucun ticket. */
    @Test fun recreation_pendant_la_lecture() {
        val ctl = Robolectric.buildActivity(MainActivityV2::class.java).setup()
        idle()
        openFromAdd(ctl.get())
        val gate = CompletableDeferred<String>()
        ScanServices.reader = ScanTextReader { gate.await() }
        vm(ctl.get().scan()).process(image(), deleteAfter = false)
        waitFor { ctl.get().findViewById<View>(R.id.stepProcessing)?.visibility == View.VISIBLE }
        ctl.recreate(); idle()
        val a = ctl.get()
        assertEquals(View.VISIBLE, a.findViewById<View>(R.id.stepProcessing).visibility)
        gate.complete(ESSO)
        waitFor { a.supportFragmentManager.findFragmentById(R.id.overlayContainer) == null && a.text(R.id.inputStore).isNotEmpty() }
        assertEquals("ESSO", a.text(R.id.inputStore))
        assertEquals("Détecté", a.text(R.id.badgeAmount))
        assertEquals(existing, tickets())
    }

    /** Recréation après correction : corrections, date lue, catégorie, marques et bandeau conservés ; puis un seul ticket. */
    @Test fun recreation_conserve_les_corrections_sans_doublon() {
        val ctl = Robolectric.buildActivity(MainActivityV2::class.java).setup()
        idle()
        openFromAdd(ctl.get())
        scanInto(ctl.get())
        ctl.get().add().findViewById<EditText>(R.id.inputAmount).setText("23,40")
        ctl.recreate(); idle()
        val a = ctl.get()
        waitFor { a.text(R.id.inputStore) == "ESSO" }
        assertEquals("23,40", a.text(R.id.inputAmount))
        assertEquals("Carburant", a.text(R.id.tvCategoryValue))
        assertTrue(a.visible(R.id.scanBanner))
        assertTrue("marque du magasin conservée", a.visible(R.id.badgeStore))
        assertTrue("montant corrigé : plus de marque", !a.visible(R.id.badgeAmount))
        assertTrue(a.visible(R.id.badgeDate))
        val c = Calendar.getInstance().apply { timeInMillis = a.formDate() }
        assertEquals(12, c.get(Calendar.DAY_OF_MONTH)); assertEquals(Calendar.JANUARY, c.get(Calendar.MONTH))
        assertEquals(existing, tickets())
        a.add().findViewById<View>(R.id.btnSaveTicket).performClick()
        waitFor { tickets().size == 3 && a.text(R.id.inputStore).isEmpty() }
        assertEquals(23.40, tickets().last().amount, 0.001)
    }

    /** Recréation pendant l'insertion : un seul ticket, formulaire vidé dans le nouvel écran (pas de doublon possible). */
    @Test fun recreation_pendant_l_enregistrement_sans_doublon() {
        val ctl = Robolectric.buildActivity(MainActivityV2::class.java).setup()
        idle()
        openFromAdd(ctl.get())
        scanInto(ctl.get())
        ctl.get().add().findViewById<View>(R.id.btnSaveTicket).performClick()
        ctl.recreate()
        val a = ctl.get()
        waitFor { tickets().size == 3 && a.text(R.id.inputStore).isEmpty() }
        assertTrue(!a.visible(R.id.scanBanner))
        a.add().findViewById<View>(R.id.btnSaveTicket).performClick(); idle()   // formulaire vide : refusé
        Thread.sleep(300); idle()
        assertEquals(3, tickets().size)
    }

    /** 320 dp, police 2,0 : barre basse limitée (≤ 40 %), boutons secondaires dans le contenu, illustration masquée. */
    @Test @Config(qualifiers = "w320dp-h569dp-hdpi")
    fun petit_ecran_police_2_barre_compacte() {
        RuntimeEnvironment.setFontScale(2.0f)
        val a = launch()
        openFromAdd(a)
        idle()
        val step = a.findViewById<View>(R.id.stepIntro)
        val bar = a.findViewById<View>(R.id.introBar)
        assertTrue("barre ${bar.height} px / étape ${step.height} px", bar.height <= step.height * ScanFlowFragment.MAX_BAR_FRACTION)
        // mode compact (si la barre complète dépassait 40 %) : boutons secondaires sous le titre, illustration masquée
        val compact = a.findViewById<View>(R.id.introCompactActions).visibility == View.VISIBLE
        if (compact) {
            assertTrue(a.findViewById<View>(R.id.btnPickImage).parent === a.findViewById<View>(R.id.introCompactActions))
            assertEquals(View.GONE, a.findViewById<View>(R.id.imgScanFrame).visibility)
        }
        for (id in listOf(R.id.btnTakePhoto, R.id.btnPickImage, R.id.btnScanCancel)) {
            val v = a.findViewById<View>(id)
            val r = android.graphics.Rect()
            assertTrue("bouton ${a.resources.getResourceEntryName(id)} entièrement visible (compact=$compact)",
                v.getGlobalVisibleRect(r) && r.height() == v.height)
        }
        capture(a, "l9_10_scan_intro_320dp_police_2_compact_light")
    }
}
