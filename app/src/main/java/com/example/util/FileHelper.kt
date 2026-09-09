package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat

object FileHelper {
    private const val TAG = "FileHelper"

    fun getFileInfoFromUri(context: Context, uri: Uri): Pair<String, Long> {
        var name = "attachment_${System.currentTimeMillis()}"
        var size = 0L

        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        name = cursor.getString(nameIndex) ?: name
                    }
                    if (sizeIndex != -1) {
                        size = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to resolve file name/size: ${e.message}")
        }

        return Pair(name, size)
    }

    fun getMimeType(context: Context, uri: Uri, fallbackName: String): String {
        return context.contentResolver.getType(uri)
            ?: run {
                val ext = MimeTypeMap.getFileExtensionFromUrl(fallbackName).lowercase()
                if (ext.isNotEmpty()) {
                    MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
                } else {
                    "application/octet-stream"
                }
            }
    }

    suspend fun copyUriToSentFolder(context: Context, uri: Uri, fileName: String): File? = withContext(Dispatchers.IO) {
        try {
            val sentDir = File(context.filesDir, "sent_files").apply { mkdirs() }
            val sanitizedName = fileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            val targetFile = File(sentDir, "${System.currentTimeMillis()}_$sanitizedName")

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            targetFile
        } catch (e: Exception) {
            Log.e(TAG, "Error copying uri to sent folder: ${e.message}", e)
            null
        }
    }

    suspend fun saveIncomingBytes(context: Context, fileName: String, bytes: ByteArray): File? = withContext(Dispatchers.IO) {
        try {
            val recvDir = File(context.filesDir, "received_files").apply { mkdirs() }
            val sanitizedName = fileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            val targetFile = File(recvDir, "${System.currentTimeMillis()}_$sanitizedName")

            FileOutputStream(targetFile).use { output ->
                output.write(bytes)
            }
            targetFile
        } catch (e: Exception) {
            Log.e(TAG, "Error saving incoming bytes: ${e.message}", e)
            null
        }
    }

    suspend fun downloadRemoteFile(context: Context, fileUrl: String, fileName: String, client: OkHttpClient): File? = withContext(Dispatchers.IO) {
        try {
            val recvDir = File(context.filesDir, "received_files").apply { mkdirs() }
            val sanitizedName = fileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            val targetFile = File(recvDir, "${System.currentTimeMillis()}_$sanitizedName")

            val request = Request.Builder().url(fileUrl).build()
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                response.body?.byteStream()?.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
                targetFile
            } else {
                Log.w(TAG, "Download failed with code: ${response.code}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading remote file: ${e.message}", e)
            null
        }
    }

    fun openFile(context: Context, file: File, mimeType: String?) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)

            val resolvedMime = mimeType ?: run {
                val ext = MimeTypeMap.getFileExtensionFromUrl(file.name).lowercase()
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, resolvedMime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(intent, "Open with").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open file: ${e.message}", e)
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
        val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
        return DecimalFormat("#,##0.#").format(value) + " " + units[digitGroups]
    }
}
