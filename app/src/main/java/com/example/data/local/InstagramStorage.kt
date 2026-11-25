package com.example.data.local

import android.content.Context
import com.example.data.DefaultSampleData
import com.example.data.model.AppDataState
import com.example.data.network.AuthSession
import com.example.data.network.SubscriptionAccessResult
import com.example.ui.theme.ThemeMode
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class InstagramStorage(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val adapter = moshi.adapter(AppDataState::class.java)

    fun loadState(): AppDataState {
        val raw = prefs.getString(KEY_STATE, null)
        val seedVersion = prefs.getInt(KEY_DEMO_VERSION, 1)
        if (raw == null) {
            val fresh = DefaultSampleData.createInitialState()
            prefs.edit().putInt(KEY_DEMO_VERSION, CURRENT_DEMO_VERSION).apply()
            return fresh
        }

        return try {
            val parsed = adapter.fromJson(raw)
            if (parsed != null && parsed.posts.isNotEmpty()) {
                if (seedVersion < CURRENT_DEMO_VERSION) {
                    val migrated = migrateToDemoV2(parsed)
                    saveState(migrated)
                    prefs.edit().putInt(KEY_DEMO_VERSION, CURRENT_DEMO_VERSION).apply()
                    migrated
                } else {
                    parsed
                }
            } else {
                val fresh = DefaultSampleData.createInitialState()
                prefs.edit().putInt(KEY_DEMO_VERSION, CURRENT_DEMO_VERSION).apply()
                fresh
            }
        } catch (_: Exception) {
            val fresh = DefaultSampleData.createInitialState()
            prefs.edit().putInt(KEY_DEMO_VERSION, CURRENT_DEMO_VERSION).apply()
            fresh
        }
    }

    /**
     * Upgrades the demo feed/reels/stories playlist to include the bundled demo videos and avatars
     * without discarding user-fetched Apify profiles or customized profile/insights edits.
     */
    private fun migrateToDemoV2(oldState: AppDataState): AppDataState {
        val fresh = DefaultSampleData.createInitialState()
        val preservedOwnPosts = oldState.posts.filter { it.isOwnPost }
        val newDemoFeedPosts = fresh.posts.filter { !it.isOwnPost }

        val updatedProfile = if (oldState.lastFetchedUsername == null && oldState.profile.username == "themovietimez") {
            oldState.profile.copy(
                avatarDrawableKey = "themovietimez",
                bio = fresh.profile.bio,
            )
        } else {
            oldState.profile
        }

        val ownPostsToKeep = if (oldState.lastFetchedUsername != null) {
            preservedOwnPosts
        } else {
            // Merge preserved insights on matching own post IDs while keeping new showInFeed metadata
            val freshOwnById = fresh.posts.filter { it.isOwnPost }.associateBy { it.id }
            val oldOwnById = preservedOwnPosts.associateBy { it.id }
            val merged = fresh.posts.filter { it.isOwnPost }.map { freshOwn ->
                val old = oldOwnById[freshOwn.id]
                if (old != null) {
                    freshOwn.copy(
                        likesCount = old.likesCount,
                        commentsCount = old.commentsCount,
                        repostsCount = old.repostsCount,
                        sharesCount = old.sharesCount,
                        savesCount = old.savesCount,
                        isLiked = old.isLiked,
                        isSaved = old.isSaved,
                        isReposted = old.isReposted,
                        viewsDisplay = old.viewsDisplay,
                        insights = old.insights,
                    )
                } else freshOwn
            }
            val extraOwn = preservedOwnPosts.filter { it.id !in freshOwnById }
            merged + extraOwn
        }

        return oldState.copy(
            profile = updatedProfile,
            stories = fresh.stories.map { story ->
                if (story.isOwnStory) story.copy(avatarUrl = updatedProfile.avatarUrl) else story
            },
            posts = newDemoFeedPosts + ownPostsToKeep,
        )
    }

    fun saveState(state: AppDataState) {
        try {
            val json = adapter.toJson(state)
            prefs.edit()
                .putString(KEY_STATE, json)
                .putInt(KEY_DEMO_VERSION, CURRENT_DEMO_VERSION)
                .apply()
        } catch (_: Exception) {
            // Ignore serialization errors gracefully
        }
    }

    fun clearLegacyApifyToken() {
        if (prefs.contains(KEY_APIFY_TOKEN)) {
            prefs.edit().remove(KEY_APIFY_TOKEN).apply()
        }
    }

    fun clearState(): AppDataState {
        prefs.edit().remove(KEY_STATE).apply()
        return DefaultSampleData.createInitialState()
    }

    fun saveAuthSession(session: AuthSession) {
        prefs.edit()
            .putString(KEY_AUTH_USER_ID, session.userId)
            .putString(KEY_AUTH_EMAIL, session.email)
            .putString(KEY_AUTH_USERNAME, session.username)
            .putString(KEY_AUTH_ACCESS_TOKEN, session.accessToken)
            .putString(KEY_AUTH_REFRESH_TOKEN, session.refreshToken)
            .putLong(KEY_AUTH_EXPIRES_AT, session.expiresAt)
            .apply()
    }

    fun loadAuthSession(): AuthSession? {
        val token = prefs.getString(KEY_AUTH_ACCESS_TOKEN, null) ?: return null
        val userId = prefs.getString(KEY_AUTH_USER_ID, null) ?: return null
        val email = prefs.getString(KEY_AUTH_EMAIL, "") ?: ""
        val username = prefs.getString(KEY_AUTH_USERNAME, null)
        val refreshToken = prefs.getString(KEY_AUTH_REFRESH_TOKEN, "") ?: ""
        val expiresAt = prefs.getLong(KEY_AUTH_EXPIRES_AT, 0L)

        return AuthSession(
            userId = userId,
            email = email,
            username = username,
            accessToken = token,
            refreshToken = refreshToken,
            expiresAt = expiresAt,
        )
    }

    fun clearAuthSession() {
        prefs.edit()
            .remove(KEY_AUTH_USER_ID)
            .remove(KEY_AUTH_EMAIL)
            .remove(KEY_AUTH_USERNAME)
            .remove(KEY_AUTH_ACCESS_TOKEN)
            .remove(KEY_AUTH_REFRESH_TOKEN)
            .remove(KEY_AUTH_EXPIRES_AT)
            .remove(KEY_SUB_IS_ALLOWED)
            .remove(KEY_SUB_STATUS)
            .remove(KEY_SUB_EXPIRY)
            .remove(KEY_SUB_PRODUCT)
            .apply()
    }

    fun saveSubscriptionAccess(access: SubscriptionAccessResult) {
        prefs.edit()
            .putBoolean(KEY_SUB_IS_ALLOWED, access.isAllowed)
            .putString(KEY_SUB_STATUS, access.subscriptionStatus)
            .putString(KEY_SUB_EXPIRY, access.expiryDate)
            .putString(KEY_SUB_PRODUCT, access.productName)
            .apply()
    }

    fun loadSubscriptionAccess(): SubscriptionAccessResult? {
        if (!prefs.contains(KEY_SUB_IS_ALLOWED)) return null
        return SubscriptionAccessResult(
            isAllowed = prefs.getBoolean(KEY_SUB_IS_ALLOWED, false),
            accountStatus = "active",
            subscriptionStatus = prefs.getString(KEY_SUB_STATUS, null),
            expiryDate = prefs.getString(KEY_SUB_EXPIRY, null),
            productName = prefs.getString(KEY_SUB_PRODUCT, null),
            message = "Cached access status",
        )
    }

    fun loadThemeMode(): ThemeMode {
        val raw = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        return runCatching { ThemeMode.valueOf(raw ?: ThemeMode.SYSTEM.name) }
            .getOrDefault(ThemeMode.SYSTEM)
    }

    fun saveThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    companion object {
        private const val KEY_THEME_MODE = "app_theme_mode"
        private const val PREFS_NAME = "instagram_clone_prefs_v1"
        private const val KEY_STATE = "app_data_state_json"
        private const val KEY_APIFY_TOKEN = "saved_apify_token"
        private const val KEY_DEMO_VERSION = "demo_seed_version"
        private const val CURRENT_DEMO_VERSION = 2

        private const val KEY_AUTH_USER_ID = "auth_user_id"
        private const val KEY_AUTH_EMAIL = "auth_email"
        private const val KEY_AUTH_USERNAME = "auth_username"
        private const val KEY_AUTH_ACCESS_TOKEN = "auth_access_token"
        private const val KEY_AUTH_REFRESH_TOKEN = "auth_refresh_token"
        private const val KEY_AUTH_EXPIRES_AT = "auth_expires_at"

        private const val KEY_SUB_IS_ALLOWED = "sub_is_allowed"
        private const val KEY_SUB_STATUS = "sub_status"
        private const val KEY_SUB_EXPIRY = "sub_expiry"
        private const val KEY_SUB_PRODUCT = "sub_product"
    }
}
