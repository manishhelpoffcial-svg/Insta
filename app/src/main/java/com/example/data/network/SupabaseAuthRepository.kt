package com.example.data.network

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AuthSession(
    val userId: String,
    val email: String,
    val username: String?,
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Long,
)

data class SubscriptionAccessResult(
    val isAllowed: Boolean,
    val accountStatus: String,
    val subscriptionStatus: String?,
    val expiryDate: String?,
    val productName: String?,
    val message: String,
)

class SupabaseAuthRepository {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun getSupabaseUrl(): String {
        return try {
            val url = BuildConfig.SUPABASE_URL.trim().trimEnd('/')
            if (url.startsWith("http://") || url.startsWith("https://")) url else ""
        } catch (_: Exception) {
            ""
        }
    }

    private fun getPublishableKey(): String {
        return try {
            val key = BuildConfig.SUPABASE_PUBLISHABLE_KEY.trim()
            if (key.isNotBlank() && key != "MY_SUPABASE_PUBLISHABLE_KEY") key else ""
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Resolves a username to its corresponding Supabase Auth email using a security-definer RPC function.
     * Never exposes user email lists to client queries.
     */
    suspend fun resolveUsernameToEmail(username: String): Result<String> = withContext(Dispatchers.IO) {
        val baseUrl = getSupabaseUrl()
        val apiKey = getPublishableKey()

        if (baseUrl.isBlank() || apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Supabase configuration is not set in .env"))
        }

        try {
            val payload = JSONObject().apply {
                put("p_username", username.trim())
            }.toString()

            val request = Request.Builder()
                .url("$baseUrl/rest/v1/rpc/resolve_username_to_email")
                .post(payload.toRequestBody(jsonMediaType))
                .header("apikey", apiKey)
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(RuntimeException("Username lookup failed (${response.code})"))
                }

                // If function returns string directly or json string
                val cleanEmail = body.trim().removeSurrounding("\"")
                if (cleanEmail.isBlank() || cleanEmail == "null") {
                    return@withContext Result.failure(IllegalArgumentException("No account found for username @$username"))
                }

                Result.success(cleanEmail)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Authenticates with Email or Username and Password.
     */
    suspend fun login(identifier: String, password: String): Result<AuthSession> = withContext(Dispatchers.IO) {
        val baseUrl = getSupabaseUrl()
        val apiKey = getPublishableKey()

        if (baseUrl.isBlank() || apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Supabase URL and API Key must be configured in .env"))
        }

        val cleanIdentifier = identifier.trim()
        val emailToUse = if (cleanIdentifier.contains("@")) {
            cleanIdentifier
        } else {
            val resolveResult = resolveUsernameToEmail(cleanIdentifier)
            if (resolveResult.isSuccess) {
                resolveResult.getOrThrow()
            } else {
                "${cleanIdentifier.lowercase()}@insta.app"
            }
        }

        try {
            val payload = JSONObject().apply {
                put("email", emailToUse)
                put("password", password)
            }.toString()

            val request = Request.Builder()
                .url("$baseUrl/auth/v1/token?grant_type=password")
                .post(payload.toRequestBody(jsonMediaType))
                .header("apikey", apiKey)
                .header("Content-Type", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                val json = if (body.isNotBlank()) JSONObject(body) else JSONObject()

                if (!response.isSuccessful) {
                    val errMsg = json.optString("error_description", json.optString("msg", "Invalid credentials."))
                    return@withContext Result.failure(RuntimeException(errMsg))
                }

                val accessToken = json.getString("access_token")
                val refreshToken = json.getString("refresh_token")
                val expiresIn = json.optLong("expires_in", 3600L)
                val userObj = json.getJSONObject("user")
                val userId = userObj.getString("id")
                val email = userObj.optString("email", emailToUse)

                // Fetch username from profiles table
                val username = fetchUsernameForUser(userId, accessToken)

                Result.success(
                    AuthSession(
                        userId = userId,
                        email = email,
                        username = username,
                        accessToken = accessToken,
                        refreshToken = refreshToken,
                        expiresAt = System.currentTimeMillis() + (expiresIn * 1000L),
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Registers a new account with email, username, and password.
     */
    suspend fun register(email: String, username: String, password: String): Result<AuthSession> = withContext(Dispatchers.IO) {
        val baseUrl = getSupabaseUrl()
        val apiKey = getPublishableKey()

        if (baseUrl.isBlank() || apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Supabase URL and API Key must be configured in .env"))
        }

        val cleanEmail = email.trim().lowercase()
        val cleanUsername = username.trim().lowercase()

        try {
            val payload = JSONObject().apply {
                put("email", cleanEmail)
                put("password", password)
                put("data", JSONObject().apply {
                    put("username", cleanUsername)
                })
            }.toString()

            val request = Request.Builder()
                .url("$baseUrl/auth/v1/signup")
                .post(payload.toRequestBody(jsonMediaType))
                .header("apikey", apiKey)
                .header("Content-Type", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                val json = if (body.isNotBlank()) JSONObject(body) else JSONObject()

                if (!response.isSuccessful) {
                    val errMsg = json.optString("msg", json.optString("error_description", "Registration failed."))
                    return@withContext Result.failure(RuntimeException(errMsg))
                }

                val userObj = json.optJSONObject("user") ?: json
                val userId = userObj.optString("id", "")
                val accessToken = json.optString("access_token", "")
                val refreshToken = json.optString("refresh_token", "")

                // Insert into public.profiles
                if (accessToken.isNotBlank()) {
                    createProfileRecord(userId, cleanUsername, cleanEmail, accessToken)
                }

                Result.success(
                    AuthSession(
                        userId = userId,
                        email = cleanEmail,
                        username = cleanUsername,
                        accessToken = accessToken,
                        refreshToken = refreshToken,
                        expiresAt = System.currentTimeMillis() + 3600000L,
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sends password recovery / reset email.
     */
    suspend fun sendPasswordReset(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        val baseUrl = getSupabaseUrl()
        val apiKey = getPublishableKey()

        if (baseUrl.isBlank() || apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Supabase configuration missing."))
        }

        try {
            val payload = JSONObject().apply {
                put("email", email.trim().lowercase())
            }.toString()

            val request = Request.Builder()
                .url("$baseUrl/auth/v1/recover")
                .post(payload.toRequestBody(jsonMediaType))
                .header("apikey", apiKey)
                .header("Content-Type", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val body = response.body?.string().orEmpty()
                    return@withContext Result.failure(RuntimeException("Could not send reset email (${response.code}): $body"))
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Verifies whether the account exists, is active, and has an unexpired subscription.
     */
    suspend fun verifyUserAccess(userId: String, accessToken: String): Result<SubscriptionAccessResult> = withContext(Dispatchers.IO) {
        val baseUrl = getSupabaseUrl()
        val apiKey = getPublishableKey()

        if (baseUrl.isBlank() || apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Supabase configuration missing."))
        }

        try {
            val payload = JSONObject().apply {
                put("p_user_id", userId)
            }.toString()

            val request = Request.Builder()
                .url("$baseUrl/rest/v1/rpc/verify_user_access")
                .post(payload.toRequestBody(jsonMediaType))
                .header("apikey", apiKey)
                .header("Authorization", "Bearer $accessToken")
                .header("Content-Type", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(RuntimeException("Verification check failed (${response.code})"))
                }

                val array = JSONArray(body)
                if (array.length() == 0) {
                    return@withContext Result.success(
                        SubscriptionAccessResult(
                            isAllowed = false,
                            accountStatus = "inactive",
                            subscriptionStatus = null,
                            expiryDate = null,
                            productName = null,
                            message = "You don't have active subscription",
                        )
                    )
                }

                val row = array.getJSONObject(0)
                val isAllowed = row.optBoolean("is_allowed", false)
                val rawMessage = row.optString("message", "").trim()
                val finalMessage = if (!isAllowed && (rawMessage.isBlank() || rawMessage.contains("no subscription", ignoreCase = true))) {
                    "You don't have active subscription"
                } else {
                    rawMessage.ifBlank { if (isAllowed) "Active" else "You don't have active subscription" }
                }
                Result.success(
                    SubscriptionAccessResult(
                        isAllowed = isAllowed,
                        accountStatus = row.optString("account_status", "active"),
                        subscriptionStatus = row.optString("subscription_status", null),
                        expiryDate = row.optString("expiry_date", null),
                        productName = row.optString("product_name", null),
                        message = finalMessage,
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Refreshes an expired access token using the persisted refresh token so users stay logged in.
     */
    suspend fun refreshSession(refreshToken: String): Result<AuthSession> = withContext(Dispatchers.IO) {
        val baseUrl = getSupabaseUrl()
        val apiKey = getPublishableKey()

        if (baseUrl.isBlank() || apiKey.isBlank() || refreshToken.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Missing refresh credentials"))
        }

        try {
            val payload = JSONObject().apply {
                put("refresh_token", refreshToken)
            }.toString()

            val request = Request.Builder()
                .url("$baseUrl/auth/v1/token?grant_type=refresh_token")
                .post(payload.toRequestBody(jsonMediaType))
                .header("apikey", apiKey)
                .header("Content-Type", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                val json = if (body.isNotBlank()) JSONObject(body) else JSONObject()
                if (!response.isSuccessful) {
                    val errMsg = json.optString("error_description", json.optString("msg", "Session refresh failed"))
                    return@withContext Result.failure(RuntimeException(errMsg))
                }
                val accessToken = json.getString("access_token")
                val newRefresh = json.optString("refresh_token", refreshToken)
                val expiresIn = json.optLong("expires_in", 3600L)
                val userObj = json.getJSONObject("user")
                val userId = userObj.getString("id")
                val email = userObj.optString("email", "")
                val username = fetchUsernameForUser(userId, accessToken)
                Result.success(
                    AuthSession(
                        userId = userId,
                        email = email,
                        username = username,
                        accessToken = accessToken,
                        refreshToken = newRefresh,
                        expiresAt = System.currentTimeMillis() + (expiresIn * 1000L),
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Locks the account to the first device that signs in, rejecting any other device.
     */
    suspend fun verifyAndLockDevice(
        userId: String,
        accessToken: String,
        deviceId: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val baseUrl = getSupabaseUrl()
        val apiKey = getPublishableKey()
        if (baseUrl.isBlank() || apiKey.isBlank() || userId.isBlank() || deviceId.isBlank()) {
            return@withContext Result.success(Unit)
        }

        try {
            val getReq = Request.Builder()
                .url("$baseUrl/rest/v1/profiles?id=eq.$userId&select=device_id")
                .get()
                .header("apikey", apiKey)
                .header("Authorization", "Bearer $accessToken")
                .build()

            client.newCall(getReq).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    // Column may not exist yet in older Supabase schemas; do not block valid users
                    if (resp.code == 400 || body.contains("device_id", ignoreCase = true)) {
                        return@withContext Result.success(Unit)
                    }
                    return@withContext Result.success(Unit)
                }
                val arr = JSONArray(body)
                val lockedDevice = if (arr.length() > 0) {
                    arr.getJSONObject(0).optString("device_id", "").trim()
                } else ""

                if (lockedDevice.isNotBlank() && lockedDevice != "null") {
                    if (lockedDevice != deviceId) {
                        return@withContext Result.failure(
                            IllegalStateException("This account is locked to another device.")
                        )
                    }
                    return@withContext Result.success(Unit)
                }
            }

            // Not yet bound -> bind to this device
            val patchPayload = JSONObject().apply {
                put("device_id", deviceId)
            }.toString()
            val patchReq = Request.Builder()
                .url("$baseUrl/rest/v1/profiles?id=eq.$userId")
                .patch(patchPayload.toRequestBody(jsonMediaType))
                .header("apikey", apiKey)
                .header("Authorization", "Bearer $accessToken")
                .header("Content-Type", "application/json")
                .build()
            client.newCall(patchReq).execute().close()
            Result.success(Unit)
        } catch (_: Exception) {
            Result.success(Unit)
        }
    }

    private fun fetchUsernameForUser(userId: String, accessToken: String): String? {
        val baseUrl = getSupabaseUrl()
        val apiKey = getPublishableKey()
        return try {
            val request = Request.Builder()
                .url("$baseUrl/rest/v1/profiles?id=eq.$userId&select=username")
                .get()
                .header("apikey", apiKey)
                .header("Authorization", "Bearer $accessToken")
                .build()

            client.newCall(request).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                val arr = JSONArray(body)
                if (arr.length() > 0) arr.getJSONObject(0).optString("username", null) else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun createProfileRecord(userId: String, username: String, email: String, accessToken: String) {
        val baseUrl = getSupabaseUrl()
        val apiKey = getPublishableKey()
        try {
            val payload = JSONObject().apply {
                put("id", userId)
                put("username", username)
                put("email", email)
                put("account_status", "active")
            }.toString()

            val request = Request.Builder()
                .url("$baseUrl/rest/v1/profiles")
                .post(payload.toRequestBody(jsonMediaType))
                .header("apikey", apiKey)
                .header("Authorization", "Bearer $accessToken")
                .header("Prefer", "resolution=merge-duplicates")
                .build()

            client.newCall(request).execute().close()
        } catch (_: Exception) {}
    }
}
