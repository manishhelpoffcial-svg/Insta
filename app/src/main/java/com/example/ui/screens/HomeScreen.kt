package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.outlined.Add
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
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.ui.AspectRatioFrameLayout
import com.example.data.DefaultSampleData
import com.example.data.model.MediaPost
import com.example.data.model.ProfileData
import com.example.data.model.StoryItem
import com.example.data.network.ApifyRepository
import com.example.ui.components.AvatarView
import com.example.ui.components.DoubleTapHeartOverlay
import com.example.ui.components.IgAddUserIcon
import com.example.ui.components.IgBookmarkStrokeIcon
import com.example.ui.components.IgCommentBubbleIcon
import com.example.ui.components.IgHeartStrokeIcon
import com.example.ui.components.IgMoreDotsIcon
import com.example.ui.components.IgPlusIcon
import com.example.ui.components.IgRepostLoopIcon
import com.example.ui.components.IgSharePlaneIcon
import com.example.ui.components.InstagramVideoPlayer
import com.example.ui.components.PostMediaImage
import com.example.ui.components.VerifiedBlueBadge
import com.example.ui.theme.GrandHotelFontFamily
import com.example.ui.theme.IgNotifRed
import com.example.ui.theme.IgTypography
import com.example.ui.theme.InstagramTheme
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    profile: ProfileData,
    stories: List<StoryItem>,
    posts: List<MediaPost>,
    isScreenActive: Boolean = true,
    onStoryClick: (String) -> Unit,
    onLikeClick: (String) -> Unit,
    onDoubleTapLike: (String) -> Unit = onLikeClick,
    onCommentClick: (String) -> Unit,
    onRepostClick: (String) -> Unit,
    onSaveClick: (String) -> Unit,
    onFollowClick: (String) -> Unit,
    onAuthorClick: (String) -> Unit = {},
    onPostClick: (String) -> Unit,
    onNotificationsClick: () -> Unit,
) {
    val colors = InstagramTheme.colors
    val feedPosts = remember(posts) {
        val primary = posts.filter { it.showInFeed }
        if (primary.isNotEmpty()) primary else posts.take(8)
    }

    val listState = rememberLazyListState()
    val activeVideoPostId by remember(feedPosts) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visible = layoutInfo.visibleItemsInfo
            if (visible.isEmpty()) return@derivedStateOf null
            val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
            visible
                .filter { it.index >= 1 }
                .mapNotNull { info ->
                    val post = feedPosts.getOrNull(info.index - 1)
                    if (post != null && !post.videoUrl.isNullOrBlank()) {
                        val itemCenter = info.offset + info.size / 2
                        post.id to abs(itemCenter - viewportCenter)
                    } else null
                }
                .minByOrNull { it.second }
                ?.first
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding(),
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(
                onClick = { onStoryClick(stories.firstOrNull()?.id ?: "s0") },
                modifier = Modifier.size(48.dp),
            ) {
                IgPlusIcon(
                    tint = colors.textPrimary,
                    size = 22.dp,
                    contentDescription = "Create",
                )
            }

            Text(
                text = "Instagram",
                fontFamily = GrandHotelFontFamily,
                fontSize = 31.sp,
                color = colors.textPrimary,
            )

            IconButton(
                onClick = onNotificationsClick,
                modifier = Modifier.size(48.dp),
            ) {
                IgHeartStrokeIcon(
                    filled = false,
                    tint = colors.textPrimary,
                    size = 25.dp,
                )
            }
        }

        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val maxMediaHeight = (maxHeight * 0.62f).coerceAtLeast(280.dp)
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(bottom = 16.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                // Stories Tray
                item {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(stories, key = { it.id }) { story ->
                            StoryTrayItem(
                                story = story,
                                profile = profile,
                                onClick = { onStoryClick(story.id) },
                            )
                        }
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = colors.hairline.copy(alpha = 0.45f))
                }

                // Feed Posts
                itemsIndexed(feedPosts, key = { _, post -> post.id }) { index, post ->
                    val effectiveAuthor = if (post.isOwnPost && profile.username.isNotBlank()) profile.username else post.authorUsername
                    FeedPostCard(
                        post = post,
                        postIndex = index,
                        profile = profile,
                        maxMediaHeight = maxMediaHeight,
                        isVideoActive = isScreenActive && post.id == activeVideoPostId,
                        onLikeClick = { onLikeClick(post.id) },
                        onDoubleTapLike = { onDoubleTapLike(post.id) },
                        onCommentClick = { onCommentClick(post.id) },
                        onRepostClick = { onRepostClick(post.id) },
                        onSaveClick = { onSaveClick(post.id) },
                        onFollowClick = { onFollowClick(effectiveAuthor) },
                        onAuthorClick = { onAuthorClick(effectiveAuthor) },
                        onMediaClick = { onPostClick(post.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun StoryTrayItem(
    story: StoryItem,
    profile: ProfileData,
    onClick: () -> Unit,
) {
    val colors = InstagramTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(74.dp)
            .clickable(onClick = onClick),
    ) {
        val hasUploadedStory = story.hasActiveRing &&
            (story.storyImageKey.isNotBlank() || story.storyCaption.isNotBlank())
        Box(
            modifier = Modifier.size(72.dp),
            contentAlignment = Alignment.Center,
        ) {
            AvatarView(
                avatarUrl = if (story.isOwnStory) profile.avatarUrl else story.avatarUrl,
                avatarKey = if (story.isOwnStory) profile.avatarDrawableKey else story.avatarKey,
                size = 64.dp,
                showGradientRing = hasUploadedStory,
                ringStrokeWidth = 2.5.dp,
                contentDescription = story.username,
            )

            if (story.isOwnStory) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-1).dp, y = (-1).dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(colors.background)
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(colors.textPrimary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add story",
                        tint = colors.background,
                        modifier = Modifier.size(14.dp),
                    )
                }
            } else if (story.showAddFriendBadge) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 2.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.background)
                        .border(1.dp, colors.hairline, RoundedCornerShape(14.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    IgAddUserIcon(
                        tint = colors.textPrimary,
                        size = 14.dp,
                        contentDescription = "Follow suggestion",
                    )
                }
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text = story.username,
            style = IgTypography.secondaryMeta,
            lineHeight = 14.sp,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(bottom = 2.dp),
        )
    }
}

@Composable
private fun FeedPostCard(
    post: MediaPost,
    postIndex: Int,
    profile: ProfileData,
    maxMediaHeight: Dp,
    isVideoActive: Boolean,
    onLikeClick: () -> Unit,
    onDoubleTapLike: () -> Unit,
    onCommentClick: () -> Unit,
    onRepostClick: () -> Unit,
    onSaveClick: () -> Unit,
    onFollowClick: () -> Unit,
    onAuthorClick: () -> Unit,
    onMediaClick: () -> Unit,
) {
    val colors = InstagramTheme.colors
    val scope = rememberCoroutineScope()
    var isMuted by remember(post.id) { mutableStateOf(true) }
    var isHoldPaused by remember(post.id) { mutableStateOf(false) }
    var heartBurstTrigger by remember(post.id) { mutableIntStateOf(0) }
    var captionExpanded by remember(post.id) { mutableStateOf(false) }
    var detectedVideoRatio by remember(post.id) {
        mutableFloatStateOf(if (post.isReel) 9f / 16f else 0.8f)
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp),
    ) {
        // Post Header (avatar + username + verified badge clickable to creator profile)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
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
                    size = 34.dp,
                    showGradientRing = true,
                    ringStrokeWidth = 1.6.dp,
                    contentDescription = effectiveUsername,
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = effectiveUsername,
                            style = IgTypography.username,
                            color = colors.textPrimary,
                        )
                        if (isVerified) {
                            Spacer(Modifier.width(4.dp))
                            VerifiedBlueBadge(size = 14.dp)
                        }
                    }
                    if (post.subtitle.isNotBlank()) {
                        Text(
                            text = post.subtitle,
                            style = IgTypography.secondaryMeta,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            if (!post.isOwnPost) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.pillBackground)
                        .clickable(onClick = onFollowClick)
                        .padding(horizontal = 16.dp, vertical = 7.dp),
                ) {
                    Text(
                        text = if (post.isFollowing) "Following" else "Follow",
                        style = IgTypography.button,
                        color = colors.textPrimary,
                    )
                }
            }

            IconButton(
                onClick = onMediaClick,
                modifier = Modifier.size(36.dp),
            ) {
                IgMoreDotsIcon(
                    tint = colors.textPrimary,
                    size = 20.dp,
                    contentDescription = "More options",
                )
            }
        }

        // 100% edge-to-edge full-width media/video container (zero side black bars, clipped bounds)
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val ratio = detectedVideoRatio.coerceIn(9f / 16f, 16f / 9f)
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
                                } else {
                                    onMediaClick()
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
                        isPlaying = isVideoActive && !isHoldPaused,
                        shouldPrepare = isVideoActive,
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT,
                        onVideoAspectRatioChanged = { ratioFromPlayer ->
                            detectedVideoRatio = ratioFromPlayer.coerceIn(9f / 16f, 16f / 9f)
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }

            DoubleTapHeartOverlay(
                burstTrigger = heartBurstTrigger,
                modifier = Modifier.align(Alignment.Center),
            )

            // Top-right duration badge (e.g., 0:50)
            if (post.durationText.isNotBlank()) {
                Text(
                    text = post.durationText,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }

            // Bottom-right Mute toggle for video posts
            if (hasVideo) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable { isMuted = !isMuted },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.AutoMirrored.Outlined.VolumeOff else Icons.AutoMirrored.Outlined.VolumeUp,
                        contentDescription = if (isMuted) "Unmute" else "Mute",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            }
        }

        // Action Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val effectiveLikesCount = remember(post.id, post.likesCount, post.viewsDisplay, post.insights.views) {
                resolveDefaultHomeLikes(post)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onLikeClick)
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                        .testTag("feed_like_${post.id}"),
                ) {
                    IgHeartStrokeIcon(
                        filled = post.isLiked,
                        tint = if (post.isLiked) IgNotifRed else colors.textPrimary,
                        size = 24.dp,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = ApifyRepository.formatCompactCount(effectiveLikesCount.toLong()),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textPrimary,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onCommentClick)
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                ) {
                    IgCommentBubbleIcon(
                        tint = colors.textPrimary,
                        size = 23.dp,
                    )
                    if (post.commentsCount > 0) {
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = ApifyRepository.formatCompactCount(post.commentsCount.toLong()),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textPrimary,
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onRepostClick)
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                ) {
                    IgRepostLoopIcon(
                        tint = if (post.isReposted) Color(0xFF22C55E) else colors.textPrimary,
                        size = 23.dp,
                    )
                    if (post.repostsCount > 0) {
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = ApifyRepository.formatCompactCount(post.repostsCount.toLong()),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textPrimary,
                        )
                    }
                }

                IconButton(onClick = onCommentClick) {
                    IgSharePlaneIcon(
                        filled = false,
                        tint = colors.textPrimary,
                        size = 22.dp,
                    )
                }
            }

            IconButton(onClick = onSaveClick) {
                IgBookmarkStrokeIcon(
                    filled = post.isSaved,
                    tint = colors.textPrimary,
                    size = 23.dp,
                )
            }
        }

        // Likes summary & Caption
        Column(modifier = Modifier.padding(horizontal = 14.dp)) {
            if (effectiveLikesCount > 0) {
                val likedUsername = remember(post.likedByText, effectiveUsername, postIndex) {
                    resolveLikerUsername(post, postIndex, effectiveUsername)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(bottom = 5.dp)
                        .testTag("feed_liked_by_${post.id}"),
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
                                if (effectiveLikesCount > 1) {
                                    withStyle(SpanStyle(color = colors.textPrimary)) {
                                        append(" and others")
                                    }
                                }
                            },
                            style = IgTypography.postCaption,
                        )
                    }
                }

                if (post.caption.isNotBlank()) {
                    val displayText = if (!captionExpanded && post.caption.length > 52) {
                        post.caption.take(46).trimEnd() + "... more"
                    } else {
                        post.caption
                    }
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = colors.textPrimary)) {
                                append(effectiveUsername)
                                append(" ")
                            }
                            withStyle(SpanStyle(color = colors.textPrimary)) {
                                append(displayText)
                            }
                        },
                        style = IgTypography.postCaption,
                        modifier = Modifier.clickable { captionExpanded = !captionExpanded },
                    )
                }
        }
    }
}

private fun parseCompactViewsCount(raw: String): Long {
    val clean = raw.replace(",", "").trim().uppercase()
    if (clean.isBlank()) return 0L
    return when {
        clean.endsWith("M") -> ((clean.dropLast(1).toDoubleOrNull() ?: 0.0) * 1_000_000.0).toLong()
        clean.endsWith("K") -> ((clean.dropLast(1).toDoubleOrNull() ?: 0.0) * 1_000.0).toLong()
        else -> clean.toLongOrNull() ?: 0L
    }
}

private fun resolveDefaultHomeLikes(post: MediaPost): Int {
    if (post.likesCount > 0) return post.likesCount
    val views = maxOf(
        parseCompactViewsCount(post.viewsDisplay),
        parseCompactViewsCount(post.insights.views),
    )
    val seed = kotlin.math.abs(post.id.hashCode())
    return when {
        views >= 100_000L -> listOf(2400, 3100, 4200, 5700, 6400)[seed % 5]
        views >= 25_000L -> listOf(895, 1200, 1800, 2400)[seed % 4]
        views >= 5_000L -> listOf(257, 412, 569, 895)[seed % 4]
        views >= 1_000L -> listOf(64, 118, 189, 257)[seed % 4]
        else -> listOf(64, 189, 257, 569, 895)[seed % 5]
    }
}
