package com.etix.fragments

import android.content.ActivityNotFoundException
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.etix.CrashLogs
import com.etix.R
import com.etix.data.AppDatabase
import com.etix.data.TicketRepository
import com.etix.features.settings.AppPreferences
import com.etix.features.settings.ThemeChoice
import com.etix.features.store.TimeRange
import com.etix.ui.login.LoginActivity
import com.etix.utils.CsvExporter
import com.etix.utils.SessionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Réglages — lot 10, référence iOS SettingsView : Apparence (thème Système / Clair / Sombre), Affichage (période
 * par défaut), Données (export CSV de tous les tickets par le partage Android, suppression globale désactivée,
 * compteur), Informations (version, build). Sections Android conservées en fin d'écran : Compte (connexion locale,
 * présentée comme non protectrice) et Diagnostic (journaux de plantage, jamais envoyés).
 * Ouverts par l'engrenage de l'Accueil (pas de 6ᵉ onglet). Aucune écriture en base.
 */
class SettingsFragment : Fragment() {

    private lateinit var session: SessionManager
    private lateinit var prefs: AppPreferences

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        session = SessionManager(requireContext())
        prefs = AppPreferences(requireContext())

        view.findViewById<View>(R.id.btnSettingsBack).setOnClickListener { parentFragmentManager.popBackStack() }

        // Apparence : choix appliqué tout de suite (AppCompat recrée l'écran si le thème change) et conservé
        val tvTheme = view.findViewById<TextView>(R.id.tvThemeValue)
        tvTheme.text = currentTheme().label
        view.findViewById<View>(R.id.rowTheme).setOnClickListener {
            choose("Thème", ThemeChoice.values().map { it.label }, currentTheme().ordinal) { i ->
                val choice = ThemeChoice.values()[i]
                if (choice == currentTheme()) return@choose
                session.setThemeMode(choice.mode)
                tvTheme.text = choice.label
                AppCompatDelegate.setDefaultNightMode(choice.mode)
            }
        }

        // Période par défaut : appliquée à l'ouverture de l'Accueil, des Catégories et des Magasins
        val tvRange = view.findViewById<TextView>(R.id.tvDefaultRangeValue)
        tvRange.text = prefs.defaultRange.title
        view.findViewById<View>(R.id.rowDefaultRange).setOnClickListener {
            choose("Période par défaut", RANGES.map { it.title }, RANGES.indexOf(prefs.defaultRange)) { i ->
                prefs.defaultRange = RANGES[i]
                tvRange.text = RANGES[i].title
            }
        }

        // Données : export de tous les tickets ; compteur ; suppression globale désactivée (règle validée)
        val repository = TicketRepository(AppDatabase.getInstance(requireContext()).ticketDao())
        val btnExport = view.findViewById<Button>(R.id.btnExportAllCsv)
        val tvCount = view.findViewById<TextView>(R.id.tvSettingsTicketCount)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.getAllFlow().collect { tickets ->
                    tvCount.text = "${tickets.size} ticket(s) enregistré(s)"
                    btnExport.isEnabled = tickets.isNotEmpty()
                }
            }
        }
        btnExport.setOnClickListener { exportAll(repository) }
        view.findViewById<Button>(R.id.btnClearAll).isEnabled = false

        // Informations
        val (versionName, versionCode) = appVersion()
        view.findViewById<TextView>(R.id.textVersion).text = versionName
        view.findViewById<TextView>(R.id.textBuild).text = versionCode.toString()

        // Compte (connexion locale, conservée telle quelle)
        view.findViewById<TextView>(R.id.textUser).text = "Connecté en tant que : ${session.getUsername()}"
        view.findViewById<Button>(R.id.btnLogout).setOnClickListener {
            session.logout()
            startActivity(Intent(requireActivity(), LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            requireActivity().finish()
        }

        // Diagnostic : journaux de plantage locaux, affichés à la demande, jamais transmis
        val crashPreview = view.findViewById<TextView>(R.id.textCrashPreview)
        view.findViewById<Button>(R.id.btnShowCrash).setOnClickListener {
            val file = CrashLogs.latestLog(requireContext())
            if (file == null) {
                crashPreview.visibility = View.GONE
                Toast.makeText(requireContext(), "Aucun journal de plantage", Toast.LENGTH_SHORT).show()
            } else {
                crashPreview.text = CrashLogs.readFile(file).take(4000)
                crashPreview.visibility = View.VISIBLE
            }
        }
        view.findViewById<Button>(R.id.btnClearCrash).setOnClickListener {
            CrashLogs.deleteAll(requireContext())
            crashPreview.text = ""
            crashPreview.visibility = View.GONE
            Toast.makeText(requireContext(), "Journaux de plantage supprimés", Toast.LENGTH_SHORT).show()
        }
    }

    private fun exportAll(repository: TicketRepository) {
        viewLifecycleOwner.lifecycleScope.launch {
            val tickets = repository.getAllFlow().first()
            if (tickets.isEmpty()) {
                Toast.makeText(requireContext(), "Aucun ticket à exporter", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val ctx = requireContext().applicationContext
            val file = withContext(Dispatchers.IO) { CsvExporter.writeFile(ctx, tickets, "eTix_tous_les_tickets") }
            try {
                startActivity(CsvExporter.shareIntent(requireContext(), file, "Exporter tous les tickets"))
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(requireContext(), "Aucune application ne peut recevoir le fichier", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun currentTheme() = ThemeChoice.fromMode(session.getThemeMode())

    /** Liste à choix unique (équivalent Android du Picker iOS) : un appui choisit et ferme ; « Annuler » ne change rien. */
    private fun choose(title: String, labels: List<String>, checked: Int, onChoice: (Int) -> Unit) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setSingleChoiceItems(labels.toTypedArray(), checked) { d, which ->
                d.dismiss()
                // Après la fermeture de la liste : un changement de thème recrée l'écran, la liste ne doit plus y être
                view?.post { if (isAdded) onChoice(which) }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun appVersion(): Pair<String, Long> {
        val ctx = requireContext()
        return try {
            val info = if (Build.VERSION.SDK_INT >= 33) {
                ctx.packageManager.getPackageInfo(ctx.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION") ctx.packageManager.getPackageInfo(ctx.packageName, 0)
            }
            val code = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode
            else @Suppress("DEPRECATION") info.versionCode.toLong()
            (info.versionName ?: "?") to code
        } catch (_: Exception) {
            "?" to 0L
        }
    }

    private companion object {
        val RANGES = TimeRange.values().toList()
    }
}
