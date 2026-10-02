package com.etix.ui.add

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.etix.data.AppDatabase
import com.etix.data.TicketRepository
import com.etix.model.Ticket
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Lot 9 — enregistrement depuis « Ajouter », conservé à la rotation :
 * - un seul enregistrement à la fois : un 2e appui sur « Enregistrer » pendant l'insertion est ignoré
 *   (avant : deux appuis rapprochés créaient deux tickets) ;
 * - l'insertion ne dépend pas de l'écran : si l'écran est recréé pendant l'insertion, le ticket est enregistré
 *   une seule fois et le nouvel écran reçoit le résultat (formulaire vidé), sans doublon.
 */
class AddTicketSaveViewModel(app: Application) : AndroidViewModel(app) {

    sealed interface Result {
        data object Saved : Result
        data object Failed : Result
    }

    private val repository = TicketRepository(AppDatabase.getInstance(app).ticketDao())
    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving
    private val _results = Channel<Result>(Channel.BUFFERED)
    /** Résultats non encore traités par l'écran (conservés si l'écran est recréé entre-temps). */
    val results = _results.receiveAsFlow()

    /** Renvoie false si un enregistrement est déjà en cours (appel ignoré). Appelé sur le thread principal. */
    fun save(ticket: Ticket): Boolean {
        if (_saving.value) return false
        _saving.value = true
        viewModelScope.launch {
            val r = try {
                repository.insert(ticket); Result.Saved
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.w("eTixAdd", "échec de l'enregistrement : ${e.javaClass.simpleName}")
                Result.Failed
            }
            _results.send(r)   // « saving » reste vrai jusqu'à ce que l'écran ait traité le résultat
        }
        return true
    }

    /** L'écran a traité le résultat (formulaire vidé ou erreur affichée) : un nouvel enregistrement est possible. */
    fun acknowledge() { _saving.value = false }
}
