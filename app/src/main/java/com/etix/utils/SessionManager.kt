package com.etix.utils

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "etix_prefs"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun login(username: String, password: String) {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_USERNAME, username)
            .putString(KEY_PASSWORD, password)
            .apply()
    }

    fun logout() {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .remove(KEY_USERNAME)
            .remove(KEY_PASSWORD)
            .apply()
    }

    fun isLoggedIn(): Boolean =
        prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    fun getUsername(): String =
        prefs.getString(KEY_USERNAME, "Utilisateur") ?: "Utilisateur"
}
