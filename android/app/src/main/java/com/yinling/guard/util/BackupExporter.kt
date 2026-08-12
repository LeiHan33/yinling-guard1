package com.yinling.guard.util

import android.content.Context
import com.google.gson.GsonBuilder
import com.yinling.guard.core.model.BackupFile
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupExporter {
    fun exportToDownloads(context: Context, backup: BackupFile): File {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val downloads = context.getExternalFilesDir(null) ?: context.filesDir
        val date = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val file = File(downloads, "yinling_guard_backup_$date.json")
        file.writeText(gson.toJson(backup), Charsets.UTF_8)
        return file
    }
}
