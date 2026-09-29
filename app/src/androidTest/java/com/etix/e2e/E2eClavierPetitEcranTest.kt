package com.etix.e2e

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.clearText
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.etix.R
import com.etix.data.AppDatabase
import com.etix.e2e.E2e.ctx
import com.etix.e2e.E2e.shot
import com.etix.e2e.E2e.waitFor
import com.etix.ui.main.MainActivityV2
import com.etix.utils.SessionManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.allOf
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.io.File

/**
 * Accessibilité du bouton d'enregistrement CLAVIER OUVERT, petit écran et grande police (lot 6).
 * Espresso `isDisplayed` ignore la fenêtre du clavier : on MESURE donc la position du bouton par rapport au haut du
 * clavier (insets IME, API 30+), puis on touche réellement le bouton et on vérifie l'effet (ticket enregistré).
 * Passe (taille d'écran, densité, police) fixée par le script CI ; argument d'instrumentation « passe ».
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class E2eClavierPetitEcranTest {

    private val instr get() = InstrumentationRegistry.getInstrumentation()
    private val passe: String get() = InstrumentationRegistry.getArguments().getString("passe", "x")
    private val store get() = "Clavier $passe"

    @Before fun session() {
        assumeTrue("Mesure des insets clavier : API 30+", Build.VERSION.SDK_INT >= 30)
        SessionManager(ctx).apply { markFirstLaunchDone(); login("Testeur clavier") }
    }

    private fun startMain() {
        ctx.startActivity(Intent(ctx, MainActivityV2::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        waitFor(withId(R.id.bottomNav))
    }

    /** À appeler SUR le thread principal. */
    private fun resumedOnMain(): Activity =
        ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).first()

    /** Hauteur du clavier en px, stable sur deux lectures (fin d'animation) ; 0 si absent. */
    private fun waitImeStable(): Int {
        var last = -1; var stable = 0
        val end = SystemClock.uptimeMillis() + 8000
        while (SystemClock.uptimeMillis() < end) {
            var h = 0
            instr.runOnMainSync {
                h = ViewCompat.getRootWindowInsets(resumedOnMain().window.decorView)
                    ?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0
            }
            if (h > 0 && h == last) { if (++stable >= 2) return h } else stable = 0
            last = h
            SystemClock.sleep(250)
        }
        return last.coerceAtLeast(0)
    }

    private data class Mesure(val top: Int, val bottom: Int, val haut: Int, val clavierHaut: Int, val clavier: Int) {
        val visible get() = clavier > 0 && top >= haut && bottom <= clavierHaut
    }

    private fun mesure(buttonId: Int): Mesure {
        var m: Mesure? = null
        instr.runOnMainSync {
            val act = resumedOnMain()
            val decor = act.window.decorView
            val ins = ViewCompat.getRootWindowInsets(decor)
            val ime = ins?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0
            val bars = ins?.getInsets(WindowInsetsCompat.Type.statusBars())?.top ?: 0
            val d = IntArray(2).also(decor::getLocationOnScreen)
            val b = IntArray(2).also(act.findViewById<View>(buttonId)::getLocationOnScreen)
            val v = act.findViewById<View>(buttonId)
            m = Mesure(b[1], b[1] + v.height, d[1] + bars, d[1] + decor.height - ime, ime)
        }
        return m!!
    }

    private fun log(ecran: String, avant: Mesure, apres: Mesure) {
        val res = ctx.resources
        val dm = res.displayMetrics
        val line = "passe=$passe sdk=${Build.VERSION.SDK_INT} police=${res.configuration.fontScale} " +
            "ecran=${(dm.widthPixels / dm.density).toInt()}x${(dm.heightPixels / dm.density).toInt()}dp " +
            "$ecran clavier=${apres.clavier}px haut_clavier=${apres.clavierHaut} " +
            "bouton_avant=[${avant.top},${avant.bottom}] bouton_apres_defilement=[${apres.top},${apres.bottom}] " +
            "accessible=${apres.visible}\n"
        File(File(ctx.filesDir, "shots").apply { mkdirs() }, "mesures_clavier.txt").appendText(line)
    }

    @Test
    fun k01_ajout_bouton_enregistrer_atteignable_clavier_ouvert() {
        startMain()
        onView(withId(R.id.menu_add)).perform(click())
        onView(allOf(withId(R.id.inputStore), isDisplayed())).perform(click(), replaceText(store))
        onView(withId(R.id.inputAmount)).perform(scrollTo(), click(), typeText("4,20"))
        assertTrue("Clavier non affiché", waitImeStable() > 0)
        val avant = mesure(R.id.btnSaveTicket)
        onView(withId(R.id.btnSaveTicket)).perform(scrollTo())
        waitImeStable()
        val apres = mesure(R.id.btnSaveTicket)
        log("Ajouter", avant, apres)
        shot("k01_ajout_clavier_${passe}")
        assertTrue("Bouton Enregistrer masqué par le clavier après défilement : $apres", apres.visible)
        var saisi = ""
        instr.runOnMainSync { saisi = resumedOnMain().findViewById<android.widget.TextView>(R.id.inputAmount).text.toString() }
        onView(withId(R.id.btnSaveTicket)).perform(click()) // vrai toucher à l'emplacement mesuré
        // Enregistrement asynchrone (coroutine) : on attend jusqu'à 5 s
        var saved = false
        val end = SystemClock.uptimeMillis() + 5000
        while (!saved && SystemClock.uptimeMillis() < end) {
            saved = runBlocking {
                AppDatabase.getInstance(ctx).ticketDao().getAllFlow().first().any { it.store == store && it.amount == 4.20 }
            }
            if (!saved) SystemClock.sleep(200)
        }
        if (!saved) {
            shot("k01_apres_toucher_${passe}")
            E2e.diagnostic("k01 passe $passe : non enregistré")
        }
        val proches = runBlocking {
            AppDatabase.getInstance(ctx).ticketDao().getAllFlow().first().filter { it.store.startsWith("Clavier") }
                .joinToString { "${it.store}=${it.amount}" }
        }
        assertTrue("Ticket non enregistré 5 s après le toucher du bouton (montant saisi « $saisi », tickets : $proches)", saved)
    }

    @Test
    fun k02_modification_bouton_enregistrer_atteignable_clavier_ouvert() {
        // Ticket propre à ce test (indépendant de k01)
        runBlocking {
            AppDatabase.getInstance(ctx).ticketDao().insert(com.etix.model.Ticket(store = "$store modif", amount = 2.0,
                category = "Autre", dateMillis = System.currentTimeMillis()))
        }
        startMain()
        onView(withId(R.id.menu_history)).perform(click())
        onView(withId(R.id.inputSearch)).perform(replaceText("$store modif"))
        E2e.closeKeyboard()
        // Le champ de recherche contient aussi le texte : on vise la ligne de la liste
        val row = allOf(withText("$store modif"), isDescendantOfA(withId(R.id.recyclerHistory)))
        waitFor(row)
        onView(allOf(row, isDisplayed())).perform(click())
        waitFor(allOf(withId(R.id.btnEdit), isDescendantOfA(withId(R.id.overlayContainer))))
        onView(allOf(withId(R.id.btnEdit), isDescendantOfA(withId(R.id.overlayContainer)))).perform(E2e.nestedScrollTo(), click())
        val amount = allOf(withId(R.id.inputAmount), isDescendantOfA(withId(R.id.overlayContainer)))
        waitFor(amount)
        onView(amount).perform(scrollTo(), click(), clearText(), typeText("5,30"))
        assertTrue("Clavier non affiché", waitImeStable() > 0)
        val save = allOf(withId(R.id.btnSave), isDescendantOfA(withId(R.id.overlayContainer)))
        val avant = mesure(R.id.btnSave)
        onView(save).perform(scrollTo())
        waitImeStable()
        val apres = mesure(R.id.btnSave)
        log("Modifier", avant, apres)
        shot("k02_modification_clavier_${passe}")
        assertTrue("Bouton Enregistrer masqué par le clavier après défilement : $apres", apres.visible)
        onView(save).perform(click())
        waitFor(allOf(withId(R.id.tvAmount), isDescendantOfA(withId(R.id.overlayContainer)), withText("5,30 €")))
    }
}
