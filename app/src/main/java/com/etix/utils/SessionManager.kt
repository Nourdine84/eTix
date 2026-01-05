package com.etix.utils

import android.content.Context

class SessionManager(context: Context) {

    private val prefs =
        context.getSharedPreferences("etix_session", Context.MODE_PRIVATE)

    fun login(username: String) {
        prefs.edit()
            .putBoolean("logged_in", true)
            .putString("username", username)
            .apply()
    }

    fun logout() {
        prefs.edit().clear().apply()
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean("logged_in", false)
    }

    fun getUsername(): String {
        return prefs.getString("username", "Utilisateur") ?: "Utilisateur"
    }
}
