package com.etix.e2e

import android.content.Context
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.etix.e2e.E2e.ctx
import com.etix.features.ocr.scan.MlKitTextReader
import com.etix.features.ocr.scan.ReceiptScanParser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Lot 9 — reconnaissance ML Kit SANS RÉSEAU au premier lancement : lancé par la CI juste après l'installation
 * (app neuve, jamais ouverte), mode avion activé et Wi-Fi / données coupés. Le modèle de reconnaissance est
 * embarqué dans l'APK (com.google.mlkit:text-recognition) : aucun téléchargement ne doit être nécessaire.
 */
@RunWith(AndroidJUnit4::class)
class E2eScanHorsLigneTest {

    @Test fun lecture_sans_reseau_au_premier_lancement() {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        @Suppress("DEPRECATION")
        val net = cm.activeNetworkInfo
        ScanE2e.log("scan_hors_ligne", "réseau actif avant lecture : ${net?.typeName ?: "aucun"} (connecté=${net?.isConnected == true})")
        assertTrue("le test doit tourner sans réseau", net?.isConnected != true)
        val bmp = BitmapFactory.decodeFile(ScanE2e.ticketImage("hors_ligne.png").path)
        val t0 = System.currentTimeMillis()
        val text = runBlocking { MlKitTextReader.read(bmp) }
        val scan = ReceiptScanParser.parse(text)
        ScanE2e.log("scan_hors_ligne", "1re lecture ML Kit sans réseau en ${System.currentTimeMillis() - t0} ms : " +
            "magasin=${scan.store.value} montant=${scan.amount.value} date lue=${scan.date.value != null}")
        assertEquals("ESSO", scan.store.value)
        assertEquals(23.45, scan.amount.value!!, 0.001)
    }
}
