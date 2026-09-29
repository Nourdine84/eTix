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
}
