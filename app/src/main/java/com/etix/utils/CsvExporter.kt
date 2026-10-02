package com.etix.utils

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.etix.model.Ticket
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Export CSV des tickets (lot 10), format aligné sur iOS (SettingsViewModel.exportAllTickets) :
 * en-tête `Date,Magasin,Montant (€),Catégorie,Description`, séparateur virgule, date courte française jj/mm/aaaa,
 * montant à 2 décimales avec un point (« 23.45 »), UTF-8, une ligne par ticket (« \n »). Un champ contenant une
 * virgule, un guillemet ou un retour à la ligne est entouré de guillemets, les guillemets internes doublés.
 * Champs texte (magasin, catégorie, description) commençant par = + - @, une tabulation ou un retour chariot :
 * précédés d'une apostrophe dans le fichier, pour qu'un tableur ne les interprète pas comme formules (écart iOS
 * voulu, demande de Nourdine du 02/10/2026). Lecture seule : aucun ticket n'est modifié, seul le fichier diffère.
 */
object CsvExporter {

    const val HEADER = "Date,Magasin,Montant (€),Catégorie,Description"

    /** Contenu du fichier : tickets du plus récent au plus ancien (puis par identifiant). */
    fun toCsv(tickets: List<Ticket>, timeZone: TimeZone = TimeZone.getDefault()): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).apply { this.timeZone = timeZone }
        val sb = StringBuilder(HEADER).append('\n')
        tickets.sortedWith(compareByDescending<Ticket> { it.dateMillis }.thenBy { it.id }).forEach { t ->
            sb.append(sdf.format(Date(t.dateMillis))).append(',')
                .append(text(t.store)).append(',')
                .append(String.format(Locale.ROOT, "%.2f", t.amount)).append(',')
                .append(text(t.category)).append(',')
                .append(text(t.description.orEmpty())).append('\n')
        }
        return sb.toString()
    }

    /** Champ texte saisi par l'utilisateur : neutralisé contre les formules, puis échappé. */
    fun text(value: String): String = escape(neutralizeFormula(value))

    /** Préfixe « ' » si le texte commence par un caractère qu'un tableur lit comme le début d'une formule. */
    fun neutralizeFormula(value: String): String =
        if (value.isNotEmpty() && value[0] in FORMULA_START) "'$value" else value

    private val FORMULA_START = charArrayOf('=', '+', '-', '@', '\t', '\r')

    fun escape(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + value.replace("\"", "\"\"") + "\""
        else value

    /** Fichier écrit dans le cache de l'app (dossier `exports`, partagé par le FileProvider existant). */
    fun writeFile(context: Context, tickets: List<Ticket>, prefix: String): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "${prefix}_${System.currentTimeMillis()}.csv")
        file.writeText(toCsv(tickets), Charsets.UTF_8)
        return file
    }

    /**
     * Feuille de partage Android (ACTION_SEND, type text/csv) avec lecture accordée au seul destinataire choisi.
     * Aucune permission requise, aucun réseau utilisé par eTix ; annuler le partage n'a aucun effet sur les données.
     */
    fun shareIntent(context: Context, file: File, title: String): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            clipData = ClipData.newRawUri(file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, title)
    }
}
