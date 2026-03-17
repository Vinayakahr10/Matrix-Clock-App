package com.dotmatrix.app.ota

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

data class OTAReleaseInfo(
    val version: String,
    val downloadUrl: String,
    val size: Long,
    val releaseNotes: String
)

class OTAManager(private val context: Context) {
    private val client = OkHttpClient()
    private val repoUrl = "https://api.github.com/repos/Vinayakahr10/matrixclocktest/releases/latest"

    suspend fun fetchLatestRelease(): OTAReleaseInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(repoUrl)
                .addHeader("Accept", "application/vnd.github.v3+json")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val responseData = response.body?.string() ?: return@withContext null
            val json = JSONObject(responseData)
            
            val version = json.getString("tag_name")
            val releaseNotes = json.optString("body", "No release notes provided.")
            val assets = json.getJSONArray("assets")
            
            var downloadUrl = ""
            var size = 0L

            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.getString("name")
                if (name.endsWith(".bin")) {
                    downloadUrl = asset.getString("browser_download_url")
                    size = asset.getLong("size")
                    break
                }
            }

            if (downloadUrl.isEmpty()) return@withContext null

            return@withContext OTAReleaseInfo(version, downloadUrl, size, releaseNotes)
        } catch (e: Exception) {
            Log.e("OTAManager", "Error fetching release", e)
            return@withContext null
        }
    }

    suspend fun downloadFirmware(url: String, onProgress: (Float) -> Unit): File? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val body = response.body ?: return@withContext null
            val totalBytes = body.contentLength()
            val file = File(context.cacheDir, "firmware_update.bin")
            
            val inputStream = body.byteStream()
            val outputStream = FileOutputStream(file)
            val buffer = ByteArray(8192)
            var bytesRead: Int
            var downloadedBytes = 0L

            inputStream.use { input ->
                outputStream.use { output ->
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        if (totalBytes > 0) {
                            val progress = downloadedBytes.toFloat() / totalBytes.toFloat()
                            withContext(Dispatchers.Main) {
                                onProgress(progress)
                            }
                        }
                    }
                }
            }
            return@withContext file
        } catch (e: Exception) {
            Log.e("OTAManager", "Error downloading firmware", e)
            return@withContext null
        }
    }
}
