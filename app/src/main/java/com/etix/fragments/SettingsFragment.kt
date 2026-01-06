// 📁 com.etix.fragments.SettingsFragment.kt
package com.etix.fragments

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.etix.CrashLogs
import com.etix.R
import com.etix.ui.login.LoginActivity
import com.etix.utils.SessionManager

class SettingsFragment : Fragment() {

    private lateinit var session: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d("SETTINGS", "SettingsFragment ATTACHÉ")

        session = SessionManager(requireContext())

        val textUser = view.findViewById<TextView>(R.id.textUser)
        val textVersion = view.findViewById<TextView>(R.id.textVersion)
        val textCrashPreview = view.findViewById<TextView>(R.id.textCrashPreview)

        val btnToggleTheme = view.findViewById<Button>(R.id.btnToggleTheme)
        val btnShowCrash = view.findViewById<Button>(R.id.btnShowCrash)
        val btnClearCrash = view.findViewById<Button>(R.id.btnClearCrash)
        val btnLogout = view.findViewById<Button>(R.id.btnLogout)

        // 👤 Utilisateur connecté
        textUser.text = "Connecté en tant que : ${session.getUsername()}"

        // ℹ️ Version app
        val (versionName, versionCode) = getAppVersionSafe()
        textVersion.text = "Version $versionName ($versionCode)"

        // 🌗 Toggle thème (SOURCE UNIQUE = SessionManager)
        btnToggleTheme.setOnClickListener {

            val currentMode = session.getThemeMode()

            val newMode =
                if (currentMode == AppCompatDelegate.MODE_NIGHT_YES)
                    AppCompatDelegate.MODE_NIGHT_NO
                else
                    AppCompatDelegate.MODE_NIGHT_YES

            // 🔐 Sauvegarde centrale
            session.setThemeMode(newMode)

            // 🌗 Application immédiate
            AppCompatDelegate.setDefaultNightMode(newMode)

            Toast.makeText(requireContext(), "Thème appliqué", Toast.LENGTH_SHORT).show()

            // 🔄 Recréation Activity (ViewPager-safe)
            activity?.recreate()
        }

        // 🐞 Afficher crash log
        btnShowCrash.setOnClickListener {
            val file = CrashLogs.latestLog(requireContext())
            if (file == null) {
                textCrashPreview.visibility = View.GONE
                Toast.makeText(requireContext(), "Aucun crash log", Toast.LENGTH_SHORT).show()
            } else {
                textCrashPreview.text = CrashLogs.readFile(file).take(4000)
                textCrashPreview.visibility = View.VISIBLE
            }
        }

        // 🗑️ Supprimer crash logs
        btnClearCrash.setOnClickListener {
            CrashLogs.deleteAll(requireContext())
            textCrashPreview.text = ""
            textCrashPreview.visibility = View.GONE
            Toast.makeText(requireContext(), "Crash logs supprimés", Toast.LENGTH_SHORT).show()
        }

        // 🔐 Logout propre (ViewPager-safe)
        btnLogout.setOnClickListener {
            session.logout()

            val intent = Intent(requireActivity(), LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            requireActivity().finish()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("SETTINGS", "SettingsFragment VISIBLE")
    }

    private fun getAppVersionSafe(): Pair<String, Long> {
        val ctx = requireContext()
        val pm = ctx.packageManager

        return try {
            val pInfo = if (Build.VERSION.SDK_INT >= 33) {
                pm.getPackageInfo(ctx.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(ctx.packageName, 0)
            }

            val name = pInfo.versionName ?: "0.0"
            val code =
                if (Build.VERSION.SDK_INT >= 28) pInfo.longVersionCode
                else @Suppress("DEPRECATION") pInfo.versionCode.toLong()

            name to code
        } catch (_: Exception) {
            "0.0" to 0L
        }
    }
}
