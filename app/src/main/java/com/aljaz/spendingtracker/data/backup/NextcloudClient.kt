package com.aljaz.spendingtracker.data.backup

import com.aljaz.spendingtracker.core.AppResult
import com.aljaz.spendingtracker.domain.backup.CloudCredentials
import com.aljaz.spendingtracker.domain.backup.CloudStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLException

/**
 * [CloudStorage] for Nextcloud, using its WebDAV interface at
 * `<server>/remote.php/dav/files/<username>/`.
 *
 * WebDAV commands used:
 * - PROPFIND: "describe this folder" (used to check the login)
 * - MKCOL: "create this folder"
 * - PUT: "store this file" (replaces an existing one)
 */
@Singleton
class NextcloudClient @Inject constructor() : CloudStorage {

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    override suspend fun testConnection(credentials: CloudCredentials): AppResult<Unit> =
        withContext(Dispatchers.IO) {
            safely {
                // 1. Log in and look at the user's root folder. 207 = success for PROPFIND.
                val code = execute(
                    newRequest(credentials, userRootUrl(credentials))
                        .method("PROPFIND", null)
                        .header("Depth", "0")
                )
                if (code != 207) AppResult.Error(explain(code)) else ensureFolder(credentials)
            }
        }

    override suspend fun upload(
        credentials: CloudCredentials,
        fileName: String,
        content: ByteArray
    ): AppResult<Unit> = withContext(Dispatchers.IO) {
        safely {
            when (val folder = ensureFolder(credentials)) {
                is AppResult.Error -> folder
                is AppResult.Success -> {
                    val url = folderUrl(credentials).newBuilder().addPathSegment(fileName).build()
                    val body = content.toRequestBody("text/csv; charset=utf-8".toMediaType())
                    val code = execute(newRequest(credentials, url).put(body))
                    // 201 = created, 204 = replaced an existing file.
                    if (code in 200..299) AppResult.Success(Unit) else AppResult.Error(explain(code))
                }
            }
        }
    }

    /** Creates the backup folder if needed. 201 = created, 405 = already exists. */
    private fun ensureFolder(credentials: CloudCredentials): AppResult<Unit> {
        val code = execute(newRequest(credentials, folderUrl(credentials)).method("MKCOL", null))
        return if (code == 201 || code == 405) AppResult.Success(Unit) else AppResult.Error(explain(code))
    }

    /** Sends a request and returns only the HTTP status code. */
    private fun execute(builder: Request.Builder): Int =
        http.newCall(builder.build()).execute().use { response -> response.code }

    /** A request with the login attached (HTTP Basic auth, sent over HTTPS only). */
    private fun newRequest(credentials: CloudCredentials, url: HttpUrl): Request.Builder =
        Request.Builder()
            .url(url)
            .header(
                "Authorization",
                Credentials.basic(credentials.username, credentials.password, Charsets.UTF_8)
            )

    /** e.g. https://cloud.example.com/remote.php/dav/files/aljaz/ */
    private fun userRootUrl(credentials: CloudCredentials): HttpUrl =
        credentials.serverUrl.toHttpUrl().newBuilder()
            .addPathSegments("remote.php/dav/files")
            .addPathSegment(credentials.username) // encodes spaces and special characters
            .build()

    /** e.g. .../files/aljaz/SpendingTracker/ */
    private fun folderUrl(credentials: CloudCredentials): HttpUrl =
        userRootUrl(credentials).newBuilder()
            .addPathSegments(credentials.folder.trim('/'))
            .build()

    /** Runs a network action and turns typical failures into readable messages. */
    private inline fun safely(action: () -> AppResult<Unit>): AppResult<Unit> =
        try {
            action()
        } catch (e: UnknownHostException) {
            AppResult.Error("Server not found. Check the address and the internet connection.")
        } catch (e: SocketTimeoutException) {
            AppResult.Error("The server did not respond in time.")
        } catch (e: SSLException) {
            AppResult.Error("Secure connection failed (certificate problem).")
        } catch (e: IOException) {
            AppResult.Error("Network error: ${e.message}")
        } catch (e: IllegalArgumentException) {
            AppResult.Error("Invalid server address.")
        }

    /** Explains an HTTP status code in plain language. */
    private fun explain(code: Int): String = when (code) {
        401 -> "Wrong username or app password."
        403 -> "Access denied by the server."
        404 -> "Not found. Check the address and username " +
                "(Nextcloud → Files → Settings shows your WebDAV address)."
        409 -> "A parent folder doesn't exist on the server."
        507 -> "Not enough storage space on the server."
        else -> "The server responded with error $code."
    }
}