package com.etix.features.settings

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.etix.features.store.TimeRange
import com.etix.utils.SessionManager
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Lot 10 — valeurs par défaut et conservation des préférences existantes (thème, période). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AppPreferencesTest {

    private val ctx get() = RuntimeEnvironment.getApplication()

    @Before fun clean() {
        ctx.getSharedPreferences(AppPreferences.PREFS, Context.MODE_PRIVATE).edit().clear().commit()
        ctx.getSharedPreferences("etix_session", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test fun sans_preference_theme_systeme_et_periode_ce_mois() {
        assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, SessionManager(ctx).getThemeMode())
        assertEquals(ThemeChoice.SYSTEM, ThemeChoice.fromMode(SessionManager(ctx).getThemeMode()))
        assertEquals(TimeRange.MONTH, AppPreferences(ctx).defaultRange)
    }

    /** Choix enregistré par l'ancien bouton « Changer de thème » (clair / sombre) : conservé tel quel. */
    @Test fun choix_existant_conserve() {
        SessionManager(ctx).setThemeMode(AppCompatDelegate.MODE_NIGHT_NO)
        assertEquals(ThemeChoice.LIGHT, ThemeChoice.fromMode(SessionManager(ctx).getThemeMode()))
        SessionManager(ctx).setThemeMode(AppCompatDelegate.MODE_NIGHT_YES)
        assertEquals(ThemeChoice.DARK, ThemeChoice.fromMode(SessionManager(ctx).getThemeMode()))
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, SessionManager(ctx).getThemeMode())
    }

    @Test fun mode_inconnu_affiche_systeme() {
        assertEquals(ThemeChoice.SYSTEM, ThemeChoice.fromMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY))
        assertEquals(ThemeChoice.SYSTEM, ThemeChoice.fromMode(AppCompatDelegate.MODE_NIGHT_UNSPECIFIED))
        assertEquals(ThemeChoice.SYSTEM, ThemeChoice.fromMode(42))
    }

    @Test fun periode_enregistree_relue_par_une_nouvelle_instance() {
        AppPreferences(ctx).defaultRange = TimeRange.YEAR
        assertEquals(TimeRange.YEAR, AppPreferences(ctx).defaultRange)
        AppPreferences(ctx).defaultRange = TimeRange.TODAY
        assertEquals(TimeRange.TODAY, AppPreferences(ctx).defaultRange)
    }

    @Test fun periode_illisible_donne_ce_mois() {
        ctx.getSharedPreferences(AppPreferences.PREFS, Context.MODE_PRIVATE).edit()
            .putString(AppPreferences.KEY_DEFAULT_RANGE, "SEMAINE").commit()
        assertEquals(TimeRange.MONTH, AppPreferences(ctx).defaultRange)
    }

    @Test fun periode_initiale_d_un_ecran() {
        // Écran neuf : période par défaut
        assertEquals(TimeRange.YEAR, AppPreferences.initialRange(null, null, TimeRange.YEAR))
        // Écran recréé, réglage inchangé : le choix fait sur l'écran est conservé
        assertEquals(TimeRange.TODAY, AppPreferences.initialRange(TimeRange.TODAY, TimeRange.MONTH, TimeRange.MONTH))
        // Réglage modifié entre-temps : nouvelle période par défaut
        assertEquals(TimeRange.YEAR, AppPreferences.initialRange(TimeRange.TODAY, TimeRange.MONTH, TimeRange.YEAR))
        // État enregistré par une version précédente (sans période par défaut) : période par défaut
        assertEquals(TimeRange.MONTH, AppPreferences.initialRange(TimeRange.TODAY, null, TimeRange.MONTH))
    }

    @Test fun ecoute_du_changement_de_periode() {
        val seen = mutableListOf<TimeRange>()
        val p = AppPreferences(ctx)
        val l = p.listenDefaultRange { seen += it }
        p.defaultRange = TimeRange.YEAR
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        p.stopListening(l)
        p.defaultRange = TimeRange.TODAY
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        assertEquals(listOf(TimeRange.YEAR), seen)
    }
}
