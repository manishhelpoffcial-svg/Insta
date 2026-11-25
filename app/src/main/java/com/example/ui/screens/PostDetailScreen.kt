package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.ui.AspectRatioFrameLayout
import com.example.data.DefaultSampleData
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.data.model.MediaPost
import com.example.data.model.ProfileData
import com.example.data.network.ApifyRepository
import com.example.ui.components.AvatarView
import com.example.ui.components.DoubleTapHeartOverlay
import com.example.ui.components.IgBackIcon
import com.example.ui.components.IgBookmarkStrokeIcon
import com.example.ui.components.IgCommentBubbleIcon
import com.example.ui.components.IgEyeIcon
import com.example.ui.components.IgHeartStrokeIcon
import com.example.ui.components.IgRepostLoopIcon
import com.example.ui.components.IgSharePlaneIcon
import com.example.ui.components.IgTwoLineMenuIcon
import com.example.ui.components.InstagramVideoPlayer
import com.example.ui.components.PostMediaImage
import com.example.ui.components.PostOptionsAndEditSheet
import com.example.ui.components.VerifiedBlueBadge
import com.example.ui.theme.IgBoostBlue
import com.example.ui.theme.IgDarkMutedTimestamp
import com.example.ui.theme.IgDarkPostsBackground
import com.example.ui.theme.IgNotifRed
import com.example.ui.theme.IgOnlineGreen
import com.example.ui.theme.IgTypography
import com.example.ui.theme.InstagramTheme
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PostDetailScreen(
    posts: List<MediaPost>,
    initialPostId: String,
    profile: ProfileData,
    isScreenActive: Boolean = true,
    onBack: () -> Unit,
    onViewInsights: (String) -> Unit,
    onLikeClick: (String) -> Unit,
    onDoubleTapLike: (String) -> Unit = onLikeClick,
    onCommentClick: (String) -> Unit,
    onRepostClick: (String) -> Unit,
    onSaveClick: (String) -> Unit,
    onAuthorClick: (String) -> Unit = {},
    onUpdatePostDetails: (postId: String, audioTitle: String, likedByUsername: String) -> Unit = { _, _, _ -> },
) {
    BackHandler(onBack = onBack)
    val colors = InstagramTheme.colors
    val screenBg = if (colors.isDark) IgDarkPostsBackground else colors.background

    var optionsTargetPostId by remember { mutableStateOf<String?>(null) }

    val startIndex = remember(posts, initialPostId) {
        posts.indexOfFirst { it.id == initialPostId }.coerceAtLeast(0)
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)
    val activeItemIndex by remember(posts) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visible = layoutInfo.visibleItemsInfo
            if (visible.isEmpty()) return@derivedStateOf listState.firstVisibleItemIndex
            val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
            visible.minByOrNull { info ->
                abs((info.offset + info.size / 2) - viewportCenter)
            }?.index ?: listState.firstVisibleItemIndex
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // Top Header Bar: <- Posts
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("post_detail_back"),
            ) {
                IgBackIcon(
                    size = 24.dp,
                    tint = colors.textPrimary,
                )
            }
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Posts",
                style = IgTypography.screenHeading,
                color = colors.textPrimary,
            )
        }

        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val maxMediaHeight = (maxHeight * 0.62f).coerceAtLeast(280.dp)
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                itemsIndexed(posts, key = { _, post -> post.id }) { index, post ->
                    val effectiveAuthor = if (post.isOwnPost && profile.username.isNotBlank()) profile.username else post.authorUsername
                    PostDetailFeedItem(
                        post = post,
                        postIndex = index,
                        profile = profile,
                        maxMediaHeight = maxMediaHeight,
                        isActive = isScreenActive && optionsTargetPostId == null && index == activeItemIndex,
                        onViewInsights = { onViewInsights(post.id) },
                        onOpenPostOptions = { optionsTargetPostId = post.id },
                        onLikeClick = { onLikeClick(post.id) },
                        onDoubleTapLike = { onDoubleTapLike(post.id) },
                        onCommentClick = { onCommentClick(post.id) },
                        onRepostClick = { onRepostClick(post.id) },
                        onSaveClick = { onSaveClick(post.id) },
                        onAuthorClick = { onAuthorClick(effectiveAuthor) },
                    )
                }
            }
        }
    }

    optionsTargetPostId?.let { targetId ->
        val targetPost = posts.find { it.id == targetId }
        if (targetPost != null) {
            val targetOwner = if (targetPost.isOwnPost && profile.username.isNotBlank()) profile.username else targetPost.authorUsername
            val currentAudio = resolveAudioSubtitle(targetPost.subtitle, targetOwner)
            val currentCustomLiker = extractCustomLikerOverride(targetPost.likedByText).orEmpty()
            PostOptionsAndEditSheet(
                audioTitle = currentAudio,
                likedByUsername = currentCustomLiker,
                onDismiss = { optionsTargetPostId = null },
                onOpenInsights = { onViewInsights(targetPost.id) },
                onSave = { audio, liker -> onUpdatePostDetails(targetPost.id, audio, liker) },
            )
        }
    }
}

@Composable
private fun PostDetailFeedItem(
    post: MediaPost,
    postIndex: Int,
    profile: ProfileData,
    maxMediaHeight: Dp,
    isActive: Boolean,
    onViewInsights: () -> Unit,
    onOpenPostOptions: () -> Unit,
    onLikeClick: () -> Unit,
    onDoubleTapLike: () -> Unit,
    onCommentClick: () -> Unit,
    onRepostClick: () -> Unit,
    onSaveClick: () -> Unit,
    onAuthorClick: () -> Unit,
) {
    val colors = InstagramTheme.colors
    val scope = rememberCoroutineScope()
    val mutedColor = if (colors.isDark) IgDarkMutedTimestamp else colors.textSecondary
    val dividerColor = if (colors.isDark) Color(0xFF1E2228) else colors.hairline.copy(alpha = 0.6f)

    var isMuted by remember(post.id) { mutableStateOf(true) }
    var isHoldPaused by remember(post.id) { mutableStateOf(false) }
    var heartBurstTrigger by remember(post.id) { mutableIntStateOf(0) }
    var captionExpanded by remember(post.id) { mutableStateOf(false) }
    var showTranslationNote by remember(post.id) { mutableStateOf(false) }
    var detectedVideoRatio by remember(post.id) {
        mutableFloatStateOf(if (post.isReel) 0.66f else 0.8f)
    }

    val hasVideo = !post.videoUrl.isNullOrBlank()
    val effectiveUsername = remember(post.isOwnPost, post.authorUsername, profile.username) {
        if (post.isOwnPost && profile.username.isNotBlank()) profile.username else post.authorUsername
    }
    val effectiveAvatarUrl = remember(post.isOwnPost, post.authorAvatarUrl, profile.avatarUrl) {
        if (post.isOwnPost) (profile.avatarUrl ?: post.authorAvatarUrl) else post.authorAvatarUrl
    }
    val effectiveAvatarKey = remember(post.isOwnPost, post.authorAvatarKey, profile.avatarDrawableKey) {
        if (post.isOwnPost) {
            profile.avatarDrawableKey.ifBlank { post.authorAvatarKey }
        } else {
            post.authorAvatarKey.ifBlank { profile.avatarDrawableKey }
        }
    }
    val isVerified = remember(effectiveUsername, post.isOwnPost, profile.isVerified) {
        if (post.isOwnPost) profile.isVerified else DefaultSampleData.isVerifiedCreator(effectiveUsername)
    }
    val audioSubtitle = remember(post.subtitle, effectiveUsername) {
        resolveAudioSubtitle(post.subtitle, effectiveUsername)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp),
    ) {
        // 100% edge-to-edge full-width media container (fills width, dynamic vertical ratio capped at 72% height, zero side bars)
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
        ) {
            val ratio = detectedVideoRatio.coerceIn(9f / 16f, 1.0f)
            val computedHeight = (maxWidth / ratio).coerceAtMost(maxMediaHeight)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(computedHeight)
                    .clipToBounds()
                    .background(Color.Black)
                    .pointerInput(post.id, hasVideo) {
                        detectTapGestures(
                            onTap = {
                                if (hasVideo) {
                                    isMuted = !isMuted
                                }
                            },
                            onDoubleTap = {
                                heartBurstTrigger += 1
                                onDoubleTapLike()
                            },
                            onPress = {
                                if (hasVideo) {
                                    val holdJob = scope.launch {
                                        delay(180)
                                        isHoldPaused = true
                                    }
                                    try {
                                        tryAwaitRelease()
                                    } finally {
                                        holdJob.cancel()
                                        isHoldPaused = false
                                    }
                                }
                            },
                        )
                    },
            ) {
                PostMediaImage(
                    mediaUrl = post.mediaUrl,
                    drawableKey = post.drawableKey,
                    videoUrl = post.videoUrl,
                    overlayTopCaption = post.overlayTopCaption,
                    overlaySubtitle = post.overlaySubtitle,
                    useLetterboxMemeStyle = !hasVideo && post.mediaUrl == null && post.overlayTopCaption.isNotBlank(),
                    contentDescription = post.caption,
                    modifier = Modifier.fillMaxSize(),
                )
                if (hasVideo) {
                    InstagramVideoPlayer(
                        videoUrl = post.videoUrl!!,
                        isMuted = isMuted,
                        isPlaying = isActive && !isHoldPaused,
                        shouldPrepare = isActive,
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
                        onVideoAspectRatioChanged = { ratioFromPlayer ->
                            detectedVideoRatio = ratioFromPlayer.coerceIn(9f / 16f, 1.0f)
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                // Top Header Overlay (avatar + username + audio track + right 2-line menu)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.58f), Color.Transparent)
                            )
                        ),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(start = 12.dp, end = 4.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onAuthorClick),
                    ) {
                        AvatarView(
                            avatarUrl = effectiveAvatarUrl,
                            avatarKey = effectiveAvatarKey,
                            size = 32.dp,
                            contentDescription = effectiveUsername,
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = effectiveUsername,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (isVerified) {
                                    Spacer(Modifier.width(5.dp))
                                    VerifiedBlueBadge(size = 13.dp)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.MusicNote,
                                    contentDescription = null,
                                    tint = Color(0xFFE5E7EB),
                                    modifier = Modifier.size(12.dp),
                                )
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    text = audioSubtitle,
                                    fontSize = 11.5.sp,
                                    color = Color(0xFFE5E7EB),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onOpenPostOptions,
                        modifier = Modifier.testTag("post_detail_overlay_menu_${post.id}"),
                    ) {
                        IgTwoLineMenuIcon(
                            tint = Color.White,
                            size = 20.dp,
                        )
                    }
                }

                DoubleTapHeartOverlay(
                    burstTrigger = heartBurstTrigger,
                    modifier = Modifier.align(Alignment.Center),
                )

                // Bottom-right circular translucent dark pill mute toggle (28dp pill inside 48dp target)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable { isMuted = !isMuted }
                        .testTag("post_detail_mute_${post.id}"),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(20, 20, 20, 178)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.AutoMirrored.Outlined.VolumeOff else Icons.AutoMirrored.Outlined.VolumeUp,
                            contentDescription = if (isMuted) "Unmute" else "Mute",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                }
            }
        }

        // Creator Insights & Boost Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onViewInsights)
                    .padding(vertical = 6.dp, horizontal = 2.dp)
                    .testTag("view_insights_button_${post.id}"),
            ) {
                IgEyeIcon(tint = colors.textPrimary, size = 15.dp)
                Spacer(Modifier.width(7.dp))
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = colors.textPrimary)) {
                            append(post.viewsDisplay)
                        }
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = colors.textPrimary)) {
                            append(" · View insights")
                        }
                    },
                    fontSize = 14.sp,
                )
            }

            Button(
                onClick = onViewInsights,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = IgBoostBlue,
                    contentColor = Color.White,
                ),
                modifier = Modifier
                    .height(36.dp)
                    .testTag("boost_post_button_${post.id}"),
            ) {
                Text(
                    text = "Boost post",
                    style = IgTypography.button,
                )
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = dividerColor)

        // Exact 5-Icon Engagement Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 1. Like (Heart outline + inline count)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .height(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onLikeClick)
                        .padding(horizontal = 10.dp)
                        .testTag("post_detail_like_${post.id}"),
                ) {
                    IgHeartStrokeIcon(
                        filled = post.isLiked,
                        tint = if (post.isLiked) IgNotifRed else colors.textPrimary,
                        size = 24.dp,
                    )
                    if (post.likesUnavailable) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "N/A",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = mutedColor,
                        )
                    } else if (post.likesCount > 0) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = ApifyRepository.formatCompactCount(post.likesCount.toLong()),
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary,
                        )
                    }
                }

                // 2. Comment (Circular speech bubble with bottom-right tail)
                IconButton(
                    onClick = onCommentClick,
                    modifier = Modifier.testTag("post_detail_comment_${post.id}"),
                ) {
                    IgCommentBubbleIcon(
                        tint = colors.textPrimary,
                        size = 23.dp,
                    )
                }

                // 3. Repost (Two-arrow loop + inline numeric count)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .height(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onRepostClick)
                        .padding(horizontal = 10.dp)
                        .testTag("post_detail_repost_${post.id}"),
                ) {
                    IgRepostLoopIcon(
                        tint = if (post.isReposted) IgOnlineGreen else colors.textPrimary,
                        size = 23.dp,
                    )
                    if (post.repostsUnavailable) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "N/A",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = mutedColor,
                        )
                    } else if (post.repostsCount > 0) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = ApifyRepository.formatCompactCount(post.repostsCount.toLong()),
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary,
                        )
                    }
                }

                // 4. Share / Send (Paper plane + inline numeric count)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .height(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onCommentClick)
                        .padding(horizontal = 10.dp)
                        .testTag("post_detail_share_${post.id}"),
                ) {
                    IgSharePlaneIcon(
                        filled = false,
                        tint = colors.textPrimary,
                        size = 22.dp,
                    )
                    if (post.sharesUnavailable) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "N/A",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = mutedColor,
                        )
                    } else if (post.sharesCount > 0) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = ApifyRepository.formatCompactCount(post.sharesCount.toLong()),
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary,
                        )
                    }
                }
            }

            // 5. Save / Bookmark (Far right)
            IconButton(
                onClick = onSaveClick,
                modifier = Modifier.testTag("post_detail_save_${post.id}"),
            ) {
                IgBookmarkStrokeIcon(
                    filled = post.isSaved,
                    tint = colors.textPrimary,
                    size = 23.dp,
                )
            }
        }

        // Metadata Section: Liked by, Caption, Timestamp
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
        ) {
            // Liked by line with distinct 18dp circular liker avatar
            val likedUsername = remember(post.likedByText, effectiveUsername, postIndex) {
                resolveLikerUsername(post, postIndex, effectiveUsername)
            }

            if (!post.likesUnavailable && post.likesCount > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 6.dp),
                ) {
                    LikerAvatarDot(
                        username = likedUsername,
                        size = 18.dp,
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = colors.textPrimary)) {
                                append("Liked by ")
                            }
                            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = colors.textPrimary)) {
                                append(likedUsername)
                            }
                            if (post.likesCount > 1) {
                                withStyle(SpanStyle(color = colors.textPrimary)) {
                                    append(" and others")
                                }
                            }
                        },
                        fontSize = 14.sp,
                    )
                }
            }

            // Caption line: Bold username + inline 2-line clamp with "... more" on line 2
            val cleanCaption = post.caption.replace("\n", " ").trim()
            if (cleanCaption.isNotBlank()) {
                val captionStyle = TextStyle(fontSize = 14.sp, lineHeight = 19.sp)
                val textMeasurer = rememberTextMeasurer()

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { captionExpanded = !captionExpanded }
                        .testTag("post_detail_caption_${post.id}"),
                ) {
                    val maxW = constraints.maxWidth
                    val fullAnnotated = remember(effectiveUsername, cleanCaption, colors.textPrimary) {
                        buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = colors.textPrimary)) {
                                append(effectiveUsername)
                                append(" ")
                            }
                            withStyle(SpanStyle(color = colors.textPrimary)) {
                                append(cleanCaption)
                            }
                        }
                    }

                    val fullMeasure = remember(fullAnnotated, maxW) {
                        textMeasurer.measure(
                            text = fullAnnotated,
                            style = captionStyle,
                            constraints = Constraints(maxWidth = maxW),
                            maxLines = 2,
                        )
                    }

                    if (!captionExpanded && fullMeasure.hasVisualOverflow && fullMeasure.lineCount >= 2) {
                        val collapsedAnnotated = remember(effectiveUsername, cleanCaption, maxW, mutedColor, colors.textPrimary) {
                            val line2End = fullMeasure.getLineEnd(1, visibleEnd = true)
                            val prefixLen = effectiveUsername.length + 1
                            var sliceEnd = (line2End - prefixLen - 6).coerceIn(0, cleanCaption.length)
                            var candidate = buildCollapsedCaption(effectiveUsername, cleanCaption.take(sliceEnd).trimEnd(), colors.textPrimary, mutedColor)
                            while (sliceEnd > 0) {
                                val m = textMeasurer.measure(
                                    text = candidate,
                                    style = captionStyle,
                                    constraints = Constraints(maxWidth = maxW),
                                    maxLines = 2,
                                )
                                if (!m.hasVisualOverflow && m.lineCount <= 2) break
                                sliceEnd = (sliceEnd - 2).coerceAtLeast(0)
                                candidate = buildCollapsedCaption(effectiveUsername, cleanCaption.take(sliceEnd).trimEnd(), colors.textPrimary, mutedColor)
                            }
                            candidate
                        }
                        Text(
                            text = collapsedAnnotated,
                            style = captionStyle,
                            maxLines = 2,
                        )
                    } else {
                        Text(
                            text = buildAnnotatedString {
                                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = colors.textPrimary)) {
                                    append(effectiveUsername)
                                    append(" ")
                                }
                                withStyle(SpanStyle(color = colors.textPrimary)) {
                                    append(post.caption)
                                }
                                if (captionExpanded && fullMeasure.hasVisualOverflow) {
                                    withStyle(SpanStyle(color = mutedColor)) {
                                        append("  less")
                                    }
                                }
                            },
                            style = captionStyle,
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // Relative Timestamp + "• See translation"
            val relativeDate = remember(post.dateLabel) {
                ApifyRepository.formatRelativeDate(post.dateLabel)
            }
            val translationTextColor = if (colors.isDark) Color(0xFFE5E7EB) else colors.textPrimary
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { showTranslationNote = !showTranslationNote }
                    .testTag("post_detail_date_translation_${post.id}"),
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = mutedColor)) {
                            append(relativeDate)
                            append("  •  ")
                        }
                        withStyle(SpanStyle(color = translationTextColor, fontWeight = FontWeight.Medium)) {
                            append(if (showTranslationNote) "See original" else "See translation")
                        }
                    },
                    fontSize = 12.sp,
                )
            }
            if (showTranslationNote) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "Translated from English",
                    fontSize = 11.5.sp,
                    color = mutedColor,
                )
            }
        }
    }
}

private fun buildCollapsedCaption(
    username: String,
    truncatedBody: String,
    primaryColor: Color,
    mutedColor: Color,
) = buildAnnotatedString {
    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = primaryColor)) {
        append(username)
        append(" ")
    }
    withStyle(SpanStyle(color = primaryColor)) {
        append(truncatedBody)
    }
    withStyle(SpanStyle(color = mutedColor)) {
        append("... more")
    }
}

internal val LIKER_USERNAMES_POOL = listOf(
    "catchthis.us",
    "aarav.sharma99",
    "diya_kapoor.x",
    "rohan.vibes_",
    "neha.aesthetic",
    "kabir_edits07",
    "ananya.frames",
    "devansh.rx",
    "isha_malhotra21",
    "aryan.xd_",
    "tanya.diary",
    "sidharth.plays",
    "meera.clicks",
    "yash.nights",
    "priya.stills",
    "kunal_motion",
    "sanya.reels",
    "harsh.004",
    "naina.creates",
    "vikram.visuals",
    "riya_chatterjee",
    "aditya.raw",
    "kiara.moods",
    "manav.zone",
    "shruti.wave",
    "pranav.lens",
    "avani.stories",
    "rishabh.core",
    "pooja.daily_",
    "nikhil.cuts",
    "zoya.frames_",
    "kartik.vfx",
    "sneha.archive",
    "rahul.drift",
    "alisha.glow",
    "varun.media_",
    "mira.unfiltered",
    "ojas.studio",
)

internal fun resolveAudioSubtitle(rawSubtitle: String, ownerUsername: String): String {
    val raw = rawSubtitle.trim()
    val cleanOwner = ownerUsername.ifBlank { "instagram" }
    return when {
        raw.equals("N/A", ignoreCase = true) -> "N/A"
        raw.isBlank() ||
            raw.equals("Suggested for you", ignoreCase = true) ||
            raw.equals("Original audio", ignoreCase = true) ||
            raw.endsWith("· Original audio", ignoreCase = true) -> "$cleanOwner · Original audio"
        else -> raw
    }
}

internal fun extractCustomLikerOverride(likedByText: String): String? {
    val raw = likedByText.trim()
    if (raw.startsWith("Liked by ", ignoreCase = true)) {
        val afterPrefix = raw.substring(9).trim()
        val extracted = when {
            afterPrefix.endsWith(" and others", ignoreCase = true) ->
                afterPrefix.substring(0, afterPrefix.length - 11).trim()
            afterPrefix.endsWith(" people", ignoreCase = true) -> ""
            else -> afterPrefix
        }
        if (extracted.isNotBlank() && extracted.firstOrNull()?.isDigit() != true) {
            return extracted
        }
    }
    return null
}

internal fun resolveLikerUsername(
    post: MediaPost,
    postIndex: Int = 0,
    authorUsername: String = post.authorUsername,
): String {
    extractCustomLikerOverride(post.likedByText)?.let { return it }
    val candidates = LIKER_USERNAMES_POOL.filterNot { it.equals(authorUsername, ignoreCase = true) }
    return if (candidates.isEmpty()) {
        LIKER_USERNAMES_POOL[postIndex.coerceAtLeast(0) % LIKER_USERNAMES_POOL.size]
    } else {
        candidates[postIndex.coerceAtLeast(0) % candidates.size]
    }
}

private val LIKER_AVATAR_GRADIENTS = listOf(
    listOf(Color(0xFFFD1D1D), Color(0xFF833AB4)),
    listOf(Color(0xFF2563EB), Color(0xFF06B6D4)),
    listOf(Color(0xFFEA580C), Color(0xFFFACC15)),
    listOf(Color(0xFF059669), Color(0xFF34D399)),
    listOf(Color(0xFFDB2777), Color(0xFFF472B6)),
    listOf(Color(0xFF7C3AED), Color(0xFF3B82F6)),
    listOf(Color(0xFFDC2626), Color(0xFFF97316)),
    listOf(Color(0xFF0D9488), Color(0xFF2DD4BF)),
    listOf(Color(0xFF4F46E5), Color(0xFFA855F7)),
    listOf(Color(0xFFBE185D), Color(0xFFFB7185)),
)

@Composable
internal fun LikerAvatarDot(
    username: String,
    size: Dp = 18.dp,
) {
    val bundledKey = remember(username) {
        when (username.lowercase()) {
            "cineverxhub" -> "cineverxhub"
            "themovietimez" -> "themovietimez"
            "blox.ibakke" -> "blox_ibakke"
            "nightvibe.rbx" -> "nightvibe_rbx"
            "tom.autumn" -> "tom_autumn"
            "peter.frames" -> "peter_frames"
            "aarav.rides" -> "aarav_rides"
            else -> null
        }
    }
    if (bundledKey != null) {
        AvatarView(
            avatarUrl = null,
            avatarKey = bundledKey,
            size = size,
            contentDescription = "Liked by $username",
        )
    } else {
        val gradient = remember(username) {
            val idx = abs(username.hashCode()) % LIKER_AVATAR_GRADIENTS.size
            LIKER_AVATAR_GRADIENTS[idx]
        }
        val initial = remember(username) {
            username.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "U"
        }
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(Brush.linearGradient(gradient)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initial,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
    }
}
