package com.etix.fragments

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.etix.ui.home.HomeFragmentV2
import com.etix.ui.add.AddTicketFragmentV2
import com.etix.ui.history.TicketHistoryFragmentV2
import com.etix.ui.category.CategoryFragmentV2
import com.etix.ui.store.StoreListFragment

class FragmentAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 5

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> HomeFragmentV2()
            1 -> AddTicketFragmentV2()
            2 -> TicketHistoryFragmentV2()
            // Lot 5 : Catégories V2 (iOS CategoryView). L'écran V1 CategoryFragment reste dans le code, non branché.
            3 -> CategoryFragmentV2()
            // Lot 2 : Magasins (parité iOS). Réglages → accessibles depuis l'Accueil (MainActivityV2.openSettings)
            4 -> StoreListFragment()
            else -> HomeFragmentV2()
        }
    }
}
