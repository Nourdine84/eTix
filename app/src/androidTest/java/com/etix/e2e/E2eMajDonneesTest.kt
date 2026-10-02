package com.etix.e2e

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.etix.data.AppDatabase
import com.etix.e2e.E2e.ctx
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Mise à jour lot 8 (versionCode 10) → lot 9 (11) sans désinstallation : instantané complet des tickets et des
 * budgets AVANT (app du lot 8, données créées par les tests du lot 8), comparé APRÈS l'installation du lot 9.
 * Lecture seule ; utilise uniquement des classes identiques dans les deux versions (Room, SharedPreferences).
 */
internal object MajDonnees {
    private val file get() = File(File(ctx.filesDir, "maj").apply { mkdirs() }, "instantane_avant.txt")

    fun snapshot(): String {
        val tickets = runBlocking { AppDatabase.getInstance(ctx).ticketDao().getAllFlow().first() }.sortedBy { it.id }
        val budgets = ctx.getSharedPreferences("etix_budgets", Context.MODE_PRIVATE).all.toSortedMap()
        return buildString {
            appendLine("tickets=${tickets.size}")
            tickets.forEach { appendLine(it.toString()) }
            appendLine("budgets=${budgets.size}")
            budgets.forEach { (k, v) -> appendLine("$k=$v") }
        }
    }

    fun save(s: String) = file.writeText(s)
    fun saved(): String = file.readText()
    fun counts(s: String) = s.lines().filter { it.startsWith("tickets=") || it.startsWith("budgets=") }.joinToString(", ")
}

@RunWith(AndroidJUnit4::class)
class E2eMajInstantaneAvantTest {
    @Test fun instantane_des_donnees_du_lot_8() {
        val s = MajDonnees.snapshot()
        assertTrue("données du lot 8 attendues (tickets et budget)", !s.startsWith("tickets=0") && !s.contains("budgets=0"))
        MajDonnees.save(s)
        ScanE2e.log("maj_donnees", "avant (lot 8, versionCode ${ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionCode}) : ${MajDonnees.counts(s)}")
    }
}

@RunWith(AndroidJUnit4::class)
class E2eMajInstantaneApresTest {
    @Test fun donnees_identiques_apres_mise_a_jour_lot_9() {
        val avant = MajDonnees.saved()
        val apres = MajDonnees.snapshot()
        @Suppress("DEPRECATION")
        val code = ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionCode
        ScanE2e.log("maj_donnees", "après (lot 9, versionCode $code) : ${MajDonnees.counts(apres)} ; identiques=${avant == apres}")
        assertEquals("tickets et budgets inchangés par la mise à jour", avant, apres)
    }
}
