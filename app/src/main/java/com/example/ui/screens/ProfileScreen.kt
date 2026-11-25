package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Link
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.example.R
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MediaPost
import com.example.data.model.ProfileData
import com.example.ui.components.AvatarView
import com.example.ui.components.CreatorDashedBadge
import com.example.ui.components.IgEyeIcon
import com.example.ui.components.IgGridTabIcon
import com.example.ui.components.IgPlusIcon
import com.example.ui.components.IgReelsSquircleIcon
import com.example.ui.components.IgRepostLoopIcon
import com.example.ui.components.IgTaggedPersonIcon
import com.example.ui.components.IgTwoLineMenuIcon
import com.example.ui.components.PostMediaImage
import com.example.ui.theme.IgLinkBlue
import com.example.ui.theme.IgNotifRed
import com.example.ui.theme.IgTypography
import com.example.ui.theme.InstagramTheme

@Composable
fun ProfileScreen(
    profile: ProfileData,
    posts: List<MediaPost>,
    onEditProfileClick: () -> Unit,
    onPostClick: (String, List<String>) -> Unit,
    onOpenDashboard: () -> Unit,
    onShareProfileClick: () -> Unit,
    onOpenMenuClick: () -> Unit = {},
) {
    val colors = InstagramTheme.colors
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Grid, 1 = Reels, 2 = Reposts, 3 = Tagged

    val ownPosts = remember(posts, profile.privacyStatus) {
        val own = posts.filter { it.isOwnPost }
        if (own.isNotEmpty() || profile.privacyStatus.isNotBlank()) own else posts
    }

    val displayedGrid = remember(posts, ownPosts, selectedTab) {
        when (selectedTab) {
            1 -> ownPosts.filter { it.isReel }
            2 -> posts.filter { it.isReposted }.ifEmpty { ownPosts.filter { it.repostsCount > 0 } }
            3 -> ownPosts.reversed()
            else -> ownPosts
        }
    }

    val rows = remember(displayedGrid) { displayedGrid.chunked(3) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding(),
    ) {
        // Top Bar: +, username v •, @ Threads, ≡
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = {}) {
                IgPlusIcon(
                    tint = colors.textPrimary,
                    size = 22.dp,
                    contentDescription = "Create",
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center,
            ) {
                if (profile.privacyStatus.equals("Private", ignoreCase = true)) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "Private account",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                }
                Text(
                    text = profile.username,
                    style = IgTypography.screenHeading,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(3.dp))
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Account menu",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(IgNotifRed),
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {}) {
                    Icon(
                        painter = painterResource(R.drawable.ic_ig_threads),
                        contentDescription = "Threads",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(24.dp),
                    )
                }
                IconButton(
                    onClick = onOpenMenuClick,
                    modifier = Modifier.testTag("profile_top_menu_button"),
                ) {
                    IgTwoLineMenuIcon(
                        size = 24.dp,
                        tint = colors.textPrimary,
                    )
                }
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // Avatar + Stats Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.size(84.dp)) {
                        AvatarView(
                            avatarUrl = profile.avatarUrl,
                            avatarKey = profile.avatarDrawableKey,
                            size = 82.dp,
                            contentDescription = profile.displayName,
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = 2.dp, y = 2.dp)
                                .size(24.dp)
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
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    }

                    Spacer(Modifier.width(20.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = profile.displayName,
                                style = IgTypography.username,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (profile.isVerified) {
                                Spacer(Modifier.width(5.dp))
                                CreatorDashedBadge(size = 16.dp)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            ProfileStatColumn(value = profile.postsCount, label = "posts")
                            ProfileStatColumn(value = profile.followersCount, label = "followers")
                            ProfileStatColumn(value = profile.followingCount, label = "following")
                        }
                    }
                }
            }

            // Bio + Banners + Your Dashboard + Action Buttons
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    if (profile.bio.isNotBlank()) {
                        Text(
                            text = profile.bio,
                            style = IgTypography.postCaption,
                            color = if (profile.bio == "N/A") colors.textSecondary else colors.textPrimary,
                        )
                    }

                    if (profile.externalUrl.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        val isRealLink = !profile.externalUrl.equals("N/A", ignoreCase = true)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = if (isRealLink) {
                                Modifier.clickable {
                                    runCatching {
                                        val normalizedUrl = if (profile.externalUrl.startsWith("http", ignoreCase = true)) {
                                            profile.externalUrl
                                        } else {
                                            "https://${profile.externalUrl}"
                                        }
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(normalizedUrl)))
                                    }
                                }
                            } else Modifier,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Link,
                                contentDescription = "External link",
                                tint = if (isRealLink) IgLinkBlue else colors.textSecondary,
                                modifier = Modifier.size(15.dp),
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = profile.externalUrl,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isRealLink) IgLinkBlue else colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    if (profile.privacyStatus.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Account status: ${profile.privacyStatus}",
                            fontSize = 12.sp,
                            color = colors.textSecondary,
                            modifier = Modifier.testTag("profile_privacy_status"),
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.cardBackground)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = "+ Add banners",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textSecondary,
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // Your dashboard card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.cardBackground)
                            .clickable(onClick = onOpenDashboard)
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .testTag("profile_dashboard_card"),
                    ) {
                        Text(
                            text = "Your dashboard",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = profile.dashboardSubtitle,
                            fontSize = 13.sp,
                            color = colors.textSecondary,
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    // Edit profile & Share profile buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(colors.pillBackground)
                                .clickable(onClick = onEditProfileClick)
                                .testTag("edit_profile_button"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Edit profile",
                                style = IgTypography.button,
                                color = colors.textPrimary,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(colors.pillBackground)
                                .clickable(onClick = onShareProfileClick)
                                .testTag("share_profile_button"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Share profile",
                                style = IgTypography.button,
                                color = colors.textPrimary,
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            // Content Tabs (Grid, Reels, Reposts, Tagged)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    ProfileTabButton(
                        selected = selectedTab == 0,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("profile_tab_grid"),
                        onClick = { selectedTab = 0 },
                    ) {
                        IgGridTabIcon(
                            active = selectedTab == 0,
                            tint = if (selectedTab == 0) colors.textPrimary else colors.textSecondary,
                            size = 22.dp,
                        )
                    }
                    ProfileTabButton(
                        selected = selectedTab == 1,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("profile_tab_reels"),
                        onClick = { selectedTab = 1 },
                    ) {
                        IgReelsSquircleIcon(
                            tint = if (selectedTab == 1) colors.textPrimary else colors.textSecondary,
                            size = 24.dp,
                        )
                    }
                    ProfileTabButton(
                        selected = selectedTab == 2,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("profile_tab_reposts"),
                        onClick = { selectedTab = 2 },
                    ) {
                        IgRepostLoopIcon(
                            tint = if (selectedTab == 2) colors.textPrimary else colors.textSecondary,
                            size = 24.dp,
                        )
                    }
                    ProfileTabButton(
                        selected = selectedTab == 3,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("profile_tab_tagged"),
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

            // 3-Column Profile Media Grid
            if (rows.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = if (profile.privacyStatus.equals("Private", ignoreCase = true)) {
                                "This account is private"
                            } else {
                                "No posts yet"
                            },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary,
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
                                        .testTag("profile_grid_item_${post.id}"),
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

                                    // Top-right Reel badge
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

                                    // Bottom gradient + eye icon & view count
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
private fun ProfileStatColumn(value: String, label: String) {
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
private fun ProfileTabButton(
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
