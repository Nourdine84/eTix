package com.etix.e2e

import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.view.View
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.pressImeActionButton
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.etix.R
import com.etix.data.AppDatabase
import com.etix.data.BudgetStore
import com.etix.e2e.E2e.ctx
import com.etix.e2e.E2e.shot
import com.etix.e2e.E2e.waitFor
import com.etix.model.Ticket
import com.etix.ui.main.MainActivityV2
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.io.File
import java.util.Calendar
import java.util.Locale

/** Cas « montants exacts » : budget, libellé et montants attendus, nom de la capture. */
private data class Quad(val limit: Double, val status: String, val amounts: String, val name: String)

/**
 * Revue visuelle de Catégories et des budgets mensuels (captures seulement, aucun changement de l'app), émulateur en
 * français, clair puis sombre. Données FICTIVES ISOLÉES : les tickets et budgets présents (ceux de la revue de
 * l'Accueil) sont mis de côté, remplacés par le jeu de ce test, puis rétablis à l'identique à la fin (même en cas
 * d'échec) ; les classes suivantes du mode « revue » les retrouvent inchangés.
 *
 * Parcours : liste vide (Catégories et réglage), liste remplie sans budget (noms longs, invitation), création d'un
 * budget (montant invalide, validation par la touche du clavier), modification (préremplissage, « Appliquer » touché
 * clavier ouvert), annulation de la saisie, budgets sous le seuil d'alerte, en alerte, à exactement 100 %, dépassé,
 * partagé entre deux catégories ne différant que par la casse, confirmation de suppression annulée puis confirmée.
 * Montants juste sous le budget, exactement égaux et juste au-dessus (« Budget atteint » seulement à égalité exacte,
 * décision du 09/10/2026), y compris quand le pourcentage arrondi affiche 100 %.
 * BLOQUANT : annulations sans effet, suppression limitée au budget visé (tickets et autres budgets intacts), actions
 * de la saisie accessibles clavier ouvert, barre d'onglets masquée pendant la saisie et rétablie à la sortie, boutons
 * de la confirmation ENTIÈREMENT visibles puis réellement touchés (effet vérifié), fin du message accessible.
 * Mesuré (shots/mesures_budgets.txt) puis BLOQUANT après toutes les captures : lisibilité des textes (troncature, mot
 * coupé, texte masqué), haut de la saisie non rogné, message d'erreur visible. Captures prises avant les assertions.
 * Argument facultatif `passe` (« petit » : 320 dp, police 2,0 ; « moyen » : 320 dp, police 1,5) ajouté au nom des
 * captures.
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class E2eRevueBudgetsTest {

    private val instr get() = InstrumentationRegistry.getInstrumentation()
    private val passe: String? = InstrumentationRegistry.getArguments().getString("passe")
    private val dao get() = AppDatabase.getInstance(ctx).ticketDao()

    private fun allTickets() = runBlocking { dao.getAllFlow().first() }
    private fun resumed() = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).first()

    // Catégories fictives (noms longs compris) et budgets de chaque état
    private val longAlim = "Alimentation et produits frais du marché"
    private val longAbo = "Abonnements numériques et services en ligne"

    /** Dates de ce mois : maintenant moins quelques secondes, jamais avant le 1er du mois à 00:00. */
    private fun thisMonth(i: Int): Long {
        val start = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return maxOf(System.currentTimeMillis() - 5_000L * (i + 1), start)
    }

    private fun fixtureTickets(): List<Ticket> = listOf(
        Ticket(store = "Marché du Centre", amount = 60.00, category = longAlim, dateMillis = thisMonth(0)),
        Ticket(store = "Primeur des Halles", amount = 40.00, category = longAlim, dateMillis = thisMonth(1)),
        Ticket(store = "Brasserie de la Gare", amount = 42.00, category = "Restaurant", dateMillis = thisMonth(2)),
        Ticket(store = "Station du Pont", amount = 60.00, category = "Carburant", dateMillis = thisMonth(3)),
        Ticket(store = "Cinéma du Port", amount = 45.00, category = "Loisirs", dateMillis = thisMonth(4)),
        Ticket(store = "Épicerie Fictive", amount = 30.00, category = "Courses", dateMillis = thisMonth(5)),
        Ticket(store = "Épicerie Fictive", amount = 20.00, category = "courses", dateMillis = thisMonth(6)),
        Ticket(store = "Service Fictif", amount = 12.99, category = longAbo, dateMillis = thisMonth(7)),
    )

    /** Budgets des états : 50 % (sous le seuil), 84 % (alerte), 100 % pile, 150 % (dépassé), partagé 50 / 40 (125 %). */
    private val stateBudgets = mapOf(longAlim to 200.0, "Restaurant" to 50.0, "Carburant" to 60.0,
        "Loisirs" to 30.0, "Courses" to 40.0)

    private fun setBudgets(target: Map<String, Double>) {
        val store = BudgetStore(ctx)
        store.load().keys.forEach { store.set(it, null) }
        target.forEach { (c, l) -> store.set(c, l) }
    }

    private fun replaceTickets(list: List<Ticket>) = runBlocking { dao.deleteAll(); list.forEach { dao.insert(it) } }

    private fun log(line: String) {
        val c = ctx.resources.configuration
        File(File(ctx.filesDir, "shots").apply { mkdirs() }, "mesures_budgets.txt").appendText(
            "budgets ${suffixNow} $line (sdk=${Build.VERSION.SDK_INT} ${c.screenWidthDp}dp police=${c.fontScale})\n")
    }

    private var suffixNow = ""

    /** Anomalies de lisibilité ou d'accès constatées : BLOQUANTES, vérifiées après toutes les captures. */
    private val anomalies = mutableListOf<String>()

    private fun startMain() {
        ctx.startActivity(Intent(ctx, MainActivityV2::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        E2e.waitForAppReady("E2eRevueBudgetsTest"); waitFor(withId(R.id.bottomNav))
    }

    private fun chooseTheme(theme: String) {
        onView(withId(R.id.menu_home)).perform(click())
        onView(withId(R.id.btnSettings)).perform(E2e.nestedScrollTo(), click())
        waitFor(withId(R.id.rowTheme))
        E2e.chooseTheme(theme)
        waitFor(withId(R.id.rowTheme))
        pressBack()
        E2e.waitForAppReady("E2eRevueBudgetsTest.$theme"); waitFor(withId(R.id.bottomNav))
    }

    private fun openCategoriesMonth() {
        onView(withId(R.id.menu_category)).perform(click())
        waitFor(withId(R.id.togglePeriodCategory))
        onView(withId(R.id.btnCatMonth)).perform(click())
        SystemClock.sleep(500)
    }

    private fun openSettings() {
        onView(withId(R.id.btnBudgets)).perform(click())
        waitFor(withId(R.id.tvBudgetsTitle))
        SystemClock.sleep(300)
    }

    private fun openEdit(category: String) {
        onView(allOf(withId(R.id.tvBudgetCategory), withText(category))).perform(E2e.nestedScrollTo(), click())
        waitFor(withId(R.id.inputBudget))
    }

    private fun waitImeStable(): Int {
        var last = -1; var stable = 0
        val end = SystemClock.uptimeMillis() + 8000
        while (SystemClock.uptimeMillis() < end) {
            var h = 0
            instr.runOnMainSync {
                h = ViewCompat.getRootWindowInsets(resumed().window.decorView)
                    ?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0
            }
            if (h > 0 && h == last) { if (++stable >= 2) return h } else stable = 0
            last = h
            SystemClock.sleep(250)
        }
        return last.coerceAtLeast(0)
    }

    /** Problèmes de lecture d'un texte affiché (troncature, mot coupé, texte plus haut que la vue, masqué). */
    private fun textProblems(v: TextView, top: Int, bottom: Int): List<String> {
        val problems = mutableListOf<String>()
        val text = v.text.toString()
        val l = v.layout ?: return listOf("non mis en page")
        for (i in 0 until l.lineCount) if (l.getEllipsisCount(i) > 0) problems += "tronqué (…)"
        for (i in 0 until l.lineCount - 1) {
            val end = l.getLineEnd(i)
            val b = text.getOrNull(end - 1); val a = text.getOrNull(end)
            if (b != null && a != null && b.isLetterOrDigit() && a.isLetterOrDigit()) problems += "mot coupé après « ${text.substring(l.getLineStart(i), end)} »"
        }
        val inner = v.height - v.totalPaddingTop - v.totalPaddingBottom
        if (l.height > inner + 1) problems += "texte plus haut que la vue (${l.height}/${inner}px)"
        // Zone du texte (marges intérieures exclues) entre la barre d'état et le clavier ou le bas de la fenêtre
        val loc = IntArray(2); v.getLocationOnScreen(loc)
        val r = android.graphics.Rect()
        val tTop = loc[1] + v.totalPaddingTop; val tBottom = tTop + l.height
        if (!v.isShown || !v.getGlobalVisibleRect(r)) problems += "masqué"
        else if (tTop < top || tBottom > bottom) problems += "hors zone visible [$tTop,$tBottom] / [$top,$bottom]"
        return problems.distinct()
    }

    /** Zone utile de l'écran : sous la barre d'état, au-dessus du clavier (ou du bas de la fenêtre). */
    private fun zone(act: android.app.Activity): Pair<Int, Int> {
        val decor = act.window.decorView
        val ins = ViewCompat.getRootWindowInsets(decor)
        val ime = ins?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0
        val nav = ins?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0
        val bars = ins?.getInsets(WindowInsetsCompat.Type.statusBars())?.top ?: 0
        val d = IntArray(2); decor.getLocationOnScreen(d)
        return (d[1] + bars) to (d[1] + decor.height - maxOf(ime, nav))
    }

    /** Lisibilité de textes identifiés (consignée ; anomalies BLOQUANTES en fin de test) ; renvoie leur nombre. */
    private fun readability(label: String, vararg ids: Int): Int {
        var count = 0
        instr.runOnMainSync {
            val act = resumed()
            val (top, bottom) = zone(act)
            for (id in ids) {
                val v = act.findViewById<TextView>(id) ?: continue
                if (v.visibility != View.VISIBLE) continue
                val p = textProblems(v, top, bottom)
                count += p.size
                if (p.isNotEmpty()) anomalies += "$suffixNow $label : ${act.resources.getResourceEntryName(id)} « ${v.text} » : ${p.joinToString(" ; ")}"
                log("$label : ${act.resources.getResourceEntryName(id)} « ${v.text.toString().replace('\n', '⏎')} » " +
                    "${v.layout?.lineCount ?: 0} ligne(s), texte ${"%.1f".format(Locale.ROOT, v.textSize / act.resources.displayMetrics.density)} dp" +
                    (if (p.isEmpty()) " lisible" else " ANOMALIE : ${p.joinToString(" ; ")}"))
            }
        }
        return count
    }

    /**
     * Textes de Catégories et du réglage entièrement à l'écran (un texte en bord d'écran n'est pas jugé) : tous ceux
     * portant l'un des [textIds] (consigné ; anomalies BLOQUANTES en fin de test).
     */
    private fun rowsReadability(label: String, containerId: Int, textIds: List<Int>) {
        instr.runOnMainSync {
            val act = resumed()
            val (top, bottom) = zone(act)
            val c = act.findViewById<android.view.ViewGroup>(containerId) ?: return@runOnMainSync
            val lines = mutableListOf<String>()
            fun walk(v: View) {
                val vr = android.graphics.Rect()
                if (v is TextView && v.id in textIds && v.isShown && !v.text.isNullOrEmpty() &&
                    v.getGlobalVisibleRect(vr) && vr.height() == v.height && vr.width() == v.width) {
                    val p = textProblems(v, top, bottom)
                    if (p.isNotEmpty()) lines += "${act.resources.getResourceEntryName(v.id)} « ${v.text} » : ${p.joinToString(" ; ")}"
                }
                if (v is android.view.ViewGroup) for (i in 0 until v.childCount) walk(v.getChildAt(i))
            }
            for (i in 0 until c.childCount) walk(c.getChildAt(i))
            anomalies += lines.map { "$suffixNow $label : $it" }
            log("$label : " + (if (lines.isEmpty()) "lignes entièrement visibles lisibles" else "ANOMALIE : " + lines.joinToString(" | ")))
        }
    }

    private fun bottomNavShown(): Boolean {
        var shown = false
        instr.runOnMainSync { shown = resumed().findViewById<View>(R.id.bottomNav).isShown }
        return shown
    }

    /** Textes du haut de la saisie : entièrement visibles ou entièrement sortis de la vue, jamais coupés (anomalie). */
    private fun noTopCut(label: String) {
        instr.runOnMainSync {
            val act = resumed()
            // Diagnostic (consigné) : état de la zone défilante de la saisie
            val sv = act.findViewById<android.view.ViewGroup>(R.id.budgetEditScroll)
            val content = act.findViewById<android.view.ViewGroup>(R.id.budgetEditContent)
            if (sv != null && content != null) {
                val parts = (0 until content.childCount).map { content.getChildAt(it) }.joinToString(" ") {
                    "${if (it.id != View.NO_ID) act.resources.getResourceEntryName(it.id) else it.javaClass.simpleName}" +
                        "[${it.top},${it.bottom}]${if (it.visibility != View.VISIBLE) "(${it.visibility})" else ""}"
                }
                val loc = IntArray(2); sv.getLocationOnScreen(loc)
                log("$label diagnostic : ${sv.javaClass.simpleName} scrollY=${sv.scrollY} hauteur=${sv.height} écran=${loc[1]} " +
                    "contenu=${content.height} padding=${content.paddingTop} focus=${act.currentFocus?.let { f ->
                        if (f.id != View.NO_ID) act.resources.getResourceEntryName(f.id) else f.javaClass.simpleName }} : $parts")
            }
            for (id in listOf(R.id.tvBudgetEditName, R.id.tvBudgetEditHeader)) {
                val v = act.findViewById<View>(id) ?: continue
                if (v.visibility != View.VISIBLE) continue
                val r = android.graphics.Rect()
                val visible = v.getGlobalVisibleRect(r)
                val line = "$label : ${act.resources.getResourceEntryName(id)} visible ${if (visible) r.height() else 0}/${v.height}px"
                if (visible && r.height() < v.height) {
                    anomalies += "$suffixNow $line (rogné)"; log("$line ANOMALIE : rogné")
                } else log(line)
            }
        }
    }

    /**
     * Textes du haut de la saisie, clavier ouvert : ils peuvent être entièrement sortis de la vue au-dessus du champ
     * ([noTopCut] vérifie qu'ils ne sont jamais coupés) ; chacun est alors amené à l'écran par défilement, comme le
     * ferait l'utilisateur, puis doit être lisible en entier (anomalie sinon).
     */
    private fun readableByScroll(label: String, vararg ids: Int) {
        for (id in ids) {
            var visible = false
            instr.runOnMainSync { visible = resumed().findViewById<View>(id)?.visibility == View.VISIBLE }
            if (!visible) continue
            bringFullyOnScreen(id)
            readability("$label, après défilement", id)
        }
    }

    /**
     * Contexte de la catégorie pendant la saisie, clavier ouvert, SANS défiler : le nom complet est visible soit comme
     * titre de la barre haute, soit dans la zone fixe sous la barre (anomalie BLOQUANTE sinon). Le nom n'est affiché
     * qu'une fois et l'en-tête de la zone défilante se limite à « Budget mensuel ».
     */
    private fun categoryContext(label: String) {
        instr.runOnMainSync {
            val act = resumed()
            val (top, bottom) = zone(act)
            val title = act.findViewById<TextView>(R.id.tvBudgetEditTitle)
            val name = act.findViewById<TextView>(R.id.tvBudgetEditName)
            val header = act.findViewById<TextView>(R.id.tvBudgetEditHeader)
            val shown = listOf(title, name).filter { it.visibility == View.VISIBLE && it.text.isNotEmpty() }
            val ok = shown.size == 1 && textProblems(shown[0], top, bottom).isEmpty()
            val line = "$label : contexte « ${shown.joinToString { it.text }} » " +
                "(${shown.joinToString { act.resources.getResourceEntryName(it.id) }}), en-tête « ${header.text} »"
            if (!ok) anomalies += "$suffixNow $line : nom absent, répété ou illisible sans défiler"
            if (header.text.toString() != "Budget mensuel") anomalies += "$suffixNow $label : en-tête « ${header.text} » au lieu de « Budget mensuel »"
            log(line + if (ok) " visible" else " ANOMALIE")
        }
    }

    /** Vue entièrement visible entre la barre d'état et le clavier, SANS défiler (anomalie BLOQUANTE sinon). */
    private fun visibleWithoutScroll(id: Int, label: String) {
        instr.runOnMainSync {
            val act = resumed()
            val (top, bottom) = zone(act)
            val v = act.findViewById<View>(id)
            val loc = IntArray(2); v.getLocationOnScreen(loc)
            val r = android.graphics.Rect()
            val ok = v.isShown && v.getGlobalVisibleRect(r) && r.height() == v.height && loc[1] >= top && loc[1] + v.height <= bottom
            val line = "$label : [${loc[1]},${loc[1] + v.height}] zone [$top,$bottom] sans défiler=$ok"
            if (!ok) anomalies += "$suffixNow $line"
            log(line + if (ok) "" else " ANOMALIE")
        }
    }

    /** Message d'erreur sous le champ : lisible entre la barre d'état et le clavier (anomalie sinon). */
    private fun errorVisible(label: String, message: String) {
        instr.runOnMainSync {
            val act = resumed()
            val (top, bottom) = zone(act)
            var found: TextView? = null
            fun walk(v: View) {
                if (found != null) return
                if (v is TextView && v.text?.toString() == message && v.isShown) found = v
                if (v is android.view.ViewGroup) for (i in 0 until v.childCount) walk(v.getChildAt(i))
            }
            walk(act.window.decorView)
            val p = found?.let { textProblems(it, top, bottom) } ?: listOf("absent")
            if (p.isNotEmpty()) anomalies += "$suffixNow $label : « $message » : ${p.joinToString(" ; ")}"
            log("$label : « $message » " + (if (p.isEmpty()) "lisible" else "ANOMALIE : ${p.joinToString(" ; ")}"))
        }
    }

    /** Bouton de la confirmation ENTIÈREMENT visible (100 %) sur l'écran. */
    private fun dialogButtonFull(label: String): Boolean {
        var ok = false
        onView(withText(label)).inRoot(isDialog()).check { v, _ ->
            val r = android.graphics.Rect()
            val dm = v.resources.displayMetrics
            ok = v.isShown && v.getGlobalVisibleRect(r) && r.width() == v.width && r.height() == v.height &&
                IntArray(2).also { v.getLocationOnScreen(it) }.let { it[1] >= 0 && it[1] + v.height <= dm.heightPixels }
        }
        return ok
    }

    /** Vue amenée à l'écran par son parent défilant (vue entière, pas seulement 90 %). */
    private fun bringFullyOnScreen(id: Int) {
        instr.runOnMainSync {
            val v = resumed().findViewById<View>(id) ?: return@runOnMainSync
            v.requestRectangleOnScreen(android.graphics.Rect(0, 0, v.width, v.height), true)
        }
        SystemClock.sleep(500)
    }

    /** BLOQUANT : action entièrement visible entre la barre d'état et le clavier (ou le bas de l'écran). */
    private fun assertReachable(id: Int, label: String) = assertTrue("Action accessible ($label)", reachable(id, label))

    /** Action amenée à l'écran puis entièrement visible entre la barre d'état et le clavier (mesure consignée). */
    private fun reachable(id: Int, label: String): Boolean {
        bringFullyOnScreen(id)
        var ok = false; var line = ""
        instr.runOnMainSync {
            val act = resumed()
            val (top, bottom) = zone(act)
            val v = act.findViewById<View>(id)
            val b = IntArray(2); v.getLocationOnScreen(b)
            val r = android.graphics.Rect()
            ok = v.isShown && v.getGlobalVisibleRect(r) && r.height() == v.height && r.width() == v.width &&
                b[1] >= top && b[1] + v.height <= bottom
            line = "$label : ${act.resources.getResourceEntryName(id)} [${b[1]},${b[1] + v.height}] zone [$top,$bottom] accessible=$ok"
        }
        log(line)
        return ok
    }

    /** Part visible (%) d'un bouton de la fenêtre de confirmation. */
    private fun dialogButtonPct(label: String): Int {
        var pct = 0
        onView(withText(label)).inRoot(isDialog()).check { v, _ ->
            val r = android.graphics.Rect()
            if (v != null && v.width * v.height > 0 && v.getGlobalVisibleRect(r))
                pct = 100 * r.width() * r.height() / (v.width * v.height)
        }
        return pct
    }

    /**
     * Toucher RÉEL d'un bouton de la confirmation, au centre de sa partie visible (comme le ferait l'utilisateur quand
     * il est en partie masqué). BLOQUANT : bouton invisible. L'effet du toucher est vérifié ensuite par l'appelant.
     */
    private fun tapDialogButton(label: String) {
        onView(withText(label)).inRoot(isDialog()).perform(object : androidx.test.espresso.ViewAction {
            override fun getConstraints(): org.hamcrest.Matcher<View> = org.hamcrest.Matchers.any(View::class.java)
            override fun getDescription() = "toucher sur la partie visible de « $label »"
            override fun perform(uiController: androidx.test.espresso.UiController, view: View) {
                val r = android.graphics.Rect()
                assertTrue("« $label » invisible dans la confirmation", view.getGlobalVisibleRect(r) && r.height() > 0 && r.width() > 0)
                val off = IntArray(2); view.rootView.getLocationOnScreen(off)
                val xy = floatArrayOf((off[0] + r.centerX()).toFloat(), (off[1] + r.centerY()).toFloat())
                androidx.test.espresso.action.Tap.SINGLE.sendTap(uiController, xy, floatArrayOf(1f, 1f), 0, 0)
                uiController.loopMainThreadForAtLeast(300)
            }
        })
    }

    /** Clavier ouvert à l'arrivée sur la saisie (ouverture automatique) ; son absence est CONSIGNÉE. */
    private fun keyboardOnEdit(label: String): Int {
        SystemClock.sleep(600)
        var ime = waitImeStable()
        if (ime == 0) {
            log("$label ANOMALIE : clavier non ouvert automatiquement, champ touché comme le ferait l'utilisateur")
            onView(withId(R.id.inputBudget)).perform(click())
            ime = waitImeStable()
        }
        log("$label : clavier ${ime}px")
        return ime
    }

    /** Valeur du réglage pour une catégorie, ligne amenée à l'écran (elle peut être plus bas en grande police). */
    private fun waitSettingsValue(category: String, value: String) {
        val m = allOf(withId(R.id.tvBudgetValue), androidx.test.espresso.matcher.ViewMatchers.hasSibling(
            allOf(withId(R.id.tvBudgetCategory), withText(category))), withText(value))
        val end = SystemClock.uptimeMillis() + 10_000
        var last: Throwable? = null
        while (SystemClock.uptimeMillis() < end) {
            try { onView(m).perform(E2e.nestedScrollTo()); return } catch (e: Throwable) { last = e }
            SystemClock.sleep(200)
        }
        E2e.diagnostic("waitSettingsValue $category $value")
        throw AssertionError("Réglage : « $value » attendu pour « $category »", last)
    }

    /** Catégories : captures page par page jusqu'en bas de la liste. */
    private fun captureCategoryPages(name: String) {
        instr.runOnMainSync { resumed().findViewById<RecyclerView>(R.id.recyclerViewCategories).scrollToPosition(0) }
        SystemClock.sleep(500)
        for (page in 1..6) {
            rowsReadability("$name page $page", R.id.recyclerViewCategories,
                listOf(R.id.tvCategoryName, R.id.tvCategoryTotal, R.id.tvCategoryPercent, R.id.tvBudgetStatus,
                    R.id.tvBudgetAmounts, R.id.tvBudgetShared, R.id.legendName, R.id.legendPercent))
            shot("revue_budgets_${name}_p${page}_$suffixNow")
            var moved = false
            instr.runOnMainSync {
                val r = resumed().findViewById<RecyclerView>(R.id.recyclerViewCategories)
                if (r.canScrollVertically(1)) { r.scrollBy(0, (r.height * 0.8f).toInt()); moved = true }
            }
            if (!moved) break
            SystemClock.sleep(500)
        }
        instr.runOnMainSync { resumed().findViewById<RecyclerView>(R.id.recyclerViewCategories).scrollToPosition(0) }
        SystemClock.sleep(300)
    }

    private fun captureTheme(theme: String, themeSuffix: String) {
        suffixNow = if (passe == null) themeSuffix else "${themeSuffix}_$passe"
        val s = suffixNow
        // 1) Liste vide : aucun ticket, aucun budget
        replaceTickets(emptyList()); setBudgets(emptyMap())
        startMain()
        chooseTheme(theme)
        openCategoriesMonth()
        waitFor(withId(R.id.emptyCategories))
        readability("Catégories vide", R.id.tvEmptyCategories, R.id.tvCategoriesTitle, R.id.btnCatToday, R.id.btnCatMonth, R.id.btnCatYear)
        shot("revue_budgets_01_categories_vide_$s")
        openSettings()
        waitFor(withId(R.id.tvBudgetsEmpty))
        readability("Réglage vide", R.id.tvBudgetsTitle, R.id.tvBudgetsEmpty)
        shot("revue_budgets_02_reglage_vide_$s")
        pressBack()

        // 2) Liste remplie, aucun budget : noms longs, invitation « Définir des budgets mensuels »
        replaceTickets(fixtureTickets())
        val tickets = allTickets()
        waitFor(withId(R.id.recyclerViewCategories))
        captureCategoryPages("03_categories_sans_budget")
        openSettings()
        waitSettingsValue(longAbo, "—")
        rowsReadability("Réglage sans budget", R.id.budgetRows, listOf(R.id.tvBudgetCategory, R.id.tvBudgetValue))
        shot("revue_budgets_04_reglage_sans_budget_$s")

        // 3) Création : saisie vide, montant invalide (« Appliquer » inactif), validation par la touche du clavier
        openEdit(longAbo)
        keyboardOnEdit("Création")
        shot("revue_budgets_05_creation_vide_$s")
        assertTrue("Barre d'onglets masquée pendant la saisie ($s)", !bottomNavShown())
        onView(withId(R.id.btnDeleteBudget)).check(matches(not(androidx.test.espresso.matcher.ViewMatchers.isDisplayed())))
        // Contexte de la catégorie visible clavier ouvert, sans défiler : titre de la barre ou nom dans la zone fixe
        readability("Création", R.id.tvBudgetEditTitle, R.id.tvBudgetEditName, R.id.btnBudgetCancel, R.id.btnBudgetApply)
        categoryContext("Création")
        noTopCut("Création")
        assertReachable(R.id.btnBudgetCancel, "Création clavier ouvert")
        assertReachable(R.id.btnBudgetApply, "Création clavier ouvert")
        onView(withId(R.id.inputBudget)).perform(replaceText("0"))
        SystemClock.sleep(500)
        shot("revue_budgets_06_creation_invalide_$s")
        onView(withId(R.id.btnBudgetApply)).check(matches(not(isEnabled())))
        errorVisible("Création, montant invalide", "Montant invalide")
        readability("Création, montant invalide", R.id.tvBudgetEditTitle, R.id.tvBudgetEditName)
        categoryContext("Création, montant invalide")
        visibleWithoutScroll(R.id.budgetInputLayout, "Création, champ et message d'erreur")
        noTopCut("Création, montant invalide")
        assertReachable(R.id.budgetInputLayout, "Création, champ et message d'erreur, clavier ouvert")
        readableByScroll("Création", R.id.tvBudgetEditName, R.id.tvBudgetEditHeader)
        onView(withId(R.id.inputBudget)).perform(replaceText("25"))
        onView(withId(R.id.btnBudgetApply)).check(matches(isEnabled()))
        shot("revue_budgets_07_creation_saisie_$s")
        onView(withId(R.id.inputBudget)).perform(pressImeActionButton())   // touche « OK » du clavier
        waitSettingsValue(longAbo, "25 €")
        assertTrue("Barre d'onglets rétablie à la sortie de la saisie ($s)", bottomNavShown())
        assertEquals(mapOf(longAbo.lowercase(Locale.ROOT) to 25.0), BudgetStore(ctx).load())
        assertEquals("Création : tickets intacts", tickets, allTickets())

        // 4) Budgets des états (fictifs, écrits directement), puis modification au clavier
        setBudgets(stateBudgets + (longAbo to 25.0))
        val withStates = BudgetStore(ctx).load()
        waitSettingsValue("Restaurant", "50 €")
        waitSettingsValue("Courses", "40 €"); waitSettingsValue("courses", "40 €")   // une seule clé, deux lignes
        // Annulation de la saisie : montant modifié puis « Annuler » → rien ne change (BLOQUANT)
        openEdit("Restaurant")
        keyboardOnEdit("Annulation")
        onView(withId(R.id.inputBudget)).perform(replaceText("999"))
        onView(withId(R.id.btnBudgetCancel)).perform(click())
        waitSettingsValue("Restaurant", "50 €")
        assertEquals("« Annuler » de la saisie sans effet sur les budgets", withStates, BudgetStore(ctx).load())
        assertEquals("« Annuler » de la saisie sans effet sur les tickets", tickets, allTickets())
        // Modification : préremplie, nouveau montant, « Appliquer » touché clavier ouvert
        openEdit(longAlim)
        onView(withId(R.id.inputBudget)).check(matches(withText("200")))
        keyboardOnEdit("Modification")
        shot("revue_budgets_08_modification_preremplie_$s")
        readability("Modification", R.id.tvBudgetEditTitle, R.id.tvBudgetEditName, R.id.btnBudgetCancel, R.id.btnBudgetApply)
        categoryContext("Modification")
        visibleWithoutScroll(R.id.budgetInputLayout, "Modification, champ")
        noTopCut("Modification")
        readableByScroll("Modification", R.id.tvBudgetEditName, R.id.tvBudgetEditHeader)
        onView(withId(R.id.inputBudget)).perform(replaceText("180,50"))
        SystemClock.sleep(300)
        shot("revue_budgets_09_modification_saisie_$s")
        assertReachable(R.id.btnBudgetApply, "Modification clavier ouvert")
        onView(withId(R.id.btnBudgetApply)).perform(click())
        waitSettingsValue(longAlim, "180,50 €")
        assertEquals(withStates + (longAlim.lowercase(Locale.ROOT) to 180.5), BudgetStore(ctx).load())
        assertEquals("Modification : tickets intacts", tickets, allTickets())
        rowsReadability("Réglage rempli", R.id.budgetRows, listOf(R.id.tvBudgetCategory, R.id.tvBudgetValue))
        shot("revue_budgets_10_reglage_rempli_$s")
        instr.runOnMainSync { resumed().findViewById<androidx.core.widget.NestedScrollView>(R.id.budgetsScroll).fullScroll(View.FOCUS_DOWN) }
        SystemClock.sleep(500)
        rowsReadability("Réglage rempli bas", R.id.budgetRows, listOf(R.id.tvBudgetCategory, R.id.tvBudgetValue))
        shot("revue_budgets_11_reglage_rempli_bas_$s")
        pressBack()

        // 5) Catégories : sous le seuil, alerte, 100 % pile (« Budget atteint »), dépassé, partagé (casse)
        captureCategoryPages("12_categories_etats")
        BudgetE2e.row(withText(longAlim), withText("100 € / 180,50 €"))
        BudgetE2e.row(withText("Restaurant"), withText("Attention — 84%"), withText("42 € / 50 €"))
        BudgetE2e.row(withText("Carburant"), withText("Budget atteint"), withText("60 € / 60 €"))
        BudgetE2e.row(withText("Loisirs"), withText("Dépassé — 150%"), withText("45 € / 30 €"))
        BudgetE2e.row(withText("Courses"), withText("Dépassé — 125%"), withText("50 € / 40 €"),
            withText("Budget partagé avec « courses » · consommation cumulée"))
        BudgetE2e.row(withText("courses"), withText("Dépassé — 125%"),
            withText("Budget partagé avec « Courses » · consommation cumulée"))
        BudgetE2e.row(withText(longAbo), withText("12,99 € / 25 €"))
        // Montants exacts au centime (décision du 09/10/2026) : juste sous le budget (« 100% » une fois arrondi),
        // exactement égal, juste au-dessus (« 100% » une fois arrondi). Budget de « Carburant » (fictif) modifié puis rétabli.
        for ((limit, status, amounts, name) in listOf(
            Quad(60.01, "Attention — 100%", "60 € / 60,01 €", "12b_carburant_juste_sous"),
            Quad(60.0, "Budget atteint", "60 € / 60 €", "12c_carburant_egal"),
            Quad(59.99, "Dépassé — 100%", "60 € / 59,99 €", "12d_carburant_juste_dessus"))) {
            BudgetStore(ctx).set("Carburant", limit)
            SystemClock.sleep(1200)
            BudgetE2e.row(withText("Carburant"))
            SystemClock.sleep(300)
            shot("revue_budgets_${name}_$s")
            log("Carburant 60 € pour un budget de $limit € : attendu « $status », « $amounts »")
            BudgetE2e.row(withText("Carburant"), withText(status), withText(amounts))
        }
        BudgetStore(ctx).set("Carburant", 60.0)
        SystemClock.sleep(800)

        // 6) Suppression : confirmation, « Annuler » sans effet, puis « Supprimer » (seul ce budget)
        openSettings()
        openEdit("Loisirs")
        keyboardOnEdit("Suppression")
        // Clavier ouvert : accès CONSIGNÉ ; s'il manque, clavier fermé comme le ferait l'utilisateur, puis accès BLOQUANT
        if (!reachable(R.id.btnDeleteBudget, "Suppression clavier ouvert")) {
            log("Suppression ANOMALIE : « Supprimer le budget » inaccessible clavier ouvert, clavier fermé")
            E2e.closeKeyboard(); SystemClock.sleep(600)
        }
        assertReachable(R.id.btnDeleteBudget, "Suppression")
        val budgetsAvant = BudgetStore(ctx).load()
        onView(withId(R.id.btnDeleteBudget)).perform(click())
        waitFor(withText("Supprimer le budget ?"))
        SystemClock.sleep(600)
        shot("revue_budgets_13_suppression_confirmation_$s")
        // Message : entier ou accessible par défilement jusqu'à sa dernière ligne
        var msgLine = ""; var msgEnd = false; var scrolled = false
        onView(withId(R.id.confirmScroll)).inRoot(isDialog()).perform(object : androidx.test.espresso.ViewAction {
            override fun getConstraints(): org.hamcrest.Matcher<View> = org.hamcrest.Matchers.any(View::class.java)
            override fun getDescription() = "défilement du message de confirmation jusqu'à la fin"
            override fun perform(uiController: androidx.test.espresso.UiController, view: View) {
                val sv = view as android.widget.ScrollView
                scrolled = sv.canScrollVertically(1)
                sv.fullScroll(View.FOCUS_DOWN); uiController.loopMainThreadForAtLeast(300)
                val m = sv.findViewById<TextView>(R.id.confirmMessage)
                val r = android.graphics.Rect(); val loc = IntArray(2); m.getLocationOnScreen(loc)
                val off = IntArray(2); m.rootView.getLocationOnScreen(off)
                msgEnd = m.getGlobalVisibleRect(r) && off[1] + r.bottom >= loc[1] + m.height &&
                    (0 until (m.layout?.lineCount ?: 0)).none { m.layout.getEllipsisCount(it) > 0 }
                msgLine = "Confirmation : « ${m.text} » ${if (scrolled) "défilant" else "entier"}, fin " +
                    (if (msgEnd) "visible" else "INACCESSIBLE")
            }
        })
        log(msgLine)
        if (scrolled) shot("revue_budgets_13b_suppression_message_fin_$s")
        val fullAnnuler = dialogButtonFull("Annuler"); val fullSupprimer = dialogButtonFull("Supprimer")
        val pctAnnuler = dialogButtonPct("Annuler"); val pctSupprimer = dialogButtonPct("Supprimer")
        log("Confirmation : boutons visibles Annuler $pctAnnuler % (entier : $fullAnnuler), Supprimer $pctSupprimer % (entier : $fullSupprimer)")
        // BLOQUANT : les deux boutons ENTIÈREMENT visibles (un bouton partiellement visible n'est pas conforme),
        // fin du message accessible ; puis vrai toucher et effet vérifié
        assertTrue("Confirmation ($s) : « Annuler » entièrement visible ($pctAnnuler %)", fullAnnuler)
        assertTrue("Confirmation ($s) : « Supprimer » entièrement visible ($pctSupprimer %)", fullSupprimer)
        assertTrue("Confirmation ($s) : fin du message accessible ($msgLine)", msgEnd)
        tapDialogButton("Annuler")
        waitFor(withId(R.id.btnBudgetApply))
        onView(withId(R.id.inputBudget)).check(matches(withText("30")))
        assertEquals("« Annuler » de la confirmation sans effet sur les budgets", budgetsAvant, BudgetStore(ctx).load())
        assertEquals("« Annuler » de la confirmation sans effet sur les tickets", tickets, allTickets())
        shot("revue_budgets_14_suppression_annulee_$s")
        bringFullyOnScreen(R.id.btnDeleteBudget)
        onView(withId(R.id.btnDeleteBudget)).perform(click())
        waitFor(withText("Supprimer le budget ?"))
        SystemClock.sleep(600)
        assertTrue("Confirmation ($s) : « Supprimer » entièrement visible", dialogButtonFull("Supprimer"))
        tapDialogButton("Supprimer")
        waitSettingsValue("Loisirs", "—")
        assertNull(BudgetStore(ctx).limit("Loisirs"))
        assertEquals("Suppression : autres budgets intacts", budgetsAvant - "loisirs", BudgetStore(ctx).load())
        assertEquals("Suppression : tickets intacts", tickets, allTickets())
        shot("revue_budgets_15_reglage_apres_suppression_$s")
        pressBack()
        BudgetE2e.noBudgetOn("Loisirs")
        shot("revue_budgets_16_categories_apres_suppression_$s")
    }

    @Test fun c01_categories_budgets_clair_puis_sombre() {
        assertEquals("Émulateur attendu en français", "fr", Locale.getDefault().language)
        // Données présentes mises de côté (rétablies à l'identique, identifiants compris)
        val savedTickets = allTickets(); val savedBudgets = BudgetStore(ctx).load()
        try {
            captureTheme("Clair", "clair")
            captureTheme("Sombre", "sombre")
            chooseTheme("Système")
            log("bilan : ${if (anomalies.isEmpty()) "aucune anomalie" else "ANOMALIE : ${anomalies.size} anomalie(s)"}")
            assertEquals("Catégories et budgets : textes lisibles, rien de rogné, message d'erreur visible",
                emptyList<String>(), anomalies)
        } finally {
            replaceTickets(savedTickets); setBudgets(savedBudgets)
        }
        assertEquals(savedTickets, allTickets()); assertEquals(savedBudgets, BudgetStore(ctx).load())
    }
}
