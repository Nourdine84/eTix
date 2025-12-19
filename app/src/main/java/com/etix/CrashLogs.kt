package com.etix.util

import android.content.Context
import java.io.File

object CrashLogs {

    fun listLogs(context: Context): List<File> {
        val dir = File(context.filesDir, "crash")
        if (!dir.exists() || !dir.isDirectory) return emptyList()
        return dir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    fun latestLog(context: Context): File? = listLogs(context).firstOrNull()

    fun readFile(f: File): String = runCatching { f.readText() }.getOrDefault("")

    fun deleteAll(context: Context): Boolean {
        val dir = File(context.filesDir, "crash")
        if (!dir.exists()) return true
        var ok = true
        dir.listFiles()?.forEach { ok = ok && it.delete() }
        return ok
    }
}