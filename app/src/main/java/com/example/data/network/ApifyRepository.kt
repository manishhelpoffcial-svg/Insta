package com.example.data.network

import com.example.BuildConfig
import com.example.data.model.MediaPost
import com.example.data.model.ProfileData
import com.example.data.model.ReelInsights
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

data class FetchedAccountResult(
    val profile: ProfileData,
    val posts: List<MediaPost>,
    val isSinglePostTarget: Boolean = false,
)

class ApifyRepository {

    private val client = MediaClient.getOkHttpClient()

    private fun resolveBackendUrl(): String {
        val raw = BuildConfig.APIFY_BACKEND_URL.trim()
        if (raw.isBlank() || raw == "MY_APIFY_BACKEND_URL" || raw.startsWith("apify_", ignoreCase = true)) {
            return ""
        }
        val withScheme = if (!raw.startsWith("http://", ignoreCase = true) && !raw.startsWith("https://", ignoreCase = true)) {
            if (!raw.contains(".") || raw.contains(" ")) return ""
            "https://$raw"
        } else {
            raw
        }
        return withScheme.trimEnd('/')
    }

    private fun getBuildConfigString(key: String): String {
        return runCatching {
            val field = BuildConfig::class.java.getField(key)
            field.get(null)?.toString().orEmpty()
        }.getOrDefault("")
    }

    private fun resolveTokenList(): List<String> {
        val list = mutableListOf<String>()

        val primaryEnv = BuildConfig.APIFY_API_TOKEN.trim()
            .takeIf { it.isNotBlank() && it != "MY_APIFY_API_TOKEN" }
        if (!primaryEnv.isNullOrBlank()) list.add(primaryEnv)

        val legacyEnv = BuildConfig.APIFY_TOKEN.trim()
            .takeIf { it.isNotBlank() && it != "MY_APIFY_TOKEN" }
        if (!legacyEnv.isNullOrBlank() && !list.contains(legacyEnv)) list.add(legacyEnv)

        // Automatic Fallback Backup Tokens 1 through 7
        for (i in 1..7) {
            val backup = getBuildConfigString("APIFY_BACKUP_TOKEN_$i").trim()
            if (backup.isNotBlank() && !backup.startsWith("MY_") && !list.contains(backup)) {
                list.add(backup)
            }
        }

        val backendUrlAsToken = BuildConfig.APIFY_BACKEND_URL.trim()
            .takeIf { it.startsWith("apify_", ignoreCase = true) }
        if (!backendUrlAsToken.isNullOrBlank() && !list.contains(backendUrlAsToken)) {
            list.add(backendUrlAsToken)
        }

        return list
    }

    suspend fun fetchInstagramProfile(
        usernameInput: String,
    ): Result<FetchedAccountResult> = withContext(Dispatchers.IO) {
        val cleanUsername = extractUsernameFromTarget(usernameInput)
        if (cleanUsername.isBlank()) {
            return@withContext Result.failure(
                IllegalArgumentException("Please enter an Instagram username.")
            )
        }

        val backendUrl = resolveBackendUrl()
        if (backendUrl.isNotBlank()) {
            return@withContext fetchViaSecureBackend(backendUrl, cleanUsername)
        }

        val tokenList = resolveTokenList()
        if (tokenList.isEmpty()) {
            return@withContext Result.failure(
                IllegalStateException(
                    "Configure APIFY_API_TOKEN or APIFY_BACKUP_TOKEN_1..7 in the .env file."
                )
            )
        }

        var lastError: Exception? = null

        for ((index, token) in tokenList.withIndex()) {
            val tokenName = if (index == 0) "Primary Token" else "Backup Token #$index"
            val result = executeScraperWithToken(cleanUsername, token)
            if (result.isSuccess) {
                return@withContext result
            }

            val err = result.exceptionOrNull() as? Exception ?: RuntimeException(result.exceptionOrNull()?.message)
            lastError = err
            val msg = err.message.orEmpty().lowercase()

            val isCreditOrAuthLimit = msg.contains("401") ||
                msg.contains("402") ||
                msg.contains("403") ||
                msg.contains("429") ||
                msg.contains("usage limit") ||
                msg.contains("credits") ||
                msg.contains("rate limit") ||
                msg.contains("unauthorized")

            if (isCreditOrAuthLimit && index < tokenList.size - 1) {
                android.util.Log.w(
                    "ApifyRepository",
                    "$tokenName failed with: ${err.message}. Rotating automatically to next backup token..."
                )
                continue
            } else if (!isCreditOrAuthLimit) {
                // If it's another kind of error (e.g. empty target), return directly
                return@withContext result
            }
        }

        Result.failure(lastError ?: RuntimeException("All Apify tokens failed."))
    }

    private fun executeScraperWithToken(
        cleanUsername: String,
        resolvedToken: String,
    ): Result<FetchedAccountResult> {
        return try {
            // Exact Actor input for apify/instagram-profile-scraper
            val payload = buildProfileScraperPayload(cleanUsername)

            val startRunRequest = Request.Builder()
                .url(APIFY_PROFILE_SCRAPER_RUNS_URL)
                .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .header("Authorization", "Bearer $resolvedToken")
                .header("Accept", "application/json")
                .build()

            var runData: JSONObject
            client.newCall(startRunRequest).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val hint = when (response.code) {
                        401, 403 -> "Invalid or unauthorized APIFY_API_TOKEN (${response.code})."
                        402 -> "Apify account usage limit reached (402 credits expired)."
                        429 -> "Apify rate limit exceeded (429)."
                        else -> "Apify error (${response.code}): ${body.take(180)}"
                    }
                    return Result.failure(RuntimeException(hint))
                }
                runData = JSONObject(body).optJSONObject("data")
                    ?: return Result.failure(RuntimeException("Unexpected Apify run response."))
            }

            val runId = runData.optString("id", "")
            var status = runData.optString("status", "")
            var datasetId = runData.optString("defaultDatasetId", "")

            var attempts = 0
            while ((status == "READY" || status == "RUNNING") && runId.isNotBlank() && attempts < 4) {
                attempts++
                val pollRequest = Request.Builder()
                    .url("https://api.apify.com/v2/actor-runs/$runId?waitForFinish=60")
                    .get()
                    .header("Authorization", "Bearer $resolvedToken")
                    .header("Accept", "application/json")
                    .build()

                client.newCall(pollRequest).execute().use { pollResponse ->
                    val pollBody = pollResponse.body?.string().orEmpty()
                    if (!pollResponse.isSuccessful) {
                        return Result.failure(
                            RuntimeException("Failed polling Apify run (${pollResponse.code}).")
                        )
                    }
                    runData = JSONObject(pollBody).optJSONObject("data") ?: runData
                    status = runData.optString("status", status)
                    datasetId = runData.optString("defaultDatasetId", datasetId)
                }
            }

            if (status != "SUCCEEDED") {
                return Result.failure(
                    RuntimeException("Apify run did not succeed (status: $status).")
                )
            }
            if (datasetId.isBlank()) {
                return Result.failure(
                    RuntimeException("Apify run completed without a dataset ID.")
                )
            }

            val itemsUrl =
                "https://api.apify.com/v2/datasets/$datasetId/items?clean=true&format=json"
            val itemsRequest = Request.Builder()
                .url(itemsUrl)
                .get()
                .header("Authorization", "Bearer $resolvedToken")
                .header("Accept", "application/json")
                .build()

            client.newCall(itemsRequest).execute().use { itemsResponse ->
                val itemsBody = itemsResponse.body?.string().orEmpty()
                if (!itemsResponse.isSuccessful) {
                    return Result.failure(
                        RuntimeException("Failed fetching Apify dataset items (${itemsResponse.code}).")
                    )
                }
                parseApifyResponseJson(
                    rawJson = itemsBody,
                    fallbackUsername = cleanUsername,
                    isSinglePostTarget = false,
                )
            }
        } catch (e: Exception) {
            Result.failure(
                RuntimeException(
                    e.message ?: "Unable to load @$cleanUsername right now."
                )
            )
        }
    }

    private fun fetchViaSecureBackend(
        backendUrl: String,
        cleanUsername: String,
    ): Result<FetchedAccountResult> {
        return try {
            val payload = JSONObject().apply {
                put("username", cleanUsername)
            }.toString()

            val request = Request.Builder()
                .url("$backendUrl/api/fetch-account")
                .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val serverError = runCatching { JSONObject(body).optString("error", "") }
                        .getOrDefault("")
                        .ifBlank { "Backend request failed (${response.code})." }
                    return Result.failure(RuntimeException(serverError))
                }
                parseApifyResponseJson(
                    rawJson = body,
                    fallbackUsername = cleanUsername,
                    isSinglePostTarget = false,
                )
            }
        } catch (e: Exception) {
            Result.failure(
                RuntimeException(e.message ?: "Failed connecting to backend for @$cleanUsername.")
            )
        }
    }

    fun parseApifyResponseJson(
        rawJson: String,
        fallbackUsername: String = "instagram",
        isSinglePostTarget: Boolean = false,
    ): Result<FetchedAccountResult> {
        return try {
            val trimmed = rawJson.trim()
            if (trimmed.isBlank()) {
                return Result.failure(IllegalArgumentException("Empty JSON response."))
            }

            val itemsArray: JSONArray = when {
                trimmed.startsWith("[") -> JSONArray(trimmed)
                trimmed.startsWith("{") -> {
                    val rootObj = JSONObject(trimmed)
                    if (rootObj.has("error") && !rootObj.has("items") && !rootObj.has("username") && !rootObj.has("shortCode")) {
                        return Result.failure(
                            RuntimeException(rootObj.optString("error", "Apify returned an error."))
                        )
                    }
                    when {
                        rootObj.optJSONArray("items") != null -> rootObj.getJSONArray("items")
                        rootObj.optJSONArray("data") != null -> rootObj.getJSONArray("data")
                        else -> JSONArray().apply { put(rootObj) }
                    }
                }
                else -> return Result.failure(IllegalArgumentException("Invalid JSON format."))
            }

            if (itemsArray.length() == 0) {
                return Result.failure(
                    IllegalStateException("No Instagram account data was returned for @$fallbackUsername.")
                )
            }

            val objects = mutableListOf<JSONObject>()
            for (i in 0 until itemsArray.length()) {
                val obj = itemsArray.optJSONObject(i) ?: continue
                if (obj.has("error") &&
                    !obj.has("username") &&
                    !obj.has("shortCode") &&
                    !obj.has("ownerUsername")
                ) {
                    if (objects.isEmpty() && i == itemsArray.length() - 1) {
                        val errText = obj.optString("errorDescription", "")
                            .ifBlank { obj.optString("error", "Apify returned an error.") }
                        return Result.failure(RuntimeException(errText))
                    }
                    continue
                }
                objects.add(obj)
            }

            if (objects.isEmpty()) {
                return Result.failure(
                    IllegalStateException("Apify returned an empty dataset for @$fallbackUsername.")
                )
            }

            parseInstagramScraperDataset(objects, fallbackUsername, isSinglePostTarget)
        } catch (e: Exception) {
            Result.failure(RuntimeException("Failed to parse Apify dataset: ${e.message}"))
        }
    }

    private fun parseInstagramScraperDataset(
        items: List<JSONObject>,
        fallbackUsername: String,
        isSinglePostTarget: Boolean,
    ): Result<FetchedAccountResult> {
        val profileObj = items.firstOrNull { obj ->
            obj.has("followersCount") ||
                obj.has("followsCount") ||
                obj.has("followingCount") ||
                obj.has("biography") ||
                obj.has("latestPosts") ||
                obj.has("latestIgtvVideos") ||
                obj.has("latestReels") ||
                obj.has("isPrivate") ||
                obj.has("private") ||
                obj.optString("type", "").equals("Profile", ignoreCase = true)
        } ?: items.firstOrNull { it.has("owner") }?.optJSONObject("owner")

        val postJsonList = mutableListOf<JSONObject>()
        val seenKeys = mutableSetOf<String>()

        fun addPostCandidate(candidate: JSONObject) {
            val isPureProfileOnly = (candidate.has("followersCount") || candidate.has("latestPosts")) &&
                !candidate.has("shortCode") &&
                !candidate.has("shortcode") &&
                !candidate.has("displayUrl") &&
                !candidate.has("videoUrl") &&
                !candidate.has("imageUrl")
            if (isPureProfileOnly) return

            val dedupKey = extractDedupKey(candidate)
            if (dedupKey.isNotBlank()) {
                if (seenKeys.add(dedupKey)) {
                    postJsonList.add(candidate)
                }
            } else {
                postJsonList.add(candidate)
            }
        }

        items.forEach { item ->
            listOf("latestPosts", "latestIgtvVideos", "latestReels", "posts", "reels").forEach { arrKey ->
                val arr = item.optJSONArray(arrKey)
                if (arr != null) {
                    for (j in 0 until arr.length()) {
                        arr.optJSONObject(j)?.let { addPostCandidate(it) }
                    }
                }
            }
            addPostCandidate(item)
        }

        val firstPost = postJsonList.firstOrNull()
        val firstPostOwner = firstPost?.optJSONObject("owner")

        val resolvedUsername = firstNonBlank(
            profileObj?.optString("username", ""),
            firstPost?.optString("ownerUsername", ""),
            firstPostOwner?.optString("username", ""),
            items.firstOrNull()?.optString("username", ""),
            fallbackUsername,
        ).removePrefix("@").ifBlank { "N/A" }

        val resolvedFullName = firstNonBlank(
            profileObj?.optString("fullName", ""),
            profileObj?.optString("full_name", ""),
            profileObj?.optString("name", ""),
            firstPost?.optString("ownerFullName", ""),
            firstPostOwner?.optString("fullName", ""),
            firstPostOwner?.optString("full_name", ""),
        ).ifBlank { "N/A" }

        val resolvedAvatar = firstNonBlank(
            profileObj?.optString("profilePicUrlHD", ""),
            profileObj?.optString("profilePicUrl", ""),
            profileObj?.optString("profile_pic_url_hd", ""),
            profileObj?.optString("profile_pic_url", ""),
            firstPost?.optString("ownerProfilePicUrl", ""),
            firstPostOwner?.optString("profilePicUrlHD", ""),
            firstPostOwner?.optString("profilePicUrl", ""),
            firstPostOwner?.optString("profile_pic_url", ""),
        ).ifBlank { null }

        val resolvedBio = firstNonBlank(
            profileObj?.optString("biography", ""),
            profileObj?.optString("bio", ""),
            firstPostOwner?.optString("biography", ""),
        ).ifBlank { "N/A" }

        val externalLinksFromArr = profileObj?.optJSONArray("externalUrls")
            ?.let { arr -> (0 until arr.length()).firstNotNullOfOrNull { idx -> arr.optString(idx).takeIf { it.isNotBlank() } } }
            ?: profileObj?.optJSONArray("bioLinks")
                ?.let { arr -> arr.optJSONObject(0)?.optString("url", "")?.takeIf { it.isNotBlank() } }
            ?: ""

        val resolvedExternalUrl = firstNonBlank(
            profileObj?.optString("externalUrl", ""),
            profileObj?.optString("external_url", ""),
            profileObj?.optString("website", ""),
            externalLinksFromArr,
            firstPostOwner?.optString("externalUrl", ""),
        ).ifBlank { "N/A" }

        val isVerified = readBooleanNullable(profileObj, "verified", "isVerified", "is_verified")
            ?: readBooleanNullable(firstPost, "ownerIsVerified")
            ?: readBooleanNullable(firstPostOwner, "verified", "isVerified", "is_verified")
            ?: false

        val isPrivate = readBooleanNullable(profileObj, "private", "isPrivate", "is_private")
            ?: readBooleanNullable(firstPostOwner, "private", "isPrivate", "is_private")
        val privacyStatus = when (isPrivate) {
            true -> "Private"
            false -> "Public"
            null -> "N/A"
        }

        val followersLong = readNonNegativeLong(profileObj, "followersCount", "followerCount")
            ?: readNonNegativeLong(profileObj?.optJSONObject("edge_followed_by"), "count")
            ?: readNonNegativeLong(firstPostOwner, "followersCount")
        val followingLong = readNonNegativeLong(profileObj, "followsCount", "followingCount")
            ?: readNonNegativeLong(profileObj?.optJSONObject("edge_follow"), "count")
            ?: readNonNegativeLong(firstPostOwner, "followsCount", "followingCount")
        val postsCountLong = readNonNegativeLong(profileObj, "postsCount", "mediaCount")
            ?: readNonNegativeLong(profileObj?.optJSONObject("edge_owner_to_timeline_media"), "count")

        val followersDisplay = if (followersLong != null) formatCompactCount(followersLong) else "N/A"
        val followingDisplay = if (followingLong != null) formatStatCount(followingLong.toInt()) else "N/A"
        val postsDisplay = if (postsCountLong != null) formatStatCount(postsCountLong.toInt()) else "N/A"

        var totalRealViews = 0L
        var anyPostHasViews = false

        val mappedPosts = postJsonList.mapIndexed { idx, postJson ->
            val postAuthor = firstNonBlank(
                postJson.optString("ownerUsername", ""),
                postJson.optJSONObject("owner")?.optString("username", ""),
                resolvedUsername,
            ).removePrefix("@").ifBlank { resolvedUsername }
            val postAvatar = firstNonBlank(
                postJson.optString("ownerProfilePicUrl", ""),
                resolvedAvatar.orEmpty(),
            ).ifBlank { null }

            val mapped = mapPostJsonToMediaPost(
                postJson = postJson,
                idx = idx,
                username = postAuthor,
                avatarUrl = postAvatar,
            )
            val realViews = mapped.insights.views.replace(",", "").toLongOrNull()
            if (realViews != null) {
                anyPostHasViews = true
                totalRealViews += realViews
            }
            mapped
        }.distinctBy { it.shortCode.ifBlank { it.postUrl.ifBlank { it.id } } }

        val dashboardSubtitleText = if (anyPostHasViews) {
            "${formatCompactCount(totalRealViews)} views across ${mappedPosts.size} fetched posts."
        } else {
            "N/A views across ${mappedPosts.size} fetched posts."
        }

        val profile = ProfileData(
            username = resolvedUsername,
            displayName = resolvedFullName,
            isVerified = isVerified,
            avatarUrl = resolvedAvatar,
            avatarDrawableKey = "themovietimez",
            postsCount = postsDisplay,
            followersCount = followersDisplay,
            followingCount = followingDisplay,
            bio = resolvedBio,
            externalUrl = resolvedExternalUrl,
            privacyStatus = privacyStatus,
            dashboardSubtitle = dashboardSubtitleText,
            noteText = "",
        )

        return Result.success(
            FetchedAccountResult(
                profile = profile,
                posts = mappedPosts,
                isSinglePostTarget = isSinglePostTarget,
            )
        )
    }

    private fun mapPostJsonToMediaPost(
        postJson: JSONObject,
        idx: Int,
        username: String,
        avatarUrl: String?,
    ): MediaPost {
        val postUrl = firstNonBlank(
            postJson.optString("url", ""),
            postJson.optString("postUrl", ""),
            postJson.optString("permalink", ""),
        )
        val shortCode = firstNonBlank(
            postJson.optString("shortCode", ""),
            postJson.optString("shortcode", ""),
            postJson.optString("code", ""),
            extractShortCodeFromUrl(postUrl),
        )
        val id = postJson.optString("id", "")
            .ifBlank { shortCode }
            .ifBlank { postUrl }
            .ifBlank { "apify_${username}_$idx" }

        val caption = firstNonBlank(
            postJson.optString("caption", ""),
            postJson.optString("text", ""),
        ).ifBlank { "N/A" }

        val childPosts = postJson.optJSONArray("childPosts")
        val firstChildDisplay = childPosts?.optJSONObject(0)?.optString("displayUrl", "").orEmpty()
        val firstChildVideo = childPosts?.optJSONObject(0)?.optString("videoUrl", "").orEmpty()

        val displayUrl = firstNonBlank(
            postJson.optString("displayUrl", ""),
            postJson.optString("thumbnailUrl", ""),
            postJson.optString("imageUrl", ""),
            postJson.optString("display_url", ""),
            postJson.optJSONArray("images")?.optString(0),
            firstChildDisplay,
        ).ifBlank { null }

        val videoUrl = firstNonBlank(
            postJson.optString("videoUrl", ""),
            postJson.optString("video_url", ""),
            postJson.optJSONArray("videoVersions")?.optJSONObject(0)?.optString("url", ""),
            firstChildVideo,
        ).ifBlank { null }

        val rawLikes = readNonNegativeLong(postJson, "likesCount", "likeCount", "likes")
            ?: readNonNegativeLong(postJson.optJSONObject("edge_liked_by"), "count")
        val likesAvailable = rawLikes != null
        val likes = rawLikes?.toInt()?.coerceAtLeast(0) ?: 0

        val rawComments = readNonNegativeLong(postJson, "commentsCount", "commentCount", "comments")
            ?: readNonNegativeLong(postJson.optJSONObject("edge_media_to_comment"), "count")
        val commentsAvailable = rawComments != null
        val comments = rawComments?.toInt()?.coerceAtLeast(0) ?: 0

        val rawReposts = readNonNegativeLong(postJson, "repostsCount", "repostCount", "reposts")
        val repostsAvailable = rawReposts != null
        val reposts = rawReposts?.toInt()?.coerceAtLeast(0) ?: 0

        val rawShares = readNonNegativeLong(postJson, "sharesCount", "shareCount", "shares")
        val sharesAvailable = rawShares != null
        val shares = rawShares?.toInt()?.coerceAtLeast(0) ?: 0

        val rawSaves = readNonNegativeLong(postJson, "savesCount", "saveCount", "saves", "bookmarkCount")
        val savesAvailable = rawSaves != null
        val saves = rawSaves?.toInt()?.coerceAtLeast(0) ?: 0

        val playCount = readNonNegativeLong(postJson, "videoPlayCount", "playCount", "playsCount")
        val viewCount = readNonNegativeLong(postJson, "videoViewCount", "viewCount", "viewsCount", "views")
        val rawViews = when {
            playCount != null && viewCount != null -> max(playCount, viewCount)
            playCount != null -> playCount
            else -> viewCount
        }
        val viewsAvailable = rawViews != null

        val typeStr = postJson.optString("type", "")
        val productType = postJson.optString("productType", "")
        val isReel = videoUrl != null ||
            typeStr.equals("Video", ignoreCase = true) ||
            typeStr.equals("Reel", ignoreCase = true) ||
            productType.equals("clips", ignoreCase = true)

        val durationRaw = if (postJson.has("videoDuration") && !postJson.isNull("videoDuration")) {
            postJson.optDouble("videoDuration", Double.NaN)
        } else if (postJson.has("duration") && !postJson.isNull("duration")) {
            postJson.optDouble("duration", Double.NaN)
        } else {
            Double.NaN
        }
        val durationSec = if (!durationRaw.isNaN() && durationRaw > 0.0) {
            durationRaw.roundToInt().coerceAtLeast(1)
        } else {
            0
        }
        val durationFormatted = if (durationSec > 0) {
            String.format(Locale.US, "%d:%02d", durationSec / 60, durationSec % 60)
        } else {
            "N/A"
        }

        val timestampRaw = firstNonBlank(
            postJson.optString("timestamp", ""),
            postJson.optString("date", ""),
            postJson.optString("taken_at", ""),
        )
        val epochSecs = readNonNegativeLong(postJson, "takenAtTimestamp")
        val dateLabel = when {
            timestampRaw.isNotBlank() -> formatTimestampLabel(timestampRaw)
            epochSecs != null && epochSecs > 0L ->
                formatTimestampLabel(Instant.ofEpochSecond(epochSecs).toString())
            else -> "N/A"
        }

        val musicObj = postJson.optJSONObject("musicInfo") ?: postJson.optJSONObject("audioInfo")
        val artistName = firstNonBlank(
            musicObj?.optString("artist_name", ""),
            musicObj?.optString("artistName", ""),
            musicObj?.optString("artist", ""),
        )
        val songName = firstNonBlank(
            musicObj?.optString("song_name", ""),
            musicObj?.optString("songName", ""),
            musicObj?.optString("title", ""),
            postJson.optString("songName", ""),
            postJson.optString("audioName", ""),
            postJson.optString("musicTitle", ""),
        )
        val usesOriginalAudio = readBooleanNullable(musicObj, "uses_original_audio", "usesOriginalAudio")
            ?: readBooleanNullable(postJson, "usesOriginalAudio")
        val audioLabel = when {
            artistName.isNotBlank() && songName.isNotBlank() -> "$artistName · $songName"
            songName.isNotBlank() -> songName
            usesOriginalAudio == true -> "$username · Original audio"
            else -> "N/A"
        }

        val nf = NumberFormat.getNumberInstance(Locale.US)
        val viewersRaw = readNonNegativeLong(postJson, "viewersCount", "reachCount")
        val followsRaw = readNonNegativeLong(postJson, "followsCount")

        val insights = ReelInsights(
            views = if (viewsAvailable) nf.format(rawViews) else "N/A",
            viewers = if (viewersRaw != null) nf.format(viewersRaw) else "N/A",
            averageWatchTime = postJson.optString("averageWatchTime", "").ifBlank { "N/A" },
            follows = if (followsRaw != null) nf.format(followsRaw) else "N/A",
            likes = if (likesAvailable) nf.format(likes) else "N/A",
            comments = if (commentsAvailable) nf.format(comments) else "N/A",
            reposts = if (repostsAvailable) nf.format(reposts) else "N/A",
            shares = if (sharesAvailable) nf.format(shares) else "N/A",
            saves = if (savesAvailable) nf.format(saves) else "N/A",
            profileVisits = "N/A",
            engagementFollows = if (followsRaw != null) nf.format(followsRaw) else "N/A",
            impacts = emptyList(),
            retentionUnavailable = true,
            retentionCurve = emptyList(),
            topSources = emptyList(),
            likeCurvePoints = emptyList(),
            durationLabel = durationFormatted,
            followersViewPercent = 0.0,
            nonFollowersViewPercent = 0.0,
            ageBreakdown = emptyList(),
            countryBreakdown = emptyList(),
            genderBreakdown = emptyList(),
            viewsOverTimeAll = emptyList(),
            viewsOverTimeFollowers = emptyList(),
            viewsOverTimeNonFollowers = emptyList(),
        )

        val viewsDisplayText = if (viewsAvailable) formatCompactCount(rawViews!!) else "N/A"

        return MediaPost(
            id = id,
            shortCode = shortCode,
            postUrl = postUrl,
            authorUsername = username,
            authorAvatarUrl = avatarUrl,
            authorAvatarKey = "themovietimez",
            subtitle = audioLabel,
            isSuggested = false,
            isFollowing = true,
            mediaUrl = displayUrl,
            videoUrl = videoUrl,
            drawableKey = if (idx % 3 == 0) "beth" else if (idx % 3 == 1) "feed_cinema" else "cute",
            overlayTopCaption = "",
            overlaySubtitle = "",
            durationText = durationFormatted,
            caption = caption,
            dateLabel = dateLabel,
            likedByText = "",
            viewsDisplay = viewsDisplayText,
            likesCount = likes,
            commentsCount = comments,
            repostsCount = reposts,
            sharesCount = shares,
            savesCount = saves,
            likesUnavailable = !likesAvailable,
            commentsUnavailable = !commentsAvailable,
            repostsUnavailable = !repostsAvailable,
            sharesUnavailable = !sharesAvailable,
            savesUnavailable = !savesAvailable,
            isLiked = false,
            isSaved = false,
            isReposted = false,
            isReel = isReel,
            isOwnPost = true,
            showInFeed = idx < 3,
            showInExplore = true,
            insights = insights,
        )
    }

    private fun extractDedupKey(obj: JSONObject): String {
        val sc = firstNonBlank(
            obj.optString("shortCode", ""),
            obj.optString("shortcode", ""),
            obj.optString("code", ""),
            extractShortCodeFromUrl(obj.optString("url", "")),
        )
        if (sc.isNotBlank()) return "sc:$sc"
        val url = firstNonBlank(obj.optString("url", ""), obj.optString("postUrl", ""))
        if (url.isNotBlank()) return "url:$url"
        val id = obj.optString("id", "").trim()
        return if (id.isNotBlank()) "id:$id" else ""
    }

    private fun readNonNegativeLong(obj: JSONObject?, vararg keys: String): Long? {
        if (obj == null) return null
        for (k in keys) {
            if (obj.has(k) && !obj.isNull(k)) {
                val raw = obj.opt(k)
                val parsed = when (raw) {
                    is Number -> raw.toLong()
                    is String -> raw.trim().replace(",", "").toLongOrNull()
                    else -> null
                }
                if (parsed != null && parsed >= 0L) return parsed
            }
        }
        return null
    }

    private fun readBooleanNullable(obj: JSONObject?, vararg keys: String): Boolean? {
        if (obj == null) return null
        for (k in keys) {
            if (obj.has(k) && !obj.isNull(k)) {
                val raw = obj.opt(k)
                when (raw) {
                    is Boolean -> return raw
                    is String -> {
                        if (raw.equals("true", ignoreCase = true)) return true
                        if (raw.equals("false", ignoreCase = true)) return false
                    }
                }
            }
        }
        return null
    }

    private fun firstNonBlank(vararg values: String?): String {
        for (v in values) {
            val clean = v?.trim().orEmpty()
            if (clean.isNotBlank() && !clean.equals("null", ignoreCase = true)) {
                return clean
            }
        }
        return ""
    }

    private fun formatTimestampLabel(timestamp: String): String {
        return formatRelativeDate(timestamp)
    }

    companion object {
        internal const val APIFY_ACTOR_ID = "apify/instagram-profile-scraper"
        internal const val APIFY_PROFILE_SCRAPER_RUNS_URL =
            "https://api.apify.com/v2/acts/apify~instagram-profile-scraper/runs?waitForFinish=60"

        internal fun buildProfileScraperPayload(cleanUsername: String): String {
            return JSONObject().apply {
                put("usernames", JSONArray().apply {
                    put(cleanUsername)
                })
            }.toString()
        }

        internal fun normalizeTargetInput(raw: String): String {
            val trimmed = raw.trim()
            if (trimmed.isBlank()) return ""
            return if (trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true)
            ) {
                trimmed
            } else {
                trimmed.removePrefix("@")
            }
        }

        internal fun normalizeDirectUrl(rawInput: String, cleanUsername: String): String {
            val trimmed = rawInput.trim()
            return if (trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true)
            ) {
                trimmed
            } else {
                "https://www.instagram.com/$cleanUsername/"
            }
        }

        internal fun isPostOrReelUrl(target: String): Boolean {
            return target.contains("/p/", ignoreCase = true) ||
                target.contains("/reel/", ignoreCase = true) ||
                target.contains("/tv/", ignoreCase = true)
        }

        internal fun extractShortCodeFromUrl(url: String): String {
            val match = Regex("""instagram\.com/(?:p|reel|reels|tv)/([^/?#]+)""", RegexOption.IGNORE_CASE)
                .find(url)
            return match?.groupValues?.getOrNull(1).orEmpty()
        }

        internal fun extractUsernameFromTarget(target: String): String {
            val clean = target.trim()
            if (clean.isBlank()) return ""
            if (clean.startsWith("http", ignoreCase = true)) {
                val match = Regex("""instagram\.com/([^/?#]+)""", RegexOption.IGNORE_CASE).find(clean)
                val seg = match?.groupValues?.getOrNull(1).orEmpty()
                if (seg.isNotBlank() && seg !in setOf("p", "reel", "reels", "tv", "stories", "explore")) {
                    return seg.removePrefix("@").trim()
                }
                return ""
            }
            return clean.removePrefix("@").trim()
        }

        fun formatInsightsIconCount(rawValue: String): String {
            val trimmed = rawValue.trim()
            if (trimmed.isBlank() || trimmed == "—" || trimmed.equals("N/A", ignoreCase = true)) return trimmed
            if (trimmed.endsWith("K", ignoreCase = true) ||
                trimmed.endsWith("M", ignoreCase = true) ||
                trimmed.endsWith("B", ignoreCase = true)
            ) {
                return trimmed.uppercase(Locale.US)
            }
            val parsed = trimmed.replace(",", "").toLongOrNull() ?: return trimmed
            return when {
                parsed >= 1_000_000_000L -> {
                    val tenths = (parsed * 10L) / 1_000_000_000L
                    if (tenths % 10L == 0L) "${tenths / 10L}B" else "${tenths / 10L}.${tenths % 10L}B"
                }
                parsed >= 1_000_000L -> {
                    val tenths = (parsed * 10L) / 1_000_000L
                    if (tenths % 10L == 0L) "${tenths / 10L}M" else "${tenths / 10L}.${tenths % 10L}M"
                }
                parsed >= 1_000L -> {
                    val tenths = (parsed * 10L) / 1_000L
                    if (tenths % 10L == 0L) "${tenths / 10L}K" else "${tenths / 10L}.${tenths % 10L}K"
                }
                else -> parsed.toString()
            }
        }

        fun formatRelativeDate(raw: String, nowMillis: Long = System.currentTimeMillis()): String {
            val trimmed = raw.trim()
            if (trimmed.isBlank()) return "N/A"
            if (trimmed.equals("N/A", ignoreCase = true)) return "N/A"
            if (trimmed.contains("ago", ignoreCase = true) || trimmed.equals("Today", ignoreCase = true)) {
                return trimmed
            }
            return try {
                val parsedInstant: Instant? = runCatching { Instant.parse(trimmed) }.getOrNull()
                    ?: runCatching { OffsetDateTime.parse(trimmed).toInstant() }.getOrNull()
                    ?: runCatching {
                        if (Regex("""^\d{4}-\d{2}-\d{2}$""").matches(trimmed)) {
                            LocalDate.parse(trimmed).atStartOfDay(ZoneOffset.UTC).toInstant()
                        } else null
                    }.getOrNull()

                if (parsedInstant == null) return trimmed

                val diffMinutes = ChronoUnit.MINUTES.between(parsedInstant, Instant.ofEpochMilli(nowMillis)).coerceAtLeast(0L)
                val diffHours = diffMinutes / 60L
                val diffDays = diffHours / 24L
                when {
                    diffMinutes < 1L -> "Just now"
                    diffMinutes < 60L -> if (diffMinutes == 1L) "1 minute ago" else "$diffMinutes minutes ago"
                    diffHours < 24L -> if (diffHours == 1L) "1 hour ago" else "$diffHours hours ago"
                    diffDays < 7L -> if (diffDays == 1L) "1 day ago" else "$diffDays days ago"
                    diffDays < 28L -> {
                        val weeks = (diffDays / 7L).coerceAtLeast(1L)
                        if (weeks == 1L) "1 week ago" else "$weeks weeks ago"
                    }
                    else -> {
                        val months = (diffDays / 30L).coerceAtLeast(1L)
                        if (months == 1L) "1 month ago" else "$months months ago"
                    }
                }
            } catch (_: Exception) {
                trimmed
            }
        }

        fun formatCompactCount(count: Long): String {
            return when {
                count >= 1_000_000_000L -> String.format(Locale.US, "%.1fB", count / 1_000_000_000.0).replace(".0B", "B")
                count >= 1_000_000L -> String.format(Locale.US, "%.1fM", count / 1_000_000.0).replace(".0M", "M")
                count >= 10_000L -> String.format(Locale.US, "%.1fK", count / 1_000.0).replace(".0K", "K")
                count >= 1_000L -> NumberFormat.getNumberInstance(Locale.US).format(count)
                else -> count.toString()
            }
        }

        fun formatStatCount(count: Int): String {
            return when {
                count >= 1_000_000 -> formatCompactCount(count.toLong())
                else -> NumberFormat.getNumberInstance(Locale.US).format(count)
            }
        }
    }
}
