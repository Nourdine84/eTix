package com.etix.fragments

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.etix.ui.home.HomeFragmentV2
import com.etix.ui.add.AddTicketFragmentV2
import com.etix.ui.history.TicketHistoryFragmentV2
import com.etix.ui.store.StoreListFragment

class FragmentAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 5

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> HomeFragmentV2()
            1 -> AddTicketFragmentV2()
            2 -> TicketHistoryFragmentV2()
            // CategoryFragmentV2 est un placeholder vide (empty_layout) :
            // on branche la version V1 fonctionnelle en attendant sa refonte V2.
            3 -> CategoryFragment()
            // Lot 2 : Magasins (parité iOS). Réglages → accessibles depuis l'Accueil (MainActivityV2.openSettings)
            4 -> StoreListFragment()
            else -> HomeFragmentV2()
        }
    }
}
