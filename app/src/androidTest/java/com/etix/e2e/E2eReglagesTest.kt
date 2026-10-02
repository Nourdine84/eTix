package com.etix.e2e

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.net.Uri
import android.os.SystemClock
import androidx.appcompat.app.AppCompatDelegate
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.etix.R
import com.etix.data.AppDatabase
import com.etix.e2e.E2e.ctx
import com.etix.e2e.E2e.shot
import com.etix.e2e.E2e.waitFor
import com.etix.features.history.HistoryRules
import com.etix.features.settings.AppPreferences
import com.etix.features.settings.ThemeChoice
import com.etix.features.store.TimeRange
import com.etix.model.Ticket
import com.etix.ui.main.MainActivityV2
import com.etix.utils.CsvExporter
import com.etix.utils.SessionManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

/** Outils des tests des Réglages (lot 10). Données fictives uniquement. */
internal object ReglagesE2e {
    private val instr get() = InstrumentationRegistry.getInstrumentation()
    val dao get() = AppDatabase.getInstance(ctx).ticketDao()

    fun startMain(label: String) {
        ctx.startActivity(Intent(ctx, MainActivityV2::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        E2e.waitForAppReady(label); waitFor(withId(R.id.bottomNav))
    }

    fun openSettings() {
        onView(withId(R.id.menu_home)).perform(click())
        onView(withId(R.id.btnSettings)).perform(click())
        waitFor(withId(R.id.rowTheme))
    }

    fun choosePeriod(label: String) {
        onView(withId(R.id.rowDefaultRange)).perform(click())
        onView(withText(label)).inRoot(isDialog()).perform(click())
        waitFor(allOf(withId(R.id.tvDefaultRangeValue), withText(label)))
    }

    /** Tickets et budgets (le thème et la période, eux, changent volontairement pendant ces tests). */
    fun data(): String {
        val tickets = runBlocking { dao.getAllFlow().first() }.sortedBy { it.id }
        val budgets = ctx.getSharedPreferences("etix_budgets", Context.MODE_PRIVATE).all.toSortedMap()
        return tickets.joinToString("\n") + "\n" + budgets
    }

    /** Thème réellement appliqué à l'activité au premier plan. */
    fun nightNow(): Boolean {
        var night = false
        instr.runOnMainSync {
            val act = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).first()
            night = (act.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        }
        return night
    }

    fun systemNight() =
        (Resources.getSystem().configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    /** Attend l'écriture effective sur disque d'une préférence (fichier XML des SharedPreferences). */
    fun waitPrefOnDisk(file: String, fragment: String) {
        val f = File(File(ctx.filesDir.parentFile, "shared_prefs"), "$file.xml")
        val end = SystemClock.uptimeMillis() + 5000
        while (SystemClock.uptimeMillis() < end) {
            if (f.isFile && f.readText().contains(fragment)) return
            SystemClock.sleep(100)
        }
        throw AssertionError("$fragment absent de ${f.name} : ${if (f.isFile) f.readText() else "fichier absent"}")
    }

    fun log(line: String) = ScanE2e.log("reglages", line)

    /** Contenu réellement transmis : lu par l'URI du partage, comme le ferait l'application destinataire. */
    fun sharedContent(chooser: Intent): Pair<Intent, ByteArray> {
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        @Suppress("DEPRECATION")
        val send = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
        assertEquals(Intent.ACTION_SEND, send.action)
        assertEquals("text/csv", send.type)
        assertTrue("lecture accordée au destinataire", send.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        @Suppress("DEPRECATION")
        val uri = send.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)!!
        assertEquals("content", uri.scheme)
        assertEquals("${ctx.packageName}.fileprovider", uri.authority)
        val bytes = ctx.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
        return send to bytes
    }

    fun lastChooser(): Intent {
        val end = SystemClock.uptimeMillis() + 10_000
        while (SystemClock.uptimeMillis() < end) {
            Intents.getIntents().lastOrNull { it.action == Intent.ACTION_CHOOSER }?.let { return it }
            SystemClock.sleep(100)
        }
        throw AssertionError("aucune feuille de partage demandée")
    }
}

/**
 * Lot 10 sur émulateur : thème Système / Clair / Sombre, période par défaut (Accueil, Catégories, Magasins ; ni
 * réinitialisation entre onglets, ni effet sur les filtres de l'Historique), export CSV de tous les tickets et des
 * tickets affichés (contenu lu par l'URI partagée), vraie feuille de partage annulée. Tickets et budgets inchangés
 * (hors tickets fictifs ajoutés par le test). Prérequis : session ouverte (E2eParcoursTest déjà exécuté).
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class E2eReglagesTest {

    private val dao get() = ReglagesE2e.dao

    /** Tickets délicats pour l'export (fictifs, préfixe « E2E CSV »). */
    private fun ensureFixtures(): List<Ticket> {
        val existing = runBlocking { dao.getAllFlow().first() }.filter { it.store.startsWith("E2E CSV") }
        if (existing.size == 4) return existing
        val now = System.currentTimeMillis()
        val old = Calendar.getInstance().apply { add(Calendar.MONTH, -2); set(Calendar.DAY_OF_MONTH, 3) }.timeInMillis
        runBlocking {
            dao.insert(Ticket(store = "E2E CSV Café de l'Été", amount = 4.5, category = "E2E Réglages", dateMillis = now - 60_000))
            dao.insert(Ticket(store = "E2E CSV Durand, fils", amount = 1234.56, category = "E2E Réglages",
                description = "Vis \"inox\"", dateMillis = now - 120_000))
            dao.insert(Ticket(store = "E2E CSV Marché", amount = 0.05, category = "E2E Réglages",
                description = "ligne 1\nligne 2", dateMillis = now - 180_000))
            dao.insert(Ticket(store = "E2E CSV Ancien", amount = 12.0, category = "E2E Réglages",
                description = "=SOMME(A1:A2)", dateMillis = old))
        }
        return runBlocking { dao.getAllFlow().first() }.filter { it.store.startsWith("E2E CSV") }
    }

    private fun day(t: Ticket) = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(Date(t.dateMillis))

    @Test
    fun r01_theme_trois_etats() {
        val before = ReglagesE2e.data()
        ReglagesE2e.startMain("E2eReglagesTest r01")
        ReglagesE2e.openSettings()
        val initial = ThemeChoice.fromMode(SessionManager(ctx).getThemeMode())
        waitFor(allOf(withId(R.id.tvThemeValue), withText(initial.label)))
        ReglagesE2e.log("thème initial : ${initial.label} (mode ${SessionManager(ctx).getThemeMode()})")

        E2e.chooseTheme("Sombre")
        waitFor(allOf(withId(R.id.tvThemeValue), withText("Sombre")))
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, SessionManager(ctx).getThemeMode())
        assertTrue("thème sombre appliqué", ReglagesE2e.nightNow())
        shot("r01_reglages_sombre_haut")
        onView(withId(R.id.btnClearCrash)).perform(scrollTo())
        shot("r01_reglages_sombre_bas")

        onView(withId(R.id.rowTheme)).perform(scrollTo())
        E2e.chooseTheme("Clair")
        waitFor(allOf(withId(R.id.tvThemeValue), withText("Clair")))
        assertEquals(AppCompatDelegate.MODE_NIGHT_NO, SessionManager(ctx).getThemeMode())
        assertTrue("thème clair appliqué", !ReglagesE2e.nightNow())
        shot("r01_reglages_clair_haut")
        onView(withId(R.id.btnClearCrash)).perform(scrollTo())
        shot("r01_reglages_clair_bas")

        onView(withId(R.id.rowTheme)).perform(scrollTo())
        E2e.chooseTheme("Système")
        waitFor(allOf(withId(R.id.tvThemeValue), withText("Système")))
        assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, SessionManager(ctx).getThemeMode())
        assertEquals("Système : thème du téléphone", ReglagesE2e.systemNight(), ReglagesE2e.nightNow())
        shot("r01_reglages_systeme")

        // Annuler la liste ne change rien
        onView(withId(R.id.rowTheme)).perform(click())
        onView(withText("Annuler")).inRoot(isDialog()).perform(click())
        waitFor(allOf(withId(R.id.tvThemeValue), withText("Système")))
        assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, SessionManager(ctx).getThemeMode())

        // Compteur, version et build, suppression globale désactivée
        val n = runBlocking { dao.getAllFlow().first() }.size
        onView(withId(R.id.tvSettingsTicketCount)).perform(scrollTo())
        waitFor(allOf(withId(R.id.tvSettingsTicketCount), withText("$n ticket(s) enregistré(s)")))
        @Suppress("DEPRECATION")
        val info = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
        onView(withId(R.id.textVersion)).perform(scrollTo()).check(matches(withText(info.versionName)))
        @Suppress("DEPRECATION")
        onView(withId(R.id.textBuild)).check(matches(withText(info.versionCode.toString())))
        assertTrue("version 1.10.0-lot10 attendue : ${info.versionName}", info.versionName.startsWith("1.10.0-lot10"))
        onView(withId(R.id.btnClearAll)).perform(scrollTo()).check(matches(not(isEnabled())))
        onView(withId(R.id.btnClearAll)).perform(click())
        ReglagesE2e.log("version ${info.versionName}, build ${info.versionCode}, $n tickets")
        pressBack()
        waitFor(withId(R.id.tvTicketCount))
        assertEquals("tickets et budgets inchangés", before, ReglagesE2e.data())
    }

    @Test
    fun r02_periode_par_defaut() {
        val before = ReglagesE2e.data()
        ReglagesE2e.startMain("E2eReglagesTest r02")
        // Sélection faite sur l'Accueil avant de changer le réglage
        onView(allOf(withId(R.id.btnHomeToday), isDisplayed())).perform(click())
        waitFor(allOf(withId(R.id.btnHomeToday), isChecked()))
        val all = runBlocking { dao.getAllFlow().first() }.size

        ReglagesE2e.openSettings()
        ReglagesE2e.choosePeriod("Cette année")
        assertEquals(TimeRange.YEAR, AppPreferences(ctx).defaultRange)
        shot("r02_reglages_periode_annee")
        pressBack()
        // Écrans déjà ouverts : sélections inchangées
        waitFor(allOf(withId(R.id.btnHomeToday), isChecked()))
        onView(withId(R.id.menu_category)).perform(click())
        waitFor(allOf(withId(R.id.btnCatMonth), isChecked()))
        onView(withId(R.id.menu_stores)).perform(click())
        waitFor(allOf(withId(R.id.btnPeriodMonth), isChecked()))
        // Historique : aucun filtre ajouté, tous les tickets affichés et exportables
        onView(withId(R.id.menu_history)).perform(click())
        waitFor(allOf(withId(R.id.btnExportCsv), withText("Exporter les $all tickets affichés (CSV)")))
        onView(withId(R.id.tvFilterSummary)).check(matches(not(isDisplayed())))
        // Retour sur l'Accueil : sélection toujours conservée
        onView(withId(R.id.menu_home)).perform(click())
        waitFor(allOf(withId(R.id.btnHomeToday), isChecked()))
        shot("r02_accueil_choix_conserve")

        // Nouvelle ouverture de l'app : période par défaut sur les trois écrans
        ReglagesE2e.startMain("E2eReglagesTest r02 réouverture")
        waitFor(allOf(withId(R.id.btnHomeYear), isChecked()))
        shot("r02_accueil_reouverture_annee")
        onView(withId(R.id.menu_category)).perform(click())
        waitFor(allOf(withId(R.id.btnCatYear), isChecked()))
        onView(withId(R.id.menu_stores)).perform(click())
        waitFor(allOf(withId(R.id.btnPeriodYear), isChecked()))

        // Rétablissement : « Ce mois » (l'Accueil ouvert garde « Cette année »)
        ReglagesE2e.openSettings()
        ReglagesE2e.choosePeriod("Ce mois")
        pressBack()
        waitFor(allOf(withId(R.id.btnHomeYear), isChecked()))
        assertEquals(TimeRange.MONTH, AppPreferences(ctx).defaultRange)
        assertEquals("tickets et budgets inchangés", before, ReglagesE2e.data())
    }

    @Test
    fun r03_export_de_tous_les_tickets() {
        val fixtures = ensureFixtures()
        val before = ReglagesE2e.data()
        Intents.init()
        try {
            intending(hasAction(Intent.ACTION_CHOOSER)).respondWith(Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null))
            ReglagesE2e.startMain("E2eReglagesTest r03")
            ReglagesE2e.openSettings()
            onView(withId(R.id.btnExportAllCsv)).perform(scrollTo(), click())
            val (send, bytes) = ReglagesE2e.sharedContent(ReglagesE2e.lastChooser())
            val text = String(bytes, Charsets.UTF_8)
            val all = runBlocking { dao.getAllFlow().first() }
            assertEquals("UTF-8 sans BOM, en-tête en premier", 'D'.code.toByte(), bytes[0])
            assertEquals(CsvExporter.toCsv(all), text)
            assertEquals(all.size + 1, Regex("\n(?=\\d{2}/\\d{2}/\\d{4},|$)").findAll(text).count())
            val f = fixtures.associateBy { it.store }
            assertTrue(text.startsWith("Date,Magasin,Montant (€),Catégorie,Description\n"))
            assertTrue(text.contains("${day(f.getValue("E2E CSV Café de l'Été"))},E2E CSV Café de l'Été,4.50,E2E Réglages,\n"))
            assertTrue(text.contains("${day(f.getValue("E2E CSV Durand, fils"))},\"E2E CSV Durand, fils\",1234.56,E2E Réglages,\"Vis \"\"inox\"\"\"\n"))
            assertTrue(text.contains("${day(f.getValue("E2E CSV Marché"))},E2E CSV Marché,0.05,E2E Réglages,\"ligne 1\nligne 2\"\n"))
            // Formule neutralisée dans le fichier, description enregistrée inchangée
            assertTrue(text.contains("${day(f.getValue("E2E CSV Ancien"))},E2E CSV Ancien,12.00,E2E Réglages,'=SOMME(A1:A2)\n"))
            assertEquals("=SOMME(A1:A2)", all.first { it.store == "E2E CSV Ancien" }.description)
            assertTrue("nom du fichier : ${send.getStringExtra(Intent.EXTRA_SUBJECT)}",
                send.getStringExtra(Intent.EXTRA_SUBJECT)!!.matches(Regex("eTix_tous_les_tickets_\\d+\\.csv")))
            File(File(ctx.filesDir, "shots").apply { mkdirs() }, "export_tous_les_tickets.csv").writeBytes(bytes)
            ReglagesE2e.log("export Réglages : ${all.size} tickets, ${bytes.size} octets, fichier ${send.getStringExtra(Intent.EXTRA_SUBJECT)}")
            waitFor(withId(R.id.rowTheme))
            shot("r03_reglages_apres_export")
        } finally {
            Intents.release()
        }
        assertEquals("tickets et budgets inchangés", before, ReglagesE2e.data())
    }

    @Test
    fun r04_export_historique_tickets_affiches() {
        ensureFixtures()
        val before = ReglagesE2e.data()
        Intents.init()
        try {
            intending(hasAction(Intent.ACTION_CHOOSER)).respondWith(Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null))
            ReglagesE2e.startMain("E2eReglagesTest r04")
            onView(withId(R.id.menu_history)).perform(click())
            onView(allOf(withId(R.id.inputSearch), isDisplayed())).perform(replaceText("E2E CSV"))
            E2e.closeKeyboard()
            waitFor(allOf(withId(R.id.btnExportCsv), withText("Exporter les 4 tickets affichés (CSV)")))
            shot("r04_historique_export_filtre")
            onView(allOf(withId(R.id.btnExportCsv), isDisplayed())).perform(click())
            val (send, bytes) = ReglagesE2e.sharedContent(ReglagesE2e.lastChooser())
            val all = runBlocking { dao.getAllFlow().first() }
            val shown = HistoryRules.filter(all, "E2E CSV", null, null)
            assertEquals(4, shown.size)
            assertEquals(CsvExporter.toCsv(shown), String(bytes, Charsets.UTF_8))
            assertTrue(send.getStringExtra(Intent.EXTRA_SUBJECT)!!.startsWith("eTix_tickets_affiches_"))
            File(File(ctx.filesDir, "shots").apply { mkdirs() }, "export_tickets_affiches.csv").writeBytes(bytes)
            ReglagesE2e.log("export Historique (recherche « E2E CSV ») : ${shown.size} tickets sur ${all.size}")
            onView(allOf(withId(R.id.inputSearch), isDisplayed())).perform(replaceText(""))
            E2e.closeKeyboard()
        } finally {
            Intents.release()
        }
        assertEquals("tickets et budgets inchangés", before, ReglagesE2e.data())
    }

    /** Vraie feuille de partage du système, annulée par Retour : aucune donnée modifiée, retour aux Réglages. */
    @Test
    fun r05_partage_reel_annule() {
        val before = ReglagesE2e.data()
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        ReglagesE2e.startMain("E2eReglagesTest r05")
        ReglagesE2e.openSettings()
        onView(withId(R.id.btnExportAllCsv)).perform(scrollTo(), click())
        val shown = device.wait(Until.hasObject(By.pkg(Pattern.compile("android|com\\.android\\.intentresolver"))), 10_000)
        assertTrue("feuille de partage du système affichée", shown == true)
        SystemClock.sleep(800)
        shot("r05_feuille_de_partage")
        ReglagesE2e.log("feuille de partage : ${device.currentPackageName}")
        device.pressBack()
        if (!device.wait(Until.hasObject(By.pkg(ctx.packageName).depth(0)), 5_000)) device.pressBack()
        waitFor(withId(R.id.rowTheme))
        shot("r05_retour_apres_annulation")
        assertEquals("annuler le partage ne modifie aucune donnée", before, ReglagesE2e.data())
    }
}

/** Persistance (1/2) : Sombre + « Cette année » choisis, écrits sur disque ; la CI arrête ensuite l'app. */
@RunWith(AndroidJUnit4::class)
class E2eReglagesAvantRedemarrageTest {
    @Test
    fun choix_enregistres_avant_arret() {
        ReglagesE2e.startMain("E2eReglagesAvantRedemarrageTest")
        ReglagesE2e.openSettings()
        E2e.chooseTheme("Sombre")
        waitFor(allOf(withId(R.id.tvThemeValue), withText("Sombre")))
        ReglagesE2e.choosePeriod("Cette année")
        ReglagesE2e.waitPrefOnDisk("etix_session", "name=\"theme_mode\" value=\"${AppCompatDelegate.MODE_NIGHT_YES}\"")
        ReglagesE2e.waitPrefOnDisk(AppPreferences.PREFS, ">YEAR<")
        ReglagesE2e.log("avant arrêt : Sombre, Cette année (écrits sur disque)")
    }
}

/** Persistance (2/2) : après arrêt complet de l'app, choix relus et appliqués ; puis valeurs par défaut rétablies. */
@RunWith(AndroidJUnit4::class)
class E2eReglagesApresRedemarrageTest {
    @Test
    fun choix_conserves_apres_redemarrage() {
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, SessionManager(ctx).getThemeMode())
        assertEquals(TimeRange.YEAR, AppPreferences(ctx).defaultRange)
        ReglagesE2e.startMain("E2eReglagesApresRedemarrageTest")
        assertTrue("thème sombre appliqué au redémarrage", ReglagesE2e.nightNow())
        waitFor(allOf(withId(R.id.btnHomeYear), isChecked()))
        shot("r06_accueil_apres_redemarrage")
        onView(withId(R.id.menu_category)).perform(click())
        waitFor(allOf(withId(R.id.btnCatYear), isChecked()))
        onView(withId(R.id.menu_stores)).perform(click())
        waitFor(allOf(withId(R.id.btnPeriodYear), isChecked()))
        ReglagesE2e.openSettings()
        waitFor(allOf(withId(R.id.tvThemeValue), withText("Sombre")))
        waitFor(allOf(withId(R.id.tvDefaultRangeValue), withText("Cette année")))
        shot("r06_reglages_apres_redemarrage")
        ReglagesE2e.log("après redémarrage : Sombre, Cette année relus et appliqués")
        // Valeurs par défaut rétablies (Système, Ce mois)
        E2e.chooseTheme("Système")
        waitFor(allOf(withId(R.id.tvThemeValue), withText("Système")))
        ReglagesE2e.choosePeriod("Ce mois")
        assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, SessionManager(ctx).getThemeMode())
        assertEquals(TimeRange.MONTH, AppPreferences(ctx).defaultRange)
    }
}

/**
 * Mise à jour (job maj) : le thème enregistré par la version de base est conservé et affiché par les Réglages ;
 * aucune période enregistrée → « Ce mois ». Lecture seule (aucun choix modifié).
 */
@RunWith(AndroidJUnit4::class)
class E2eMajReglagesApresTest {
    @Test
    fun reglages_apres_mise_a_jour() {
        val saved = MajDonnees.savedThemeMode()
        val expected = saved ?: AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        assertEquals("thème de la version de base conservé", expected, SessionManager(ctx).getThemeMode())
        ReglagesE2e.startMain("E2eMajReglagesApresTest")
        when (expected) {
            AppCompatDelegate.MODE_NIGHT_YES -> assertTrue(ReglagesE2e.nightNow())
            AppCompatDelegate.MODE_NIGHT_NO -> assertTrue(!ReglagesE2e.nightNow())
        }
        waitFor(allOf(withId(R.id.btnHomeMonth), isChecked()))
        ReglagesE2e.openSettings()
        val label = ThemeChoice.fromMode(expected).label
        waitFor(allOf(withId(R.id.tvThemeValue), withText(label)))
        waitFor(allOf(withId(R.id.tvDefaultRangeValue), withText("Ce mois")))
        @Suppress("DEPRECATION")
        val code = ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionCode
        onView(withId(R.id.textBuild)).perform(scrollTo()).check(matches(withText(code.toString())))
        shot("m01_reglages_apres_maj")
        assertEquals(expected, SessionManager(ctx).getThemeMode())
        ScanE2e.log("maj_donnees", "Réglages après mise à jour : thème « $label » (enregistré : ${saved ?: "aucun"}), période « Ce mois », build $code")
    }
}
