package com.etix.ui.preview

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import com.etix.R
import com.etix.data.AppDatabase
import com.etix.data.BudgetStore
import com.etix.features.settings.AppPreferences
import com.etix.features.store.TimeRange
import com.etix.model.Ticket
import com.etix.testutil.Screens.capture
import com.etix.testutil.Screens.idle
import com.etix.testutil.Screens.waitFor
import com.etix.testutil.TestDb
import com.etix.ui.main.MainActivityV2
import com.etix.utils.CsvExporter
import com.etix.utils.SessionManager
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Lot 10 — Réglages (iOS SettingsView) : thème Système / Clair / Sombre, période par défaut, export CSV (Réglages :
 * tous les tickets ; Historique : tickets affichés), compteur, version et build, suppression globale désactivée.
 * Rendu Robolectric : aperçu, pas une validation visuelle. Tickets et budgets jamais modifiés.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w360dp-h780dp-hdpi")
class Lot10ScreenshotTest {

    private val ctx get() = RuntimeEnvironment.getApplication()
    private val dao get() = AppDatabase.getInstance(ctx).ticketDao()
    private val now = System.currentTimeMillis()

    /** Données délicates pour l'export : accents, virgule, guillemets, retour à la ligne, centimes, formule. */
    private val seeded = listOf(
        Ticket(id = 1, store = "Café de l'Été", amount = 4.5, category = "Loisirs", description = "=SOMME(A1:A3)",
            dateMillis = now - 60_000),
        Ticket(id = 2, store = "Durand, fils", amount = 1234.56, category = "Maison", description = "Vis \"inox\"",
            dateMillis = now - 120_000),
        Ticket(id = 3, store = "Marché", amount = 0.05, category = "Courses", description = "ligne 1\nligne 2",
            dateMillis = now - 180_000),
    )

    @Before fun seed() {
        TestDb.reset(ctx)
        TestDb.seed(ctx, seeded)
        ctx.getSharedPreferences("etix_budgets", Context.MODE_PRIVATE).edit().clear().commit()
        ctx.getSharedPreferences(AppPreferences.PREFS, Context.MODE_PRIVATE).edit().clear().commit()
        ctx.getSharedPreferences("etix_session", Context.MODE_PRIVATE).edit().clear().commit()
        BudgetStore(ctx).set("Courses", 100.0)
    }

    private val controllers = mutableListOf<org.robolectric.android.controller.ActivityController<MainActivityV2>>()

    /**
     * Activités fermées AVANT de rétablir le thème par défaut : sinon AppCompat recrée les activités encore ouvertes
     * et Robolectric échoue (barrière de synchronisation de la file de messages, constaté le 02/10/2026).
     */
    @After fun closeActivitiesThenResetNightMode() {
        controllers.forEach { runCatching { it.pause().stop().destroy() } }
        controllers.clear()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }

    /**
     * FileProvider garde en mémoire, par autorité, les dossiers du premier test ; Robolectric change de dossier de
     * données à chaque test. Mémoire vidée pour que l'URI partagée corresponde au cache de CE test (sur un téléphone,
     * le dossier de l'app ne change pas).
     */
    @Before fun forgetFileProviderRoots() {
        runCatching {
            val f = androidx.core.content.FileProvider::class.java.getDeclaredField("sCache").apply { isAccessible = true }
            (f.get(null) as MutableMap<*, *>).clear()
        }
    }

    private fun launch(): MainActivityV2 =
        Robolectric.buildActivity(MainActivityV2::class.java).also { controllers += it }.setup().get().also { idle() }

    private fun settings(a: MainActivityV2): View {
        a.openSettings(); idle()
        waitFor { a.findViewById<TextView>(R.id.tvSettingsTicketCount)?.text?.startsWith("3 ") == true }
        return a.findViewById(R.id.rowTheme)
    }

    private fun checked(a: MainActivityV2, group: Int) = a.findViewById<MaterialButtonToggleGroup>(group).checkedButtonId

    private fun unchanged() {
        assertEquals("tickets inchangés", seeded.sortedBy { it.id }, runBlocking { dao.getAllFlow().first() }.sortedBy { it.id })
        assertEquals("budgets inchangés", mapOf("courses" to 100.0), BudgetStore(ctx).load().mapKeys { it.key.lowercase() })
    }

    private fun scrollBottom(a: MainActivityV2) {
        val s = a.findViewById<ScrollView>(R.id.settingsScroll)
        s.scrollTo(0, s.getChildAt(0).height); idle()
    }

    private fun texts(v: View): List<TextView> = when (v) {
        is TextView -> listOf(v)
        is android.view.ViewGroup -> (0 until v.childCount).flatMap { texts(v.getChildAt(it)) }
        else -> emptyList()
    }

    /**
     * Lignes Thème / Période : textes entiers (ni tronqués ni coupés en milieu de mot). Version / Build : libellé sur
     * une ligne, valeur jamais tronquée (elle peut passer à la ligne).
     */
    private fun checkRows(a: MainActivityV2) {
        for (t in listOf(R.id.rowTheme, R.id.rowDefaultRange).flatMap { texts(a.findViewById(it)) }) {
            val layout = t.layout
            assertNotNull("texte non mesuré : ${t.text}", layout)
            for (l in 0 until layout.lineCount) assertEquals("texte tronqué : ${t.text}", 0, layout.getEllipsisCount(l))
            val breaks = t.text.toString().count { it == ' ' || it == '\u00A0' }
            assertTrue("mot coupé : ${t.text} (${layout.lineCount} lignes)", layout.lineCount <= breaks + 1)
        }
        for (id in listOf(R.id.textVersion, R.id.textBuild)) {
            val row = a.findViewById<View>(id).parent as android.view.ViewGroup
            val label = row.getChildAt(0) as TextView
            assertEquals("libellé ${label.text} sur une ligne", 1, label.layout.lineCount)
            val value = a.findViewById<TextView>(id)
            for (l in 0 until value.layout.lineCount) assertEquals("valeur tronquée : ${value.text}", 0, value.layout.getEllipsisCount(l))
        }
    }

    private fun value(a: MainActivityV2, id: Int) = a.findViewById<TextView>(id).text.toString()

    /** Ouvre la liste à choix unique d'une ligne et choisit [label] (comme un appui). */
    private fun choose(a: MainActivityV2, row: Int, label: String, shot: String? = null) {
        a.findViewById<View>(row).performClick(); idle()
        val d = org.robolectric.shadows.ShadowDialog.getLatestDialog() as androidx.appcompat.app.AlertDialog
        assertTrue(d.isShowing)
        if (shot != null) captureDialog(d, shot)
        val list = d.listView
        val i = (0 until list.adapter.count).first { list.adapter.getItem(it).toString() == label }
        list.performItemClick(null, i, i.toLong()); idle()
        assertFalse("la liste se ferme après le choix", d.isShowing)
    }

    private fun captureDialog(d: android.app.Dialog, name: String) {
        idle()
        val root = d.window!!.decorView
        if (root.width == 0 || root.height == 0) { println("aperçu $name : fenêtre de dialogue non mesurée"); return }
        val bmp = android.graphics.Bitmap.createBitmap(root.width, root.height, android.graphics.Bitmap.Config.ARGB_8888)
        root.draw(android.graphics.Canvas(bmp))
        val dir = File("build/screenshots").apply { mkdirs() }
        java.io.FileOutputStream(File(dir, "$name.png")).use { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** Attente d'un libellé, avec le libellé réellement affiché en cas d'échec. */
    private fun waitText(v: TextView, expected: String) {
        try {
            waitFor { v.text.toString() == expected }
        } catch (e: AssertionError) {
            throw AssertionError("attendu « $expected », affiché « ${v.text} » (activé=${v.isEnabled})", e)
        }
    }

    private fun sharedFile(started: Intent): Pair<Intent, File> {
        assertEquals(Intent.ACTION_CHOOSER, started.action)
        @Suppress("DEPRECATION")
        val send = started.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
        assertEquals(Intent.ACTION_SEND, send.action)
        assertEquals("text/csv", send.type)
        assertTrue(send.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        @Suppress("DEPRECATION")
        val uri = send.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)!!
        assertEquals("content", uri.scheme)
        assertEquals("${ctx.packageName}.fileprovider", uri.authority)
        // cache-path « cache » = cacheDir (res/xml/file_paths.xml)
        val file = File(ctx.cacheDir, uri.path!!.removePrefix("/cache/"))
        assertTrue("fichier partagé présent : $uri", file.isFile)
        return send to file
    }

    @Test fun reglages_par_defaut_clair() {
        val a = launch()
        settings(a)
        assertEquals("Système", value(a, R.id.tvThemeValue))
        assertEquals("Ce mois", value(a, R.id.tvDefaultRangeValue))
        assertEquals("3 ticket(s) enregistré(s)", a.findViewById<TextView>(R.id.tvSettingsTicketCount).text.toString())
        @Suppress("DEPRECATION")
        val info = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
        assertEquals(info.versionName, a.findViewById<TextView>(R.id.textVersion).text.toString())
        @Suppress("DEPRECATION")
        assertEquals(info.versionCode.toString(), a.findViewById<TextView>(R.id.textBuild).text.toString())
        assertTrue("version du lot 10 : ${info.versionName}", info.versionName.startsWith("1.10.0-lot10"))
        assertFalse("suppression globale désactivée", a.findViewById<View>(R.id.btnClearAll).isEnabled)
        assertTrue(a.findViewById<View>(R.id.btnExportAllCsv).isEnabled)
        checkRows(a)
        capture(a, "l10_01_reglages_haut_light")
        scrollBottom(a)
        capture(a, "l10_02_reglages_bas_light")
        a.findViewById<View>(R.id.btnClearAll).performClick(); idle()
        unchanged()
    }

    @Test @Config(qualifiers = "+night")
    fun reglages_sombre() {
        val a = launch()
        settings(a)
        checkRows(a)
        capture(a, "l10_03_reglages_haut_dark")
        scrollBottom(a)
        capture(a, "l10_04_reglages_bas_dark")
        unchanged()
    }

    @Test @Config(qualifiers = "w320dp-h569dp-hdpi")
    fun reglages_320dp_police_2() {
        RuntimeEnvironment.setFontScale(2.0f)
        val a = launch()
        settings(a)
        checkRows(a)
        capture(a, "l10_05_reglages_320dp_police_2_haut_light")
        a.findViewById<View>(R.id.rowDefaultRange).performClick(); idle()
        val d = org.robolectric.shadows.ShadowDialog.getLatestDialog() as androidx.appcompat.app.AlertDialog
        captureDialog(d, "l10_11_choix_periode_320dp_police_2_light")
        d.getButton(android.content.DialogInterface.BUTTON_NEGATIVE).performClick(); idle()
        assertEquals(TimeRange.MONTH, AppPreferences(ctx).defaultRange)
        scrollBottom(a)
        capture(a, "l10_06_reglages_320dp_police_2_bas_light")
    }

    @Test @Config(qualifiers = "w320dp-h569dp-night-hdpi")
    fun reglages_320dp_police_2_sombre() {
        RuntimeEnvironment.setFontScale(2.0f)
        val a = launch()
        settings(a)
        checkRows(a)
        capture(a, "l10_07_reglages_320dp_police_2_haut_dark")
    }

    /**
     * Choix existant de l'ancien bouton (clair) conservé et affiché ; liste Système / Clair / Sombre avec le choix
     * actuel coché ; « Annuler » ne change rien ; Sombre puis Système enregistrés et transmis à AppCompat, relus à
     * la réouverture. L'apparence réellement appliquée (écran recréé en sombre) est vérifiée sur émulateur (r01).
     */
    @Test fun theme_existant_conserve_puis_choix() {
        SessionManager(ctx).setThemeMode(AppCompatDelegate.MODE_NIGHT_NO)
        val a = launch()
        settings(a)
        assertEquals("Clair", value(a, R.id.tvThemeValue))
        assertEquals(AppCompatDelegate.MODE_NIGHT_NO, SessionManager(ctx).getThemeMode())

        // « Annuler » ne change rien
        a.findViewById<View>(R.id.rowTheme).performClick(); idle()
        val d = org.robolectric.shadows.ShadowDialog.getLatestDialog() as androidx.appcompat.app.AlertDialog
        assertEquals(listOf("Système", "Clair", "Sombre"), (0 until d.listView.adapter.count).map { d.listView.adapter.getItem(it).toString() })
        assertEquals("choix actuel coché", 1, d.listView.checkedItemPosition)
        captureDialog(d, "l10_09_choix_theme_light")
        d.getButton(android.content.DialogInterface.BUTTON_NEGATIVE).performClick(); idle()
        assertEquals(AppCompatDelegate.MODE_NIGHT_NO, SessionManager(ctx).getThemeMode())

        choose(a, R.id.rowTheme, "Sombre")
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, SessionManager(ctx).getThemeMode())
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, AppCompatDelegate.getDefaultNightMode())

        val b = launch()
        settings(b)
        assertEquals("Sombre", value(b, R.id.tvThemeValue))
        choose(b, R.id.rowTheme, "Système")
        assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, SessionManager(ctx).getThemeMode())
        assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, AppCompatDelegate.getDefaultNightMode())
        unchanged()
    }

    /**
     * Période par défaut : lue à l'ouverture de l'Accueil, des Catégories et des Magasins ; changer le réglage ne
     * modifie pas les écrans déjà ouverts ; un choix fait sur un écran n'est pas réinitialisé en changeant d'onglet.
     */
    @Test fun periode_par_defaut() {
        val a = launch()
        assertEquals(R.id.btnHomeMonth, checked(a, R.id.homeTogglePeriod))
        a.findViewById<View>(R.id.btnHomeToday).performClick(); idle()
        settings(a)
        choose(a, R.id.rowDefaultRange, "Cette année", shot = "l10_10_choix_periode_light")
        assertEquals("Cette année", value(a, R.id.tvDefaultRangeValue))
        assertEquals(TimeRange.YEAR, AppPreferences(ctx).defaultRange)
        a.onBackPressedDispatcher.onBackPressed(); idle()
        assertEquals("Accueil déjà ouvert : sélection conservée", R.id.btnHomeToday, checked(a, R.id.homeTogglePeriod))

        val nav = a.findViewById<BottomNavigationView>(R.id.bottomNav)
        nav.selectedItemId = R.id.menu_category; idle()
        assertEquals("Catégories déjà ouvertes : inchangées", R.id.btnCatMonth, checked(a, R.id.togglePeriodCategory))
        nav.selectedItemId = R.id.menu_stores; idle()
        assertEquals("Magasins déjà ouverts : inchangés", R.id.btnPeriodMonth, checked(a, R.id.togglePeriod))

        // Choix conservé en changeant d'onglet
        nav.selectedItemId = R.id.menu_history; idle()
        nav.selectedItemId = R.id.menu_home; idle()
        assertEquals("pas de réinitialisation au retour", R.id.btnHomeToday, checked(a, R.id.homeTogglePeriod))
        assertEquals(TimeRange.YEAR, AppPreferences(ctx).defaultRange)

        // Nouvelle ouverture de l'app : période par défaut sur les trois écrans
        val b = launch()
        assertEquals(R.id.btnHomeYear, checked(b, R.id.homeTogglePeriod))
        val navB = b.findViewById<BottomNavigationView>(R.id.bottomNav)
        navB.selectedItemId = R.id.menu_category; idle()
        assertEquals(R.id.btnCatYear, checked(b, R.id.togglePeriodCategory))
        navB.selectedItemId = R.id.menu_stores; idle()
        assertEquals(R.id.btnPeriodYear, checked(b, R.id.togglePeriod))
        unchanged()
    }

    /** Écran recréé (rotation, thème) : la période choisie est conservée, même si le réglage a changé entre-temps. */
    @Test fun periode_conservee_apres_recreation() {
        AppPreferences(ctx).defaultRange = TimeRange.MONTH
        val c = Robolectric.buildActivity(MainActivityV2::class.java).also { controllers += it }.setup(); idle()
        c.get().findViewById<View>(R.id.btnHomeYear).performClick(); idle()
        AppPreferences(ctx).defaultRange = TimeRange.TODAY
        c.recreate(); idle()
        assertEquals(R.id.btnHomeYear, checked(c.get(), R.id.homeTogglePeriod))
    }

    /** Collecte des journaux de plantage inactive (ETixApp non déclarée) : indisponibilité affichée, actions désactivées. */
    @Test fun journaux_de_plantage_indisponibles() {
        val a = launch()
        settings(a)
        assertFalse(com.etix.CrashLogs.isCollectionActive(ctx))
        assertEquals("Journaux de plantage indisponibles : leur collecte n’est pas active dans cette version. Aucun journal n’est enregistré ni envoyé.",
            value(a, R.id.tvCrashNote))
        assertFalse(a.findViewById<View>(R.id.btnShowCrash).isEnabled)
        assertFalse(a.findViewById<View>(R.id.btnClearCrash).isEnabled)
    }

    @Test fun export_de_tous_les_tickets() {
        val a = launch()
        settings(a)
        a.findViewById<View>(R.id.btnExportAllCsv).performClick()
        waitFor { shadowOf(a).peekNextStartedActivity() != null }
        val (send, file) = sharedFile(shadowOf(a).nextStartedActivity)
        assertTrue(file.name.startsWith("eTix_tous_les_tickets_"))
        val bytes = file.readBytes()
        val text = String(bytes, Charsets.UTF_8)
        assertEquals("UTF-8 sans BOM", 'D'.code.toByte(), bytes[0])
        assertEquals(CsvExporter.toCsv(seeded), text)
        assertTrue(text.contains("\"Durand, fils\",1234.56,Maison,\"Vis \"\"inox\"\"\""))
        assertTrue(text.contains("Marché,0.05,Courses,\"ligne 1\nligne 2\""))
        assertTrue("formule neutralisée dans le fichier", text.contains("Café de l'Été,4.50,Loisirs,'=SOMME(A1:A3)\n"))
        assertEquals(file.name, send.getStringExtra(Intent.EXTRA_SUBJECT))
        unchanged()
    }

    /** Historique : seuls les tickets affichés (recherche active) sont exportés ; libellé explicite. */
    @Test fun export_historique_tickets_affiches() {
        val a = launch()
        a.findViewById<BottomNavigationView>(R.id.bottomNav).selectedItemId = R.id.menu_history; idle()
        val history = a.supportFragmentManager.findFragmentByTag("f2")!!.requireView()
        val btn = history.findViewById<Button>(R.id.btnExportCsv)
        waitText(btn, "Exporter les 3 tickets affichés (CSV)")
        history.findViewById<EditText>(R.id.inputSearch).setText("Durand"); idle()
        waitText(btn, "Exporter le ticket affiché (CSV)")
        capture(a, "l10_08_historique_export_filtre_light")
        assertTrue("bouton actif", btn.isEnabled && btn.hasOnClickListeners())
        val uncaught = java.util.Collections.synchronizedList(mutableListOf<Throwable>())
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { _, e -> uncaught += e }
        try {
            btn.performClick()
            waitFor { shadowOf(a).peekNextStartedActivity() != null || uncaught.isNotEmpty() }
        } catch (e: AssertionError) {
            val toast = org.robolectric.shadows.ShadowToast.getTextOfLatestToast()
            val files = File(ctx.cacheDir, "exports").list()?.toList()
            val app = shadowOf(ctx).peekNextStartedActivity()
            throw AssertionError("aucun partage lancé ; dernier toast « $toast », fichiers $files, application : $app", e)
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(previous)
        }
        if (uncaught.isNotEmpty()) throw AssertionError("exception pendant l'export : ${uncaught.first()}", uncaught.first())
        val (_, file) = sharedFile(shadowOf(a).nextStartedActivity)
        assertTrue(file.name.startsWith("eTix_tickets_affiches_"))
        assertEquals(CsvExporter.toCsv(seeded.filter { it.id == 2L }), file.readText())

        history.findViewById<EditText>(R.id.inputSearch).setText("introuvable"); idle()
        waitFor { !btn.isEnabled }
        unchanged()
    }
}
