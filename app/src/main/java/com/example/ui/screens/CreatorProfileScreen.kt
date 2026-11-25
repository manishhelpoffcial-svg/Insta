package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DefaultSampleData
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.data.model.MediaPost
import com.example.data.model.ProfileData
import com.example.ui.components.AvatarView
import com.example.ui.components.IgBackIcon
import com.example.ui.components.IgEyeIcon
import com.example.ui.components.IgGridTabIcon
import com.example.ui.components.IgMoreDotsIcon
import com.example.ui.components.IgReelsSquircleIcon
import com.example.ui.components.IgRepostLoopIcon
import com.example.ui.components.IgTaggedPersonIcon
import com.example.ui.components.PostMediaImage
import com.example.ui.components.VerifiedBlueBadge
import com.example.ui.theme.IgBoostBlue
import com.example.ui.theme.IgTypography
import com.example.ui.theme.InstagramTheme

@Composable
fun CreatorProfileScreen(
    username: String,
    ownProfile: ProfileData,
    posts: List<MediaPost>,
    onBack: () -> Unit,
    onPostClick: (String, List<String>) -> Unit,
    onFollowClick: (String) -> Unit,
    onMessageClick: (String) -> Unit,
) {
    BackHandler(onBack = onBack)
    val colors = InstagramTheme.colors
    val creator = remember(username, ownProfile) {
        DefaultSampleData.getCreatorProfile(username, ownProfile)
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Grid, 1 = Reels, 2 = Reposts, 3 = Tagged
    val creatorPosts = remember(posts, creator.username) {
        posts.filter { it.authorUsername.equals(creator.username, ignoreCase = true) }
    }
    val isFollowing = remember(creatorPosts) { creatorPosts.any { it.isFollowing } }

    val displayedGrid = remember(creatorPosts, selectedTab) {
        when (selectedTab) {
            1 -> creatorPosts.filter { it.isReel }
            2 -> creatorPosts.filter { it.isReposted }
            3 -> creatorPosts.filter { !it.isReel }
            else -> creatorPosts
        }
    }
    val rows = remember(displayedGrid) { displayedGrid.chunked(3) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // Top Header: <- @username [Verified] ...
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("creator_profile_back"),
            ) {
                IgBackIcon(
                    tint = colors.textPrimary,
                    size = 22.dp,
                    contentDescription = "Back",
                )
            }
            Spacer(Modifier.width(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = creator.username,
                    style = IgTypography.screenHeading,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (creator.isVerified) {
                    Spacer(Modifier.width(5.dp))
                    VerifiedBlueBadge(size = 16.dp)
                }
            }
            IconButton(onClick = {}) {
                IgMoreDotsIcon(
                    tint = colors.textPrimary,
                    size = 20.dp,
                    contentDescription = "More options",
                )
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // Avatar + Display Name + Stats
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AvatarView(
                        avatarUrl = creator.avatarUrl,
                        avatarKey = creator.avatarDrawableKey,
                        size = 82.dp,
                        showGradientRing = true,
                        ringStrokeWidth = 2.5.dp,
                        contentDescription = creator.displayName,
                    )
                    Spacer(Modifier.width(20.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = creator.displayName,
                            style = IgTypography.username,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            CreatorStatColumn(value = creator.postsCount, label = "posts")
                            CreatorStatColumn(value = creator.followersCount, label = "followers")
                            CreatorStatColumn(value = creator.followingCount, label = "following")
                        }
                    }
                }
            }

            // Bio + Follow & Message Action Buttons
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    if (creator.bio.isNotBlank() && !creator.bio.equals("N/A", ignoreCase = true)) {
                        Text(
                            text = creator.bio,
                            style = IgTypography.postCaption,
                            color = colors.textPrimary,
                        )
                    }
                    if (creator.privacyStatus.equals("Private", ignoreCase = true)) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Private account",
                            style = IgTypography.secondaryMeta,
                            color = colors.textSecondary,
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (isFollowing) colors.pillBackground else IgBoostBlue)
                                .clickable { onFollowClick(creator.username) }
                                .testTag("creator_follow_button"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (isFollowing) "Following" else "Follow",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isFollowing) colors.textPrimary else Color.White,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(colors.pillBackground)
                                .clickable { onMessageClick(creator.username) }
                                .testTag("creator_message_button"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Message",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary,
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            // 4-Tab Bar (Grid, Reels, Reposts, Tagged)
            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    CreatorTabButton(
                        selected = selectedTab == 0,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = 0 },
                    ) {
                        IgGridTabIcon(
                            active = selectedTab == 0,
                            tint = if (selectedTab == 0) colors.textPrimary else colors.textSecondary,
                            size = 22.dp,
                        )
                    }
                    CreatorTabButton(
                        selected = selectedTab == 1,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = 1 },
                    ) {
                        IgReelsSquircleIcon(
                            tint = if (selectedTab == 1) colors.textPrimary else colors.textSecondary,
                            size = 24.dp,
                        )
                    }
                    CreatorTabButton(
                        selected = selectedTab == 2,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = 2 },
                    ) {
                        IgRepostLoopIcon(
                            tint = if (selectedTab == 2) colors.textPrimary else colors.textSecondary,
                            size = 24.dp,
                        )
                    }
                    CreatorTabButton(
                        selected = selectedTab == 3,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = 3 },
                    ) {
                        IgTaggedPersonIcon(
                            tint = if (selectedTab == 3) colors.textPrimary else colors.textSecondary,
                            size = 24.dp,
                        )
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = colors.hairline)
            }

            if (displayedGrid.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No posts in this tab yet",
                            fontSize = 14.sp,
                            color = colors.textSecondary,
                        )
                    }
                }
            } else {
                items(rows.size) { rowIdx ->
                    val rowItems = rows[rowIdx]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 1.5.dp),
                        horizontalArrangement = Arrangement.spacedBy(1.5.dp),
                    ) {
                        for (colIdx in 0 until 3) {
                            val post = rowItems.getOrNull(colIdx)
                            if (post != null) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(0.68f)
                                        .background(Color.Black)
                                        .clickable {
                                            onPostClick(post.id, displayedGrid.map { it.id })
                                        }
                                        .testTag("creator_grid_item_${post.id}"),
                                ) {
                                    PostMediaImage(
                                        mediaUrl = post.mediaUrl,
                                        drawableKey = post.drawableKey,
                                        videoUrl = post.videoUrl,
                                        overlayTopCaption = post.overlayTopCaption,
                                        overlaySubtitle = post.overlaySubtitle,
                                        useLetterboxMemeStyle = post.mediaUrl == null && post.videoUrl.isNullOrBlank() && post.overlayTopCaption.isNotBlank(),
                                        contentDescription = post.caption,
                                        modifier = Modifier.fillMaxSize(),
                                    )

                                    if (post.isReel) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(7.dp),
                                        ) {
                                            IgReelsSquircleIcon(
                                                tint = Color.White,
                                                size = 18.dp,
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(30.dp)
                                            .align(Alignment.BottomCenter)
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                                                )
                                            ),
                                    )
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(horizontal = 6.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        IgEyeIcon(tint = Color.White, size = 14.dp)
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = post.viewsDisplay,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreatorStatColumn(value: String, label: String) {
    val colors = InstagramTheme.colors
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = value,
            style = IgTypography.profileStatValue,
            color = colors.textPrimary,
        )
        Text(
            text = label,
            style = IgTypography.profileStatLabel,
            color = colors.textPrimary,
        )
    }
}

@Composable
private fun CreatorTabButton(
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    val colors = InstagramTheme.colors
    Column(
        modifier = modifier
            .height(44.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Spacer(Modifier.height(2.dp))
        icon()
        Box(
            modifier = Modifier
                .fillMaxWidth(0.70f)
                .height(2.dp)
                .background(if (selected) colors.textPrimary else Color.Transparent),
        )
    }
}
