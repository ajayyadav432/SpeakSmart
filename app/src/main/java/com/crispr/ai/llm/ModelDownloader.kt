package com.crispr.ai.llm

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

sealed class DownloadStatus {
    object Idle : DownloadStatus()
    data class Downloading(val progress: Float, val downloadedBytes: Long, val totalBytes: Long) : DownloadStatus()
    data class Completed(val file: File) : DownloadStatus()
    data class Error(val message: String) : DownloadStatus()
}

class ModelDownloader(private val context: Context) {

    companion object {
        private const val TAG = "ModelDownloader"

        // Gemma 2B GPU quantized model URL for MediaPipe LLM Inference
        const val DEFAULT_MODEL_URL =
            "https://huggingface.co/alexdlov/gemma-2b-it-gpu-int4.bin/resolve/main/gemma-2b-it-gpu-int4.bin"

        fun getModelFile(context: Context): File {
            val dir = File(context.filesDir, "llm")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return File(dir, "model.bin")
        }
    }

    private val _status = MutableStateFlow<DownloadStatus>(DownloadStatus.Idle)
    val status: StateFlow<DownloadStatus> = _status.asStateFlow()

    fun isModelDownloaded(): Boolean {
        val file = getModelFile(context)
        // Check if file exists and has size > 10MB (to ensure it's not a incomplete download)
        return file.exists() && file.length() > 10 * 1024 * 1024
    }

    suspend fun downloadModel(urlStr: String = DEFAULT_MODEL_URL): Boolean = withContext(Dispatchers.IO) {
        val targetFile = getModelFile(context)

        // Temporary file while downloading
        val tempFile = File(targetFile.parentFile, "model.bin.tmp")

        try {
            Log.d(TAG, "Starting download from: $urlStr")
            _status.value = DownloadStatus.Downloading(0f, 0L, -1L)

            var currentUrlStr = urlStr
            var connection: HttpURLConnection
            var responseCode: Int
            var redirectCount = 0

            while (true) {
                val url = URL(currentUrlStr)
                connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                connection.instanceFollowRedirects = true
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                connection.connect()

                responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                    responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                    responseCode == 307 || responseCode == 308) {
                    val newUrl = connection.getHeaderField("Location")
                    if (newUrl != null && redirectCount < 10) {
                        currentUrlStr = newUrl
                        redirectCount++
                        connection.disconnect()
                        continue
                    }
                }
                break
            }

            if (responseCode != HttpURLConnection.HTTP_OK) {
                val err = "Server returned HTTP ${responseCode}: ${connection.responseMessage}"
                Log.e(TAG, err)
                _status.value = DownloadStatus.Error(err)
                return@withContext false
            }

            val totalBytes = connection.contentLengthLong
            val input = connection.inputStream
            val output = FileOutputStream(tempFile)

            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalDownloaded = 0L

            var lastReportTime = System.currentTimeMillis()

            while (input.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
                totalDownloaded += bytesRead

                val currentTime = System.currentTimeMillis()
                // Update progress every 200ms
                if (currentTime - lastReportTime > 200 || totalDownloaded == totalBytes) {
                    lastReportTime = currentTime
                    val progress = if (totalBytes > 0) totalDownloaded.toFloat() / totalBytes else 0f
                    _status.value = DownloadStatus.Downloading(progress, totalDownloaded, totalBytes)
                }
            }

            output.flush()
            output.close()
            input.close()
            connection.disconnect()

            // Rename temp file to target file
            if (tempFile.exists()) {
                if (targetFile.exists()) {
                    targetFile.delete()
                }
                tempFile.renameTo(targetFile)
            }

            Log.i(TAG, "Model downloaded successfully to ${targetFile.absolutePath}")
            _status.value = DownloadStatus.Completed(targetFile)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download model: ${e.message}", e)
            tempFile.delete()
            _status.value = DownloadStatus.Error(e.message ?: "Download failed")
            false
        }
    }

    fun cancelDownload() {
        // Can be triggered if needed
        val tempFile = File(getModelFile(context).parentFile, "model.bin.tmp")
        if (tempFile.exists()) {
            tempFile.delete()
        }
        _status.value = DownloadStatus.Idle
    }

    fun deleteModel(): Boolean {
        val file = getModelFile(context)
        if (file.exists()) {
            val deleted = file.delete()
            _status.value = DownloadStatus.Idle
            return deleted
        }
        return false
    }
}
