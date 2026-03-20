package com.dotmatrix.app.update

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class AppUpdateInfo(
    val versionName: String,
    val versionCode: Long,
    val apkUrl: String,
    val notes: List<String>
)

private data class AppUpdateDto(
    val versionName: String? = null,
    val versionCode: Long? = null,
    val apkUrl: String? = null,
    val notes: List<String>? = null
)

data class AppUpdateResult<T>(
    val value: T? = null,
    val errorMessage: String? = null
)

class AppUpdateManager(private val context: Context) {
    companion object {
        private const val TAG = "AppUpdateManager"
        private const val UPDATE_URL =
            "https://raw.githubusercontent.com/Vinayakahr10/Matrix-Clock-App/main/app_update.json"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val gson = Gson()

    suspend fun fetchUpdateInfo(): AppUpdateResult<AppUpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(UPDATE_URL)
                .header("User-Agent", "${context.packageName}/${getInstalledVersionName()}")
                .header("Cache-Control", "no-cache")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext AppUpdateResult(
                        errorMessage = "App update check failed (${response.code} ${response.message})"
                    )
                }

                val body = response.body?.string()
                if (body.isNullOrBlank()) {
                    return@withContext AppUpdateResult(errorMessage = "App update response was empty")
                }

                val dto = gson.fromJson(body, AppUpdateDto::class.java)
                val info = AppUpdateInfo(
                    versionName = dto.versionName?.trim().orEmpty(),
                    versionCode = dto.versionCode ?: 0L,
                    apkUrl = dto.apkUrl?.trim().orEmpty(),
                    notes = dto.notes.orEmpty()
                )

                if (info.versionName.isBlank() || info.versionCode <= 0L || info.apkUrl.isBlank()) {
                    return@withContext AppUpdateResult(errorMessage = "app_update.json is missing required fields")
                }

                AppUpdateResult(value = info)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch app update info", e)
            AppUpdateResult(errorMessage = e.message ?: "Unable to check app updates")
        }
    }

    fun getInstalledVersionName(): String {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        return packageInfo.versionName ?: "Unknown"
    }

    fun getInstalledVersionCode(): Long {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
    }
}
