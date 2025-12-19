package com.etix

import android.app.Application
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

class ETixApp : Application() {

    override fun onCreate() {
        super.onCreate()

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                // 1) sérialise la stacktrace
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val stack = sw.toString()

                // 2) horodatage + fichier
                val sdf = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
                val name = "crash_${sdf.format(Date())}.log"
                val dir = File(filesDir, "crash")
                if (!dir.exists()) dir.mkdirs()
                val f = File(dir, name)
                f.writeText(stack)

                Log.e("ETixApp", "Crash log saved: ${f.absolutePath}")
            } catch (e: Exception) {
                Log.e("ETixApp", "Failed to write crash log", e)
            } finally {
                // délègue au handler par défaut (affiche le crash normal)
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }
}