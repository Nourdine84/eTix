// 📁 com.etix.utils.SessionManager.kt
package com.etix.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

class SessionManager(context: Context) {

    private val prefs =
        context.getSharedPreferences("etix_session", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LOGGED_IN = "logged_in"
        private const val KEY_USERNAME = "username"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_THEME_MODE = "theme_mode"
    }

    // 🔐 AUTH
    fun login(username: String) {
        prefs.edit()
            .putBoolean(KEY_LOGGED_IN, true)
            .putString(KEY_USERNAME, username)
            .apply()
    }

    fun logout() {
        prefs.edit()
            .remove(KEY_LOGGED_IN)
            .remove(KEY_USERNAME)
            .apply()
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_LOGGED_IN, false)
    }

    fun getUsername(): String {
        return prefs.getString(KEY_USERNAME, "Utilisateur") ?: "Utilisateur"
    }

    // 🚀 FIRST LAUNCH / ONBOARDING
    fun isFirstLaunch(): Boolean {
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true)
    }

    fun markFirstLaunchDone() {
        prefs.edit()
            .putBoolean(KEY_FIRST_LAUNCH, false)
            .apply()
    }

    // 🌗 THEME (SOURCE UNIQUE)
    fun getThemeMode(): Int {
        return prefs.getInt(
            KEY_THEME_MODE,
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        )
    }

    fun setThemeMode(mode: Int) {
        prefs.edit()
            .putInt(KEY_THEME_MODE, mode)
            .apply()
    }
}
