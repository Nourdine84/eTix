package com.etix.ui.home

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.etix.R

class HomeFragmentV2 : Fragment(R.layout.fragment_home_v2) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvToday = view.findViewById<TextView>(R.id.tvTodayAmount)
        val tvMonth = view.findViewById<TextView>(R.id.tvMonthAmount)

        // 🧪 Fake KPI (PACK 4 = vrai data Room)
        tvToday.text = "12,40 €"
        tvMonth.text = "214,90 €"

        view.findViewById<Button>(R.id.btnAddTicket).setOnClickListener {
            findNavController().navigate(R.id.menu_add)
        }

        view.findViewById<Button>(R.id.btnHistory).setOnClickListener {
            findNavController().navigate(R.id.menu_history)
        }
    }
}
