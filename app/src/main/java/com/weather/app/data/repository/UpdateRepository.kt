package com.weather.app.data.repository

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.weather.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

private const val TAG = "UpdateRepository"

data class ReleaseInfo(val tag: String, val apkUrl: String)

sealed class DownloadProgress {
    data class InProgress(val fraction: Float) : DownloadProgress()
    data class Complete(val uri: Uri?) : DownloadProgress()
    data class Failed(val reason: String) : DownloadProgress()
}

class UpdateRepository {
    private val client = OkHttpClient()

    suspend fun checkForUpdate(): Result<ReleaseInfo?> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.github.com/repos/jives00/Weather/releases/latest")
                .header("Accept", "application/vnd.github+json")
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string()
            if (!response.isSuccessful || body == null) {
                val reason = "GitHub API returned HTTP ${response.code}${body?.let { ": $it" } ?: ""}"
                Log.e(TAG, "Update check failed: $reason")
                return@withContext Result.failure(IOException(reason))
            }
            val json = JSONObject(body)
            if (!json.has("tag_name")) {
                val reason = json.optString("message", "Response had no tag_name")
                Log.e(TAG, "Update check failed: $reason")
                return@withContext Result.failure(IOException(reason))
            }
            val tag = json.getString("tag_name")
            if (tag == "v${BuildConfig.VERSION_NAME}") return@withContext Result.success(null)
            val assets = json.getJSONArray("assets")
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.getString("name").endsWith(".apk")) {
                    return@withContext Result.success(ReleaseInfo(tag, asset.getString("browser_download_url")))
                }
            }
            val reason = "Release $tag has no .apk asset"
            Log.e(TAG, "Update check failed: $reason")
            Result.failure(IOException(reason))
        } catch (e: Exception) {
            Log.e(TAG, "Update check failed", e)
            Result.failure(e)
        }
    }

    fun downloadApk(context: Context, apkUrl: String, tag: String): Flow<DownloadProgress> = flow {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = dm.enqueue(
            DownloadManager.Request(Uri.parse(apkUrl))
                .setTitle("Weather $tag")
                .setDescription("Downloading update…")
                .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "weather-update.apk")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
        )
        while (true) {
            val cursor = dm.query(DownloadManager.Query().setFilterById(downloadId))
            if (cursor.moveToFirst()) {
                when (cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        cursor.close()
                        emit(DownloadProgress.Complete(dm.getUriForDownloadedFile(downloadId)))
                        break
                    }
                    DownloadManager.STATUS_FAILED -> {
                        val reasonCode = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                        cursor.close()
                        val reason = downloadFailureReason(reasonCode)
                        Log.e(TAG, "Download failed: $reason (code $reasonCode)")
                        emit(DownloadProgress.Failed(reason))
                        break
                    }
                    else -> {
                        val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                        val done = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                        cursor.close()
                        emit(DownloadProgress.InProgress(if (total > 0) done.toFloat() / total else 0f))
                    }
                }
            } else {
                cursor.close()
            }
            delay(300)
        }
    }

    private fun downloadFailureReason(code: Int): String = when (code) {
        DownloadManager.ERROR_CANNOT_RESUME -> "Download could not be resumed"
        DownloadManager.ERROR_DEVICE_NOT_FOUND -> "Storage not found"
        DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "A previous download is already in the way"
        DownloadManager.ERROR_FILE_ERROR -> "Storage error while writing the file"
        DownloadManager.ERROR_HTTP_DATA_ERROR -> "Network error while downloading"
        DownloadManager.ERROR_INSUFFICIENT_SPACE -> "Not enough storage space"
        DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "Too many redirects"
        DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "Unexpected HTTP response"
        else -> "Unknown error (code $code)"
    }
}
