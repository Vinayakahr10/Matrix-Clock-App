package com.dotmatrix.app.ota

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class OTAReleaseInfo(
    val version: String,
    val title: String,
    val new_features: List<String>,
    val improvements: List<String>,
    val notes: List<String>,
    val bin_url: String,
    val md5: String,
    val fileName: String? = null,
    val size: Long? = null,
    val sha256: String? = null
)

private data class OTAReleaseInfoDto(
    val version: String? = null,
    val title: String? = null,
    val new_features: List<String>? = null,
    val improvements: List<String>? = null,
    val notes: List<String>? = null,
    val bin_url: String? = null,
    val md5: String? = null,
    val fileName: String? = null,
    val size: Long? = null,
    val sha256: String? = null,
    val changes: List<String>? = null
)

data class OTAOperationResult<T>(
    val value: T? = null,
    val errorMessage: String? = null
) {
    val isSuccess: Boolean
        get() = value != null
}

class OTAManager(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
        
    private val gson = Gson()
    private val updateUrl = "https://raw.githubusercontent.com/Vinayakahr10/Matrix-Clock-Firmware/main/update.json"

    suspend fun fetchUpdateMetadata(): OTAOperationResult<OTAReleaseInfo> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(updateUrl)
                .header("User-Agent", "DotMatrixApp/1.0")
                .header("Cache-Control", "no-cache")
                .build()

            Log.d("OTAManager", "Fetching update metadata from: $updateUrl")
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val error = "Update check failed (${response.code} ${response.message})"
                    Log.e("OTAManager", error)
                    return@withContext OTAOperationResult(errorMessage = error)
                }

                val responseData = response.body?.string()
                if (responseData.isNullOrBlank()) {
                    return@withContext OTAOperationResult(errorMessage = "Update metadata response was empty")
                }

                Log.d("OTAManager", "Update metadata received: $responseData")

                val releaseInfoDto = gson.fromJson(responseData, OTAReleaseInfoDto::class.java)
                val releaseInfo = OTAReleaseInfo(
                    version = releaseInfoDto.version?.trim().orEmpty(),
                    title = releaseInfoDto.title?.trim().takeUnless { it.isNullOrBlank() }
                        ?: releaseInfoDto.fileName?.trim().takeUnless { it.isNullOrBlank() }
                        ?: "Firmware Update",
                    new_features = releaseInfoDto.new_features.orEmpty(),
                    improvements = releaseInfoDto.improvements.orEmpty(),
                    notes = when {
                        !releaseInfoDto.notes.isNullOrEmpty() -> releaseInfoDto.notes
                        !releaseInfoDto.changes.isNullOrEmpty() -> releaseInfoDto.changes
                        else -> emptyList()
                    },
                    bin_url = releaseInfoDto.bin_url?.trim().orEmpty(),
                    md5 = releaseInfoDto.md5?.trim().orEmpty(),
                    fileName = releaseInfoDto.fileName?.trim(),
                    size = releaseInfoDto.size,
                    sha256 = releaseInfoDto.sha256?.trim()
                )

                if (releaseInfo.version.isBlank() || releaseInfo.bin_url.isBlank()) {
                    return@withContext OTAOperationResult(errorMessage = "Update metadata is missing required fields")
                }

                return@withContext OTAOperationResult(value = releaseInfo)
            }
        } catch (e: Exception) {
            Log.e("OTAManager", "Error fetching update metadata", e)
            return@withContext OTAOperationResult(errorMessage = e.message ?: "Unable to fetch update metadata")
        }
    }

    suspend fun downloadFirmware(
        url: String,
        expectedMd5: String?,
        onProgress: (Float) -> Unit
    ): OTAOperationResult<File> = withContext(Dispatchers.IO) {
        try {
            Log.d("OTAManager", "Starting firmware download from: $url")
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "DotMatrixApp/1.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val error = "Firmware download failed (${response.code} ${response.message})"
                    Log.e("OTAManager", error)
                    return@withContext OTAOperationResult(errorMessage = error)
                }

                val body = response.body ?: run {
                    Log.e("OTAManager", "Firmware download body is null")
                    return@withContext OTAOperationResult(errorMessage = "Firmware download response was empty")
                }

                val totalBytes = body.contentLength()
                Log.d("OTAManager", "Total bytes to download: $totalBytes")

                val file = File(context.cacheDir, "firmware_update.bin")
                if (file.exists()) {
                    val deleted = file.delete()
                    Log.d("OTAManager", "Deleted existing file: $deleted")
                }

                val inputStream = body.byteStream()
                val outputStream = FileOutputStream(file)
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var downloadedBytes = 0L

                try {
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
                            output.flush()
                        }
                    }

                    if (file.length() <= 0L) {
                        return@withContext OTAOperationResult(errorMessage = "Downloaded firmware file was empty")
                    }

                    if (!expectedMd5.isNullOrBlank()) {
                        val actualMd5 = calculateMD5(file)
                        if (!actualMd5.equals(expectedMd5.trim(), ignoreCase = true)) {
                            file.delete()
                            return@withContext OTAOperationResult(
                                errorMessage = "Downloaded firmware checksum did not match"
                            )
                        }
                    }

                    Log.d("OTAManager", "Firmware download complete. File size: ${file.length()}")
                    return@withContext OTAOperationResult(value = file)
                } catch (e: Exception) {
                    Log.e("OTAManager", "Error during file write", e)
                    return@withContext OTAOperationResult(errorMessage = e.message ?: "Unable to save firmware file")
                }
            }
        } catch (e: Exception) {
            Log.e("OTAManager", "Error downloading firmware", e)
            return@withContext OTAOperationResult(errorMessage = e.message ?: "Unable to download firmware")
        }
    }

    private fun calculateMD5(file: File): String {
        val digest = MessageDigest.getInstance("MD5")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
