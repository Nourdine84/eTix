package com.etix.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.etix.R
import com.etix.AddTicketActivity // ✅ AJOUT IMPORT ESSENTIEL

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // ✅ Références aux éléments de la vue
        val quickAddButton = view.findViewById<Button>(R.id.buttonQuickAdd)
        val welcomeText = view.findViewById<TextView>(R.id.textWelcome)
        val summaryText = view.findViewById<TextView>(R.id.textSummary)

        // ✅ Action du bouton
        quickAddButton.setOnClickListener {
            val intent = Intent(requireContext(), AddTicketActivity::class.java)
            startActivity(intent)
        }

        // ✅ Messages par défaut
        welcomeText.text = "Bienvenue, utilisateur !"
        summaryText.text = "Total ce mois : 123,45 €"

        return view
    }
}
