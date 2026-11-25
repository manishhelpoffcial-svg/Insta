package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.ui.AspectRatioFrameLayout
import com.example.data.DefaultSampleData
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.data.model.MediaPost
import com.example.data.model.StoryItem
import com.example.data.network.ApifyRepository
import com.example.ui.components.AvatarView
import com.example.ui.components.DoubleTapHeartOverlay
import com.example.ui.components.IgBookmarkStrokeIcon
import com.example.ui.components.IgCommentBubbleIcon
import com.example.ui.components.IgEyeIcon
import com.example.ui.components.IgCommentBubbleIcon
import com.example.ui.components.IgHeartStrokeIcon
import com.example.ui.components.IgMoreDotsIcon
import com.example.ui.components.IgPlusIcon
import com.example.ui.components.IgRepostLoopIcon
import com.example.ui.components.IgSharePlaneIcon
import com.example.ui.components.InstagramVideoPlayer
import com.example.ui.components.PostMediaImage
import com.example.ui.components.VerifiedBlueBadge
import com.example.ui.theme.IgNotifRed
import com.example.ui.theme.IgTypography
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ReelsScreen(
    posts: List<MediaPost>,
    stories: List<StoryItem> = emptyList(),
    isScreenActive: Boolean = true,
    onLikeClick: (String) -> Unit,
    onDoubleTapLike: (String) -> Unit = onLikeClick,
    onCommentClick: (String) -> Unit,
    onRepostClick: (String) -> Unit,
    onSaveClick: (String) -> Unit,
    onFollowClick: (String) -> Unit,
    onAuthorClick: (String) -> Unit = {},
    onOpenInsights: (String) -> Unit,
) {
    val reels = remember(posts) {
        val reelItems = posts.filter { it.isReel }
        if (reelItems.isNotEmpty()) {
            val priorityOrder = listOf(
                "reel_1_cineverx",
                "reel_3_blox",
                "reel_4_tom",
                "reel_5_peter",
                "reel_6_music_hits",
                "post_own_1",
                "reel_6_cineverx_aladdin",
            )
            reelItems.sortedWith(
                compareBy<MediaPost> { post ->
                    val idx = priorityOrder.indexOf(post.id)
                    when {
                        idx >= 0 -> idx
                        !post.videoUrl.isNullOrBlank() -> 100
                        else -> 200
                    }
                }
            )
        } else {
            posts
        }
    }

    var selectedReelMode by remember { mutableStateOf("Reels") }
    val pagerState = rememberPagerState(pageCount = { reels.size.coerceAtLeast(1) })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (reels.isNotEmpty()) {
            VerticalPager(
                state = pagerState,
                beyondViewportPageCount = 0,
                modifier = Modifier.fillMaxSize(),
            ) { pageIndex ->
                val reel = reels[pageIndex]
                val isCurrentPage = isScreenActive && pagerState.currentPage == pageIndex
                val authorHasStory = stories.any {
                    it.username.equals(reel.authorUsername, ignoreCase = true) &&
                        it.hasActiveRing &&
                        (it.storyImageKey.isNotBlank() || it.storyCaption.isNotBlank())
                }
                ReelPage(
                    post = reel,
                    hasAuthorActiveStory = authorHasStory,
                    isActive = isCurrentPage,
                    onLikeClick = { onLikeClick(reel.id) },
                    onDoubleTapLike = { onDoubleTapLike(reel.id) },
                    onCommentClick = { onCommentClick(reel.id) },
                    onRepostClick = { onRepostClick(reel.id) },
                    onSaveClick = { onSaveClick(reel.id) },
                    onFollowClick = { onFollowClick(reel.authorUsername) },
                    onAuthorClick = { onAuthorClick(reel.authorUsername) },
                    onMoreClick = { onOpenInsights(reel.id) },
                )
            }
        }

        // Top Overlay: + , Reels , Friends + overlapping avatars
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = {}) {
                IgPlusIcon(
                    tint = Color.White,
                    forceWhite = true,
                    size = 22.dp,
                    contentDescription = "Create Reel",
                )
            }
            Spacer(Modifier.width(24.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Text(
                    text = "Reels",
                    color = if (selectedReelMode == "Reels") Color.White else Color.White.copy(alpha = 0.65f),
                    style = IgTypography.screenHeading.copy(fontSize = 19.sp),
                    modifier = Modifier.clickable { selectedReelMode = "Reels" },
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { selectedReelMode = "Friends" },
                ) {
                    Text(
                        text = "Friends",
                        color = if (selectedReelMode == "Friends") Color.White else Color.White.copy(alpha = 0.72f),
                        style = IgTypography.screenHeading.copy(fontSize = 19.sp),
                    )
                    Spacer(Modifier.width(8.dp))
                    // Overlapping friend avatars
                    Box(modifier = Modifier.width(56.dp).height(24.dp)) {
                        listOf("cineverxhub", "blox_ibakke", "tom_autumn").forEachIndexed { idx, key ->
                            Box(
                                modifier = Modifier
                                    .offset(x = (idx * 15).dp)
                                    .size(23.dp)
                                    .clip(CircleShape)
                                    .border(1.2.dp, Color.White, CircleShape),
                            ) {
                                AvatarView(
                                    avatarUrl = null,
                                    avatarKey = key,
                                    size = 23.dp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReelPage(
    post: MediaPost,
    hasAuthorActiveStory: Boolean = false,
    isActive: Boolean,
    onLikeClick: () -> Unit,
    onDoubleTapLike: () -> Unit,
    onCommentClick: () -> Unit,
    onRepostClick: () -> Unit,
    onSaveClick: () -> Unit,
    onFollowClick: () -> Unit,
    onAuthorClick: () -> Unit,
    onMoreClick: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var isMuted by remember(post.id) { mutableStateOf(false) }
    var isHoldPaused by remember(post.id) { mutableStateOf(false) }
    var heartBurstTrigger by remember(post.id) { mutableIntStateOf(0) }
    var showAudioIndicator by remember(post.id) { mutableStateOf(false) }
    val isVerified = remember(post.authorUsername) {
        DefaultSampleData.isVerifiedCreator(post.authorUsername)
    }

    LaunchedEffect(showAudioIndicator, isMuted) {
        if (showAudioIndicator) {
            delay(750)
            showAudioIndicator = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .background(Color.Black)
            .pointerInput(post.id) {
                detectTapGestures(
                    onTap = {
                        isMuted = !isMuted
                        showAudioIndicator = true
                    },
                    onDoubleTap = {
                        heartBurstTrigger += 1
                        onDoubleTapLike()
                    },
                    onPress = {
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
                    },
                )
            },
    ) {
        PostMediaImage(
            mediaUrl = post.mediaUrl,
            drawableKey = post.drawableKey,
            videoUrl = post.videoUrl,
            overlayTopCaption = "",
            overlaySubtitle = post.overlaySubtitle,
            contentDescription = post.caption,
            modifier = Modifier.fillMaxSize(),
        )
        if (!post.videoUrl.isNullOrBlank()) {
            InstagramVideoPlayer(
                videoUrl = post.videoUrl,
                isMuted = isMuted,
                isPlaying = isActive && !isHoldPaused,
                shouldPrepare = isActive,
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Top & Bottom readability gradients
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent)
                    )
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))
                    )
                ),
        )

        // Centered overlay caption for static-frame demo reels without embedded video subtitles
        if (post.videoUrl.isNullOrBlank() && post.overlayTopCaption.isNotBlank()) {
            Text(
                text = post.overlayTopCaption,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 68.dp, start = 20.dp, end = 20.dp)
                    .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }

        // Center brief mute/unmute indicator + double-tap heart burst
        AnimatedVisibility(
            visible = showAudioIndicator,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.58f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.AutoMirrored.Outlined.VolumeOff else Icons.AutoMirrored.Outlined.VolumeUp,
                    contentDescription = if (isMuted) "Muted" else "Unmuted",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }
        }

        DoubleTapHeartOverlay(
            burstTrigger = heartBurstTrigger,
            modifier = Modifier.align(Alignment.Center),
        )

        // Right Action Stack
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 10.dp, bottom = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ReelActionStat(
                countText = if (post.likesUnavailable) "" else ApifyRepository.formatCompactCount(post.likesCount.toLong()),
                onClick = onLikeClick,
            ) {
                IgHeartStrokeIcon(
                    filled = post.isLiked,
                    tint = if (post.isLiked) IgNotifRed else Color.White,
                    size = 29.dp,
                )
            }

            ReelActionStat(
                countText = if (post.commentsUnavailable) "" else ApifyRepository.formatCompactCount(post.commentsCount.toLong()),
                onClick = onCommentClick,
            ) {
                IgCommentBubbleIcon(
                    tint = Color.White,
                    size = 27.dp,
                )
            }

            ReelActionStat(
                countText = if (post.repostsUnavailable) "" else ApifyRepository.formatCompactCount(post.repostsCount.toLong()),
                onClick = onRepostClick,
            ) {
                IgRepostLoopIcon(
                    tint = if (post.isReposted) Color(0xFF4ADE80) else Color.White,
                    size = 27.dp,
                )
            }

            ReelActionStat(
                countText = if (post.sharesUnavailable) "" else ApifyRepository.formatCompactCount(post.sharesCount.toLong()),
                onClick = onCommentClick,
            ) {
                IgSharePlaneIcon(
                    filled = false,
                    tint = Color.White,
                    size = 27.dp,
                )
            }

            ReelActionStat(
                countText = if (post.savesUnavailable) "" else ApifyRepository.formatCompactCount(post.savesCount.toLong()),
                onClick = onSaveClick,
            ) {
                IgBookmarkStrokeIcon(
                    filled = post.isSaved,
                    tint = Color.White,
                    size = 27.dp,
                )
            }

            IconButton(
                onClick = onMoreClick,
                modifier = Modifier.size(36.dp),
            ) {
                IgMoreDotsIcon(
                    tint = Color.White,
                    size = 21.dp,
                    contentDescription = "More",
                )
            }

            // Audio cover thumbnail
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.5.dp, Color.White, RoundedCornerShape(6.dp))
                    .clickable(onClick = onAuthorClick),
            ) {
                AvatarView(
                    avatarUrl = post.authorAvatarUrl,
                    avatarKey = post.authorAvatarKey,
                    size = 28.dp,
                )
            }
        }

        // Bottom Left Author + Verified + Follow + Caption
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 14.dp, end = 76.dp, bottom = 18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onAuthorClick)
                        .padding(vertical = 2.dp),
                ) {
                    AvatarView(
                        avatarUrl = post.authorAvatarUrl,
                        avatarKey = post.authorAvatarKey,
                        size = 36.dp,
                        showGradientRing = hasAuthorActiveStory,
                        ringStrokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = post.authorUsername,
                        color = Color.White,
                        style = IgTypography.username,
                    )
                    if (isVerified) {
                        Spacer(Modifier.width(5.dp))
                        VerifiedBlueBadge(size = 14.dp)
                    }
                }
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.75f), RoundedCornerShape(8.dp))
                        .clickable(onClick = onFollowClick)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = if (post.isFollowing) "Following" else "Follow",
                        color = Color.White,
                        style = IgTypography.button.copy(fontSize = 13.sp),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            val displayCaption = post.caption.takeIf { it.isNotBlank() && !it.equals("N/A", ignoreCase = true) }
            if (displayCaption != null) {
                Text(
                    text = displayCaption,
                    color = Color.White,
                    style = IgTypography.postCaption,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val hasViews = post.viewsDisplay.isNotBlank() && !post.viewsDisplay.equals("N/A", ignoreCase = true)
            val hasDate = post.dateLabel.isNotBlank() && !post.dateLabel.equals("N/A", ignoreCase = true)
            if (hasViews || hasDate) {
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (hasViews) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IgEyeIcon(tint = Color.White.copy(alpha = 0.9f), size = 13.dp)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "${post.viewsDisplay} views",
                                color = Color.White.copy(alpha = 0.9f),
                                style = IgTypography.secondaryMeta,
                            )
                        }
                    }
                    if (hasDate) {
                        Text(
                            text = if (hasViews) "· ${post.dateLabel}" else post.dateLabel,
                            color = Color.White.copy(alpha = 0.85f),
                            style = IgTypography.secondaryMeta,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReelActionStat(
    countText: String,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp),
    ) {
        icon()
        if (countText.isNotBlank()) {
            Spacer(Modifier.height(3.dp))
            Text(
                text = countText,
                color = Color.White,
                style = IgTypography.navLabel,
            )
        }
    }
}
