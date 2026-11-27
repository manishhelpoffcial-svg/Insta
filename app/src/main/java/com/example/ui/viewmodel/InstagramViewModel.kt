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
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    private val _themeMode = MutableStateFlow(storage.loadThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _isInsightsLoading = MutableStateFlow(false)
    val isInsightsLoading: StateFlow<Boolean> = _isInsightsLoading.asStateFlow()

    private val preparedInsightKeys = mutableSetOf<String>()

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        storage.saveThemeMode(mode)
    }

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

            val deviceId = storage.getOrCreateDeviceId()
            var activeSession = cachedSession

            // Refresh JWT if expired or about to expire so user never needs to log in again
            if (activeSession.refreshToken.isNotBlank() &&
                (activeSession.expiresAt <= System.currentTimeMillis() + 60_000L)
            ) {
                val refreshed = supabaseAuthRepository.refreshSession(activeSession.refreshToken)
                if (refreshed.isSuccess) {
                    activeSession = refreshed.getOrThrow()
                    storage.saveAuthSession(activeSession)
                }
            }

            var verifyResult = supabaseAuthRepository.verifyUserAccess(
                userId = activeSession.userId,
                accessToken = activeSession.accessToken,
            )

            // Retry once after token refresh if verification failed (e.g. 401 expired token)
            if (verifyResult.isFailure && activeSession.refreshToken.isNotBlank()) {
                val refreshed = supabaseAuthRepository.refreshSession(activeSession.refreshToken)
                if (refreshed.isSuccess) {
                    activeSession = refreshed.getOrThrow()
                    storage.saveAuthSession(activeSession)
                    verifyResult = supabaseAuthRepository.verifyUserAccess(
                        userId = activeSession.userId,
                        accessToken = activeSession.accessToken,
                    )
                }
            }

            val deviceCheck = supabaseAuthRepository.verifyAndLockDevice(
                userId = activeSession.userId,
                accessToken = activeSession.accessToken,
                deviceId = deviceId,
            )
            if (deviceCheck.isFailure) {
                storage.clearAuthSession()
                _authUiState.value = AuthUiState.Unauthenticated(
                    errorMessage = deviceCheck.exceptionOrNull()?.message
                        ?: "This account is locked to another device."
                )
                return@launch
            }

            if (verifyResult.isSuccess) {
                val access = verifyResult.getOrThrow()
                storage.saveSubscriptionAccess(access)
                if (access.isAllowed) {
                    _authUiState.value = AuthUiState.Authenticated(activeSession)
                } else {
                    _authUiState.value = AuthUiState.AccessGated(access)
                }
            } else {
                val cachedAccess = storage.loadSubscriptionAccess()
                if (cachedAccess != null && cachedAccess.isAllowed) {
                    _authUiState.value = AuthUiState.Authenticated(activeSession)
                } else if (cachedAccess != null && !cachedAccess.isAllowed) {
                    _authUiState.value = AuthUiState.AccessGated(cachedAccess)
                } else {
                    // Stay logged in if we have a valid persisted session until subscription check succeeds
                    _authUiState.value = AuthUiState.Authenticated(activeSession)
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
            val deviceId = storage.getOrCreateDeviceId()
            val deviceCheck = supabaseAuthRepository.verifyAndLockDevice(
                userId = session.userId,
                accessToken = session.accessToken,
                deviceId = deviceId,
            )
            if (deviceCheck.isFailure) {
                val lockErr = deviceCheck.exceptionOrNull()?.message
                    ?: "This account is locked to another device."
                _authUiState.value = AuthUiState.Unauthenticated(
                    screen = currentScreen,
                    isLoading = false,
                    errorMessage = lockErr,
                )
                return@launch
            }

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
                supabaseAuthRepository.verifyAndLockDevice(
                    userId = session.userId,
                    accessToken = session.accessToken,
                    deviceId = storage.getOrCreateDeviceId(),
                )
                storage.saveAuthSession(session)
                _authUiState.value = AuthUiState.AccessGated(
                    SubscriptionAccessResult(
                        isAllowed = false,
                        accountStatus = "active",
                        subscriptionStatus = "pending",
                        expiryDate = null,
                        productName = "Insight Editor",
                        message = "You don't have active subscription"
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
        // Guest mode disabled: Authorized database authentication required
        _authUiState.value = AuthUiState.Unauthenticated(
            errorMessage = "Access Denied: Authorized database login required."
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
        preparedInsightKeys.add("reel_$postId")
        _backStack.update { it + OverlayRoute.ReelInsightsScreen(postId) }
    }

    fun openProfessionalDashboard() {
        preparedInsightKeys.add("dashboard")
        _backStack.update { it + OverlayRoute.ProfessionalDashboard }
    }

    fun openAccountInsights(initialTab: Int = 0) {
        preparedInsightKeys.add("account_$initialTab")
        _backStack.update { it + OverlayRoute.AccountInsights(initialTab) }
    }

    fun openReelInsightsFromClick(postId: String) {
        openReelInsights(postId)
    }

    fun openProfessionalDashboardFromClick() {
        launchInsightsTransition(
            cacheKey = "dashboard",
            onReady = { openProfessionalDashboard() },
            prepare = {
                withContext(Dispatchers.IO) {
                    storage.loadState().accountInsights
                }
            },
        )
    }

    fun openAccountInsightsFromClick(initialTab: Int = 0) {
        launchInsightsTransition(
            cacheKey = "account_$initialTab",
            onReady = { openAccountInsights(initialTab) },
            prepare = {
                withContext(Dispatchers.IO) {
                    storage.loadState().accountInsights
                }
            },
        )
    }

    private fun launchInsightsTransition(
        cacheKey: String,
        onReady: () -> Unit,
        prepare: suspend () -> Unit,
    ) {
        if (_isInsightsLoading.value) return
        if (preparedInsightKeys.contains(cacheKey)) {
            onReady()
            return
        }
        _isInsightsLoading.value = true
        viewModelScope.launch {
            try {
                prepare()
                preparedInsightKeys.add(cacheKey)
                onReady()
            } finally {
                _isInsightsLoading.value = false
            }
        }
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

    fun fetchReelsFromApify(usernameOrUrl: String) {
        val normalizedHandle = ApifyRepository.extractUsernameFromTarget(usernameOrUrl)
        if (normalizedHandle.isBlank()) {
            _fetchUiState.value = FetchAccountUiState.Error("Please enter a valid Instagram username.")
            return
        }
        _fetchUiState.value = FetchAccountUiState.Loading
        viewModelScope.launch {
            val result = apifyRepository.fetchInstagramReels(usernameOrUrl, resultsLimit = 25)
            result.fold(
                onSuccess = { fetched ->
                    val dedupedReels = fetched.posts.distinctBy {
                        it.shortCode.ifBlank { it.postUrl.ifBlank { it.id } }
                    }
                    applyFetchedReels(dedupedReels, normalizedHandle)
                    _showFetchModal.value = false
                    _fetchUiState.value = FetchAccountUiState.Success(
                        "Loaded ${dedupedReels.size} reels & views for @$normalizedHandle"
                    )
                },
                onFailure = { err ->
                    _fetchUiState.value = FetchAccountUiState.Error(
                        err.message ?: "Unable to load reels for @$normalizedHandle."
                    )
                }
            )
        }
    }

    internal fun applyFetchedReels(fetchedReels: List<MediaPost>, handle: String) {
        if (fetchedReels.isEmpty()) return
        updateAndPersist { current ->
            val byKey = fetchedReels.associateBy { it.shortCode.ifBlank { it.id } }
            val existingOwnKeys = current.posts.filter { it.isOwnPost }.map { it.shortCode.ifBlank { it.id } }.toSet()
            val updatedExisting = current.posts.map { post ->
                val key = post.shortCode.ifBlank { post.id }
                val match = byKey[key]
                if (match != null) {
                    post.copy(
                        authorUsername = current.profile.username.ifBlank { handle },
                        mediaUrl = match.mediaUrl ?: post.mediaUrl,
                        videoUrl = match.videoUrl ?: post.videoUrl,
                        viewsDisplay = match.viewsDisplay,
                        likesCount = match.likesCount,
                        commentsCount = match.commentsCount,
                        repostsCount = match.repostsCount,
                        sharesCount = match.sharesCount,
                        savesCount = match.savesCount,
                        durationText = match.durationText,
                        dateLabel = match.dateLabel,
                        isReel = true,
                        isOwnPost = true,
                        insights = match.insights,
                    )
                } else post
            }
            val newOnes = fetchedReels.filter {
                val key = it.shortCode.ifBlank { it.id }
                key !in existingOwnKeys
            }.map {
                it.copy(
                    authorUsername = current.profile.username.ifBlank { handle },
                    isOwnPost = true,
                    isReel = true,
                )
            }
            val mergedPosts = newOnes + updatedExisting
            val ownReels = mergedPosts.filter { it.isOwnPost }
            val totalViews = ownReels.sumOf { it.insights.views.replace(",", "").trim().toLongOrNull() ?: 0L }
            val totalLikes = ownReels.sumOf { it.likesCount.toLong() }
            val totalComments = ownReels.sumOf { it.commentsCount.toLong() }
            val totalReposts = ownReels.sumOf { it.repostsCount.toLong() }
            val nf = java.text.NumberFormat.getNumberInstance(java.util.Locale.US)
            val compactViews = ApifyRepository.formatCompactCount(totalViews)

            current.copy(
                posts = mergedPosts,
                profile = current.profile.copy(
                    dashboardSubtitle = "$compactViews views in the last 30 days.",
                ),
                accountInsights = current.accountInsights.copy(
                    dashboardViews = compactViews,
                    overviewViews = nf.format(totalViews),
                    reelsViews = compactViews,
                    interactions = nf.format(totalLikes + totalComments + totalReposts),
                    interactionsLikes = nf.format(totalLikes),
                    interactionsComments = nf.format(totalComments),
                    interactionsReposts = nf.format(totalReposts),
                    contentShared = ownReels.size.toString(),
                ),
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

        var totalViews = 0L
        var reelsViews = 0L
        var postsViews = 0L
        var anyPostHasViews = false
        var totalLikes = 0L
        var totalComments = 0L
        var totalReposts = 0L

        dedupedFetched.forEach { post ->
            val v = post.insights.views.replace(",", "").trim().toLongOrNull()
            if (v != null && v >= 0L) {
                anyPostHasViews = true
                totalViews += v
                if (post.isReel) reelsViews += v else postsViews += v
            }
            if (!post.likesUnavailable) totalLikes += post.likesCount.coerceAtLeast(0)
            if (!post.commentsUnavailable) totalComments += post.commentsCount.coerceAtLeast(0)
            if (!post.repostsUnavailable) totalReposts += post.repostsCount.coerceAtLeast(0)
        }

        val totalInteractions = totalLikes + totalComments + totalReposts
        val nf = java.text.NumberFormat.getNumberInstance(java.util.Locale.US)
        val compactViews = if (anyPostHasViews) ApifyRepository.formatCompactCount(totalViews) else "0"
        val normalizedProfile = profile.copy(
            dashboardSubtitle = if (anyPostHasViews) {
                "$compactViews views in the last 30 days."
            } else {
                "Views unavailable in the last 30 days."
            },
        )

        updateAndPersist { current ->
            // Keep non-own explore/reels posts so Home/Reels/Explore stay rich,
            // while replacing own profile posts with deduplicated fetched posts!
            val externalPosts = current.posts.filter { !it.isOwnPost && it.id !in fetchedKeys }
            val mergedPosts = dedupedFetched + externalPosts

            val updatedAccountInsights = current.accountInsights.copy(
                timeframeLabel = "30 days",
                dashboardViews = if (anyPostHasViews) compactViews else "N/A",
                contentShared = dedupedFetched.size.toString(),
                overviewViews = if (anyPostHasViews) nf.format(totalViews) else "N/A",
                interactions = nf.format(totalInteractions),
                reelsViews = if (anyPostHasViews) ApifyRepository.formatCompactCount(reelsViews) else "N/A",
                postsViews = if (anyPostHasViews) ApifyRepository.formatCompactCount(postsViews) else "N/A",
                interactionsReels = nf.format(totalInteractions),
                interactionsLikes = nf.format(totalLikes),
                interactionsComments = nf.format(totalComments),
                interactionsReposts = nf.format(totalReposts),
                audienceFollowersTotal = normalizedProfile.followersCount,
            )

            current.copy(
                profile = normalizedProfile,
                posts = mergedPosts,
                accountInsights = updatedAccountInsights,
                stories = current.stories.map { story ->
                    if (story.isOwnStory) story.copy(avatarUrl = normalizedProfile.avatarUrl) else story
                },
                dmThreads = current.dmThreads.map { dm ->
                    if (dm.id == "dm_self") {
                        dm.copy(
                            username = normalizedProfile.username,
                            displayName = normalizedProfile.username,
                            avatarUrl = normalizedProfile.avatarUrl,
                        )
                    } else dm
                },
                lastFetchedUsername = normalizedProfile.username,
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
