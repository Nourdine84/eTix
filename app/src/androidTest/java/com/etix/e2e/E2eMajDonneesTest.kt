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
 * Mise à jour d'une version fusionnée (lot 8, versionCode 10 ; lot 9, versionCode 11) vers cette version sans
 * désinstallation : instantané complet des tickets, des budgets et du thème enregistré AVANT (app de base, données
 * créées par ses propres tests), comparé APRÈS l'installation. Lecture seule ; utilise uniquement des classes
 * identiques dans les versions comparées (Room, SharedPreferences).
 */
internal object MajDonnees {
    private val file get() = File(File(ctx.filesDir, "maj").apply { mkdirs() }, "instantane_avant.txt")

    fun snapshot(): String {
        val tickets = runBlocking { AppDatabase.getInstance(ctx).ticketDao().getAllFlow().first() }.sortedBy { it.id }
        val budgets = ctx.getSharedPreferences("etix_budgets", Context.MODE_PRIVATE).all.toSortedMap()
        // Lot 10 : thème enregistré (« theme_mode » des préférences de session ; absent = Système)
        val theme = ctx.getSharedPreferences("etix_session", Context.MODE_PRIVATE).all["theme_mode"]
        return buildString {
            appendLine("tickets=${tickets.size}")
            tickets.forEach { appendLine(it.toString()) }
            appendLine("budgets=${budgets.size}")
            budgets.forEach { (k, v) -> appendLine("$k=$v") }
            appendLine("theme_mode=${theme ?: "absent"}")
        }
    }

    fun save(s: String) = file.writeText(s)
    fun saved(): String = file.readText()
    fun counts(s: String) = s.lines()
        .filter { it.startsWith("tickets=") || it.startsWith("budgets=") || it.startsWith("theme_mode=") }.joinToString(", ")

    /** Thème enregistré dans l'instantané d'avant la mise à jour (null : aucune préférence). */
    fun savedThemeMode(): Int? = saved().lines().firstOrNull { it.startsWith("theme_mode=") }
        ?.substringAfter('=')?.toIntOrNull()
}

@RunWith(AndroidJUnit4::class)
class E2eMajInstantaneAvantTest {
    @Test fun instantane_des_donnees_de_la_base() {
        val s = MajDonnees.snapshot()
        assertTrue("données de la base attendues (tickets et budget)", !s.startsWith("tickets=0") && !s.contains("budgets=0"))
        MajDonnees.save(s)
        ScanE2e.log("maj_donnees", "avant (base, versionCode ${ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionCode}) : ${MajDonnees.counts(s)}")
    }
}

@RunWith(AndroidJUnit4::class)
class E2eMajInstantaneApresTest {
    @Test fun donnees_identiques_apres_mise_a_jour() {
        val avant = MajDonnees.saved()
        val apres = MajDonnees.snapshot()
        @Suppress("DEPRECATION")
        val code = ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionCode
        ScanE2e.log("maj_donnees", "après (cette version, versionCode $code) : ${MajDonnees.counts(apres)} ; identiques=${avant == apres}")
        assertEquals("tickets, budgets et thème inchangés par la mise à jour", avant, apres)
    }
}
