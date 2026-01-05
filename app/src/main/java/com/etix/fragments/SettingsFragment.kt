package com.etix.fragments

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.etix.LoginActivity
import com.etix.R
import com.etix.data.AppDatabase
import com.etix.data.TicketRepository
import com.etix.utils.CrashLogs
import com.etix.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsFragment : Fragment() {

    private lateinit var textUser: TextView
    private lateinit var textVersion: TextView
    private lateinit var textCrashPreview: TextView
    private lateinit var btnToggleTheme: Button
    private lateinit var btnClearAll: Button
    private lateinit var btnShowCrash: Button
    private lateinit var btnClearCrash: Button
    private lateinit var btnLogout: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val v = inflater.inflate(R.layout.fragment_settings, container, false)

        textUser = v.findViewById(R.id.textUser)
        textVersion = v.findViewById(R.id.textVersion)
        textCrashPreview = v.findViewById(R.id.textCrashPreview)
        btnToggleTheme = v.findViewById(R.id.btnToggleTheme)
        btnClearAll = v.findViewById(R.id.btnClearAll)
        btnShowCrash = v.findViewById(R.id.btnShowCrash)
        btnClearCrash = v.findViewById(R.id.btnClearCrash)
        btnLogout = v.findViewById(R.id.btnLogout)

        val session = SessionManager(requireContext())

        // 👤 Utilisateur connecté
        textUser.text = "Connecté en tant que : ${session.getUsername()}"

        // ℹ️ Version app
        val (versionName, versionCode) = getAppVersionSafe()
        textVersion.text = "Version $versionName ($versionCode)"

        // 🌗 Changer thème
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

        // 🧹 Vider la base Room
        btnClearAll.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                val dao = AppDatabase.getInstance(requireContext()).ticketDao()
                val repo = TicketRepository(dao)
                repo.deleteAll()
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Base vidée", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 🐞 Afficher dernier crash log
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

        // 🗑️ Supprimer tous les crash logs
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

        // 🔐 Déconnexion propre (clear session + clear backstack)
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
