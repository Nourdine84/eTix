package com.etix.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.etix.LoginActivity
import com.etix.R

class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 🔁 Layout lié au fragment : fragment_settings.xml
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        val logoutButton = view.findViewById<Button>(R.id.buttonLogout)
        logoutButton.setOnClickListener {
            Toast.makeText(requireContext(), "Déconnexion réussie", Toast.LENGTH_SHORT).show()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        return view
    }
}
