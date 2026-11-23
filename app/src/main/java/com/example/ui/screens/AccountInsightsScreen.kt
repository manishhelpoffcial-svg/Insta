package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AccountInsightsData
import com.example.data.model.MediaPost
import com.example.data.model.ProfileData
import com.example.ui.components.AccountInsightsEditModal
import com.example.ui.components.AccountOverviewLineChart
import com.example.ui.components.DualToneAgeBarRow
import com.example.ui.components.FollowerActiveTimesBarChart
import com.example.ui.components.FollowerGrowthChart
import com.example.ui.components.IgCommentBubbleIcon
import com.example.ui.components.IgEyeIcon
import com.example.ui.components.IgHeartStrokeIcon
import com.example.ui.components.IgReelsSquircleIcon
import com.example.ui.components.IgShareBoxUpIcon
import com.example.ui.components.IgSharePlaneIcon
import com.example.ui.components.InsightsUiColors
import com.example.ui.components.PostMediaImage
import com.example.ui.components.StackedContentBarRow
import com.example.ui.components.formatAxisLabels
import com.example.ui.components.rememberInsightsPalette
import com.example.ui.theme.IgBoostBlue
import com.example.ui.theme.IgInsightsMagenta
import com.example.ui.theme.IgInsightsViolet
import com.example.ui.theme.IgLinkBlue
import java.util.Locale

@Composable
fun AccountInsightsScreen(
    initialTab: Int,
    profile: ProfileData,
    posts: List<MediaPost>,
    accountInsights: AccountInsightsData,
    onBack: () -> Unit,
    onOpenReelInsights: (String) -> Unit,
    onUpdateAccountInsights: (AccountInsightsData) -> Unit,
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val palette = rememberInsightsPalette()

    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 2)) }
    var contentFilter by remember { mutableStateOf("All content") }
    var timeframeFilter by remember { mutableStateOf(accountInsights.timeframeLabel) }
    var showEditModal by remember { mutableStateOf(false) }
    var infoDialogTitle by remember { mutableStateOf<Pair<String, String>?>(null) }

    val ownPosts = remember(posts) {
        val own = posts.filter { it.isOwnPost }
        if (own.isNotEmpty()) own else posts
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // Top Header: <- Insights , share tray icon, (i), 3-dot edit
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("account_insights_back_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = palette.textPrimary,
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Insights",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "@${profile.username} Insights (${accountInsights.dateRange}): ${accountInsights.overviewViews} views · ${accountInsights.viewersTotal} viewers · ${accountInsights.audienceFollowersTotal} followers",
                            )
                        }
                        context.startActivity(Intent.createChooser(intent, "Share insights"))
                    },
                ) {
                    IgShareBoxUpIcon(tint = palette.textPrimary, size = 22.dp)
                }
                IconButton(
                    onClick = {
                        infoDialogTitle = "About Account Insights" to
                            "Metrics summarize performance across your reels, posts, stories, and audience over the selected timeframe. Tap the ⋮ menu to customize figures."
                    },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Insights info",
                        tint = palette.textPrimary,
                        modifier = Modifier.size(22.dp),
                    )
                }
                IconButton(
                    onClick = { showEditModal = true },
                    modifier = Modifier.testTag("account_insights_edit_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Edit account insights",
                        tint = palette.textPrimary,
                    )
                }
            }
        }

        // 3-Tab Bar: Overview | Content | Audience
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("Overview", "Content", "Audience").forEachIndexed { idx, title ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedTab = idx }
                        .padding(top = 8.dp)
                        .testTag("account_insights_tab_${title.lowercase()}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = if (selectedTab == idx) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (selectedTab == idx) palette.textPrimary else palette.textSecondary,
                    )
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.70f)
                            .height(2.dp)
                            .background(if (selectedTab == idx) palette.textPrimary else Color.Transparent),
                    )
                }
            }
        }
        HorizontalDivider(thickness = 0.6.dp, color = palette.border)

        when (selectedTab) {
            0 -> AccountOverviewTab(
                accountInsights = accountInsights,
                ownPosts = ownPosts,
                contentFilter = contentFilter,
                timeframeFilter = timeframeFilter,
                palette = palette,
                onCycleContentFilter = {
                    contentFilter = when (contentFilter) {
                        "All content" -> "Reels"
                        "Reels" -> "Posts"
                        else -> "All content"
                    }
                },
                onCycleTimeframe = {
                    timeframeFilter = when (timeframeFilter) {
                        "30 days" -> "7 days"
                        "7 days" -> "90 days"
                        else -> "30 days"
                    }
                },
                onSeeAllContent = { selectedTab = 1 },
                onOpenReelInsights = onOpenReelInsights,
                onShowInfo = { title, desc -> infoDialogTitle = title to desc },
            )

            1 -> AccountContentTab(
                ownPosts = ownPosts,
                contentFilter = contentFilter,
                timeframeFilter = timeframeFilter,
                palette = palette,
                onCycleContentFilter = {
                    contentFilter = when (contentFilter) {
                        "All content" -> "Reels"
                        "Reels" -> "Posts"
                        else -> "All content"
                    }
                },
                onCycleTimeframe = {
                    timeframeFilter = when (timeframeFilter) {
                        "30 days" -> "7 days"
                        "7 days" -> "90 days"
                        else -> "30 days"
                    }
                },
                onOpenReelInsights = onOpenReelInsights,
            )

            else -> AccountAudienceTab(
                accountInsights = accountInsights,
                timeframeFilter = timeframeFilter,
                palette = palette,
                onCycleTimeframe = {
                    timeframeFilter = when (timeframeFilter) {
                        "30 days" -> "7 days"
                        "7 days" -> "90 days"
                        else -> "30 days"
                    }
                },
                onShowInfo = { title, desc -> infoDialogTitle = title to desc },
            )
        }
    }

    if (showEditModal) {
        AccountInsightsEditModal(
            accountInsights = accountInsights,
            onDismiss = { showEditModal = false },
            onSave = onUpdateAccountInsights,
        )
    }

    infoDialogTitle?.let { (title, message) ->
        AlertDialog(
            onDismissRequest = { infoDialogTitle = null },
            containerColor = palette.card,
            titleContentColor = palette.textPrimary,
            textContentColor = palette.textSecondary,
            title = { Text(title, fontWeight = FontWeight.Bold, fontSize = 16.5.sp) },
            text = { Text(message, fontSize = 14.sp, lineHeight = 20.sp) },
            confirmButton = {
                TextButton(onClick = { infoDialogTitle = null }) {
                    Text("Close", color = IgBoostBlue, fontWeight = FontWeight.SemiBold)
                }
            },
        )
    }
}

@Composable
private fun AccountOverviewTab(
    accountInsights: AccountInsightsData,
    ownPosts: List<MediaPost>,
    contentFilter: String,
    timeframeFilter: String,
    palette: InsightsUiColors,
    onCycleContentFilter: () -> Unit,
    onCycleTimeframe: () -> Unit,
    onSeeAllContent: () -> Unit,
    onOpenReelInsights: (String) -> Unit,
    onShowInfo: (String, String) -> Unit,
) {
    var selectedMetricIdx by remember { mutableIntStateOf(0) } // 0=Views, 1=Net followers, 2=Interactions
    var interactionFilter by remember { mutableStateOf("All") }

    val topPosts = remember(ownPosts) {
        ownPosts.sortedByDescending { parseNumericCount(it.insights.views) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterDropdownChip(text = contentFilter, palette = palette, onClick = onCycleContentFilter)
                FilterDropdownChip(text = timeframeFilter, palette = palette, onClick = onCycleTimeframe)
            }
        }

        // Horizontal Metric Cards (Views | Net followers | Interactions)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OverviewMetricCard(
                    label = "Views",
                    value = accountInsights.overviewViews,
                    selected = selectedMetricIdx == 0,
                    palette = palette,
                    modifier = Modifier.testTag("overview_card_views"),
                    onClick = { selectedMetricIdx = 0 },
                )
                OverviewMetricCard(
                    label = "Net followers",
                    value = accountInsights.netFollowers,
                    selected = selectedMetricIdx == 1,
                    palette = palette,
                    modifier = Modifier.testTag("overview_card_followers"),
                    onClick = { selectedMetricIdx = 1 },
                )
                OverviewMetricCard(
                    label = "Interactions",
                    value = accountInsights.interactions,
                    selected = selectedMetricIdx == 2,
                    palette = palette,
                    modifier = Modifier.testTag("overview_card_interactions"),
                    onClick = { selectedMetricIdx = 2 },
                )
            }
        }

        // Breakdown subtext + Over-time Line Chart
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            ) {
                val subtext = when (selectedMetricIdx) {
                    0 -> String.format(
                        Locale.US,
                        "%.1f%% followers    %.1f%% non-followers",
                        accountInsights.followersPct,
                        accountInsights.nonFollowersPct,
                    )
                    1 -> "${accountInsights.newFollowers} follows    9 unfollows"
                    else -> "${accountInsights.interactionsLikes} likes    ${accountInsights.interactionsComments} comments    ${accountInsights.interactionsReposts} reposts"
                }
                Text(
                    text = subtext,
                    fontSize = 13.5.sp,
                    color = palette.textSecondary,
                )
                Spacer(Modifier.height(14.dp))

                val (points, topLbl, midLbl, botLbl) = when (selectedMetricIdx) {
                    0 -> {
                        val (maxL, midL) = formatAxisLabels(accountInsights.overviewViewsYAxisMax, accountInsights.overviewViews)
                        Quad(accountInsights.overviewViewsCurve, maxL, midL, "0")
                    }
                    1 -> Quad(accountInsights.overviewFollowersCurve, "2", "0", "-2")
                    else -> Quad(accountInsights.overviewInteractionsCurve, "60", "30", "0")
                }

                AccountOverviewLineChart(
                    points = points,
                    startDate = accountInsights.overviewChartStartDate,
                    midDate = accountInsights.overviewChartMidDate,
                    endDate = accountInsights.overviewChartEndDate,
                    topLabel = topLbl,
                    midLabel = midLbl,
                    bottomLabel = botLbl,
                    palette = palette,
                )
            }
        }

        // Views by content type
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                InsightsSectionTitle("Views by content type", palette) {
                    onShowInfo("Views by content type", "Shows how total views split across Reels, Posts, Stories, and Live videos by followers vs non-followers.")
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Viewers", fontSize = 14.5.sp, color = palette.textSecondary)
                    Text(
                        text = accountInsights.viewersTotal,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary,
                        style = TextStyle(fontFeatureSettings = "tnum"),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    LegendDotLabel("Followers", IgInsightsViolet, palette)
                    LegendDotLabel("Non-followers", IgInsightsMagenta, palette)
                }
                Spacer(Modifier.height(14.dp))
                val followerShare = (accountInsights.followersPct / 100.0).toFloat()
                StackedContentBarRow("Reels", accountInsights.reelsViews, 0.96f, followerShare, palette)
                Spacer(Modifier.height(12.dp))
                StackedContentBarRow("Posts", accountInsights.postsViews, 0f, 0f, palette)
                Spacer(Modifier.height(12.dp))
                StackedContentBarRow("Stories", accountInsights.storiesViews, 0f, 0f, palette)
                Spacer(Modifier.height(12.dp))
                StackedContentBarRow("Live videos", accountInsights.liveViews, 0f, 0f, palette)
            }
        }

        // Top content by views
        item {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Top content by views",
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.textPrimary,
                )
                Text(
                    text = "See all",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = IgLinkBlue,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onSeeAllContent)
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                        .testTag("overview_see_all_content"),
                )
            }
            Spacer(Modifier.height(12.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(topPosts, key = { it.id }) { post ->
                    Column(
                        modifier = Modifier
                            .width(110.dp)
                            .clickable { onOpenReelInsights(post.id) }
                            .testTag("top_content_card_${post.id}"),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(186.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black),
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
                                    IgReelsSquircleIcon(tint = Color.White, size = 18.dp)
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp)
                                    .align(Alignment.BottomCenter)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))
                                        )
                                    ),
                            )
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(horizontal = 7.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                IgEyeIcon(tint = Color.White, size = 14.dp)
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = post.viewsDisplay,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    style = TextStyle(fontFeatureSettings = "tnum"),
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = post.dateLabel,
                            fontSize = 11.5.sp,
                            color = palette.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        // Interactions by content type
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
            ) {
                InsightsSectionTitle("Interactions by content type", palette) {
                    onShowInfo("Interactions by content type", "Likes, comments, reposts, shares, and saves grouped by format.")
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf("All", "Likes", "Comments", "Reposts").forEach { pill ->
                        SelectablePillChip(
                            label = pill,
                            selected = interactionFilter == pill,
                            palette = palette,
                            onClick = { interactionFilter = pill },
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                val reelsInteractionCount = when (interactionFilter) {
                    "Likes" -> accountInsights.interactionsLikes
                    "Comments" -> accountInsights.interactionsComments
                    "Reposts" -> accountInsights.interactionsReposts
                    else -> accountInsights.interactionsReels
                }
                StackedContentBarRow("Reels", reelsInteractionCount, 0.92f, 0.14f, palette)
                Spacer(Modifier.height(12.dp))
                StackedContentBarRow("Posts", "0", 0f, 0f, palette)
                Spacer(Modifier.height(12.dp))
                StackedContentBarRow("Stories", "0", 0f, 0f, palette)
                Spacer(Modifier.height(12.dp))
                StackedContentBarRow("Live videos", "0", 0f, 0f, palette)
            }
        }

        // Profile activity
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                InsightsSectionTitle("Profile activity", palette) {
                    onShowInfo("Profile activity", "Actions people take when visiting @your profile, including visits, bio links, and address taps.")
                }
                Spacer(Modifier.height(12.dp))
                ProfileActivityRow(Icons.Outlined.AccountCircle, "Profile visits", accountInsights.profileVisits, palette)
                ProfileActivityRow(Icons.Outlined.Link, "Bio link taps", accountInsights.bioLinkTaps, palette)
                ProfileActivityRow(Icons.Outlined.LocationOn, "Business address taps", accountInsights.businessAddressTaps, palette)
            }
        }
    }
}

@Composable
private fun AccountContentTab(
    ownPosts: List<MediaPost>,
    contentFilter: String,
    timeframeFilter: String,
    palette: InsightsUiColors,
    onCycleContentFilter: () -> Unit,
    onCycleTimeframe: () -> Unit,
    onOpenReelInsights: (String) -> Unit,
) {
    val sortChips = listOf("Latest", "Views", "Viewers", "Follows", "Likes", "Comments", "Reposts", "Shares", "Saves")
    var selectedSort by remember { mutableStateOf("Latest") }

    val filteredPosts = remember(ownPosts, contentFilter, selectedSort) {
        val byType = when (contentFilter) {
            "Reels" -> ownPosts.filter { it.isReel }
            "Posts" -> ownPosts.filter { !it.isReel }
            else -> ownPosts
        }
        when (selectedSort) {
            "Views" -> byType.sortedByDescending { parseNumericCount(it.insights.views) }
            "Viewers" -> byType.sortedByDescending { parseNumericCount(it.insights.viewers) }
            "Follows" -> byType.sortedByDescending { parseNumericCount(it.insights.follows) }
            "Likes" -> byType.sortedByDescending { it.likesCount }
            "Comments" -> byType.sortedByDescending { it.commentsCount }
            "Reposts" -> byType.sortedByDescending { it.repostsCount }
            "Shares" -> byType.sortedByDescending { it.sharesCount }
            "Saves" -> byType.sortedByDescending { it.savesCount }
            else -> byType
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FilterDropdownChip(text = contentFilter, palette = palette, onClick = onCycleContentFilter)
                FilterDropdownChip(text = timeframeFilter, palette = palette, onClick = onCycleTimeframe)
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                sortChips.forEach { chip ->
                    SelectablePillChip(
                        label = chip,
                        selected = selectedSort == chip,
                        palette = palette,
                        onClick = { selectedSort = chip },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        items(filteredPosts, key = { it.id }) { post ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenReelInsights(post.id) }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .testTag("content_tab_row_${post.id}"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .width(56.dp)
                        .height(92.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black),
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
                                .padding(4.dp),
                        ) {
                            IgReelsSquircleIcon(tint = Color.White, size = 14.dp)
                        }
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.caption.ifBlank { post.overlayTopCaption.ifBlank { "Cinema Reel" } },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = palette.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = post.dateLabel,
                        fontSize = 12.sp,
                        color = palette.textSecondary,
                    )
                    Spacer(Modifier.height(7.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IgHeartStrokeIcon(filled = false, tint = palette.textSecondary, size = 13.dp)
                            Spacer(Modifier.width(4.dp))
                            Text("${post.likesCount}", fontSize = 12.sp, color = palette.textSecondary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IgCommentBubbleIcon(tint = palette.textSecondary, size = 13.dp)
                            Spacer(Modifier.width(4.dp))
                            Text("${post.commentsCount}", fontSize = 12.sp, color = palette.textSecondary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IgSharePlaneIcon(filled = false, tint = palette.textSecondary, size = 13.dp)
                            Spacer(Modifier.width(4.dp))
                            Text("${post.sharesCount}", fontSize = 12.sp, color = palette.textSecondary)
                        }
                    }
                }

                Spacer(Modifier.width(10.dp))

                Column(horizontalAlignment = Alignment.End) {
                    val metricVal = when (selectedSort) {
                        "Viewers" -> post.insights.viewers
                        "Follows" -> post.insights.follows
                        "Likes" -> post.likesCount.toString()
                        "Comments" -> post.commentsCount.toString()
                        "Reposts" -> post.repostsCount.toString()
                        "Shares" -> post.sharesCount.toString()
                        "Saves" -> post.savesCount.toString()
                        else -> post.insights.views
                    }
                    val metricLabel = if (selectedSort == "Latest") "Views" else selectedSort
                    Text(
                        text = metricVal,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary,
                        style = TextStyle(fontFeatureSettings = "tnum"),
                    )
                    Text(
                        text = metricLabel,
                        fontSize = 12.sp,
                        color = palette.textSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountAudienceTab(
    accountInsights: AccountInsightsData,
    timeframeFilter: String,
    palette: InsightsUiColors,
    onCycleTimeframe: () -> Unit,
    onShowInfo: (String, String) -> Unit,
) {
    var growthFilter by remember { mutableStateOf("Overall") }
    var locationFilter by remember { mutableStateOf("Countries") }
    var selectedDay by remember { mutableStateOf("M") }

    val growthPoints = when (growthFilter) {
        "Follows" -> accountInsights.followerGrowthFollows
        "Unfollows" -> accountInsights.followerGrowthUnfollows
        else -> accountInsights.followerGrowthOverall
    }
    val dayBars = accountInsights.activeTimesByDay[selectedDay]
        ?: accountInsights.activeTimesByDay.values.firstOrNull()
        ?: listOf(0.95f, 0.8f, 0.65f, 0.58f, 0.6f, 0.68f, 0.8f, 0.9f)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        // Followers Hero Section
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Followers",
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.textPrimary,
                    )
                    FilterDropdownChip(text = timeframeFilter, palette = palette, onClick = onCycleTimeframe)
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = accountInsights.audienceFollowersTotal,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary,
                    style = TextStyle(fontFeatureSettings = "tnum"),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = accountInsights.audienceChangeSubtitle,
                    fontSize = 13.5.sp,
                    color = palette.textSecondary,
                )
            }
        }

        // Follower growth over time
        item {
            Column {
                InsightsSectionTitle("Follower growth over time", palette) {
                    onShowInfo("Follower growth over time", "Daily net follower changes, new follows, and unfollows across the selected window.")
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Overall", "Follows", "Unfollows").forEach { tab ->
                        SelectablePillChip(
                            label = tab,
                            selected = growthFilter == tab,
                            palette = palette,
                            onClick = { growthFilter = tab },
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                FollowerGrowthChart(
                    points = growthPoints,
                    startDate = accountInsights.overviewChartStartDate,
                    midDate = accountInsights.overviewChartMidDate,
                    endDate = accountInsights.overviewChartEndDate,
                    palette = palette,
                )
            }
        }

        // Gender
        item {
            Column {
                InsightsSectionTitle("Gender", palette) {
                    onShowInfo("Gender", "Estimated gender distribution of your followers.")
                }
                Spacer(Modifier.height(12.dp))
                SimpleAudienceBarRow(
                    label = "Women",
                    pct = accountInsights.genderWomenPct,
                    barColor = IgInsightsMagenta,
                    palette = palette,
                )
                Spacer(Modifier.height(12.dp))
                SimpleAudienceBarRow(
                    label = "Men",
                    pct = accountInsights.genderMenPct,
                    barColor = Color(0xFFF48FB1),
                    palette = palette,
                )
            }
        }

        // Age range (Dual-tone Women + Men bars)
        item {
            Column {
                InsightsSectionTitle("Age range", palette) {
                    onShowInfo("Age range", "Age distribution of followers split by Women and Men.")
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    LegendDotLabel("Women", IgInsightsMagenta, palette)
                    LegendDotLabel("Men", Color(0xFFF48FB1), palette)
                }
                Spacer(Modifier.height(12.dp))
                accountInsights.ageRanges.forEach { age ->
                    DualToneAgeBarRow(
                        label = age.label,
                        totalPct = age.totalPct,
                        womenShareRatio = age.womenShareRatio,
                        palette = palette,
                    )
                    Spacer(Modifier.height(11.dp))
                }
            }
        }

        // Top locations (Countries / Cities)
        item {
            Column {
                InsightsSectionTitle("Top locations", palette) {
                    onShowInfo("Top locations", "Top countries and cities where your followers are located.")
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Countries", "Cities").forEach { mode ->
                        SelectablePillChip(
                            label = mode,
                            selected = locationFilter == mode,
                            palette = palette,
                            onClick = { locationFilter = mode },
                            modifier = Modifier.testTag("audience_loc_${mode.lowercase()}"),
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                val locList = if (locationFilter == "Countries") accountInsights.topCountries else accountInsights.topCities
                locList.forEach { loc ->
                    SimpleAudienceBarRow(
                        label = loc.label,
                        pct = loc.percentage,
                        barColor = IgInsightsMagenta,
                        palette = palette,
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
        }

        // Follower active times
        item {
            Column {
                InsightsSectionTitle("Follower active times", palette) {
                    onShowInfo("Follower active times", "Hours when your followers are most active on Instagram.")
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Based on your current time zone (IST)",
                    fontSize = 13.sp,
                    color = palette.textSecondary,
                )
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    listOf("Su", "M", "Tu", "W", "Th", "F", "Sa").forEach { day ->
                        val active = selectedDay == day
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (active) palette.pillSelected else Color.Transparent)
                                .then(
                                    if (active) Modifier.border(1.dp, palette.activeCardBorder.copy(alpha = 0.4f), CircleShape)
                                    else Modifier
                                )
                                .clickable { selectedDay = day }
                                .testTag("active_day_$day"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = day,
                                fontSize = 13.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                color = if (active) palette.textPrimary else palette.textSecondary,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                FollowerActiveTimesBarChart(bars = dayBars, palette = palette)
                Spacer(Modifier.height(18.dp))
                Text(
                    text = "When followers are most active",
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.textPrimary,
                )
                Spacer(Modifier.height(10.dp))
                PeakTimeRow("Mondays", "12 am - 3 am", palette)
                PeakTimeRow("Sundays", "12 am - 3 am", palette)
                PeakTimeRow("Wednesdays", "12 am - 3 am", palette)
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
private fun FilterDropdownChip(
    text: String,
    palette: InsightsUiColors,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(palette.card)
            .border(1.dp, palette.border, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = palette.textPrimary)
        Spacer(Modifier.width(4.dp))
        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = palette.textPrimary, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun OverviewMetricCard(
    label: String,
    value: String,
    selected: Boolean,
    palette: InsightsUiColors,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .width(128.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(palette.card)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) palette.activeCardBorder else palette.border,
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = palette.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = value,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = palette.textPrimary,
            style = TextStyle(fontFeatureSettings = "tnum"),
        )
    }
}

@Composable
private fun SelectablePillChip(
    label: String,
    selected: Boolean,
    palette: InsightsUiColors,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) palette.pillSelected else Color.Transparent)
            .then(
                if (!selected) Modifier.border(1.dp, palette.border, RoundedCornerShape(20.dp))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Medium,
            color = palette.textPrimary,
        )
    }
}

@Composable
private fun InsightsSectionTitle(
    title: String,
    palette: InsightsUiColors,
    onInfoClick: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            fontSize = 16.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = palette.textPrimary,
        )
        Spacer(Modifier.width(6.dp))
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = "$title info",
            tint = palette.textPrimary,
            modifier = Modifier
                .size(18.dp)
                .clickable(onClick = onInfoClick),
        )
    }
}

@Composable
private fun LegendDotLabel(label: String, color: Color, palette: InsightsUiColors) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 13.sp, color = palette.textSecondary)
    }
}

@Composable
private fun ProfileActivityRow(
    icon: ImageVector,
    label: String,
    value: String,
    palette: InsightsUiColors,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(palette.card)
                .border(1.dp, palette.border, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = palette.textPrimary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 15.sp, color = palette.textPrimary, modifier = Modifier.weight(1f))
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = palette.textPrimary,
            style = TextStyle(fontFeatureSettings = "tnum"),
        )
    }
}

@Composable
private fun SimpleAudienceBarRow(
    label: String,
    pct: Double,
    barColor: Color,
    palette: InsightsUiColors,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, fontSize = 14.sp, color = palette.textPrimary)
        Spacer(Modifier.height(5.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(palette.track),
            ) {
                val frac = (pct.toFloat() / 100f).coerceIn(0f, 1f)
                if (frac > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(frac.coerceAtLeast(0.012f))
                            .clip(RoundedCornerShape(4.dp))
                            .background(barColor),
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Text(
                text = String.format(Locale.US, "%.1f%%", pct),
                fontSize = 14.sp,
                color = palette.textPrimary,
                style = TextStyle(fontFeatureSettings = "tnum"),
            )
        }
    }
}

@Composable
private fun PeakTimeRow(day: String, timeRange: String, palette: InsightsUiColors) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(day, fontSize = 14.sp, color = palette.textPrimary)
        Text(timeRange, fontSize = 14.sp, color = palette.textSecondary)
    }
}

private fun parseNumericCount(raw: String): Long {
    val clean = raw.replace(",", "").trim().uppercase()
    return when {
        clean.endsWith("M") -> ((clean.dropLast(1).toDoubleOrNull() ?: 0.0) * 1_000_000).toLong()
        clean.endsWith("K") -> ((clean.dropLast(1).toDoubleOrNull() ?: 0.0) * 1_000).toLong()
        clean.endsWith("T") -> ((clean.dropLast(1).toDoubleOrNull() ?: 0.0) * 1_000).toLong()
        else -> clean.toLongOrNull() ?: 0L
    }
}
