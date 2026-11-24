package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import com.example.ui.components.CommentsBottomSheet
import com.example.ui.components.EditNoteDialog
import com.example.ui.components.EditProfileBottomSheet
import com.example.ui.components.FetchInstagramAccountModal
import com.example.ui.components.InstagramBottomBar
import com.example.ui.components.StoryViewerOverlay
import com.example.ui.screens.AccessGatedScreen
import com.example.ui.screens.AccountFetcherScreen
import com.example.ui.screens.AccountInsightsScreen
import com.example.ui.screens.ChatConversationScreen
import com.example.ui.screens.CreatorProfileScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.ForgotPasswordScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.PostDetailScreen
import com.example.ui.screens.ProfessionalDashboardScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ReelInsightsScreen
import com.example.ui.screens.ReelsScreen
import com.example.ui.screens.RegisterScreen
import com.example.ui.theme.IgBlue
import com.example.ui.theme.InstagramTheme
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AuthScreenType
import com.example.ui.viewmodel.AuthUiState
import com.example.ui.viewmodel.InstagramViewModel
import com.example.ui.viewmodel.MainTab
import com.example.ui.viewmodel.OverlayRoute
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                InstagramAppRoot()
            }
        }
    }
}

@Composable
fun InstagramAppRoot(
    viewModel: InstagramViewModel = viewModel(),
) {
    val authUiState by viewModel.authUiState.collectAsStateWithLifecycle()

    when (val state = authUiState) {
        is AuthUiState.CheckingSession -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(InstagramTheme.colors.background),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = IgBlue,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(36.dp),
                )
            }
        }
        is AuthUiState.Unauthenticated -> {
            when (state.screen) {
                AuthScreenType.LOGIN -> {
                    LoginScreen(
                        isLoading = state.isLoading,
                        errorMessage = state.errorMessage,
                        onLoginClick = { id, pass -> viewModel.login(id, pass) },
                        onNavigateRegister = { viewModel.setAuthScreen(AuthScreenType.REGISTER) },
                        onNavigateForgotPassword = { viewModel.setAuthScreen(AuthScreenType.FORGOT_PASSWORD) },
                        onContinueGuestMode = null,
                    )
                }
                AuthScreenType.REGISTER -> {
                    RegisterScreen(
                        isLoading = state.isLoading,
                        errorMessage = state.errorMessage,
                        onRegisterClick = { em, un, pass -> viewModel.register(em, un, pass) },
                        onNavigateLogin = { viewModel.setAuthScreen(AuthScreenType.LOGIN) },
                    )
                }
                AuthScreenType.FORGOT_PASSWORD -> {
                    ForgotPasswordScreen(
                        isLoading = state.isLoading,
                        message = state.infoMessage ?: state.errorMessage,
                        onSendReset = { em -> viewModel.forgotPassword(em) },
                        onNavigateBack = { viewModel.setAuthScreen(AuthScreenType.LOGIN) },
                    )
                }
            }
        }
        is AuthUiState.AccessGated -> {
            AccessGatedScreen(
                accountStatus = state.accessResult.accountStatus,
                subscriptionStatus = state.accessResult.subscriptionStatus,
                expiryDate = state.accessResult.expiryDate,
                message = state.accessResult.message,
                onLogoutClick = { viewModel.logout() },
                onRefreshClick = { viewModel.checkInitialSession() },
            )
        }
        is AuthUiState.Authenticated -> {
            InstagramCloneApp(viewModel = viewModel)
        }
    }
}

@Composable
fun InstagramCloneApp(
    viewModel: InstagramViewModel = viewModel(),
) {
    val context = LocalContext.current
    val appState by viewModel.appState.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val backStack by viewModel.backStack.collectAsStateWithLifecycle()
    val showFetchModal by viewModel.showFetchModal.collectAsStateWithLifecycle()
    val fetchUiState by viewModel.fetchUiState.collectAsStateWithLifecycle()
    val activeCommentsPostId by viewModel.activeCommentsPostId.collectAsStateWithLifecycle()
    val showEditProfileSheet by viewModel.showEditProfileSheet.collectAsStateWithLifecycle()
    val showNoteEditor by viewModel.showNoteEditor.collectAsStateWithLifecycle()
    val isInsightsEditMode by viewModel.isInsightsEditMode.collectAsStateWithLifecycle()

    val colors = InstagramTheme.colors
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val currentOverlay = backStack.lastOrNull()
    val hasModalOpen = showFetchModal || activeCommentsPostId != null || showEditProfileSheet || showNoteEditor
    var showSplash by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        if (showSplash) {
            delay(400L)
            showSplash = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (activeTab == MainTab.REELS && currentOverlay == null) Color.Black else colors.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 460.dp),
            containerColor = if (activeTab == MainTab.REELS && currentOverlay == null) Color.Black else colors.background,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (currentOverlay == null) {
                    InstagramBottomBar(
                        activeTab = activeTab,
                        profile = appState.profile,
                        onSelectTab = viewModel::selectTab,
                        onProfileTap = viewModel::onProfileNavTap,
                    )
                }
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (currentOverlay == null) {
                            Modifier
                        } else {
                            Modifier
                        }
                    ),
            ) {
                if (currentOverlay != null) {
                    when (currentOverlay) {
                        is OverlayRoute.PostDetail -> {
                            val feedIds = currentOverlay.feedPostIds
                            val resolvedPosts = if (!feedIds.isNullOrEmpty()) {
                                val postMap = appState.posts.associateBy { it.id }
                                feedIds.mapNotNull { postMap[it] }
                            } else {
                                val post = appState.posts.find { it.id == currentOverlay.postId }
                                if (post != null && post.isOwnPost) {
                                    appState.posts.filter { it.isOwnPost }
                                } else {
                                    appState.posts
                                }
                            }.ifEmpty { appState.posts }

                            if (resolvedPosts.isNotEmpty()) {
                                PostDetailScreen(
                                    posts = resolvedPosts,
                                    initialPostId = currentOverlay.postId,
                                    profile = appState.profile,
                                    isScreenActive = !hasModalOpen,
                                    onBack = { viewModel.popBackStack() },
                                    onViewInsights = { postId -> viewModel.openReelInsights(postId) },
                                    onLikeClick = { postId -> viewModel.toggleLike(postId) },
                                    onDoubleTapLike = { postId -> viewModel.likePostIfUnliked(postId) },
                                    onCommentClick = { postId -> viewModel.openComments(postId) },
                                    onRepostClick = { postId -> viewModel.toggleRepost(postId) },
                                    onSaveClick = { postId -> viewModel.toggleSave(postId) },
                                    onAuthorClick = viewModel::openCreatorProfile,
                                    onUpdatePostDetails = viewModel::updatePostDetails,
                                )
                            }
                        }

                        is OverlayRoute.ReelInsightsScreen -> {
                            val post = appState.posts.find { it.id == currentOverlay.postId }
                                ?: appState.posts.firstOrNull()
                            if (post != null) {
                                ReelInsightsScreen(
                                    post = post,
                                    profile = appState.profile,
                                    isEditMode = isInsightsEditMode,
                                    onToggleEditMode = viewModel::toggleInsightsEditMode,
                                    onBack = { viewModel.popBackStack() },
                                    onOpenAccountInsights = { viewModel.openAccountInsights(0) },
                                    onUpdateInsights = { transform ->
                                        viewModel.updatePostInsights(post.id, transform)
                                    },
                                    onUpdateImpact = { id, pct, status ->
                                        viewModel.updateImpactMetric(post.id, id, pct, status)
                                    },
                                    onUpdateTopSource = { idx, label, pct ->
                                        viewModel.updateTopSource(post.id, idx, label, pct)
                                    },
                                    onUpdateDemographic = { cat, idx, label, pct ->
                                        viewModel.updateDemographicItem(post.id, cat, idx, label, pct)
                                    },
                                    onUpdateLikeCurvePoint = { idx, value ->
                                        viewModel.updateLikeCurvePoint(post.id, idx, value)
                                    },
                                    onUpdateProfile = viewModel::updateProfile,
                                    onUpdatePostDetails = { audio, liker ->
                                        viewModel.updatePostDetails(post.id, audio, liker)
                                    },
                                )
                            }
                        }

                        is OverlayRoute.ChatConversation -> {
                            val thread = appState.dmThreads.find { it.id == currentOverlay.threadId }
                                ?: appState.dmThreads.firstOrNull()
                            if (thread != null) {
                                ChatConversationScreen(
                                    thread = thread,
                                    profile = appState.profile,
                                    onBack = { viewModel.popBackStack() },
                                    onSendMessage = { msg -> viewModel.sendDmMessage(thread.id, msg) },
                                )
                            }
                        }

                        is OverlayRoute.StoryViewer -> {
                            val story = appState.stories.find { it.id == currentOverlay.storyId }
                                ?: appState.stories.firstOrNull()
                            if (story != null) {
                                StoryViewerOverlay(
                                    story = story,
                                    ownProfile = appState.profile,
                                    onClose = { viewModel.popBackStack() },
                                    onAuthorClick = { username ->
                                        viewModel.popBackStack()
                                        viewModel.openCreatorProfile(username)
                                    },
                                )
                            }
                        }

                        is OverlayRoute.CreatorProfile -> {
                            CreatorProfileScreen(
                                username = currentOverlay.username,
                                ownProfile = appState.profile,
                                posts = appState.posts,
                                onBack = { viewModel.popBackStack() },
                                onPostClick = { postId, gridIds ->
                                    viewModel.openPostDetail(postId, gridIds)
                                },
                                onFollowClick = viewModel::toggleFollowAuthor,
                                onMessageClick = viewModel::openDmWithCreator,
                            )
                        }

                        is OverlayRoute.ProfessionalDashboard -> {
                            ProfessionalDashboardScreen(
                                profile = appState.profile,
                                accountInsights = appState.accountInsights,
                                onBack = { viewModel.popBackStack() },
                                onOpenInsightsTab = { tabIdx ->
                                    viewModel.openAccountInsights(tabIdx)
                                },
                                onUpdateAccountInsights = viewModel::updateAccountInsights,
                            )
                        }

                        is OverlayRoute.AccountInsights -> {
                            AccountInsightsScreen(
                                initialTab = currentOverlay.initialTab,
                                profile = appState.profile,
                                posts = appState.posts,
                                accountInsights = appState.accountInsights,
                                onBack = { viewModel.popBackStack() },
                                onOpenReelInsights = viewModel::openReelInsights,
                                onUpdateAccountInsights = viewModel::updateAccountInsights,
                            )
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .paddingBottomBarOnly(innerPadding),
                        ) {
                            Crossfade(targetState = activeTab, label = "main_tab_crossfade") { tab ->
                                when (tab) {
                                    MainTab.HOME -> HomeScreen(
                                        profile = appState.profile,
                                        stories = appState.stories,
                                        posts = appState.posts,
                                        isScreenActive = !hasModalOpen && activeTab == MainTab.HOME,
                                        onStoryClick = viewModel::openStory,
                                        onLikeClick = viewModel::toggleLike,
                                        onDoubleTapLike = viewModel::likePostIfUnliked,
                                        onCommentClick = viewModel::openComments,
                                        onRepostClick = viewModel::toggleRepost,
                                        onSaveClick = viewModel::toggleSave,
                                        onFollowClick = viewModel::toggleFollowAuthor,
                                        onAuthorClick = viewModel::openCreatorProfile,
                                        onPostClick = viewModel::openPostDetail,
                                        onNotificationsClick = {
                                            scope.launch {
                                                snackbarHostState.showSnackbar("You're all caught up on notifications")
                                            }
                                        },
                                    )

                                    MainTab.REELS -> ReelsScreen(
                                        posts = appState.posts,
                                        isScreenActive = !hasModalOpen && activeTab == MainTab.REELS,
                                        onLikeClick = viewModel::toggleLike,
                                        onDoubleTapLike = viewModel::likePostIfUnliked,
                                        onCommentClick = viewModel::openComments,
                                        onRepostClick = viewModel::toggleRepost,
                                        onSaveClick = viewModel::toggleSave,
                                        onFollowClick = viewModel::toggleFollowAuthor,
                                        onAuthorClick = viewModel::openCreatorProfile,
                                        onOpenInsights = viewModel::openReelInsights,
                                    )

                                    MainTab.MESSAGES -> AccountFetcherScreen(
                                        fetchState = fetchUiState,
                                        onFetchAccount = viewModel::fetchAccountFromApify,
                                        onInputChanged = viewModel::resetFetchState,
                                    )

                                    MainTab.EXPLORE -> ExploreScreen(
                                        posts = appState.posts,
                                        onPostClick = viewModel::openPostDetail,
                                    )

                                    MainTab.PROFILE -> ProfileScreen(
                                        profile = appState.profile,
                                        posts = appState.posts,
                                        onEditProfileClick = viewModel::openEditProfileSheet,
                                        onPostClick = { postId, gridIds ->
                                            viewModel.openPostDetail(postId, gridIds)
                                        },
                                        onOpenDashboard = viewModel::openProfessionalDashboard,
                                        onOpenMenuClick = viewModel::openFetchModal,
                                        onShareProfileClick = {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(
                                                    Intent.EXTRA_TEXT,
                                                    "Check out @${appState.profile.username} on Instagram: https://instagram.com/${appState.profile.username}"
                                                )
                                            }
                                            context.startActivity(
                                                Intent.createChooser(shareIntent, "Share profile")
                                            )
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Triple-tap minimal username box
    if (showFetchModal) {
        FetchInstagramAccountModal(
            fetchState = fetchUiState,
            onDismiss = viewModel::dismissFetchModal,
            onSubmitUsername = viewModel::fetchAccountFromApify,
            onInputChanged = viewModel::resetFetchState,
            onLogoutClick = viewModel::logout,
        )
    }

    // Comments Sheet
    activeCommentsPostId?.let { postId ->
        val post = appState.posts.find { it.id == postId }
        if (post != null) {
            CommentsBottomSheet(
                post = post,
                currentUser = appState.profile,
                onDismiss = viewModel::closeComments,
                onSendComment = { text -> viewModel.addComment(postId, text) },
            )
        }
    }

    // Normal Edit Profile Sheet
    if (showEditProfileSheet) {
        EditProfileBottomSheet(
            profile = appState.profile,
            onDismiss = viewModel::closeEditProfileSheet,
            onSave = { displayName, username, bio, dashboardSubtitle ->
                viewModel.updateProfile {
                    it.copy(
                        displayName = displayName.ifBlank { it.displayName },
                        username = username.ifBlank { it.username },
                        bio = bio,
                        dashboardSubtitle = dashboardSubtitle.ifBlank { it.dashboardSubtitle },
                    )
                }
            },
        )
    }

    // Edit Note Dialog
    if (showNoteEditor) {
        EditNoteDialog(
            initialNote = appState.profile.noteText,
            onDismiss = viewModel::closeNoteEditor,
            onSave = { newNote ->
                viewModel.updateProfile { it.copy(noteText = newNote) }
            },
        )
    }

    // Cold-launch #0B0D10 Splash Screen (400ms, centered 84dp logo with 20dp corner radius)
    AnimatedVisibility(
        visible = showSplash,
        exit = fadeOut(animationSpec = tween(durationMillis = 250)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B0D10))
                .testTag("app_splash_overlay"),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_instagram_brand_logo),
                contentDescription = "Instagram logo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .testTag("app_splash_logo"),
            )
        }
    }
}

@Composable
private fun Modifier.paddingBottomBarOnly(innerPadding: androidx.compose.foundation.layout.PaddingValues): Modifier {
    return this.padding(bottom = innerPadding.calculateBottomPadding())
}
