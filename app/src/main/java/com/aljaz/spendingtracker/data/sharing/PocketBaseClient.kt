package com.aljaz.spendingtracker.data.sharing

import com.aljaz.spendingtracker.core.AppResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLException

/** Answer to a successful login. [token] proves who we are on later requests. */
data class AuthResponse(
    val token: String,
    val userId: String,
    val email: String,
    val displayName: String
) {
    /** Never prints the token, e.g. in logs or crash reports. */
    override fun toString(): String =
        "AuthResponse(userId=$userId, email=$email, displayName=$displayName, token=***)"
}

/** One person in a household, as the server describes them. */
data class MemberDto(val id: String, val displayName: String)

/** One household the logged-in user belongs to, with its members. */
data class HouseholdDto(val id: String, val name: String, val members: List<MemberDto>)

/**
 * Talks to the PocketBase server over its REST API (JSON over HTTPS).
 *
 * Endpoints used:
 * - POST /api/collections/users/auth-with-password: log in, returns a token
 * - GET  /api/collections/households/records: households the user is in
 *
 * Every request after login sends the token in the "Authorization" header.
 * The server's API rules decide what each user may see and change.
 */
@Singleton
class PocketBaseClient @Inject constructor() {

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /** Logs in with email and password. */
    suspend fun authWithPassword(
        serverUrl: String,
        email: String,
        password: String
    ): AppResult<AuthResponse> = withContext(Dispatchers.IO) {
        safely {
            val body = JSONObject()
                .put("identity", email)
                .put("password", password)
                .toString()
                .toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url(apiUrl(serverUrl, "collections/users/auth-with-password"))
                .post(body)
                .build()

            call(request, isLogin = true) { json ->
                val record = json.getJSONObject("record")
                val recordEmail = record.optString("email").ifBlank { email }
                AuthResponse(
                    token = json.getString("token"),
                    userId = record.getString("id"),
                    email = recordEmail,
                    displayName = record.optString("displayName").ifBlank { recordEmail }
                )
            }
        }
    }

    /**
     * Households the logged-in user belongs to, oldest first, with member names.
     * "expand=members" asks the server to include each member's record, not just the id.
     */
    suspend fun fetchHouseholds(
        serverUrl: String,
        token: String
    ): AppResult<List<HouseholdDto>> = withContext(Dispatchers.IO) {
        safely {
            val url = apiUrl(serverUrl, "collections/households/records").newBuilder()
                .addQueryParameter("expand", "members")
                .addQueryParameter("sort", "created")
                .build()
            val request = Request.Builder()
                .url(url)
                .header("Authorization", token)
                .get()
                .build()

            call(request) { json ->
                val items = json.getJSONArray("items")
                (0 until items.length()).map { i -> items.getJSONObject(i).toHousehold() }
            }
        }
    }

    /** Reads one household record, matching member ids with their expanded names. */
    private fun JSONObject.toHousehold(): HouseholdDto {
        // Names come from the expanded user records, if the server included them.
        val names = mutableMapOf<String, String>()
        optJSONObject("expand")?.optJSONArray("members")?.let { expanded ->
            for (i in 0 until expanded.length()) {
                val user = expanded.getJSONObject(i)
                names[user.getString("id")] =
                    user.optString("displayName").ifBlank { user.optString("email") }
            }
        }

        // The ids are always in the record itself.
        val ids = optJSONArray("members")
        val members = if (ids == null) {
            emptyList()
        } else {
            (0 until ids.length()).map { i ->
                val id = ids.getString(i)
                MemberDto(id = id, displayName = names[id].orEmpty().ifBlank { "Partner" })
            }
        }

        return HouseholdDto(id = getString("id"), name = optString("name"), members = members)
    }

    /**
     * Sends a request and turns the JSON answer into [T] with [parse].
     *
     * @param isLogin true for the login request, where 400 means wrong credentials
     */
    private fun <T> call(
        request: Request,
        isLogin: Boolean = false,
        parse: (JSONObject) -> T
    ): AppResult<T> =
        http.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (response.isSuccessful) {
                AppResult.Success(parse(JSONObject(text)))
            } else {
                AppResult.Error(explain(response.code, isLogin))
            }
        }

    /** e.g. https://spender.aljaz.top/api/collections/users/auth-with-password */
    private fun apiUrl(serverUrl: String, path: String): HttpUrl =
        serverUrl.toHttpUrl().newBuilder()
            .addPathSegments("api/$path")
            .build()

    /** Runs a network action and turns typical failures into readable messages. */
    private inline fun <T> safely(action: () -> AppResult<T>): AppResult<T> =
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
        } catch (e: JSONException) {
            AppResult.Error("The server sent an unexpected answer. Is the address right?")
        } catch (e: IllegalArgumentException) {
            AppResult.Error("Invalid server address.")
        }

    /** Explains an HTTP status code in plain language. */
    private fun explain(code: Int, isLogin: Boolean): String = when (code) {
        400 -> if (isLogin) "Wrong email or password." else "The server rejected the request."
        401 -> "Login expired. Log in again."
        403 -> "Access denied by the server."
        404 -> "Not found. Check the server address."
        429 -> "Too many attempts. Wait a minute and try again."
        502, 503, 504 -> "The server is not reachable right now. Is PocketBase running?"
        else -> "The server responded with error $code."
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}