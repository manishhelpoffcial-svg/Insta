package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DemographicItem
import com.example.data.model.ImpactMetric
import com.example.data.model.MediaPost
import com.example.data.model.ProfileData
import com.example.data.model.ReelInsights
import com.example.data.model.SourceShare
import com.example.data.network.ApifyRepository
import com.example.ui.components.IgBookmarkStrokeIcon
import com.example.ui.components.IgCommentBubbleIcon
import com.example.ui.components.IgHeartStrokeIcon
import com.example.ui.components.IgRepostLoopIcon
import com.example.ui.components.IgSharePlaneIcon
import com.example.ui.components.InsightsUiColors
import com.example.ui.components.PostMediaImage
import com.example.ui.components.ReelInsightsEditModal
import com.example.ui.components.ShimmerSummaryAndGraphPlaceholder
import com.example.ui.components.ViewsOverTimeLineChart
import com.example.ui.components.formatAxisLabels
import com.example.ui.components.rememberInsightsPalette
import com.example.ui.theme.IgInsightsMagenta
import com.example.ui.theme.IgInsightsViolet
import com.example.ui.theme.InstagramTheme
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun ReelInsightsScreen(
    post: MediaPost,
    profile: ProfileData,
    isEditMode: Boolean,
    onToggleEditMode: () -> Unit,
    onBack: () -> Unit,
    onOpenAccountInsights: () -> Unit,
    onUpdateInsights: ((ReelInsights) -> ReelInsights) -> Unit,
    onUpdateImpact: (impactId: String, percentage: String?, status: String?) -> Unit,
    onUpdateTopSource: (index: Int, label: String?, percentage: Double?) -> Unit,
    onUpdateDemographic: (category: String, index: Int, label: String?, percentage: Double?) -> Unit,
    onUpdateLikeCurvePoint: (index: Int, value: Float) -> Unit,
    onUpdateProfile: ((ProfileData) -> ProfileData) -> Unit,
    onUpdatePostDetails: (audioTitle: String, likedByUsername: String) -> Unit = { _, _ -> },
) {
    BackHandler(onBack = onBack)
    val palette = rememberInsightsPalette()
    val colors = InstagramTheme.colors
    val insights = post.insights
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Overview, 1 = Engagement, 2 = Audience
    var selectedAudienceFilter by remember { mutableStateOf("Age") } // "Age", "Country", "Gender"
    var showEditModal by remember { mutableStateOf(false) }
    var showShimmer by remember(post.id) { mutableStateOf(true) }

    LaunchedEffect(post.id) {
        delay(600L)
        showShimmer = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        // Top Header: <- Reel insights , trend icon, ⋮
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
                    modifier = Modifier.testTag("insights_back_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = palette.textPrimary,
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Reel insights",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.textPrimary,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onOpenAccountInsights,
                    modifier = Modifier.testTag("insights_account_trends_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = "Account Insights",
                        tint = palette.textPrimary,
                        modifier = Modifier.size(24.dp),
                    )
                }
                IconButton(
                    onClick = { showEditModal = true },
                    modifier = Modifier.testTag("insights_more_menu"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Edit Reel Insights",
                        tint = if (isEditMode || showEditModal) IgInsightsMagenta else palette.textPrimary,
                    )
                }
            }
        }

        // Subtle Edit Mode Banner
        if (isEditMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(IgInsightsMagenta.copy(alpha = 0.12f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Edit Mode · Changes auto-save live",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = IgInsightsMagenta,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(IgInsightsMagenta)
                        .clickable(onClick = onToggleEditMode)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("insights_done_editing_chip"),
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Done", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            // Centered Reel Thumbnail Preview (132dp x 234dp, 9:16 sleek aspect, object-cover crop)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .width(132.dp)
                        .height(234.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black),
                ) {
                    PostMediaImage(
                        mediaUrl = post.mediaUrl,
                        drawableKey = post.drawableKey,
                        videoUrl = post.videoUrl,
                        overlayTopCaption = post.overlayTopCaption,
                        overlaySubtitle = post.overlaySubtitle,
                        useLetterboxMemeStyle = false,
                        contentDescription = "Reel preview",
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            // 5 Summary Metric Icons Row (tighter max 330dp centered container, 22dp icons, 1.9dp stroke)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    modifier = Modifier
                        .widthIn(max = 330.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    InsightMiniStat(
                        value = insights.likes,
                        isEditMode = isEditMode,
                        onValueChange = { v -> onUpdateInsights { it.copy(likes = v) } },
                    ) {
                        IgHeartStrokeIcon(filled = false, tint = palette.textPrimary, size = 22.dp, strokeWidthDp = 1.9.dp)
                    }
                    InsightMiniStat(
                        value = insights.comments,
                        isEditMode = isEditMode,
                        onValueChange = { v -> onUpdateInsights { it.copy(comments = v) } },
                    ) {
                        IgCommentBubbleIcon(tint = palette.textPrimary, size = 22.dp, strokeWidthDp = 1.9.dp)
                    }
                    InsightMiniStat(
                        value = insights.reposts,
                        isEditMode = isEditMode,
                        onValueChange = { v -> onUpdateInsights { it.copy(reposts = v) } },
                    ) {
                        IgRepostLoopIcon(tint = palette.textPrimary, size = 22.dp, strokeWidthDp = 1.9.dp)
                    }
                    InsightMiniStat(
                        value = insights.shares,
                        isEditMode = isEditMode,
                        onValueChange = { v -> onUpdateInsights { it.copy(shares = v) } },
                    ) {
                        IgSharePlaneIcon(filled = false, tint = palette.textPrimary, size = 22.dp, strokeWidthDp = 1.9.dp)
                    }
                    InsightMiniStat(
                        value = insights.saves,
                        isEditMode = isEditMode,
                        onValueChange = { v -> onUpdateInsights { it.copy(saves = v) } },
                    ) {
                        IgBookmarkStrokeIcon(filled = false, tint = palette.textPrimary, size = 22.dp, strokeWidthDp = 1.9.dp)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // 3 Underline Tabs: Overview | Engagement | Audience
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("Overview", "Engagement", "Audience").forEachIndexed { idx, title ->
                    val isSelected = selectedTab == idx
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTab = idx }
                            .padding(top = 10.dp)
                            .testTag("insights_tab_${title.lowercase()}"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSelected) palette.textPrimary else palette.axisText,
                        )
                        Spacer(Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.68f)
                                .height(2.dp)
                                .background(if (isSelected) palette.textPrimary else Color.Transparent),
                        )
                    }
                }
            }
            HorizontalDivider(thickness = 0.6.dp, color = palette.border)

            when (selectedTab) {
                0 -> OverviewTabContent(
                    insights = insights,
                    palette = palette,
                    showShimmer = showShimmer,
                    isEditMode = isEditMode,
                    onUpdateInsights = onUpdateInsights,
                    onUpdateImpact = onUpdateImpact,
                    onUpdateTopSource = onUpdateTopSource,
                )
                1 -> EngagementTabContent(
                    post = post,
                    insights = insights,
                    isEditMode = isEditMode,
                    onUpdateInsights = onUpdateInsights,
                    onUpdateLikeCurvePoint = onUpdateLikeCurvePoint,
                )
                else -> AudienceTabContent(
                    insights = insights,
                    selectedFilter = selectedAudienceFilter,
                    onSelectFilter = { selectedAudienceFilter = it },
                    isEditMode = isEditMode,
                    onUpdateInsights = onUpdateInsights,
                    onUpdateDemographic = onUpdateDemographic,
                )
            }

            if (isEditMode) {
                ProfileStatsEditSection(
                    profile = profile,
                    onUpdateProfile = onUpdateProfile,
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showEditModal) {
        val currentLiker = post.likedByText
            .removePrefix("Liked by ")
            .removeSuffix(" and others")
            .trim()
            .takeIf { it.isNotBlank() && !it.endsWith("people", ignoreCase = true) && it.firstOrNull()?.isDigit() != true }
            .orEmpty()
        ReelInsightsEditModal(
            insights = insights,
            audioTitle = post.subtitle,
            likedByUsername = currentLiker,
            isInlineEditMode = isEditMode,
            onToggleInlineEditMode = onToggleEditMode,
            onDismiss = { showEditModal = false },
            onSave = { updated -> onUpdateInsights { updated } },
            onSavePostMetadata = onUpdatePostDetails,
        )
    }
}

@Composable
private fun InsightMiniStat(
    value: String,
    isEditMode: Boolean,
    onValueChange: (String) -> Unit,
    icon: @Composable () -> Unit,
) {
    val palette = rememberInsightsPalette()
    val formattedValue = remember(value) { ApifyRepository.formatInsightsIconCount(value) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        icon()
        Spacer(Modifier.height(5.dp))
        if (isEditMode) {
            InlineEditableNumber(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    color = palette.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.SansSerif,
                    textAlign = TextAlign.Center,
                    fontFeatureSettings = "tnum",
                ),
                modifier = Modifier.width(52.dp),
            )
        } else {
            Text(
                text = formattedValue,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.SansSerif,
                color = palette.textPrimary,
                style = TextStyle(fontFeatureSettings = "tnum"),
            )
        }
    }
}

@Composable
private fun OverviewTabContent(
    insights: ReelInsights,
    palette: InsightsUiColors,
    showShimmer: Boolean,
    isEditMode: Boolean,
    onUpdateInsights: ((ReelInsights) -> ReelInsights) -> Unit,
    onUpdateImpact: (String, String?, String?) -> Unit,
    onUpdateTopSource: (Int, String?, Double?) -> Unit,
) {
    var viewsSeries by remember { mutableStateOf("All") }

    if (showShimmer) {
        ShimmerSummaryAndGraphPlaceholder(palette = palette)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 18.dp),
    ) {
        if (!showShimmer) {
            SectionHeaderWithInfo(title = "Summary")
            Spacer(Modifier.height(14.dp))

            // 2x2 Rounded Summary Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SummaryCard(
                    label = "Views",
                    value = insights.views,
                    palette = palette,
                    isEditMode = isEditMode,
                    onValueChange = { v -> onUpdateInsights { it.copy(views = v) } },
                    modifier = Modifier.weight(1f),
                )
                SummaryCard(
                    label = "Viewers",
                    value = insights.viewers,
                    palette = palette,
                    isEditMode = isEditMode,
                    onValueChange = { v -> onUpdateInsights { it.copy(viewers = v) } },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SummaryCard(
                    label = "Average watch time",
                    value = insights.averageWatchTime,
                    palette = palette,
                    isEditMode = isEditMode,
                    onValueChange = { v -> onUpdateInsights { it.copy(averageWatchTime = v) } },
                    modifier = Modifier.weight(1f),
                )
                SummaryCard(
                    label = "Follows",
                    value = insights.follows,
                    palette = palette,
                    isEditMode = isEditMode,
                    onValueChange = { v -> onUpdateInsights { it.copy(follows = v, engagementFollows = v) } },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(26.dp))
            SectionHeaderWithInfo(title = "Views over time")
            Spacer(Modifier.height(12.dp))

            // Filter Pills: All | Followers | Non-followers
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                listOf("All", "Followers", "Non-followers").forEach { series ->
                    val selected = viewsSeries == series
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (selected) palette.pillSelected else Color.Transparent)
                            .then(
                                if (!selected) Modifier.border(1.dp, palette.border, RoundedCornerShape(20.dp))
                                else Modifier
                            )
                            .clickable { viewsSeries = series }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("views_over_time_pill_${series.lowercase()}"),
                    ) {
                        Text(
                            text = series,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = palette.textPrimary,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        val chartPoints = when (viewsSeries) {
            "Followers" -> insights.viewsOverTimeFollowers
            "Non-followers" -> insights.viewsOverTimeNonFollowers
            else -> insights.viewsOverTimeAll
        }
        if (chartPoints.isEmpty()) {
            Text(
                text = "N/A",
                fontSize = 13.5.sp,
                color = palette.textSecondary,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        } else {
            val (yMaxLbl, yMidLbl) = formatAxisLabels(insights.viewsYAxisMax, insights.views)
            ViewsOverTimeLineChart(
                points = chartPoints,
                startDate = insights.viewsStartDate,
                midDate = insights.viewsMidDate,
                endDate = insights.viewsEndDate,
                yMaxLabel = yMaxLbl,
                yMidLabel = yMidLbl,
                palette = palette,
            )
        }

        Spacer(Modifier.height(26.dp))
    }

    SectionHeaderWithInfo(title = "What impacts your views")
    Spacer(Modifier.height(4.dp))
    Text(
        text = "Rates are listed in order of importance to reach.",
        fontSize = 13.sp,
        color = palette.textSecondary,
    )
    Spacer(Modifier.height(14.dp))

    if (insights.impacts.isEmpty()) {
        Text(
            text = "N/A",
            fontSize = 13.5.sp,
            color = palette.textSecondary,
            modifier = Modifier.padding(vertical = 6.dp),
        )
    } else {
        insights.impacts.forEach { metric ->
            ImpactRow(
                metric = metric,
                isEditMode = isEditMode,
                onPercentageChange = { pct -> onUpdateImpact(metric.id, pct, null) },
                onToggleStatus = {
                    val next = if (metric.status == "Higher") "Lower" else "Higher"
                    onUpdateImpact(metric.id, null, next)
                },
            )
        }
    }

        Spacer(Modifier.height(22.dp))
        SectionHeaderWithInfo(title = "How long people watched your reel")
        Spacer(Modifier.height(12.dp))

        if (isEditMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Show retention curve",
                    fontSize = 13.sp,
                    color = palette.textSecondary,
                )
                Switch(
                    checked = !insights.retentionUnavailable,
                    onCheckedChange = { show ->
                        onUpdateInsights { it.copy(retentionUnavailable = !show) }
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = IgInsightsMagenta),
                )
            }
        }

        RetentionCard(
            unavailable = insights.retentionUnavailable,
            curve = insights.retentionCurve,
            durationLabel = insights.durationLabel,
        )

        Spacer(Modifier.height(26.dp))
        SectionHeaderWithInfo(title = "Top sources of views")
        Spacer(Modifier.height(14.dp))

        if (insights.topSources.isEmpty()) {
            Text(
                text = "N/A",
                fontSize = 13.5.sp,
                color = palette.textSecondary,
                modifier = Modifier.padding(vertical = 6.dp),
            )
        } else {
            insights.topSources.forEachIndexed { idx, source ->
                SourceProgressRow(
                    source = source,
                    barColor = IgInsightsMagenta,
                    isEditMode = isEditMode,
                    onLabelChange = { newLabel -> onUpdateTopSource(idx, newLabel, null) },
                    onPercentChange = { newPct -> onUpdateTopSource(idx, null, newPct) },
                )
                Spacer(Modifier.height(14.dp))
            }
        }
    }
}

@Composable
private fun SummaryCard(
    label: String,
    value: String,
    palette: InsightsUiColors,
    isEditMode: Boolean,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(palette.card)
            .border(0.6.dp, palette.border, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = label,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = FontFamily.SansSerif,
            color = palette.textSecondary,
            maxLines = 1,
        )
        Spacer(Modifier.height(6.dp))
        if (isEditMode) {
            InlineEditableNumber(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    fontSize = 21.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = (-0.2).sp,
                    color = palette.textPrimary,
                    fontFeatureSettings = "tnum",
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Text(
                text = value,
                fontSize = 21.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = (-0.2).sp,
                color = palette.textPrimary,
                style = TextStyle(fontFeatureSettings = "tnum"),
            )
        }
    }
}

@Composable
private fun ImpactRow(
    metric: ImpactMetric,
    isEditMode: Boolean,
    onPercentageChange: (String) -> Unit,
    onToggleStatus: () -> Unit,
) {
    val colors = InstagramTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(colors.cardBackground),
            contentAlignment = Alignment.Center,
        ) {
            when (metric.iconType) {
                "skip" -> Icon(Icons.Outlined.Schedule, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(22.dp))
                "share" -> IgSharePlaneIcon(filled = false, tint = colors.textPrimary, size = 20.dp)
                "like" -> IgHeartStrokeIcon(filled = false, tint = colors.textPrimary, size = 22.dp)
                "save" -> IgBookmarkStrokeIcon(filled = false, tint = colors.textPrimary, size = 22.dp)
                "repost" -> IgRepostLoopIcon(tint = colors.textPrimary, size = 22.dp)
                else -> IgCommentBubbleIcon(tint = colors.textPrimary, size = 21.dp)
            }
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = metric.label,
            fontSize = 15.5.sp,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )

        Column(horizontalAlignment = Alignment.End) {
            if (isEditMode) {
                InlineEditableNumber(
                    value = metric.percentage,
                    onValueChange = onPercentageChange,
                    textStyle = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textPrimary,
                        textAlign = TextAlign.End,
                    ),
                    modifier = Modifier.width(72.dp),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = metric.status,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = IgInsightsMagenta,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(IgInsightsMagenta.copy(alpha = 0.12f))
                        .clickable(onClick = onToggleStatus)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            } else {
                Text(
                    text = metric.percentage,
                    fontSize = 15.5.sp,
                    color = colors.textPrimary,
                )
                Text(
                    text = metric.status,
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun RetentionCard(
    unavailable: Boolean,
    curve: List<Float>,
    durationLabel: String,
) {
    val colors = InstagramTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(175.dp)
            .padding(vertical = 8.dp),
    ) {
        if (unavailable) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("--", color = colors.textSecondary, fontSize = 12.sp)
                    Spacer(Modifier.width(12.dp))
                    HorizontalDivider(color = colors.hairline.copy(alpha = 0.5f))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("--", color = colors.textSecondary, fontSize = 12.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Data is currently unavailable.",
                        color = colors.textSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("--", color = colors.textSecondary, fontSize = 12.sp)
                        Spacer(Modifier.width(12.dp))
                        HorizontalDivider(color = colors.hairline.copy(alpha = 0.5f))
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("--", color = colors.textSecondary, fontSize = 12.sp)
                        Text("--", color = colors.textSecondary, fontSize = 12.sp)
                        Text("--", color = colors.textSecondary, fontSize = 12.sp)
                    }
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(start = 36.dp, top = 8.dp, bottom = 8.dp)) {
                        val w = size.width
                        val h = size.height
                        listOf(0f, 0.5f, 1f).forEach { frac ->
                            drawLine(
                                color = colors.hairline.copy(alpha = 0.5f),
                                start = Offset(0f, h * frac),
                                end = Offset(w, h * frac),
                                strokeWidth = 1.dp.toPx(),
                            )
                        }
                        if (curve.size >= 2) {
                            val path = Path()
                            curve.forEachIndexed { idx, v ->
                                val x = w * (idx.toFloat() / (curve.size - 1))
                                val y = h * (1f - (v / 100f).coerceIn(0f, 1f))
                                if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                            drawPath(
                                path = path,
                                color = IgInsightsMagenta,
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("100%", color = colors.textSecondary, fontSize = 11.sp)
                        Text("50%", color = colors.textSecondary, fontSize = 11.sp)
                        Text("0%", color = colors.textSecondary, fontSize = 11.sp)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 36.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("0:00", color = colors.textSecondary, fontSize = 11.sp)
                    Text(durationLabel, color = colors.textSecondary, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun EngagementTabContent(
    post: MediaPost,
    insights: ReelInsights,
    isEditMode: Boolean,
    onUpdateInsights: ((ReelInsights) -> ReelInsights) -> Unit,
    onUpdateLikeCurvePoint: (Int, Float) -> Unit,
) {
    val colors = InstagramTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 18.dp),
    ) {
        SectionHeaderWithInfo(title = "Actions after viewing")
        Spacer(Modifier.height(10.dp))
        KeyValueRow(
            label = "Profile visits",
            value = insights.profileVisits,
            isEditMode = isEditMode,
            onValueChange = { v -> onUpdateInsights { it.copy(profileVisits = v) } },
        )
        KeyValueRow(
            label = "Follows",
            value = insights.engagementFollows,
            isEditMode = isEditMode,
            onValueChange = { v -> onUpdateInsights { it.copy(engagementFollows = v, follows = v) } },
        )

        Spacer(Modifier.height(22.dp))
        SectionHeaderWithInfo(title = "Interactions")
        Spacer(Modifier.height(10.dp))
        KeyValueRow(
            label = "Likes",
            value = insights.likes,
            isEditMode = isEditMode,
            onValueChange = { v -> onUpdateInsights { it.copy(likes = v) } },
        )
        KeyValueRow(
            label = "Comments",
            value = insights.comments,
            isEditMode = isEditMode,
            onValueChange = { v -> onUpdateInsights { it.copy(comments = v) } },
        )
        KeyValueRow(
            label = "Reposts",
            value = insights.reposts,
            isEditMode = isEditMode,
            onValueChange = { v -> onUpdateInsights { it.copy(reposts = v) } },
        )
        KeyValueRow(
            label = "Shares",
            value = insights.shares,
            isEditMode = isEditMode,
            onValueChange = { v -> onUpdateInsights { it.copy(shares = v) } },
        )
        KeyValueRow(
            label = "Saves",
            value = insights.saves,
            isEditMode = isEditMode,
            onValueChange = { v -> onUpdateInsights { it.copy(saves = v) } },
        )

        Spacer(Modifier.height(24.dp))
        SectionHeaderWithInfo(title = "When people liked your reel")
        Spacer(Modifier.height(16.dp))

        // Centered reel thumbnail with play triangle
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .width(114.dp)
                    .height(190.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                PostMediaImage(
                    mediaUrl = post.mediaUrl,
                    drawableKey = post.drawableKey,
                    overlayTopCaption = post.overlayTopCaption.ifBlank { "Never underestimate Beth. 🔥" },
                    overlaySubtitle = post.overlaySubtitle.ifBlank { "My sister-in-law." },
                    useLetterboxMemeStyle = post.mediaUrl == null,
                    modifier = Modifier.fillMaxSize(),
                )
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(44.dp),
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Magenta Spike Chart (0%, 15%, 30% | 0:00 -> durationLabel)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(bottom = 22.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("30%", color = colors.textSecondary, fontSize = 12.sp)
                Text("15%", color = colors.textSecondary, fontSize = 12.sp)
                Text("0%", color = colors.textSecondary, fontSize = 12.sp)
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                ) {
                    val w = size.width
                    val h = size.height
                    listOf(0f, 0.5f, 1f).forEach { frac ->
                        drawLine(
                            color = colors.hairline.copy(alpha = 0.55f),
                            start = Offset(0f, h * frac),
                            end = Offset(w, h * frac),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }

                    val pts = insights.likeCurvePoints
                    if (pts.size >= 2) {
                        val path = Path()
                        pts.forEachIndexed { idx, value ->
                            val x = w * (idx.toFloat() / (pts.size - 1))
                            val y = h * (1f - (value / 30f).coerceIn(0f, 1f))
                            if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(
                            path = path,
                            color = IgInsightsMagenta,
                            style = Stroke(
                                width = 3.2.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round,
                            ),
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("0:00", color = colors.textSecondary, fontSize = 12.sp)
                    if (isEditMode) {
                        InlineEditableNumber(
                            value = insights.durationLabel,
                            onValueChange = { d -> onUpdateInsights { it.copy(durationLabel = d) } },
                            textStyle = TextStyle(
                                color = colors.textPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.End,
                            ),
                            modifier = Modifier.width(56.dp),
                        )
                    } else {
                        Text(insights.durationLabel, color = colors.textSecondary, fontSize = 12.sp)
                    }
                }
            }
        }

        // Editable Like Curve Sliders in Edit Mode
        if (isEditMode) {
            Spacer(Modifier.height(14.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.cardBackground)
                    .padding(12.dp),
            ) {
                Text(
                    text = "Adjust Like Curve Peaks (0% - 30%)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(8.dp))
                val keyIndices = listOf(0, 1, 2, 5, 8, insights.likeCurvePoints.lastIndex)
                keyIndices.distinct().forEach { ptIdx ->
                    val valFloat = insights.likeCurvePoints.getOrElse(ptIdx) { 0f }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "Pt ${ptIdx + 1}",
                            fontSize = 12.sp,
                            color = colors.textSecondary,
                            modifier = Modifier.width(40.dp),
                        )
                        Slider(
                            value = valFloat,
                            onValueChange = { onUpdateLikeCurvePoint(ptIdx, it) },
                            valueRange = 0f..30f,
                            colors = SliderDefaults.colors(
                                thumbColor = IgInsightsMagenta,
                                activeTrackColor = IgInsightsMagenta,
                            ),
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "${valFloat.toInt()}%",
                            fontSize = 12.sp,
                            color = colors.textPrimary,
                            modifier = Modifier.width(34.dp),
                            textAlign = TextAlign.End,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AudienceTabContent(
    insights: ReelInsights,
    selectedFilter: String,
    onSelectFilter: (String) -> Unit,
    isEditMode: Boolean,
    onUpdateInsights: ((ReelInsights) -> ReelInsights) -> Unit,
    onUpdateDemographic: (String, Int, String?, Double?) -> Unit,
) {
    val colors = InstagramTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 18.dp),
    ) {
        SectionHeaderWithInfo(title = "Who viewed your reel")
        Spacer(Modifier.height(14.dp))

        PercentageBarRow(
            label = "Followers",
            percentage = insights.followersViewPercent,
            barColor = IgInsightsViolet,
            isEditMode = isEditMode,
            labelEditable = false,
            onLabelChange = {},
            onPercentChange = { pct ->
                onUpdateInsights {
                    it.copy(
                        followersViewPercent = pct.coerceIn(0.0, 100.0),
                        nonFollowersViewPercent = (100.0 - pct).coerceIn(0.0, 100.0),
                    )
                }
            },
        )
        Spacer(Modifier.height(14.dp))
        PercentageBarRow(
            label = "Non-followers",
            percentage = insights.nonFollowersViewPercent,
            barColor = IgInsightsViolet,
            isEditMode = isEditMode,
            labelEditable = false,
            onLabelChange = {},
            onPercentChange = { pct ->
                onUpdateInsights {
                    it.copy(
                        nonFollowersViewPercent = pct.coerceIn(0.0, 100.0),
                        followersViewPercent = (100.0 - pct).coerceIn(0.0, 100.0),
                    )
                }
            },
        )

        Spacer(Modifier.height(24.dp))
        SectionHeaderWithInfo(title = "Audience details")
        Spacer(Modifier.height(14.dp))

        // Age, Country, Gender Filter Pills
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf("Age", "Country", "Gender").forEach { filter ->
                val selected = selectedFilter == filter
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (selected) colors.pillBackground else Color.Transparent)
                        .then(
                            if (!selected) Modifier.border(1.dp, colors.hairline, RoundedCornerShape(20.dp))
                            else Modifier
                        )
                        .clickable { onSelectFilter(filter) }
                        .padding(horizontal = 18.dp, vertical = 9.dp)
                        .testTag("audience_pill_${filter.lowercase()}"),
                ) {
                    Text(
                        text = filter,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textPrimary,
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        val itemsList: List<DemographicItem> = when (selectedFilter) {
            "Age" -> insights.ageBreakdown
            "Country" -> insights.countryBreakdown
            else -> insights.genderBreakdown
        }

        itemsList.forEachIndexed { idx, item ->
            val barColor = if (selectedFilter == "Gender" && idx == 1) IgInsightsViolet else IgInsightsMagenta
            PercentageBarRow(
                label = item.label,
                percentage = item.percentage,
                barColor = barColor,
                isEditMode = isEditMode,
                labelEditable = true,
                onLabelChange = { newLabel -> onUpdateDemographic(selectedFilter, idx, newLabel, null) },
                onPercentChange = { newPct -> onUpdateDemographic(selectedFilter, idx, null, newPct) },
            )
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
private fun SourceProgressRow(
    source: SourceShare,
    barColor: Color,
    isEditMode: Boolean,
    onLabelChange: (String) -> Unit,
    onPercentChange: (Double) -> Unit,
) {
    PercentageBarRow(
        label = source.label,
        percentage = source.percentage,
        barColor = barColor,
        isEditMode = isEditMode,
        labelEditable = isEditMode,
        onLabelChange = onLabelChange,
        onPercentChange = onPercentChange,
    )
}

@Composable
private fun PercentageBarRow(
    label: String,
    percentage: Double,
    barColor: Color,
    isEditMode: Boolean,
    labelEditable: Boolean,
    onLabelChange: (String) -> Unit,
    onPercentChange: (Double) -> Unit,
) {
    val colors = InstagramTheme.colors
    val animatedFraction by animateFloatAsState(
        targetValue = (percentage.toFloat() / 100f).coerceIn(0f, 1f),
        label = "bar_progress",
    )
    var percentDraft by remember(percentage, isEditMode) {
        mutableStateOf(String.format(Locale.US, "%.1f", percentage))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
    ) {
        if (isEditMode && labelEditable) {
            InlineEditableNumber(
                value = label,
                onValueChange = onLabelChange,
                textStyle = TextStyle(fontSize = 15.sp, color = colors.textPrimary),
                modifier = Modifier.fillMaxWidth(0.75f),
            )
        } else {
            Text(
                text = label,
                fontSize = 15.sp,
                color = colors.textPrimary,
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(colors.trackBackground),
            ) {
                if (animatedFraction > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedFraction.coerceAtLeast(0.008f))
                            .clip(RoundedCornerShape(4.dp))
                            .background(barColor),
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            if (isEditMode) {
                InlineEditableNumber(
                    value = percentDraft,
                    onValueChange = { next ->
                        percentDraft = next
                        val parsed = next.replace("%", "").trim().toDoubleOrNull()
                        if (parsed != null) onPercentChange(parsed)
                    },
                    suffix = "%",
                    textStyle = TextStyle(
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textPrimary,
                        textAlign = TextAlign.End,
                    ),
                    modifier = Modifier.width(66.dp),
                )
            } else {
                Text(
                    text = String.format(Locale.US, "%.1f%%", percentage),
                    fontSize = 15.sp,
                    color = colors.textPrimary,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(56.dp),
                )
            }
        }
        if (isEditMode) {
            Slider(
                value = percentage.toFloat().coerceIn(0f, 100f),
                onValueChange = { v ->
                    percentDraft = String.format(Locale.US, "%.1f", v)
                    onPercentChange(v.toDouble())
                },
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(thumbColor = barColor, activeTrackColor = barColor),
                modifier = Modifier.height(28.dp),
            )
        }
    }
}

@Composable
private fun KeyValueRow(
    label: String,
    value: String,
    isEditMode: Boolean,
    onValueChange: (String) -> Unit,
) {
    val colors = InstagramTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 15.5.sp,
            color = colors.textPrimary,
        )
        if (isEditMode) {
            InlineEditableNumber(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary,
                    textAlign = TextAlign.End,
                ),
                modifier = Modifier.width(90.dp),
            )
        } else {
            Text(
                text = value,
                fontSize = 15.5.sp,
                color = colors.textPrimary,
            )
        }
    }
}

@Composable
private fun SectionHeaderWithInfo(title: String) {
    val palette = rememberInsightsPalette()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = palette.textPrimary,
        )
        Spacer(Modifier.width(6.dp))
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = "$title info",
            tint = palette.textPrimary,
            modifier = Modifier.size(15.dp),
        )
    }
}

@Composable
private fun InlineEditableNumber(
    value: String,
    onValueChange: (String) -> Unit,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    suffix: String = "",
) {
    val colors = InstagramTheme.colors
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(colors.pillBackground)
            .border(1.dp, IgInsightsMagenta.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            cursorBrush = SolidColor(IgInsightsMagenta),
            textStyle = textStyle,
            modifier = Modifier.weight(1f),
        )
        if (suffix.isNotEmpty()) {
            Text(text = suffix, style = textStyle)
        }
    }
}

@Composable
private fun ProfileStatsEditSection(
    profile: ProfileData,
    onUpdateProfile: ((ProfileData) -> ProfileData) -> Unit,
) {
    val colors = InstagramTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.cardBackground)
            .border(1.dp, IgInsightsMagenta.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "Profile & Dashboard Stats (Auto-Saved)",
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Bold,
            color = IgInsightsMagenta,
        )
        OutlinedTextField(
            value = profile.displayName,
            onValueChange = { v -> onUpdateProfile { it.copy(displayName = v) } },
            label = { Text("Profile Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = profile.username,
            onValueChange = { v -> onUpdateProfile { it.copy(username = v) } },
            label = { Text("Username") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = profile.postsCount,
                onValueChange = { v -> onUpdateProfile { it.copy(postsCount = v) } },
                label = { Text("Posts") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = profile.followersCount,
                onValueChange = { v -> onUpdateProfile { it.copy(followersCount = v) } },
                label = { Text("Followers") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = profile.followingCount,
                onValueChange = { v -> onUpdateProfile { it.copy(followingCount = v) } },
                label = { Text("Following") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
        OutlinedTextField(
            value = profile.dashboardSubtitle,
            onValueChange = { v -> onUpdateProfile { it.copy(dashboardSubtitle = v) } },
            label = { Text("Your dashboard views text") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = profile.bio,
            onValueChange = { v -> onUpdateProfile { it.copy(bio = v) } },
            label = { Text("Bio") },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
