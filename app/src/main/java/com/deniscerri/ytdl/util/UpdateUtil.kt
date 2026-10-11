package com.deniscerri.ytdl.util

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.preference.PreferenceManager
import com.deniscerri.ytdl.BuildConfig
import com.deniscerri.ytdl.R
import com.deniscerri.ytdl.core.RuntimeManager
import com.deniscerri.ytdl.core.models.YTDLRequest
import com.deniscerri.ytdl.core.packages.PackageBase.Companion.sharedClient
import com.deniscerri.ytdl.database.models.GithubRelease
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.cancellation.CancellationException


class UpdateUtil(var context: Context) {
    private val tag = "UpdateUtil"
    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    private val channelMap = mapOf(
        Pair<String, RuntimeManager.UpdateChannel>("stable", RuntimeManager.UpdateChannel.STABLE),
        Pair<String, RuntimeManager.UpdateChannel>("nightly", RuntimeManager.UpdateChannel.NIGHTLY),
        Pair<String, RuntimeManager.UpdateChannel>("master", RuntimeManager.UpdateChannel.MASTER)
    )

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    fun tryGetNewVersion() : Result<GithubRelease> {
        return runCatching {
            val skipped = sharedPreferences.getString("skip_updates", "")?.split(",").orEmpty()
            val releases = getGithubReleases()
            if (releases.isEmpty()) error(context.getString(R.string.network_error))
            val release = releases
                .filter { !it.draft && !it.prerelease }
                .mapNotNull { release ->
                    val code = SolidReleaseVersion.apkVersionCode(release.tag_name)
                        ?: return@mapNotNull null
                    release to code
                }
                .filter { (_, code) -> code > BuildConfig.VERSION_CODE.toLong() / 100 * 100 }
                .maxByOrNull { it.second }?.first
                ?: error(context.getString(R.string.you_are_in_latest_version))
            if (release.tag_name in skipped) error(context.getString(R.string.you_are_in_latest_version))
            release
        }
    }

    fun getGithubReleases(): List<GithubRelease> {
        val url = "https://api.github.com/repos/TheMrAleX/ytdlnis-solid/releases?per_page=100"
        val conn: HttpURLConnection
        var json = listOf<GithubRelease>()
        try {
            val req = URL(url)
            conn = req.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 10000
            conn.readTimeout = 5000
            if (conn.responseCode < 300) {
                val myType = object : TypeToken<List<GithubRelease>>() {}.type
                json = Gson().fromJson(InputStreamReader(conn.inputStream), myType)
            }
            conn.disconnect()
        } catch (e: Exception) {
            Log.e(tag, e.toString())
        }
        return json
    }

    data class YTDLPUpdateResponse (
        val status: YTDLPUpdateStatus,
        val message: String = ""
    )

    enum class YTDLPUpdateStatus {
        DONE, ALREADY_UP_TO_DATE, PROCESSING, ERROR
    }

    suspend fun updateYTDL(c: String? = null) : YTDLPUpdateResponse =
        withContext(Dispatchers.IO){
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            if (updatingYTDL) {
                return@withContext YTDLPUpdateResponse(YTDLPUpdateStatus.PROCESSING)
            }

            updatingYTDL = true

            try {
                val channel = if (c.isNullOrBlank()) sharedPreferences.getString("ytdlp_source", "stable") else c

                when(channel) {
                    "stable", "nightly", "master" -> {
                        val res = RuntimeManager.getInstance().updateYTDL(context, channelMap[channel]!!)
                        if (res != RuntimeManager.UpdateStatus.DONE) {
                            YTDLPUpdateResponse(YTDLPUpdateStatus.ALREADY_UP_TO_DATE)
                        }else {
                            val version = RuntimeManager.getInstance().version(context)
                            YTDLPUpdateResponse(YTDLPUpdateStatus.DONE, "Updated yt-dlp to ${channel}@${version}")
                        }
                    }
                    else -> {
                        val request = YTDLRequest(emptyList())
                        request.addOption("--update-to", "$channel")

                        val res = RuntimeManager.getInstance().execute(request)
                        val out = res.out.lines().last { it.isNotBlank() }

                        when {
                            out.contains("ERROR") -> YTDLPUpdateResponse(YTDLPUpdateStatus.ERROR, out)
                            out.contains("yt-dlp is up to date") -> YTDLPUpdateResponse(YTDLPUpdateStatus.ALREADY_UP_TO_DATE, out)
                            else -> YTDLPUpdateResponse(YTDLPUpdateStatus.DONE, out)
                        }
                    }
                }
            } finally {
                updatingYTDL = false
            }
        }

    @SuppressLint("Range", "UnspecifiedRegisterReceiverFlag")
    suspend fun downloadReleaseApk(release: GithubRelease, onProgress: (Long) -> Unit) : Result<File> {
        return withContext(Dispatchers.IO) {
            try {
                val releaseVersion = release.assets.firstOrNull {
                    it.name.contains(Build.SUPPORTED_ABIS[0])
                    && !it.name.contains("foss")
                    && !it.name.contains("izzy")
                    && it.name.endsWith(".apk")
                } ?: release.assets.firstOrNull {
                    it.name.contains("universal") && it.name.endsWith("-github.apk")
                } ?: error("No compatible APK found in this Solid release")
                File(FileUtil.getDefaultApksPath()).mkdirs()
                val tempApk = File(FileUtil.getDefaultApksPath(), "${releaseVersion.browser_download_url.split("/").last()}")

                //download
                val request = Request.Builder()
                    .url(releaseVersion.browser_download_url)
                    .build()

                val response = sharedClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Throwable(response.body.string()))
                }

                val body = response.body
                val totalBytes = body.contentLength()
                var bytesDownloaded = 0L

                body.byteStream().use { inputStream ->
                    tempApk.outputStream().use { outputStream ->
                        val buffer = ByteArray(8192) // 8KB buffer
                        var bytesRead: Int

                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            if (!isActive) throw CancellationException("Download cancelled by user")

                            outputStream.write(buffer, 0, bytesRead)
                            bytesDownloaded += bytesRead

                            // Calculate percentage
                            val progress = ((bytesDownloaded * 100) / totalBytes)
                            onProgress(progress)
                        }
                    }
                }

                Result.success(tempApk)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    companion object {
        var updatingYTDL = false
        var updatingApp = false
    }
}