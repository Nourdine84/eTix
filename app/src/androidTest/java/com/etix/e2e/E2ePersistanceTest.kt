package com.etix.e2e

import android.content.Intent
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.etix.R
import com.etix.SplashActivity
import com.etix.e2e.E2e.ctx
import com.etix.e2e.E2e.shot
import com.etix.e2e.E2e.waitFor
import org.hamcrest.Matchers.allOf
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Lancé par la CI APRÈS `am force-stop com.etix` (processus tué) et après l'installation d'eTix QA à côté :
 * la session et le ticket modifié doivent être intacts.
 */
@RunWith(AndroidJUnit4::class)
class E2ePersistanceTest {

    @Test
    fun donnees_conservees_apres_arret_du_processus() {
        ctx.startActivity(Intent(ctx, SplashActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        E2e.waitForAppReady("E2ePersistanceTest"); waitFor(withId(R.id.bottomNav))          // session conservée : pas d'onboarding ni de connexion
        waitFor(withText("1 ticket enregistré"))
        onView(withId(R.id.menu_history)).perform(click())
        waitFor(allOf(withText("Boulangerie Test"), isDisplayed()))
        waitFor(allOf(withText("15,75 €"), isDisplayed()))
        shot("27_apres_redemarrage")
    }
}
