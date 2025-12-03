package com.example.loracle.managers

import android.app.DownloadManager
import android.app.DownloadManager.Query
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import kotlinx.coroutines.*

class ModelDownloader(
    private val context: Context,
    private val modelUrl: String,
    private val modelFilename: String
) {
    private val dm = context.getSystemService(DownloadManager::class.java)

    fun modelFile() =
        context.getExternalFilesDir(null)!!.resolve(modelFilename)

    fun exists(): Boolean = modelFile().exists()

    fun download(onProgress: (Int) -> Unit, onDone: (Boolean) -> Unit) {
        val request = DownloadManager.Request(Uri.parse(modelUrl))
            .setTitle("Model Download")
            .setDescription("Downloading AI model…")
            .setDestinationUri(Uri.fromFile(modelFile()))
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadId = dm.enqueue(request)

        CoroutineScope(Dispatchers.IO).launch {
            var running = true
            while (running) {
                val cursor = dm.query(Query().setFilterById(downloadId))
                if (cursor != null && cursor.moveToFirst()) {
                    val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))

                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        running = false
                        cursor.close()
                        withContext(Dispatchers.Main) { onDone(true) }
                        break
                    }
                    if (status == DownloadManager.STATUS_FAILED) {
                        running = false
                        cursor.close()
                        withContext(Dispatchers.Main) { onDone(false) }
                        break
                    }
                    if (total > 0) {
                        val progress = ((downloaded * 100) / total).toInt()
                        withContext(Dispatchers.Main) { onProgress(progress) }
                    }
                }
                cursor?.close()
                delay(500)
            }
        }
    }
}

