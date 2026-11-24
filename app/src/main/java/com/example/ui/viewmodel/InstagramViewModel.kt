package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DefaultSampleData
import com.example.data.local.InstagramStorage
import com.example.data.model.AccountInsightsData
import com.example.data.model.AppDataState
import com.example.data.model.ChatMessage
import com.example.data.model.CommentItem
import com.example.data.model.DemographicItem
import com.example.data.model.DmThread
import com.example.data.model.ImpactMetric
import com.example.data.model.MediaPost
import com.example.data.model.ProfileData
import com.example.data.model.ReelInsights
import com.example.data.model.SourceShare
import com.example.data.network.ApifyRepository
import com.example.data.network.AuthSession
import com.example.data.network.SubscriptionAccessResult
import com.example.data.network.SupabaseAuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class MainTab {
    HOME,
    REELS,
    MESSAGES,
    EXPLORE,
    PROFILE,
}

sealed class OverlayRoute {
    data class PostDetail(
        val postId: String,
        val feedPostIds: List<String>? = null,
    ) : OverlayRoute()
    data class ReelInsightsScreen(val postId: String) : OverlayRoute()
    data class ChatConversation(val threadId: String) : OverlayRoute()
    data class StoryViewer(val storyId: String) : OverlayRoute()
    data class CreatorProfile(val username: String) : OverlayRoute()
    data object ProfessionalDashboard : OverlayRoute()
    data class AccountInsights(val initialTab: Int = 0) : OverlayRoute()
}

sealed class FetchAccountUiState {
    data object Idle : FetchAccountUiState()
    data object Loading : FetchAccountUiState()
    data class Success(val message: String) : FetchAccountUiState()
    data class Error(val message: String) : FetchAccountUiState()
}

enum class AuthScreenType {
    LOGIN,
    REGISTER,
    FORGOT_PASSWORD,
}

sealed class AuthUiState {
    data object CheckingSession : AuthUiState()
    data class Unauthenticated(
        val screen: AuthScreenType = AuthScreenType.LOGIN,
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val infoMessage: String? = null,
    ) : AuthUiState()
    data class AccessGated(
        val accessResult: SubscriptionAccessResult,
    ) : AuthUiState()
    data class Authenticated(
        val session: AuthSession,
    ) : AuthUiState()
}

class InstagramViewModel(application: Application) : AndroidViewModel(application) {

    private val storage = InstagramStorage(application.applicationContext)
    private val apifyRepository = ApifyRepository()
    private val supabaseAuthRepository = SupabaseAuthRepository()

    private val _authUiState = MutableStateFlow<AuthUiState>(AuthUiState.CheckingSession)
    val authUiState: StateFlow<AuthUiState> = _authUiState.asStateFlow()

    private val _appState = MutableStateFlow(storage.loadState())
    val appState: StateFlow<AppDataState> = _appState.asStateFlow()

    private val _activeTab = MutableStateFlow(MainTab.HOME)
    val activeTab: StateFlow<MainTab> = _activeTab.asStateFlow()

    private val _backStack = MutableStateFlow<List<OverlayRoute>>(emptyList())
    val backStack: StateFlow<List<OverlayRoute>> = _backStack.asStateFlow()

    private val _showFetchModal = MutableStateFlow(false)
    val showFetchModal: StateFlow<Boolean> = _showFetchModal.asStateFlow()

    private val _fetchUiState = MutableStateFlow<FetchAccountUiState>(FetchAccountUiState.Idle)
    val fetchUiState: StateFlow<FetchAccountUiState> = _fetchUiState.asStateFlow()

    init {
        storage.clearLegacyApifyToken()
        checkInitialSession()
    }

    fun checkInitialSession() {
        viewModelScope.launch {
            _authUiState.value = AuthUiState.CheckingSession
            val cachedSession = storage.loadAuthSession()
            if (cachedSession == null) {
                _authUiState.value = AuthUiState.Unauthenticated()
                return@launch
            }

            // Verify subscription access
            val verifyResult = supabaseAuthRepository.verifyUserAccess(
                userId = cachedSession.userId,
                accessToken = cachedSession.accessToken,
            )

            if (verifyResult.isSuccess) {
                val access = verifyResult.getOrThrow()
                storage.saveSubscriptionAccess(access)
                if (access.isAllowed) {
                    _authUiState.value = AuthUiState.Authenticated(cachedSession)
                } else {
                    _authUiState.value = AuthUiState.AccessGated(access)
                }
            } else {
                val cachedAccess = storage.loadSubscriptionAccess()
                if (cachedAccess != null && cachedAccess.isAllowed) {
                    _authUiState.value = AuthUiState.Authenticated(cachedSession)
                } else {
                    _authUiState.value = AuthUiState.Unauthenticated(
                        errorMessage = "Session expired. Please log in again."
                    )
                }
            }
        }
    }

    fun setAuthScreen(screen: AuthScreenType) {
        _authUiState.value = AuthUiState.Unauthenticated(screen = screen)
    }

    fun login(identifier: String, pass: String) {
        val currentScreen = (_authUiState.value as? AuthUiState.Unauthenticated)?.screen ?: AuthScreenType.LOGIN
        _authUiState.value = AuthUiState.Unauthenticated(screen = currentScreen, isLoading = true)

        viewModelScope.launch {
            val result = supabaseAuthRepository.login(identifier, pass)
            if (result.isFailure) {
                val err = result.exceptionOrNull()?.message ?: "Login failed. Please check credentials."
                _authUiState.value = AuthUiState.Unauthenticated(screen = currentScreen, isLoading = false, errorMessage = err)
                return@launch
            }

            val session = result.getOrThrow()
            storage.saveAuthSession(session)

            if (!session.username.isNullOrBlank()) {
                _appState.update { current ->
                    current.copy(profile = current.profile.copy(username = session.username))
                }
            }

            val verifyResult = supabaseAuthRepository.verifyUserAccess(session.userId, session.accessToken)
            if (verifyResult.isSuccess) {
                val access = verifyResult.getOrThrow()
                storage.saveSubscriptionAccess(access)
                if (access.isAllowed) {
                    _authUiState.value = AuthUiState.Authenticated(session)
                } else {
                    _authUiState.value = AuthUiState.AccessGated(access)
                }
            } else {
                _authUiState.value = AuthUiState.Authenticated(session)
            }
        }
    }

    fun register(email: String, username: String, pass: String) {
        val currentScreen = AuthScreenType.REGISTER
        _authUiState.value = AuthUiState.Unauthenticated(screen = currentScreen, isLoading = true)

        viewModelScope.launch {
            val result = supabaseAuthRepository.register(email, username, pass)
            if (result.isFailure) {
                val err = result.exceptionOrNull()?.message ?: "Registration failed."
                _authUiState.value = AuthUiState.Unauthenticated(screen = currentScreen, isLoading = false, errorMessage = err)
                return@launch
            }

            val session = result.getOrThrow()
            if (session.accessToken.isNotBlank()) {
                storage.saveAuthSession(session)
                _authUiState.value = AuthUiState.AccessGated(
                    SubscriptionAccessResult(
                        isAllowed = false,
                        accountStatus = "active",
                        subscriptionStatus = "pending",
                        expiryDate = null,
                        productName = "Insight Editor",
                        message = "Account created! Please activate a subscription plan via Discord (/purchase) to access the application."
                    )
                )
            } else {
                _authUiState.value = AuthUiState.Unauthenticated(
                    screen = AuthScreenType.LOGIN,
                    infoMessage = "Account registered! Please check your email to verify your address, then log in."
                )
            }
        }
    }

    fun forgotPassword(email: String) {
        val currentScreen = AuthScreenType.FORGOT_PASSWORD
        _authUiState.value = AuthUiState.Unauthenticated(screen = currentScreen, isLoading = true)

        viewModelScope.launch {
            val result = supabaseAuthRepository.sendPasswordReset(email)
            if (result.isSuccess) {
                _authUiState.value = AuthUiState.Unauthenticated(
                    screen = currentScreen,
                    isLoading = false,
                    infoMessage = "Password reset link sent to $email. Please check your inbox."
                )
            } else {
                val err = result.exceptionOrNull()?.message ?: "Could not send reset email."
                _authUiState.value = AuthUiState.Unauthenticated(
                    screen = currentScreen,
                    isLoading = false,
                    errorMessage = err
                )
            }
        }
    }

    fun logout() {
        storage.clearAuthSession()
        _authUiState.value = AuthUiState.Unauthenticated(screen = AuthScreenType.LOGIN)
    }

    fun continueGuestMode() {
        _authUiState.value = AuthUiState.Authenticated(
            AuthSession(
                userId = "guest_demo",
                email = "demo@instagram.local",
                username = "themovietimez",
                accessToken = "",
                refreshToken = "",
                expiresAt = Long.MAX_VALUE
            )
        )
    }

    private val _activeCommentsPostId = MutableStateFlow<String?>(null)
    val activeCommentsPostId: StateFlow<String?> = _activeCommentsPostId.asStateFlow()

    private val _showEditProfileSheet = MutableStateFlow(false)
    val showEditProfileSheet: StateFlow<Boolean> = _showEditProfileSheet.asStateFlow()

    private val _showNoteEditor = MutableStateFlow(false)
    val showNoteEditor: StateFlow<Boolean> = _showNoteEditor.asStateFlow()

    private val _isInsightsEditMode = MutableStateFlow(false)
    val isInsightsEditMode: StateFlow<Boolean> = _isInsightsEditMode.asStateFlow()

    // Triple-tap gesture timestamps for the bottom navigation Profile icon
    private val profileTapTimestamps = ArrayDeque<Long>(4)

    fun selectTab(tab: MainTab) {
        _activeTab.value = tab
        _backStack.value = emptyList()
        _isInsightsEditMode.value = false
    }

    fun onProfileNavTap() {
        val now = System.currentTimeMillis()
        profileTapTimestamps.addLast(now)
        while (profileTapTimestamps.size > 3) {
            profileTapTimestamps.removeFirst()
        }
        if (profileTapTimestamps.size == 3 && (now - profileTapTimestamps.first()) <= 900L) {
            profileTapTimestamps.clear()
            _fetchUiState.value = FetchAccountUiState.Idle
            _showFetchModal.value = true
            return
        }
        selectTab(MainTab.PROFILE)
    }

    fun openFetchModal() {
        profileTapTimestamps.clear()
        _fetchUiState.value = FetchAccountUiState.Idle
        _showFetchModal.value = true
    }

    fun dismissFetchModal() {
        _showFetchModal.value = false
        _fetchUiState.value = FetchAccountUiState.Idle
    }

    fun openPostDetail(postId: String, feedPostIds: List<String>? = null) {
        _backStack.update { it + OverlayRoute.PostDetail(postId, feedPostIds) }
    }

    fun openReelInsights(postId: String) {
        _isInsightsEditMode.value = false
        _backStack.update { it + OverlayRoute.ReelInsightsScreen(postId) }
    }

    fun openProfessionalDashboard() {
        _backStack.update { it + OverlayRoute.ProfessionalDashboard }
    }

    fun openAccountInsights(initialTab: Int = 0) {
        _backStack.update { it + OverlayRoute.AccountInsights(initialTab) }
    }

    fun openChat(threadId: String) {
        _backStack.update { it + OverlayRoute.ChatConversation(threadId) }
    }

    fun openStory(storyId: String) {
        _backStack.update { it + OverlayRoute.StoryViewer(storyId) }
    }

    fun openCreatorProfile(username: String) {
        val clean = username.trim().removePrefix("@")
        if (clean.isEmpty()) return
        if (clean.equals(_appState.value.profile.username, ignoreCase = true)) {
            selectTab(MainTab.PROFILE)
        } else {
            _backStack.update { it + OverlayRoute.CreatorProfile(clean) }
        }
    }

    fun openDmWithCreator(username: String) {
        val clean = username.trim().removePrefix("@")
        val existing = _appState.value.dmThreads.find {
            it.username.equals(clean, ignoreCase = true)
        }
        val threadId = if (existing != null) {
            existing.id
        } else {
            val creator = DefaultSampleData.getCreatorProfile(clean, _appState.value.profile)
            val newId = "dm_creator_${clean.lowercase()}"
            val newThread = DmThread(
                id = newId,
                username = creator.username,
                displayName = creator.displayName,
                avatarUrl = creator.avatarUrl,
                avatarKey = creator.avatarDrawableKey,
                isVerifiedBlue = creator.isVerified,
                lastMessagePreview = "Tap to message",
                category = "Primary",
                messages = emptyList(),
            )
            updateAndPersist { state ->
                state.copy(dmThreads = listOf(newThread) + state.dmThreads)
            }
            newId
        }
        _activeTab.value = MainTab.MESSAGES
        _backStack.value = listOf(OverlayRoute.ChatConversation(threadId))
    }

    fun popBackStack(): Boolean {
        val current = _backStack.value
        if (current.isEmpty()) return false
        _isInsightsEditMode.value = false
        _backStack.value = current.dropLast(1)
        return true
    }

    fun openComments(postId: String) {
        _activeCommentsPostId.value = postId
    }

    fun closeComments() {
        _activeCommentsPostId.value = null
    }

    fun openEditProfileSheet() {
        _showEditProfileSheet.value = true
    }

    fun closeEditProfileSheet() {
        _showEditProfileSheet.value = false
    }

    fun openNoteEditor() {
        _showNoteEditor.value = true
    }

    fun closeNoteEditor() {
        _showNoteEditor.value = false
    }

    fun toggleInsightsEditMode() {
        _isInsightsEditMode.update { !it }
    }

    fun setInsightsEditMode(enabled: Boolean) {
        _isInsightsEditMode.value = enabled
    }

    private fun updateAndPersist(transform: (AppDataState) -> AppDataState) {
        _appState.update { current ->
            val updated = transform(current)
            storage.saveState(updated)
            updated
        }
    }

    fun toggleLike(postId: String) {
        updateAndPersist { state ->
            state.copy(
                posts = state.posts.map { post ->
                    if (post.id == postId) {
                        val nextLiked = !post.isLiked
                        val nextLikes = if (nextLiked) post.likesCount + 1 else (post.likesCount - 1).coerceAtLeast(0)
                        post.copy(
                            isLiked = nextLiked,
                            likesCount = nextLikes,
                            insights = post.insights.copy(likes = nextLikes.toString())
                        )
                    } else post
                }
            )
        }
    }

    fun likePostIfUnliked(postId: String) {
        val target = _appState.value.posts.find { it.id == postId } ?: return
        if (!target.isLiked) {
            toggleLike(postId)
        }
    }

    fun toggleSave(postId: String) {
        updateAndPersist { state ->
            state.copy(
                posts = state.posts.map { post ->
                    if (post.id == postId) {
                        val nextSaved = !post.isSaved
                        val nextSaves = if (nextSaved) post.savesCount + 1 else (post.savesCount - 1).coerceAtLeast(0)
                        post.copy(
                            isSaved = nextSaved,
                            savesCount = nextSaves,
                            insights = post.insights.copy(saves = nextSaves.toString())
                        )
                    } else post
                }
            )
        }
    }

    fun toggleRepost(postId: String) {
        updateAndPersist { state ->
            state.copy(
                posts = state.posts.map { post ->
                    if (post.id == postId) {
                        val nextReposted = !post.isReposted
                        val nextReposts = if (nextReposted) post.repostsCount + 1 else (post.repostsCount - 1).coerceAtLeast(0)
                        post.copy(
                            isReposted = nextReposted,
                            repostsCount = nextReposts,
                            insights = post.insights.copy(reposts = nextReposts.toString())
                        )
                    } else post
                }
            )
        }
    }

    fun toggleFollowAuthor(username: String) {
        updateAndPersist { state ->
            state.copy(
                posts = state.posts.map { post ->
                    if (post.authorUsername == username) {
                        post.copy(isFollowing = !post.isFollowing)
                    } else post
                }
            )
        }
    }

    fun addComment(postId: String, text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        updateAndPersist { state ->
            val myUsername = state.profile.username
            state.copy(
                posts = state.posts.map { post ->
                    if (post.id == postId) {
                        val newComment = CommentItem(
                            id = UUID.randomUUID().toString(),
                            username = myUsername,
                            text = clean,
                            timeAgo = "Just now",
                            likes = 0,
                        )
                        val nextCount = post.commentsCount + 1
                        post.copy(
                            commentsCount = nextCount,
                            commentsList = listOf(newComment) + post.commentsList,
                            insights = post.insights.copy(comments = nextCount.toString()),
                        )
                    } else post
                }
            )
        }
    }

    fun updateProfile(transform: (ProfileData) -> ProfileData) {
        updateAndPersist { state ->
            val nextProfile = transform(state.profile)
            state.copy(
                profile = nextProfile,
                stories = state.stories.map { story ->
                    if (story.isOwnStory) story.copy(avatarUrl = nextProfile.avatarUrl) else story
                },
                dmThreads = state.dmThreads.map { dm ->
                    if (dm.id == "dm_self") {
                        dm.copy(
                            username = nextProfile.username,
                            displayName = nextProfile.username,
                            avatarUrl = nextProfile.avatarUrl,
                        )
                    } else dm
                }
            )
        }
    }

    fun updateAccountInsights(updated: AccountInsightsData) {
        updateAndPersist { state ->
            state.copy(
                accountInsights = updated,
                profile = state.profile.copy(
                    dashboardSubtitle = "${updated.dashboardViews} views in the last ${updated.timeframeLabel}.",
                    followersCount = updated.audienceFollowersTotal.ifBlank { state.profile.followersCount },
                ),
            )
        }
    }

    fun updatePostDetails(postId: String, audioTitle: String, likedByUsername: String) {
        val cleanAudio = audioTitle.trim()
        val cleanLiker = likedByUsername.trim().removePrefix("@")
        updateAndPersist { state ->
            state.copy(
                posts = state.posts.map { post ->
                    if (post.id == postId) {
                        post.copy(
                            subtitle = cleanAudio.ifBlank { "${if (post.isOwnPost) state.profile.username else post.authorUsername} · Original audio" },
                            likedByText = if (cleanLiker.isBlank()) "" else "Liked by $cleanLiker",
                        )
                    } else post
                }
            )
        }
    }

    fun updatePostInsights(postId: String, transform: (ReelInsights) -> ReelInsights) {
        updateAndPersist { state ->
            state.copy(
                posts = state.posts.map { post ->
                    if (post.id == postId) {
                        val updatedInsights = transform(post.insights)
                        val parsedLikes = updatedInsights.likes.replace(",", "").toIntOrNull()
                        val parsedComments = updatedInsights.comments.replace(",", "").toIntOrNull()
                        val parsedReposts = updatedInsights.reposts.replace(",", "").toIntOrNull()
                        val parsedShares = updatedInsights.shares.replace(",", "").toIntOrNull()
                        val parsedSaves = updatedInsights.saves.replace(",", "").toIntOrNull()
                        val viewsFormatted = compactViewsFromRaw(updatedInsights.views)
                        post.copy(
                            viewsDisplay = viewsFormatted ?: post.viewsDisplay,
                            likesCount = parsedLikes ?: post.likesCount,
                            commentsCount = parsedComments ?: post.commentsCount,
                            repostsCount = parsedReposts ?: post.repostsCount,
                            sharesCount = parsedShares ?: post.sharesCount,
                            savesCount = parsedSaves ?: post.savesCount,
                            durationText = updatedInsights.durationLabel,
                            insights = updatedInsights,
                        )
                    } else post
                }
            )
        }
    }

    fun updateImpactMetric(postId: String, impactId: String, percentage: String? = null, status: String? = null) {
        updatePostInsights(postId) { insights ->
            insights.copy(
                impacts = insights.impacts.map { metric: ImpactMetric ->
                    if (metric.id == impactId) {
                        metric.copy(
                            percentage = percentage ?: metric.percentage,
                            status = status ?: metric.status,
                        )
                    } else metric
                }
            )
        }
    }

    fun updateTopSource(postId: String, index: Int, label: String? = null, percentage: Double? = null) {
        updatePostInsights(postId) { insights ->
            insights.copy(
                topSources = insights.topSources.mapIndexed { i, item: SourceShare ->
                    if (i == index) {
                        item.copy(
                            label = label ?: item.label,
                            percentage = (percentage ?: item.percentage).coerceIn(0.0, 100.0),
                        )
                    } else item
                }
            )
        }
    }

    fun updateDemographicItem(
        postId: String,
        category: String, // "Age", "Country", "Gender"
        index: Int,
        label: String? = null,
        percentage: Double? = null,
    ) {
        updatePostInsights(postId) { insights ->
            fun List<DemographicItem>.patchAt(): List<DemographicItem> = mapIndexed { i, item ->
                if (i == index) {
                    item.copy(
                        label = label ?: item.label,
                        percentage = (percentage ?: item.percentage).coerceIn(0.0, 100.0),
                    )
                } else item
            }

            when (category) {
                "Age" -> insights.copy(ageBreakdown = insights.ageBreakdown.patchAt())
                "Country" -> insights.copy(countryBreakdown = insights.countryBreakdown.patchAt())
                else -> insights.copy(genderBreakdown = insights.genderBreakdown.patchAt())
            }
        }
    }

    fun updateLikeCurvePoint(postId: String, index: Int, value: Float) {
        updatePostInsights(postId) { insights ->
            val updated = insights.likeCurvePoints.toMutableList()
            if (index in updated.indices) {
                updated[index] = value.coerceIn(0f, 30f)
            }
            insights.copy(likeCurvePoints = updated)
        }
    }

    fun sendDmMessage(threadId: String, text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        val nowTime = SimpleDateFormat("h:mm a", Locale.US).format(Date())
        updateAndPersist { state ->
            state.copy(
                dmThreads = state.dmThreads.map { thread ->
                    if (thread.id == threadId) {
                        val userMsg = ChatMessage(
                            id = UUID.randomUUID().toString(),
                            senderIsMe = true,
                            text = clean,
                            timestamp = nowTime,
                        )
                        val autoReply = if (thread.isMetaAi) {
                            listOf(
                                ChatMessage(
                                    id = UUID.randomUUID().toString(),
                                    senderIsMe = false,
                                    text = buildMetaAiReply(clean),
                                    timestamp = nowTime,
                                )
                            )
                        } else emptyList()

                        thread.copy(
                            lastMessagePreview = if (thread.isMetaAi) "Active now" else "Sent",
                            messages = thread.messages + userMsg + autoReply,
                        )
                    } else thread
                }
            )
        }
    }

    fun resetFetchState() {
        if (_fetchUiState.value !is FetchAccountUiState.Loading) {
            _fetchUiState.value = FetchAccountUiState.Idle
        }
    }

    fun fetchAccountFromApify(usernameOrUrl: String) {
        val normalizedHandle = ApifyRepository.extractUsernameFromTarget(usernameOrUrl)
        if (normalizedHandle.isBlank()) {
            _fetchUiState.value = FetchAccountUiState.Error("Please enter a valid Instagram username.")
            return
        }
        _fetchUiState.value = FetchAccountUiState.Loading
        viewModelScope.launch {
            val result = apifyRepository.fetchInstagramProfile(usernameOrUrl)
            result.fold(
                onSuccess = { fetched ->
                    val dedupedPosts = fetched.posts.distinctBy {
                        it.shortCode.ifBlank { it.postUrl.ifBlank { it.id } }
                    }
                    if (fetched.isSinglePostTarget && dedupedPosts.isNotEmpty()) {
                        val mappedPost = dedupedPosts.first()
                        mapFetchedPostMetricsToApp(mappedPost, fetched.profile)
                        _showFetchModal.value = false
                        _fetchUiState.value = FetchAccountUiState.Success(
                            "Loaded @${mappedPost.authorUsername}"
                        )
                    } else {
                        applyFetchedAccount(fetched.profile, dedupedPosts)
                        _showFetchModal.value = false
                        _fetchUiState.value = FetchAccountUiState.Success(
                            "Loaded @${fetched.profile.username}"
                        )
                    }
                },
                onFailure = { err ->
                    _fetchUiState.value = FetchAccountUiState.Error(
                        err.message ?: "Unable to load Instagram account."
                    )
                }
            )
        }
    }

    fun importApifyJson(rawJson: String, fallbackUsername: String) {
        _fetchUiState.value = FetchAccountUiState.Loading
        val result = apifyRepository.parseApifyResponseJson(rawJson, fallbackUsername.ifBlank { "instagram" })
        result.fold(
            onSuccess = { fetched ->
                val dedupedPosts = fetched.posts.distinctBy {
                    it.shortCode.ifBlank { it.postUrl.ifBlank { it.id } }
                }
                applyFetchedAccount(fetched.profile, dedupedPosts)
                _fetchUiState.value = FetchAccountUiState.Success(
                    "Imported @${fetched.profile.username} (${dedupedPosts.size} posts)"
                )
            },
            onFailure = { err ->
                _fetchUiState.value = FetchAccountUiState.Error(
                    err.message ?: "Invalid Apify JSON."
                )
            }
        )
    }

    private fun mapFetchedPostMetricsToApp(fetchedPost: MediaPost, fetchedProfile: ProfileData) {
        updateAndPersist { current ->
            val targetKey = fetchedPost.shortCode.ifBlank { fetchedPost.id }
            val existingIdx = current.posts.indexOfFirst {
                it.id == fetchedPost.id ||
                    (fetchedPost.shortCode.isNotBlank() && it.shortCode == fetchedPost.shortCode) ||
                    (fetchedPost.caption.isNotBlank() && fetchedPost.caption != "N/A" &&
                        it.caption.equals(fetchedPost.caption, ignoreCase = true))
            }
            val updatedPosts = if (existingIdx >= 0) {
                current.posts.mapIndexed { index, existing ->
                    if (index == existingIdx) {
                        existing.copy(
                            shortCode = fetchedPost.shortCode.ifBlank { existing.shortCode },
                            postUrl = fetchedPost.postUrl.ifBlank { existing.postUrl },
                            mediaUrl = fetchedPost.mediaUrl ?: existing.mediaUrl,
                            videoUrl = fetchedPost.videoUrl ?: existing.videoUrl,
                            caption = fetchedPost.caption,
                            subtitle = fetchedPost.subtitle,
                            dateLabel = fetchedPost.dateLabel,
                            durationText = fetchedPost.durationText,
                            viewsDisplay = fetchedPost.viewsDisplay,
                            likesCount = fetchedPost.likesCount,
                            commentsCount = fetchedPost.commentsCount,
                            repostsCount = fetchedPost.repostsCount,
                            sharesCount = fetchedPost.sharesCount,
                            savesCount = fetchedPost.savesCount,
                            likesUnavailable = fetchedPost.likesUnavailable,
                            commentsUnavailable = fetchedPost.commentsUnavailable,
                            repostsUnavailable = fetchedPost.repostsUnavailable,
                            sharesUnavailable = fetchedPost.sharesUnavailable,
                            savesUnavailable = fetchedPost.savesUnavailable,
                            insights = fetchedPost.insights,
                        )
                    } else existing
                }
            } else {
                listOf(fetchedPost.copy(isOwnPost = true, showInFeed = true)) + current.posts
            }.distinctBy { post ->
                if (post.isOwnPost) post.shortCode.ifBlank { post.id } else post.id
            }.also { list ->
                if (targetKey.isBlank()) list
            }
            current.copy(
                profile = if (current.profile.username.equals(fetchedProfile.username, ignoreCase = true)) {
                    current.profile.copy(
                        avatarUrl = fetchedProfile.avatarUrl ?: current.profile.avatarUrl,
                        isVerified = fetchedProfile.isVerified || current.profile.isVerified,
                    )
                } else current.profile,
                posts = updatedPosts,
            )
        }
    }

    internal fun applyFetchedAccount(profile: ProfileData, fetchedPosts: List<MediaPost>) {
        val dedupedFetched = fetchedPosts.distinctBy {
            it.shortCode.ifBlank { it.postUrl.ifBlank { it.id } }
        }
        val fetchedKeys = dedupedFetched.map { it.shortCode.ifBlank { it.id } }.toSet()
        updateAndPersist { current ->
            // Keep non-own explore/reels posts so Home/Reels/Explore stay rich,
            // while replacing own profile posts with deduplicated fetched posts!
            val externalPosts = current.posts.filter { !it.isOwnPost && it.id !in fetchedKeys }
            val mergedPosts = dedupedFetched + externalPosts
            current.copy(
                profile = profile,
                posts = mergedPosts,
                stories = current.stories.map { story ->
                    if (story.isOwnStory) story.copy(avatarUrl = profile.avatarUrl) else story
                },
                dmThreads = current.dmThreads.map { dm ->
                    if (dm.id == "dm_self") {
                        dm.copy(
                            username = profile.username,
                            displayName = profile.username,
                            avatarUrl = profile.avatarUrl,
                        )
                    } else dm
                },
                lastFetchedUsername = profile.username,
            )
        }
        selectTab(MainTab.PROFILE)
    }

    fun resetToDefaultProfile() {
        val fresh = DefaultSampleData.createInitialState()
        storage.saveState(fresh)
        _appState.value = fresh
        _fetchUiState.value = FetchAccountUiState.Success("Reset to default @${fresh.profile.username} account")
    }

    private fun buildMetaAiReply(prompt: String): String {
        val lower = prompt.lowercase()
        return when {
            "hook" in lower || "caption" in lower ->
                "Try an emotional 1-line hook in the first 1.5s, like: \"Wait until you realize what he whispered here... 👀\""
            "insight" in lower || "view" in lower || "reach" in lower ->
                "To lower skip rate below 18%, tighten your opening cut to 0.8s and add high-contrast centered subtitles."
            else ->
                "Got it! For cinema reels like yours, 40–55s dramatic dialogue clips with crisp top captions drive the highest non-follower shares."
        }
    }

    private fun compactViewsFromRaw(raw: String): String? {
        val num = raw.replace(",", "").trim().toLongOrNull() ?: return null
        return ApifyRepository.formatCompactCount(num)
    }
}
