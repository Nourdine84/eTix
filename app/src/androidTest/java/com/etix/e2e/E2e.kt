package com.etix.e2e

import android.graphics.Bitmap
import android.os.Build
import android.os.SystemClock
import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import java.io.File
import java.io.FileOutputStream

/**
 * Outils des tests de bout en bout sur émulateur (vraie app, vrai rendu, vrai clavier).
 * Captures : filesDir/shots de l'app (lues par la CI via `run-as`).
 */
object E2e {

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    val ctx get() = instrumentation.targetContext

    /** Attend qu'une vue corresponde (écran suivant, données Room asynchrones…). */
    fun waitFor(matcher: Matcher<View>, timeoutMs: Long = 10_000): ViewInteraction {
        val end = SystemClock.uptimeMillis() + timeoutMs
        var last: Throwable? = null
        while (SystemClock.uptimeMillis() < end) {
            try {
                // Uniquement les vues visibles : les pages hors écran du ViewPager contiennent les mêmes textes
                return onView(allOf(matcher, isDisplayed())).check(matches(isDisplayed()))
            } catch (e: NoMatchingViewException) {
                last = e
            } catch (e: AssertionError) {
                last = e
            } catch (e: RuntimeException) {
                last = e
            }
            SystemClock.sleep(200)
        }
        diagnostic("waitFor ${matcher.toString().take(160)}")
        throw AssertionError("Vue non affichée après ${timeoutMs} ms : $matcher", last)
    }

    fun shot(name: String) {
        instrumentation.waitForIdleSync()
        SystemClock.sleep(400) // fin des animations / ripple
        val bmp: Bitmap = instrumentation.uiAutomation.takeScreenshot() ?: return
        val dir = File(ctx.filesDir, "shots").apply { mkdirs() }
        FileOutputStream(File(dir, "api${Build.VERSION.SDK_INT}_$name.png")).use {
            bmp.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    /** Format de saisie texte de MaterialDatePicker (UtcDates.getDefaultTextInputFormat) pour la locale courante. */
    fun pickerText(c: java.util.Calendar): String {
        val base = (java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT, java.util.Locale.getDefault())
            as java.text.SimpleDateFormat).toPattern()
        val pattern = base.replace(Regex("\\s+"), "").replace(Regex("d{1,2}"), "dd")
            .replace(Regex("M{1,2}"), "MM").replace(Regex("y{1,4}"), "yyyy")
        val utc = java.util.TimeZone.getTimeZone("UTC")
        return java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault()).apply { timeZone = utc }
            .format(java.util.Calendar.getInstance(utc).apply {
                clear(); set(c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH), c.get(java.util.Calendar.DAY_OF_MONTH))
            }.time)
    }

    /** Ferme le clavier sans Espresso.closeSoftKeyboard (qui échoue si aucun clavier n'est ouvert sur certaines versions). */
    fun closeKeyboard() {
        try { androidx.test.espresso.Espresso.closeSoftKeyboard() } catch (_: Throwable) { }
    }

    /**
     * Lot 10 : choix du thème dans les Réglages (ligne « Thème » → liste à choix unique). L'activité est recréée si
     * le thème change ; attendre ensuite la ligne « Thème ».
     */
    fun chooseTheme(label: String) {
        onView(androidx.test.espresso.matcher.ViewMatchers.withId(com.etix.R.id.rowTheme))
            .perform(androidx.test.espresso.action.ViewActions.click())
        onView(androidx.test.espresso.matcher.ViewMatchers.withText(label))
            .inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog())
            .perform(androidx.test.espresso.action.ViewActions.click())
    }

    /** scrollTo pour NestedScrollView (non pris en charge par ViewActions.scrollTo d'Espresso 3.5). */
    fun nestedScrollTo(): androidx.test.espresso.ViewAction = object : androidx.test.espresso.ViewAction {
        override fun getConstraints(): Matcher<View> = allOf(
            androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA(
                androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom(androidx.core.widget.NestedScrollView::class.java)),
            androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility(
                androidx.test.espresso.matcher.ViewMatchers.Visibility.VISIBLE))
        override fun getDescription() = "défilement NestedScrollView jusqu'à la vue"
        override fun perform(uiController: androidx.test.espresso.UiController, view: View) {
            view.requestRectangleOnScreen(android.graphics.Rect(0, 0, view.width, view.height), true)
            uiController.loopMainThreadUntilIdle()
        }
    }

    /**
     * Diagnostic d'échec : capture « zz_echec_… » + fenêtre ayant le focus (dumpsys window), consignés dans
     * files/shots/echec.txt (publié par la CI). N'altère pas l'état de l'app.
     */
    fun diagnostic(context: String) {
        try {
            val tag = "zz_echec_${SystemClock.uptimeMillis()}"
            // Pas de waitForIdleSync ici : l'app peut justement ne jamais être « idle »
            instrumentation.uiAutomation.takeScreenshot()?.let { bmp ->
                FileOutputStream(File(File(ctx.filesDir, "shots").apply { mkdirs() }, "api${Build.VERSION.SDK_INT}_$tag.png"))
                    .use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
            val pfd = instrumentation.uiAutomation.executeShellCommand("dumpsys window")
            val dump = android.os.ParcelFileDescriptor.AutoCloseInputStream(pfd).bufferedReader().use { it.readText() }
            val focus = dump.lines().filter { l -> listOf("mCurrentFocus", "mFocusedApp", "mFocusedWindow").any { it in l } }
                .map { it.trim() }.distinct().take(6)
            File(File(ctx.filesDir, "shots").apply { mkdirs() }, "echec.txt")
                .appendText("[$tag] $context\n  " + focus.joinToString("\n  ") + "\n")
        } catch (_: Throwable) { }
    }

    /**
     * Attente fondée sur l'ÉTAT RÉEL de l'app (lot 7), et non sur un délai : activité de l'app au premier plan
     * (RESUMED) PUIS fenêtre ayant le focus. Mesure les deux temps (files/shots/demarrage.txt, publié par la CI).
     * Si la borne de sécurité est atteinte, le diagnostic distingue :
     *  - « APP » : aucune activité eTix RESUMED (lancement lent ou bloqué côté app) ;
     *  - « SYSTÈME » : activité RESUMED mais sans focus (fenêtre système / environnement émulateur).
     * La cause n'est pas présumée : c'est le constat qui est consigné.
     */
    fun waitForAppReady(label: String, boundMs: Long = 60_000) {
        val t0 = SystemClock.uptimeMillis()
        var resumedAt = -1L
        var focusedAt = -1L
        var activityName = "?"
        while (SystemClock.uptimeMillis() - t0 < boundMs) {
            var resumed = false
            var focused = false
            instrumentation.runOnMainSync {
                val act = androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED).firstOrNull()
                if (act != null) {
                    resumed = true
                    activityName = act.javaClass.simpleName
                    focused = act.window.decorView.hasWindowFocus()
                }
            }
            val now = SystemClock.uptimeMillis() - t0
            if (resumed && resumedAt < 0) resumedAt = now
            if (focused) { focusedAt = now; break }
            SystemClock.sleep(100)
        }
        val verdict = when {
            focusedAt >= 0 -> "prêt"
            resumedAt < 0 -> "APP : aucune activité au premier plan"
            else -> "SYSTÈME : activité au premier plan sans focus"
        }
        File(File(ctx.filesDir, "shots").apply { mkdirs() }, "demarrage.txt").appendText(
            "$label : $activityName au premier plan ${if (resumedAt >= 0) "$resumedAt ms" else "—"}, " +
                "focus ${if (focusedAt >= 0) "$focusedAt ms" else "—"} → $verdict\n")
        if (focusedAt < 0) {
            diagnostic("waitForAppReady $label : $verdict")
            throw AssertionError("App non prête après $boundMs ms ($label) : $verdict")
        }
    }
}
