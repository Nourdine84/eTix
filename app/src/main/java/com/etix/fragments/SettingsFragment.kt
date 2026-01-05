package com.etix.fragments

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val v = inflater.inflate(R.layout.fragment_settings, container, false)

        val session = SessionManager(requireContext())

        val textUser = v.findViewById<TextView>(R.id.textUser)
        val textVersion = v.findViewById<TextView>(R.id.textVersion)
        val textCrashPreview = v.findViewById<TextView>(R.id.textCrashPreview)

        val btnToggleTheme = v.findViewById<Button>(R.id.btnToggleTheme)
        val btnShowCrash = v.findViewById<Button>(R.id.btnShowCrash)
        val btnClearCrash = v.findViewById<Button>(R.id.btnClearCrash)
        val btnLogout = v.findViewById<Button>(R.id.btnLogout)

        textUser.text = "Connecté en tant que : ${session.getUsername()}"

        val (versionName, versionCode) = getAppVersionSafe()
        textVersion.text = "Version $versionName ($versionCode)"

        btnToggleTheme.setOnClickListener {
            val currentMode = AppCompatDelegate.getDefaultNightMode()
            val newMode =
                if (currentMode == AppCompatDelegate.MODE_NIGHT_YES)
                    AppCompatDelegate.MODE_NIGHT_NO
                else
                    AppCompatDelegate.MODE_NIGHT_YES

            AppCompatDelegate.setDefaultNightMode(newMode)
            Toast.makeText(requireContext(), "Thème mis à jour", Toast.LENGTH_SHORT).show()
        }

        btnShowCrash.setOnClickListener {
            val f = CrashLogs.latestLog(requireContext())
            if (f == null) {
                textCrashPreview.text = ""
                textCrashPreview.visibility = View.GONE
                Toast.makeText(requireContext(), "Aucun crash log", Toast.LENGTH_SHORT).show()
            } else {
                textCrashPreview.text = CrashLogs.readFile(f).take(4000)
                textCrashPreview.visibility = View.VISIBLE
                Toast.makeText(requireContext(), "Log: ${f.name}", Toast.LENGTH_SHORT).show()
            }
        }

        btnClearCrash.setOnClickListener {
            val ok = CrashLogs.deleteAll(requireContext())
            textCrashPreview.text = ""
            textCrashPreview.visibility = View.GONE
            Toast.makeText(
                requireContext(),
                if (ok) "Crash logs supprimés" else "Échec suppression",
                Toast.LENGTH_SHORT
            ).show()
        }

        btnLogout.setOnClickListener {
            session.logout()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        return v
    }

    private fun getAppVersionSafe(): Pair<String, Long> {
        val ctx = requireContext()
        val pm = ctx.packageManager
        val pkg = ctx.packageName

        return try {
            val pInfo = if (Build.VERSION.SDK_INT >= 33) {
                pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(pkg, 0)
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
