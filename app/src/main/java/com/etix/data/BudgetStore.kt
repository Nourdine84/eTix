package com.etix.data

import android.content.Context
import com.etix.features.budget.BudgetRules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject

/**
 * Budgets mensuels par catégorie — iOS BudgetStore (UserDefaults « categoryBudgets », JSON { clé minuscule: limite }).
 * Stockage ADDITIF : fichier de préférences dédié « etix_budgets », indépendant de la base Room des tickets
 * (aucune migration, aucune écriture dans les tickets ni les catégories).
 */
class BudgetStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Donnée illisible → aucun budget (iOS : échec de décodage → [:]) ; la valeur illisible n'est pas effacée. */
    fun load(): Map<String, Double> {
        val raw = prefs.getString(KEY, null) ?: return emptyMap()
        return try {
            val o = JSONObject(raw)
            o.keys().asSequence().mapNotNull { k -> o.optDouble(k).takeIf { !it.isNaN() && it > 0 }?.let { k to it } }.toMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun limit(category: String): Double? = load()[BudgetRules.key(category)]

    /** limite > 0 : enregistre ; null ou ≤ 0 : retire le budget de CETTE catégorie uniquement. */
    fun set(category: String, limit: Double?) {
        val m = load().toMutableMap()
        val k = BudgetRules.key(category)
        if (limit != null && limit > 0) m[k] = limit else m.remove(k)
        prefs.edit().putString(KEY, JSONObject(m as Map<*, *>).toString()).commit()
        _version.value = _version.value + 1
    }

    companion object {
        private const val PREFS = "etix_budgets"
        private const val KEY = "categoryBudgets"
        private val _version = MutableStateFlow(0)
        /** Incrémenté à chaque modification : les écrans recombinent leurs données. */
        val version: StateFlow<Int> get() = _version
    }
}
