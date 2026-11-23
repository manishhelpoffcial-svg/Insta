package com.example.data.local

import android.content.Context
import com.example.data.DefaultSampleData
import com.example.data.model.AppDataState
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

    companion object {
        private const val PREFS_NAME = "instagram_clone_prefs_v1"
        private const val KEY_STATE = "app_data_state_json"
        private const val KEY_APIFY_TOKEN = "saved_apify_token"
        private const val KEY_DEMO_VERSION = "demo_seed_version"
        private const val CURRENT_DEMO_VERSION = 2
    }
}
