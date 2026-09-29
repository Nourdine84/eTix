package com.etix.data

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BudgetStoreTest {

    private val ctx get() = RuntimeEnvironment.getApplication()
    private fun prefs() = ctx.getSharedPreferences("etix_budgets", Context.MODE_PRIVATE)

    @Before fun clean() { prefs().edit().clear().commit() }

    @Test fun enregistre_lit_et_retire_par_cle_minuscule() {
        val s = BudgetStore(ctx)
        assertTrue(s.load().isEmpty())
        s.set("Courses", 300.0)
        s.set("Santé", 12.5)
        assertEquals(mapOf("courses" to 300.0, "santé" to 12.5), s.load())
        assertEquals(300.0, s.limit("COURSES")!!, 0.0)
        s.set("courses", null)                         // retire uniquement « courses »
        assertEquals(mapOf("santé" to 12.5), s.load())
        s.set("Santé", 0.0)                            // ≤ 0 = retrait (iOS)
        assertTrue(s.load().isEmpty())
    }

    @Test fun persiste_pour_une_nouvelle_instance() {
        BudgetStore(ctx).set("Loisirs", 45.9)
        assertEquals(45.9, BudgetStore(ctx).limit("loisirs")!!, 0.0)
    }

    @Test fun donnee_illisible_aucun_budget_et_pas_d_effacement() {
        prefs().edit().putString("categoryBudgets", "{corrompu").commit()
        assertTrue(BudgetStore(ctx).load().isEmpty())
        assertEquals("{corrompu", prefs().getString("categoryBudgets", null))
        assertNull(BudgetStore(ctx).limit("x"))
    }

    @Test fun version_incrementee_a_chaque_modification() {
        val v0 = BudgetStore.version.value
        BudgetStore(ctx).set("A", 1.0)
        assertEquals(v0 + 1, BudgetStore.version.value)
    }

    @Test fun ne_touche_pas_aux_autres_preferences() {
        val session = ctx.getSharedPreferences("etix_session", Context.MODE_PRIVATE)
        session.edit().putString("temoin", "ok").commit()
        BudgetStore(ctx).set("A", 1.0)
        assertEquals("ok", session.getString("temoin", null))
    }
}
