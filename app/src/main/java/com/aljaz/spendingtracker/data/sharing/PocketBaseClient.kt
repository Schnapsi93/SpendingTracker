package com.aljaz.spendingtracker.data.sharing

import com.aljaz.spendingtracker.core.AppResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
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

/** A shared expense as this phone uploads it. */
data class ExpenseUpload(
    val uuid: String,
    val householdId: String,
    val paidBy: String,
    val amountMinor: Long,
    val currency: String,
    val date: Long,
    val category: String,
    val payerSharePercent: Int,
    val note: String?,
    val clientUpdatedAt: Long
)

/** A shared expense as the server returns it. */
data class RemoteExpense(
    val id: String,
    val uuid: String,
    val householdId: String,
    val paidBy: String,
    val amountMinor: Long,
    val currency: String,
    val date: Long,
    val category: String,
    val payerSharePercent: Int,
    val note: String?,
    val deleted: Boolean,
    /** The server's last-change time, as text. */
    val updated: String
)

/** A settle-up payment as this phone uploads it. */
data class SettlementUpload(
    val uuid: String,
    val householdId: String,
    val fromUser: String,
    val toUser: String,
    val amountMinor: Long,
    val date: Long,
    val note: String?
)

/** A settle-up payment as the server returns it. */
data class RemoteSettlement(
    val id: String,
    val uuid: String,
    val householdId: String,
    val fromUser: String,
    val toUser: String,
    val amountMinor: Long,
    val date: Long,
    val note: String?,
    val deleted: Boolean,
    /** The server's last-change time, as text. */
    val updated: String
)

/** One page of a list answer. */
data class RemotePage<T>(val items: List<T>, val page: Int, val totalPages: Int)

/**
 * Talks to the PocketBase server over its REST API (JSON over HTTPS).
 *
 * Endpoints used, for the collections "shared_expenses" and "settlements":
 * - POST  /api/collections/users/auth-with-password: log in, returns a token
 * - GET   /api/collections/households/records: households the user is in
 * - GET   /api/collections/<collection>/records: list / find records
 * - POST  /api/collections/<collection>/records: create one
 * - PATCH /api/collections/<collection>/records/<id>: change one
 *
 * Every request after login sends the token in the "Authorization" header.
 * The server's API rules decide what each user may see and change.
 * Records are never deleted for real, only marked "deleted", so the other
 * phone learns about the deletion on its next sync.
 */
@Singleton
class PocketBaseClient @Inject constructor() {

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // ---------- Login and households ----------

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
                .toRequestBody()
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

            call(authorized(url, token).get().build()) { json ->
                val items = json.getJSONArray("items")
                (0 until items.length()).map { i -> items.getJSONObject(i).toHousehold() }
            }
        }
    }

    // ---------- Shared expenses ----------

    /** @return the server record id of the expense with this uuid, or null if there is none */
    suspend fun findExpenseId(serverUrl: String, token: String, uuid: String): AppResult<String?> =
        findRecordId(serverUrl, token, EXPENSES, uuid)

    /** Creates a new shared expense on the server. */
    suspend fun createExpense(
        serverUrl: String,
        token: String,
        expense: ExpenseUpload
    ): AppResult<Unit> = withContext(Dispatchers.IO) {
        safely {
            val request = authorized(apiUrl(serverUrl, EXPENSES), token)
                .post(expense.toJson().toRequestBody())
                .build()
            call(request) { }
        }
    }

    /** Replaces the fields of an existing shared expense (and un-deletes it). */
    suspend fun updateExpense(
        serverUrl: String,
        token: String,
        remoteId: String,
        expense: ExpenseUpload
    ): AppResult<Unit> = patchRecord(serverUrl, token, EXPENSES, remoteId, expense.toJson())

    /** Marks a shared expense as deleted. */
    suspend fun markExpenseDeleted(
        serverUrl: String,
        token: String,
        remoteId: String,
        clientUpdatedAt: Long
    ): AppResult<Unit> = patchRecord(
        serverUrl, token, EXPENSES, remoteId,
        JSONObject()
            .put("deleted", true)
            .put("clientUpdatedAt", formatDate(clientUpdatedAt))
    )

    /** One page of the household's expenses changed at or after [since] (see [fetchChangedPage]). */
    suspend fun fetchExpensesChangedSince(
        serverUrl: String,
        token: String,
        householdId: String,
        since: String?,
        page: Int
    ): AppResult<RemotePage<RemoteExpense>> =
        fetchChangedPage(serverUrl, token, EXPENSES, householdId, since, page) {
            it.toRemoteExpense()
        }

    /** The uuids of the household's expenses that exist and aren't deleted. */
    suspend fun fetchActiveExpenseUuids(
        serverUrl: String,
        token: String,
        householdId: String
    ): AppResult<Set<String>> = fetchActiveUuids(serverUrl, token, EXPENSES, householdId)

    // ---------- Settlements ----------

    /** @return the server record id of the payment with this uuid, or null if there is none */
    suspend fun findSettlementId(serverUrl: String, token: String, uuid: String): AppResult<String?> =
        findRecordId(serverUrl, token, SETTLEMENTS, uuid)

    /** Records a settle-up payment; returns it as the server stored it. */
    suspend fun createSettlement(
        serverUrl: String,
        token: String,
        settlement: SettlementUpload
    ): AppResult<RemoteSettlement> = withContext(Dispatchers.IO) {
        safely {
            val request = authorized(apiUrl(serverUrl, SETTLEMENTS), token)
                .post(settlement.toJson().toRequestBody())
                .build()
            call(request) { json -> json.toRemoteSettlement() }
        }
    }

    /** Marks a settle-up payment as deleted. */
    suspend fun markSettlementDeleted(
        serverUrl: String,
        token: String,
        remoteId: String
    ): AppResult<Unit> =
        patchRecord(serverUrl, token, SETTLEMENTS, remoteId, JSONObject().put("deleted", true))

    /** One page of the household's payments changed at or after [since] (see [fetchChangedPage]). */
    suspend fun fetchSettlementsChangedSince(
        serverUrl: String,
        token: String,
        householdId: String,
        since: String?,
        page: Int
    ): AppResult<RemotePage<RemoteSettlement>> =
        fetchChangedPage(serverUrl, token, SETTLEMENTS, householdId, since, page) {
            it.toRemoteSettlement()
        }

    /** The uuids of the household's payments that exist and aren't deleted. */
    suspend fun fetchActiveSettlementUuids(
        serverUrl: String,
        token: String,
        householdId: String
    ): AppResult<Set<String>> = fetchActiveUuids(serverUrl, token, SETTLEMENTS, householdId)

    // ---------- Generic record helpers (used by both collections) ----------

    /** @return the record id with this uuid in [collection], or null if there is none */
    private suspend fun findRecordId(
        serverUrl: String,
        token: String,
        collection: String,
        uuid: String
    ): AppResult<String?> = withContext(Dispatchers.IO) {
        safely {
            val url = apiUrl(serverUrl, collection).newBuilder()
                .addQueryParameter("filter", "uuid = \"$uuid\"")
                .addQueryParameter("fields", "id")
                .addQueryParameter("perPage", "1")
                .addQueryParameter("skipTotal", "true")
                .build()

            call(authorized(url, token).get().build()) { json ->
                val items = json.getJSONArray("items")
                if (items.length() == 0) null else items.getJSONObject(0).getString("id")
            }
        }
    }

    /** Changes the given fields of one record. */
    private suspend fun patchRecord(
        serverUrl: String,
        token: String,
        collection: String,
        remoteId: String,
        body: JSONObject
    ): AppResult<Unit> = withContext(Dispatchers.IO) {
        safely {
            val request = authorized(recordUrl(serverUrl, collection, remoteId), token)
                .patch(body.toRequestBody())
                .build()
            call(request) { }
        }
    }

    /**
     * One page of the household's records in [collection] changed at or after
     * [since], oldest change first. Includes deleted ones, so deletions can be applied.
     *
     * @param since a server "updated" time from an earlier sync, or null for everything
     * @param page 1 for the first page
     * @param parse turns one JSON record into [T]
     */
    private suspend fun <T> fetchChangedPage(
        serverUrl: String,
        token: String,
        collection: String,
        householdId: String,
        since: String?,
        page: Int,
        parse: (JSONObject) -> T
    ): AppResult<RemotePage<T>> = withContext(Dispatchers.IO) {
        safely {
            val filter = buildString {
                append("household = \"$householdId\"")
                // ">=" rather than ">": two changes in the same millisecond are not missed.
                if (since != null) append(" && updated >= \"$since\"")
            }
            val url = apiUrl(serverUrl, collection).newBuilder()
                .addQueryParameter("filter", filter)
                .addQueryParameter("sort", "updated")
                .addQueryParameter("perPage", PAGE_SIZE.toString())
                .addQueryParameter("page", page.toString())
                .build()

            call(authorized(url, token).get().build()) { json ->
                val items = json.getJSONArray("items")
                RemotePage(
                    items = (0 until items.length()).map { i -> parse(items.getJSONObject(i)) },
                    page = json.getInt("page"),
                    totalPages = json.getInt("totalPages")
                )
            }
        }
    }

    /**
     * The uuids of every record in [collection] of the household that currently
     * exists and isn't deleted. Only the uuid is downloaded, so this stays small.
     *
     * Used to notice records removed completely (e.g. in the PocketBase admin),
     * which never show up as a "change".
     */
    private suspend fun fetchActiveUuids(
        serverUrl: String,
        token: String,
        collection: String,
        householdId: String
    ): AppResult<Set<String>> {
        val uuids = mutableSetOf<String>()
        var page = 1
        do {
            val result = fetchActiveUuidPage(serverUrl, token, collection, householdId, page)
            val (items, totalPages) = when (result) {
                is AppResult.Error -> return result
                is AppResult.Success -> result.data
            }
            uuids += items
            page++
        } while (page <= totalPages)
        return AppResult.Success(uuids)
    }

    /** One page for [fetchActiveUuids]: the uuids and the total number of pages. */
    private suspend fun fetchActiveUuidPage(
        serverUrl: String,
        token: String,
        collection: String,
        householdId: String,
        page: Int
    ): AppResult<Pair<List<String>, Int>> = withContext(Dispatchers.IO) {
        safely {
            val url = apiUrl(serverUrl, collection).newBuilder()
                .addQueryParameter("filter", "household = \"$householdId\" && deleted = false")
                .addQueryParameter("fields", "uuid")
                .addQueryParameter("perPage", UUID_PAGE_SIZE.toString())
                .addQueryParameter("page", page.toString())
                .build()

            call(authorized(url, token).get().build()) { json ->
                val items = json.getJSONArray("items")
                val uuids = (0 until items.length()).map { i ->
                    items.getJSONObject(i).getString("uuid")
                }
                uuids to json.getInt("totalPages")
            }
        }
    }

    // ---------- JSON conversion ----------

    private fun ExpenseUpload.toJson(): JSONObject = JSONObject()
        .put("uuid", uuid)
        .put("household", householdId)
        .put("paidBy", paidBy)
        .put("amountCents", amountMinor)
        .put("currency", currency)
        .put("date", formatDate(date))
        .put("category", category)
        .put("payerSharePercent", payerSharePercent)
        .put("note", note.orEmpty())
        .put("deleted", false)
        .put("clientUpdatedAt", formatDate(clientUpdatedAt))

    private fun SettlementUpload.toJson(): JSONObject = JSONObject()
        .put("uuid", uuid)
        .put("household", householdId)
        .put("fromUser", fromUser)
        .put("toUser", toUser)
        .put("amountCents", amountMinor)
        .put("date", formatDate(date))
        .put("note", note.orEmpty())
        .put("deleted", false)

    private fun JSONObject.toRemoteExpense(): RemoteExpense = RemoteExpense(
        id = getString("id"),
        uuid = getString("uuid"),
        householdId = getString("household"),
        paidBy = getString("paidBy"),
        amountMinor = getLong("amountCents"),
        currency = optString("currency").ifBlank { "EUR" },
        date = parseDate(getString("date")),
        category = optString("category"),
        payerSharePercent = optInt("payerSharePercent", 50),
        note = optString("note").ifBlank { null },
        deleted = optBoolean("deleted", false),
        updated = getString("updated")
    )

    private fun JSONObject.toRemoteSettlement(): RemoteSettlement = RemoteSettlement(
        id = getString("id"),
        uuid = getString("uuid"),
        householdId = getString("household"),
        fromUser = getString("fromUser"),
        toUser = getString("toUser"),
        amountMinor = getLong("amountCents"),
        date = parseDate(getString("date")),
        note = optString("note").ifBlank { null },
        deleted = optBoolean("deleted", false),
        updated = getString("updated")
    )

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

    private fun JSONObject.toRequestBody(): RequestBody =
        toString().toRequestBody(JSON_MEDIA_TYPE)

    // ---------- Plumbing ----------

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
                // Some answers have no body (e.g. 204); parse an empty object then.
                AppResult.Success(parse(if (text.isBlank()) JSONObject() else JSONObject(text)))
            } else {
                AppResult.Error(explain(response.code, isLogin))
            }
        }

    /** A request builder with the login token attached. */
    private fun authorized(url: HttpUrl, token: String): Request.Builder =
        Request.Builder().url(url).header("Authorization", token)

    /** e.g. https://spender.aljaz.top/api/collections/users/auth-with-password */
    private fun apiUrl(serverUrl: String, path: String): HttpUrl =
        serverUrl.toHttpUrl().newBuilder()
            .addPathSegments("api/$path")
            .build()

    /** e.g. .../api/collections/shared_expenses/records/abc123 */
    private fun recordUrl(serverUrl: String, collection: String, remoteId: String): HttpUrl =
        apiUrl(serverUrl, collection).newBuilder().addPathSegment(remoteId).build()

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
        } catch (e: DateTimeParseException) {
            AppResult.Error("The server sent a date in an unexpected format.")
        } catch (e: IllegalArgumentException) {
            AppResult.Error("Invalid server address.")
        }

    /** Explains an HTTP status code in plain language. */
    private fun explain(code: Int, isLogin: Boolean): String = when (code) {
        400 -> if (isLogin) {
            "Wrong email or password."
        } else {
            "The server rejected the data. Check the fields of the collection in PocketBase."
        }
        401 -> "Login expired. Log in again."
        403 -> "Access denied by the server."
        404 -> "Not found. Check the server address."
        429 -> "Too many attempts. Wait a minute and try again."
        502, 503, 504 -> "The server is not reachable right now. Is PocketBase running?"
        else -> "The server responded with error $code."
    }

    companion object {
        private const val EXPENSES = "collections/shared_expenses/records"
        private const val SETTLEMENTS = "collections/settlements/records"
        private const val PAGE_SIZE = 200
        private const val UUID_PAGE_SIZE = 500
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        /** PocketBase's date format, always in UTC: "2026-10-28 16:40:12.345Z". */
        private val DATE_FORMAT: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC)

        /** Epoch millis → PocketBase date text. */
        fun formatDate(epochMillis: Long): String =
            DATE_FORMAT.format(Instant.ofEpochMilli(epochMillis))

        /** PocketBase date text → epoch millis. */
        fun parseDate(text: String): Long =
            Instant.parse(text.trim().replace(' ', 'T')).toEpochMilli()
    }
}