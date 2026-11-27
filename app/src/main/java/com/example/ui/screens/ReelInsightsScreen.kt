package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DemographicItem
import com.example.data.model.ImpactMetric
import com.example.data.model.MediaPost
import com.example.data.model.ProfileData
import com.example.data.model.ReelInsights
import com.example.data.model.SourceShare
import com.example.data.model.ViewsCurvePresets
import com.example.data.model.defaultAgeBreakdown
import com.example.data.model.defaultCountryBreakdown
import com.example.data.model.defaultGenderBreakdown
import com.example.data.network.ApifyRepository
import com.example.ui.components.IgBackIcon
import com.example.ui.components.IgBookmarkStrokeIcon
import com.example.ui.components.IgCommentBubbleIcon
import com.example.ui.components.IgHeartStrokeIcon
import com.example.ui.components.IgMoreDotsIcon
import com.example.ui.components.IgRepostLoopIcon
import com.example.ui.components.IgSharePlaneIcon
import com.example.ui.components.InlineChartPointEditor
import com.example.ui.components.InsightsUiColors
import com.example.ui.components.PostMediaImage
import com.example.ui.components.ReelInsightsEditModal
import com.example.ui.components.ReelInsightsSkeletonBlock
import com.example.ui.components.ViewsOverTimeLineChart
import com.example.ui.components.formatAxisLabels
import com.example.ui.components.rememberCleanLightInsightsPalette
import com.example.ui.components.rememberInsightsPalette
import com.example.ui.theme.IgInsightsMagenta
import com.example.ui.theme.IgInsightsViolet
import com.example.ui.theme.IgTypography
import com.example.ui.theme.InstagramTheme
import java.util.Locale
import kotlinx.coroutines.delay

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
    val palette = rememberCleanLightInsightsPalette()
    val insights = post.insights
    var selectedTab by remember(post.id) { mutableIntStateOf(0) } // 0 = Overview, 1 = Engagement, 2 = Audience
    var selectedAudienceFilter by remember(post.id) { mutableStateOf("Age") } // "Age", "Country", "Gender"
    var statsLoading by remember(post.id) { mutableStateOf(true) }
    LaunchedEffect(post.id) {
        statsLoading = true
        delay(700L)
        statsLoading = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 430.dp)
                .background(palette.bg)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            // Sticky Top Header: 52dp height, #FFFFFF, 16dp horizontal padding
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .background(palette.bg)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("insights_back_button"),
                    ) {
                        IgBackIcon(
                            size = 22.dp,
                            tint = palette.textPrimary,
                        )
                    }
                    Spacer(Modifier.width(18.dp))
                    Text(
                        text = "Reel insights",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.3).sp,
                        color = palette.textPrimary,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onOpenAccountInsights,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("insights_account_trends_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = "Account Insights",
                            tint = palette.textPrimary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    IconButton(
                        onClick = onToggleEditMode,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("insights_more_menu"),
                    ) {
                        IgMoreDotsIcon(
                            size = 20.dp,
                            tint = if (isEditMode) Color(0xFFD300C5) else palette.textPrimary,
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
            // Centered Reel Thumbnail Preview (132dp x 236dp, 12dp radius, #111111, subtle shadow)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .width(132.dp)
                        .height(236.dp)
                        .shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(12.dp),
                            ambientColor = Color.Black.copy(alpha = 0.14f),
                            spotColor = Color.Black.copy(alpha = 0.14f),
                        )
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF111111)),
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

            // 5 Evenly Distributed Counters: 24dp icons, 8dp gap, 13.5sp Medium, skeleton while loading
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 20.dp),
                verticalAlignment = Alignment.Top,
            ) {
                InsightMiniStat(
                    value = insights.likes,
                    statsLoading = statsLoading,
                    palette = palette,
                    isEditMode = isEditMode,
                    onValueChange = { v -> onUpdateInsights { it.copy(likes = v) } },
                    modifier = Modifier.weight(1f),
                ) {
                    IgHeartStrokeIcon(filled = false, tint = palette.textPrimary, size = 24.dp, strokeWidthDp = 1.9.dp)
                }
                InsightMiniStat(
                    value = insights.comments,
                    statsLoading = statsLoading,
                    palette = palette,
                    isEditMode = isEditMode,
                    onValueChange = { v -> onUpdateInsights { it.copy(comments = v) } },
                    modifier = Modifier.weight(1f),
                ) {
                    IgCommentBubbleIcon(tint = palette.textPrimary, size = 24.dp, strokeWidthDp = 1.9.dp)
                }
                InsightMiniStat(
                    value = insights.reposts,
                    statsLoading = statsLoading,
                    palette = palette,
                    isEditMode = isEditMode,
                    onValueChange = { v -> onUpdateInsights { it.copy(reposts = v) } },
                    modifier = Modifier.weight(1f),
                ) {
                    IgRepostLoopIcon(tint = palette.textPrimary, size = 24.dp, strokeWidthDp = 1.9.dp)
                }
                InsightMiniStat(
                    value = insights.shares,
                    statsLoading = statsLoading,
                    palette = palette,
                    isEditMode = isEditMode,
                    onValueChange = { v -> onUpdateInsights { it.copy(shares = v) } },
                    modifier = Modifier.weight(1f),
                ) {
                    IgSharePlaneIcon(filled = false, tint = palette.textPrimary, size = 24.dp, strokeWidthDp = 1.9.dp)
                }
                InsightMiniStat(
                    value = insights.saves,
                    statsLoading = statsLoading,
                    palette = palette,
                    isEditMode = isEditMode,
                    onValueChange = { v -> onUpdateInsights { it.copy(saves = v) } },
                    modifier = Modifier.weight(1f),
                ) {
                    IgBookmarkStrokeIcon(filled = false, tint = palette.textPrimary, size = 24.dp, strokeWidthDp = 1.9.dp)
                }
            }

            // 3 Underline Tabs: Overview | Engagement | Audience with smooth 250ms sliding underline
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.bg),
            ) {
                val tabWidth = maxWidth / 3f
                val indicatorOffset by animateDpAsState(
                    targetValue = tabWidth * selectedTab,
                    animationSpec = tween(
                        durationMillis = 250,
                        easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f),
                    ),
                    label = "insights_tab_indicator",
                )
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf("Overview", "Engagement", "Audience").forEachIndexed { idx, title ->
                            val isSelected = selectedTab == idx
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedTab = idx }
                                    .padding(top = 12.dp, bottom = 14.dp)
                                    .testTag("insights_tab_${title.lowercase()}"),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = title,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) palette.textPrimary else palette.axisText,
                                )
                            }
                        }
                    }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        HorizontalDivider(thickness = 1.dp, color = palette.border)
                        Box(
                            modifier = Modifier
                                .offset(x = indicatorOffset)
                                .width(tabWidth)
                                .height(2.dp)
                                .background(palette.textPrimary),
                        )
                    }
                }
            }

            when (selectedTab) {
                0 -> OverviewTabContent(
                    post = post,
                    insights = insights,
                    palette = palette,
                    statsLoading = statsLoading,
                    isEditMode = isEditMode,
                    onUpdateInsights = onUpdateInsights,
                    onUpdateImpact = onUpdateImpact,
                    onUpdateTopSource = onUpdateTopSource,
                    onUpdatePostDetails = onUpdatePostDetails,
                    onBoostReelClick = onOpenAccountInsights,
                )
                1 -> EngagementTabContent(
                    post = post,
                    insights = insights,
                    palette = palette,
                    statsLoading = statsLoading,
                    isEditMode = isEditMode,
                    onUpdateInsights = onUpdateInsights,
                    onUpdateLikeCurvePoint = onUpdateLikeCurvePoint,
                )
                else -> AudienceTabContent(
                    insights = insights,
                    palette = palette,
                    statsLoading = statsLoading,
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
    }
}

@Composable
private fun InsightMiniStat(
    value: String,
    statsLoading: Boolean,
    palette: InsightsUiColors,
    isEditMode: Boolean,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    val formattedValue = remember(value) { ApifyRepository.formatInsightsIconCount(value) }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        icon()
        Spacer(Modifier.height(8.dp))
        if (statsLoading) {
            ReelInsightsSkeletonBlock(
                modifier = Modifier
                    .padding(vertical = 2.dp)
                    .width(30.dp)
                    .height(13.dp),
                cornerRadius = 4.dp,
            )
        } else if (isEditMode) {
            InlineEditableNumber(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    color = palette.textPrimary,
                    fontSize = 13.5.sp,
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
    post: MediaPost,
    insights: ReelInsights,
    palette: InsightsUiColors,
    statsLoading: Boolean,
    isEditMode: Boolean,
    onUpdateInsights: ((ReelInsights) -> ReelInsights) -> Unit,
    onUpdateImpact: (String, String?, String?) -> Unit,
    onUpdateTopSource: (Int, String?, Double?) -> Unit,
    onUpdatePostDetails: (audioTitle: String, likedByUsername: String) -> Unit,
    onBoostReelClick: () -> Unit,
) {
    var viewsSeries by remember { mutableStateOf("All") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        SectionHeaderWithInfo(title = "Summary", palette = palette)
        Spacer(Modifier.height(14.dp))

        // 2x2 Rounded Summary Cards (#F7F8FA, 12dp radius, 14dp/12dp padding)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SummaryCard(
                label = "Views",
                value = insights.views,
                palette = palette,
                statsLoading = statsLoading,
                isEditMode = isEditMode,
                onValueChange = { v -> onUpdateInsights { it.copy(views = v) } },
                modifier = Modifier.weight(1f),
            )
            SummaryCard(
                label = "Viewers",
                value = insights.viewers,
                palette = palette,
                statsLoading = statsLoading,
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
                statsLoading = statsLoading,
                isEditMode = isEditMode,
                onValueChange = { v -> onUpdateInsights { it.copy(averageWatchTime = v) } },
                modifier = Modifier.weight(1f),
            )
            SummaryCard(
                label = "Follows",
                value = insights.follows,
                palette = palette,
                statsLoading = statsLoading,
                isEditMode = isEditMode,
                onValueChange = { v -> onUpdateInsights { it.copy(follows = v, engagementFollows = v) } },
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(22.dp))
        SectionHeaderWithInfo(title = "Views over time", palette = palette)
        Spacer(Modifier.height(10.dp))

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
                        .background(if (selected) palette.pillSelected else palette.bg)
                        .border(
                            width = 1.dp,
                            color = if (selected) palette.activeCardBorder else palette.border,
                            shape = RoundedCornerShape(20.dp),
                        )
                        .clickable { viewsSeries = series }
                        .padding(horizontal = 18.dp, vertical = 7.dp)
                        .testTag("views_over_time_pill_${series.lowercase()}"),
                ) {
                    Text(
                        text = series,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.textPrimary,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        val rawChartPoints = when (viewsSeries) {
            "Followers" -> insights.viewsOverTimeFollowers
            "Non-followers" -> insights.viewsOverTimeNonFollowers
            else -> insights.viewsOverTimeAll
        }
        val chartPoints = remember(rawChartPoints, insights.viewsCurvePreset, viewsSeries) {
            rawChartPoints.ifEmpty {
                ViewsCurvePresets.pointsForPreset(insights.viewsCurvePreset, viewsSeries)
            }
        }
        val (yMaxLbl, yMidLbl) = formatAxisLabels(insights.viewsYAxisMax, insights.views)
        if (statsLoading) {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                ReelInsightsSkeletonBlock(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    cornerRadius = 8.dp,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ReelInsightsSkeletonBlock(modifier = Modifier.width(92.dp).height(12.dp), cornerRadius = 4.dp)
                    ReelInsightsSkeletonBlock(modifier = Modifier.width(112.dp).height(12.dp), cornerRadius = 4.dp)
                }
            }
        } else {
            ViewsOverTimeLineChart(
                points = chartPoints,
                startDate = insights.viewsStartDate,
                midDate = insights.viewsMidDate,
                endDate = insights.viewsEndDate,
                yMaxLabel = yMaxLbl,
                yMidLabel = yMidLbl,
                palette = palette,
                isEditMode = isEditMode,
                onUpdatePoint = { idx, newVal ->
                    val updatedList = chartPoints.toMutableList()
                    if (idx in updatedList.indices) {
                        updatedList[idx] = newVal.coerceIn(0f, 100f)
                    }
                    onUpdateInsights { cur ->
                        when (viewsSeries) {
                            "Followers" -> cur.copy(
                                viewsCurvePreset = ViewsCurvePresets.CUSTOM,
                                viewsOverTimeFollowers = updatedList,
                            )
                            "Non-followers" -> cur.copy(
                                viewsCurvePreset = ViewsCurvePresets.CUSTOM,
                                viewsOverTimeNonFollowers = updatedList,
                            )
                            else -> cur.copy(
                                viewsCurvePreset = ViewsCurvePresets.CUSTOM,
                                viewsOverTimeAll = updatedList,
                            )
                        }
                    }
                },
            )
        }
        if (isEditMode) {
            Spacer(Modifier.height(10.dp))
            val currentLiker = post.likedByText
                .removePrefix("Liked by ")
                .removeSuffix(" and others")
                .trim()
                .takeIf { it.isNotBlank() && !it.endsWith("people", ignoreCase = true) && it.firstOrNull()?.isDigit() != true }
                .orEmpty()
            var likerDraft by remember(post.id, currentLiker) { mutableStateOf(currentLiker) }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.card)
                    .border(1.dp, palette.border, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Timing, Axis & Liked By (Inline)",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = IgInsightsMagenta,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = insights.viewsStartDate,
                        onValueChange = { d -> onUpdateInsights { it.copy(viewsStartDate = d) } },
                        label = { Text("Start date", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = insights.viewsMidDate,
                        onValueChange = { d -> onUpdateInsights { it.copy(viewsMidDate = d) } },
                        label = { Text("Mid date", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = insights.viewsEndDate,
                        onValueChange = { d -> onUpdateInsights { it.copy(viewsEndDate = d) } },
                        label = { Text("End date", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = insights.viewsYAxisMax,
                        onValueChange = { y ->
                            val clean = y.filter { it.isDigit() || it == ',' }
                            onUpdateInsights { it.copy(viewsYAxisMax = clean) }
                        },
                        label = { Text("Y-axis max", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = insights.durationLabel,
                        onValueChange = { d -> onUpdateInsights { it.copy(durationLabel = d) } },
                        label = { Text("Duration", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                OutlinedTextField(
                    value = likerDraft,
                    onValueChange = { next ->
                        likerDraft = next
                        onUpdatePostDetails(post.subtitle, next)
                    },
                    label = { Text("Who liked this reel (username)", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(Modifier.height(26.dp))

        SectionHeaderWithInfo(title = "What impacts your views", palette = palette)
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Rates are listed in order of importance to reach.",
            fontSize = 13.sp,
            color = palette.axisText,
        )
        Spacer(Modifier.height(12.dp))

        if (statsLoading) {
            repeat(6) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ReelInsightsSkeletonBlock(modifier = Modifier.size(44.dp), cornerRadius = 22.dp)
                        Spacer(Modifier.width(14.dp))
                        ReelInsightsSkeletonBlock(modifier = Modifier.width(104.dp).height(14.dp), cornerRadius = 4.dp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        ReelInsightsSkeletonBlock(modifier = Modifier.width(42.dp).height(14.dp), cornerRadius = 4.dp)
                        Spacer(Modifier.height(4.dp))
                        ReelInsightsSkeletonBlock(modifier = Modifier.width(48.dp).height(10.dp), cornerRadius = 4.dp)
                    }
                }
            }
        } else {
            val impactsToShow = remember(insights.impacts) {
                if (insights.impacts.isEmpty() || insights.impacts.firstOrNull()?.percentage == "22.9%") {
                    com.example.data.model.defaultImpacts()
                } else {
                    insights.impacts
                }
            }
            impactsToShow.forEach { metric ->
                ImpactRow(
                    metric = metric,
                    palette = palette,
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
        SectionHeaderWithInfo(title = "How long people watched your reel", palette = palette)
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
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFD300C5)),
                )
            }
        }

        if (statsLoading) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ReelInsightsSkeletonBlock(modifier = Modifier.width(96.dp).height(170.dp), cornerRadius = 12.dp)
                Spacer(Modifier.height(16.dp))
                ReelInsightsSkeletonBlock(modifier = Modifier.fillMaxWidth().height(140.dp), cornerRadius = 8.dp)
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .width(96.dp)
                        .height(170.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF111111)),
                ) {
                    PostMediaImage(
                        mediaUrl = post.mediaUrl,
                        drawableKey = post.drawableKey,
                        videoUrl = post.videoUrl,
                        overlayTopCaption = post.overlayTopCaption,
                        overlaySubtitle = post.overlaySubtitle,
                        useLetterboxMemeStyle = false,
                        contentDescription = "Reel retention cover",
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            RetentionCard(
                unavailable = insights.retentionUnavailable,
                curve = insights.retentionCurve,
                durationLabel = insights.durationLabel,
                palette = palette,
                isEditMode = isEditMode,
                onUpdatePoint = { idx, newVal ->
                    val updated = insights.retentionCurve.toMutableList()
                    if (idx in updated.indices) updated[idx] = newVal.coerceIn(0f, 100f)
                    onUpdateInsights { it.copy(retentionCurve = updated) }
                },
            )
        }

        Spacer(Modifier.height(22.dp))
        SectionHeaderWithInfo(title = "Top sources of views", palette = palette)
        Spacer(Modifier.height(14.dp))

        if (statsLoading) {
            repeat(4) {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        ReelInsightsSkeletonBlock(modifier = Modifier.width(76.dp).height(14.dp), cornerRadius = 4.dp)
                        ReelInsightsSkeletonBlock(modifier = Modifier.width(40.dp).height(14.dp), cornerRadius = 4.dp)
                    }
                    Spacer(Modifier.height(8.dp))
                    ReelInsightsSkeletonBlock(modifier = Modifier.fillMaxWidth().height(6.dp), cornerRadius = 4.dp)
                }
            }
        } else {
            val sourcesToShow = insights.topSources.ifEmpty { com.example.data.model.defaultTopSources() }
            sourcesToShow.forEachIndexed { idx, source ->
                SourceProgressRow(
                    source = source,
                    palette = palette,
                    barColor = Color(0xFFD300C5),
                    isEditMode = isEditMode,
                    onLabelChange = { newLabel -> onUpdateTopSource(idx, newLabel, null) },
                    onPercentChange = { newPct -> onUpdateTopSource(idx, null, newPct) },
                )
                Spacer(Modifier.height(14.dp))
            }
        }

        Spacer(Modifier.height(10.dp))
        HorizontalDivider(thickness = 1.dp, color = palette.sectionDivider)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onBoostReelClick)
                .padding(vertical = 16.dp)
                .testTag("boost_this_reel_row"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    tint = palette.textPrimary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = "Boost this Reel",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.textPrimary,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = palette.textPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
        HorizontalDivider(thickness = 1.dp, color = palette.sectionDivider)
    }
}

@Composable
private fun SummaryCard(
    label: String,
    value: String,
    palette: InsightsUiColors,
    statsLoading: Boolean,
    isEditMode: Boolean,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(palette.card)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        if (statsLoading) {
            ReelInsightsSkeletonBlock(modifier = Modifier.width(78.dp).height(12.dp), cornerRadius = 4.dp)
            Spacer(Modifier.height(7.dp))
            ReelInsightsSkeletonBlock(modifier = Modifier.width(62.dp).height(19.dp), cornerRadius = 4.dp)
        } else {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = palette.textSecondary,
                maxLines = 1,
            )
            Spacer(Modifier.height(4.dp))
            if (isEditMode) {
                InlineEditableNumber(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.textPrimary,
                        fontFeatureSettings = "tnum",
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Text(
                    text = value,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.textPrimary,
                    style = TextStyle(fontFeatureSettings = "tnum"),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun IgSkipRateIcon(
    tint: Color = Color.White,
    size: Dp = 21.dp,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = 1.6.dp.toPx()
        val radius = size.toPx() / 2f
        val center = Offset(radius, radius)

        // Draw outer dashed/dotted ring
        drawCircle(
            color = tint,
            radius = radius - strokeWidth,
            center = center,
            style = Stroke(
                width = strokeWidth,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.5.dp.toPx(), 3.dp.toPx()), 0f),
            ),
        )

        // Center pivot dot
        drawCircle(
            color = tint,
            radius = 1.3.dp.toPx(),
            center = center,
        )

        // Minute hand (pointing ~12:10)
        drawLine(
            color = tint,
            start = center,
            end = Offset(center.x + radius * 0.42f, center.y - radius * 0.15f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )

        // Hour hand (pointing ~10:15)
        drawLine(
            color = tint,
            start = center,
            end = Offset(center.x - radius * 0.22f, center.y - radius * 0.38f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun ImpactRow(
    metric: ImpactMetric,
    palette: InsightsUiColors,
    isEditMode: Boolean,
    onPercentageChange: (String) -> Unit,
    onToggleStatus: () -> Unit,
) {
    val isTypical = metric.status.equals("Typical", ignoreCase = true)
    val statusColor = if (isTypical) {
        palette.axisText
    } else {
        Color(0xFF34C759) // Crisp authentic Instagram green
    }

    val isDark = InstagramTheme.colors.isDark

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (isDark) Color(0xFF1E2124) else Color(0xFFF1F3F5)),
            contentAlignment = Alignment.Center,
        ) {
            when (metric.iconType) {
                "skip" -> IgSkipRateIcon(tint = palette.textPrimary, size = 21.dp)
                "share" -> IgSharePlaneIcon(filled = false, tint = palette.textPrimary, size = 20.dp)
                "like" -> IgHeartStrokeIcon(filled = false, tint = palette.textPrimary, size = 21.dp)
                "save" -> IgBookmarkStrokeIcon(filled = false, tint = palette.textPrimary, size = 20.dp)
                "repost" -> IgRepostLoopIcon(tint = palette.textPrimary, size = 20.dp)
                else -> IgCommentBubbleIcon(tint = palette.textPrimary, size = 20.dp)
            }
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = metric.label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = palette.textPrimary,
            modifier = Modifier.weight(1f),
        )

        Column(horizontalAlignment = Alignment.End) {
            if (isEditMode) {
                InlineEditableNumber(
                    value = metric.percentage,
                    onValueChange = onPercentageChange,
                    textStyle = TextStyle(
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.textPrimary,
                        textAlign = TextAlign.End,
                        fontFeatureSettings = "tnum",
                    ),
                    modifier = Modifier.width(72.dp),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = metric.status,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = statusColor,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusColor.copy(alpha = 0.12f))
                        .clickable(onClick = onToggleStatus)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            } else {
                Text(
                    text = metric.percentage,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.textPrimary,
                    style = TextStyle(fontFeatureSettings = "tnum"),
                )
                Text(
                    text = metric.status,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = statusColor,
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
    palette: InsightsUiColors = rememberCleanLightInsightsPalette(),
    isEditMode: Boolean = false,
    onUpdatePoint: ((Int, Float) -> Unit)? = null,
) {
    var selectedIdx by remember(curve.size, isEditMode) { mutableIntStateOf(0) }
    val safeSelected = selectedIdx.coerceIn(0, (curve.size - 1).coerceAtLeast(0))

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(145.dp)
                .padding(vertical = 4.dp),
        ) {
            if (unavailable) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("100%", color = palette.axisText, fontSize = 12.sp)
                        Spacer(Modifier.width(12.dp))
                        HorizontalDivider(color = palette.chartGrid)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("50%", color = palette.axisText, fontSize = 12.sp)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Retention curve hidden",
                            color = palette.axisText,
                            fontSize = 13.5.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("0%", color = palette.axisText, fontSize = 12.sp)
                            Spacer(Modifier.width(12.dp))
                            HorizontalDivider(color = palette.chartGrid)
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 42.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("0:00", color = palette.axisText, fontSize = 12.sp)
                            Text(durationLabel, color = palette.axisText, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 42.dp, top = 8.dp, bottom = 8.dp)
                                .then(
                                    if (isEditMode && curve.isNotEmpty()) {
                                        Modifier.pointerInput(curve.size, isEditMode) {
                                            detectTapGestures { tap ->
                                                if (curve.size > 1) {
                                                    val w = size.width.toFloat().coerceAtLeast(1f)
                                                    selectedIdx = ((tap.x / w) * (curve.size - 1))
                                                        .let { kotlin.math.round(it).toInt() }
                                                        .coerceIn(0, curve.lastIndex)
                                                }
                                            }
                                        }
                                    } else Modifier
                                ),
                        ) {
                            val w = size.width
                            val h = size.height
                            listOf(0f, 0.5f, 1f).forEach { frac ->
                                drawLine(
                                    color = palette.chartGrid,
                                    start = Offset(0f, h * frac),
                                    end = Offset(w, h * frac),
                                    strokeWidth = 1.dp.toPx(),
                                )
                            }
                            if (curve.size >= 2) {
                                val coords = curve.mapIndexed { idx, v ->
                                    val x = w * (idx.toFloat() / (curve.size - 1))
                                    val y = h * (1f - (v / 100f).coerceIn(0f, 1f))
                                    Offset(x, y)
                                }
                                val path = Path()
                                coords.forEachIndexed { idx, pt ->
                                    if (idx == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
                                }
                                drawPath(
                                    path = path,
                                    color = Color(0xFFD300C5),
                                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                                )
                                if (isEditMode) {
                                    coords.forEachIndexed { idx, pt ->
                                        val isSel = idx == safeSelected
                                        if (isSel) {
                                            drawCircle(color = palette.bg, radius = 6.8.dp.toPx(), center = pt)
                                        }
                                        drawCircle(
                                            color = if (isSel) Color(0xFF4F46E5) else Color(0xFFD300C5),
                                            radius = if (isSel) 5.dp.toPx() else 3.2.dp.toPx(),
                                            center = pt,
                                        )
                                    }
                                }
                            }
                        }
                        Column(
                            modifier = Modifier.fillMaxHeight(),
                            verticalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("100%", color = palette.axisText, fontSize = 12.sp)
                            Text("50%", color = palette.axisText, fontSize = 12.sp)
                            Text("0%", color = palette.axisText, fontSize = 12.sp)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 42.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("0:00", color = palette.axisText, fontSize = 12.sp)
                        Text(durationLabel, color = palette.axisText, fontSize = 12.sp)
                    }
                }
            }
        }
        if (isEditMode && !unavailable && onUpdatePoint != null && curve.isNotEmpty()) {
            InlineChartPointEditor(
                points = curve,
                selectedIdx = safeSelected,
                onSelectIdx = { selectedIdx = it },
                onUpdatePoint = onUpdatePoint,
                palette = palette,
            )
        }
    }
}

@Composable
private fun EngagementTabContent(
    post: MediaPost,
    insights: ReelInsights,
    palette: InsightsUiColors,
    statsLoading: Boolean,
    isEditMode: Boolean,
    onUpdateInsights: ((ReelInsights) -> ReelInsights) -> Unit,
    onUpdateLikeCurvePoint: (Int, Float) -> Unit,
) {
    var selectedSpikeIdx by remember(insights.likeCurvePoints.size, isEditMode) { mutableIntStateOf(0) }
    val safeSpikeIdx = selectedSpikeIdx.coerceIn(0, (insights.likeCurvePoints.size - 1).coerceAtLeast(0))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        SectionHeaderWithInfo(title = "Actions after viewing", palette = palette)
        Spacer(Modifier.height(10.dp))
        KeyValueRow(
            label = "Profile visits",
            value = insights.profileVisits,
            palette = palette,
            statsLoading = statsLoading,
            isEditMode = isEditMode,
            onValueChange = { v -> onUpdateInsights { it.copy(profileVisits = v) } },
        )
        KeyValueRow(
            label = "Follows",
            value = insights.engagementFollows,
            palette = palette,
            statsLoading = statsLoading,
            isEditMode = isEditMode,
            onValueChange = { v -> onUpdateInsights { it.copy(engagementFollows = v, follows = v) } },
        )

        Spacer(Modifier.height(24.dp))
        SectionHeaderWithInfo(title = "Interactions", palette = palette)
        Spacer(Modifier.height(10.dp))
        KeyValueRow(
            label = "Likes",
            value = insights.likes,
            palette = palette,
            statsLoading = statsLoading,
            isEditMode = isEditMode,
            onValueChange = { v -> onUpdateInsights { it.copy(likes = v) } },
        )
        KeyValueRow(
            label = "Comments",
            value = insights.comments,
            palette = palette,
            statsLoading = statsLoading,
            isEditMode = isEditMode,
            onValueChange = { v -> onUpdateInsights { it.copy(comments = v) } },
        )
        KeyValueRow(
            label = "Reposts",
            value = insights.reposts,
            palette = palette,
            statsLoading = statsLoading,
            isEditMode = isEditMode,
            onValueChange = { v -> onUpdateInsights { it.copy(reposts = v) } },
        )
        KeyValueRow(
            label = "Shares",
            value = insights.shares,
            palette = palette,
            statsLoading = statsLoading,
            isEditMode = isEditMode,
            onValueChange = { v -> onUpdateInsights { it.copy(shares = v) } },
        )
        KeyValueRow(
            label = "Saves",
            value = insights.saves,
            palette = palette,
            statsLoading = statsLoading,
            isEditMode = isEditMode,
            onValueChange = { v -> onUpdateInsights { it.copy(saves = v) } },
        )

        Spacer(Modifier.height(24.dp))
        SectionHeaderWithInfo(title = "When people liked your reel", palette = palette)
        Spacer(Modifier.height(14.dp))

        // Centered reel thumbnail (96dp x 170dp, 12dp radius, #111111)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .width(96.dp)
                    .height(170.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF111111)),
                contentAlignment = Alignment.Center,
            ) {
                PostMediaImage(
                    mediaUrl = post.mediaUrl,
                    drawableKey = post.drawableKey,
                    videoUrl = post.videoUrl,
                    overlayTopCaption = post.overlayTopCaption,
                    overlaySubtitle = post.overlaySubtitle,
                    useLetterboxMemeStyle = false,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        // Magenta Spike Chart (0%, 15%, 30% | 0:00 -> durationLabel)
        if (statsLoading) {
            ReelInsightsSkeletonBlock(
                modifier = Modifier.fillMaxWidth().height(140.dp),
                cornerRadius = 8.dp,
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(145.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(bottom = 22.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("30%", color = palette.axisText, fontSize = 12.sp)
                    Text("15%", color = palette.axisText, fontSize = 12.sp)
                    Text("0%", color = palette.axisText, fontSize = 12.sp)
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    val pts = insights.likeCurvePoints
                    Canvas(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .then(
                                if (isEditMode && pts.isNotEmpty()) {
                                    Modifier.pointerInput(pts.size, isEditMode) {
                                        detectTapGestures { tap ->
                                            if (pts.size > 1) {
                                                val w = size.width.toFloat().coerceAtLeast(1f)
                                                selectedSpikeIdx = ((tap.x / w) * (pts.size - 1))
                                                    .let { kotlin.math.round(it).toInt() }
                                                    .coerceIn(0, pts.lastIndex)
                                            }
                                        }
                                    }
                                } else Modifier
                            ),
                    ) {
                        val w = size.width
                        val h = size.height
                        listOf(0f, 0.5f, 1f).forEach { frac ->
                            drawLine(
                                color = palette.chartGrid,
                                start = Offset(0f, h * frac),
                                end = Offset(w, h * frac),
                                strokeWidth = 1.dp.toPx(),
                            )
                        }

                        if (pts.size >= 2) {
                            val coords = pts.mapIndexed { idx, value ->
                                val x = w * (idx.toFloat() / (pts.size - 1))
                                val y = h * (1f - (value / 30f).coerceIn(0f, 1f))
                                Offset(x, y)
                            }
                            val path = Path()
                            coords.forEachIndexed { idx, pt ->
                                if (idx == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
                            }
                            drawPath(
                                path = path,
                                color = Color(0xFFD300C5),
                                style = Stroke(
                                    width = 3.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round,
                                ),
                            )
                            if (isEditMode) {
                                coords.forEachIndexed { idx, pt ->
                                    val isSel = idx == safeSpikeIdx
                                    if (isSel) {
                                        drawCircle(color = palette.bg, radius = 6.8.dp.toPx(), center = pt)
                                    }
                                    drawCircle(
                                        color = if (isSel) Color(0xFF4F46E5) else Color(0xFFD300C5),
                                        radius = if (isSel) 5.dp.toPx() else 3.2.dp.toPx(),
                                        center = pt,
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("0:00", color = palette.axisText, fontSize = 12.sp)
                        if (isEditMode) {
                            InlineEditableNumber(
                                value = insights.durationLabel,
                                onValueChange = { d -> onUpdateInsights { it.copy(durationLabel = d) } },
                                textStyle = TextStyle(
                                    color = palette.textPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.End,
                                ),
                                modifier = Modifier.width(56.dp),
                            )
                        } else {
                            Text(insights.durationLabel, color = palette.axisText, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        if (isEditMode && insights.likeCurvePoints.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            InlineChartPointEditor(
                points = insights.likeCurvePoints,
                selectedIdx = safeSpikeIdx,
                onSelectIdx = { selectedSpikeIdx = it },
                onUpdatePoint = onUpdateLikeCurvePoint,
                palette = palette,
                minVal = 0f,
                maxVal = 30f,
            )
        }
    }
}

@Composable
private fun AudienceTabContent(
    insights: ReelInsights,
    palette: InsightsUiColors,
    statsLoading: Boolean,
    selectedFilter: String,
    onSelectFilter: (String) -> Unit,
    isEditMode: Boolean,
    onUpdateInsights: ((ReelInsights) -> ReelInsights) -> Unit,
    onUpdateDemographic: (String, Int, String?, Double?) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        SectionHeaderWithInfo(title = "Who viewed your reel", palette = palette)
        Spacer(Modifier.height(14.dp))

        PercentageBarRow(
            label = "Followers",
            percentage = insights.followersViewPercent,
            palette = palette,
            barColor = Color(0xFFD300C5),
            statsLoading = statsLoading,
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
        Spacer(Modifier.height(16.dp))
        PercentageBarRow(
            label = "Non-followers",
            percentage = insights.nonFollowersViewPercent,
            palette = palette,
            barColor = Color(0xFF4F46E5),
            statsLoading = statsLoading,
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
        SectionHeaderWithInfo(title = "Audience details", palette = palette)
        Spacer(Modifier.height(10.dp))

        // Age, Country, Gender Filter Pills
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf("Age", "Country", "Gender").forEach { filter ->
                val selected = selectedFilter == filter
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (selected) palette.pillSelected else palette.bg)
                        .border(
                            width = 1.dp,
                            color = if (selected) palette.activeCardBorder else palette.border,
                            shape = RoundedCornerShape(20.dp),
                        )
                        .clickable { onSelectFilter(filter) }
                        .padding(horizontal = 18.dp, vertical = 7.dp)
                        .testTag("audience_pill_${filter.lowercase()}"),
                ) {
                    Text(
                        text = filter,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.textPrimary,
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        val itemsList: List<DemographicItem> = when (selectedFilter) {
            "Age" -> insights.ageBreakdown.ifEmpty { defaultAgeBreakdown() }
            "Country" -> insights.countryBreakdown.ifEmpty { defaultCountryBreakdown() }
            else -> insights.genderBreakdown.ifEmpty { defaultGenderBreakdown() }
        }

        itemsList.forEachIndexed { idx, item ->
            val barColor = if (selectedFilter == "Gender" && idx == 1) Color(0xFF4F46E5) else Color(0xFFD300C5)
            PercentageBarRow(
                label = item.label,
                percentage = item.percentage,
                palette = palette,
                barColor = barColor,
                statsLoading = statsLoading,
                isEditMode = isEditMode,
                labelEditable = isEditMode,
                onLabelChange = { newLabel -> onUpdateDemographic(selectedFilter, idx, newLabel, null) },
                onPercentChange = { newPct -> onUpdateDemographic(selectedFilter, idx, null, newPct) },
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SourceProgressRow(
    source: SourceShare,
    palette: InsightsUiColors,
    barColor: Color,
    isEditMode: Boolean,
    onLabelChange: (String) -> Unit,
    onPercentChange: (Double) -> Unit,
) {
    PercentageBarRow(
        label = source.label,
        percentage = source.percentage,
        palette = palette,
        barColor = barColor,
        statsLoading = false,
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
    palette: InsightsUiColors,
    barColor: Color,
    statsLoading: Boolean,
    isEditMode: Boolean,
    labelEditable: Boolean,
    onLabelChange: (String) -> Unit,
    onPercentChange: (Double) -> Unit,
) {
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isEditMode && labelEditable) {
                InlineEditableNumber(
                    value = label,
                    onValueChange = onLabelChange,
                    textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, color = palette.textPrimary),
                    modifier = Modifier.fillMaxWidth(0.65f),
                )
            } else {
                Text(
                    text = label,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = palette.textPrimary,
                )
            }
            if (statsLoading) {
                ReelInsightsSkeletonBlock(modifier = Modifier.width(42.dp).height(14.dp), cornerRadius = 4.dp)
            } else if (isEditMode) {
                InlineEditableNumber(
                    value = percentDraft,
                    onValueChange = { next ->
                        percentDraft = next
                        val parsed = next.replace("%", "").trim().toDoubleOrNull()
                        if (parsed != null) onPercentChange(parsed)
                    },
                    suffix = "%",
                    textStyle = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = palette.textPrimary,
                        textAlign = TextAlign.End,
                        fontFeatureSettings = "tnum",
                    ),
                    modifier = Modifier.width(66.dp),
                )
            } else {
                Text(
                    text = String.format(Locale.US, "%.1f%%", percentage),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = palette.textPrimary,
                    style = TextStyle(fontFeatureSettings = "tnum"),
                )
            }
        }
        Spacer(Modifier.height(7.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(palette.track),
        ) {
            if (!statsLoading && animatedFraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedFraction.coerceAtLeast(0.008f))
                        .clip(RoundedCornerShape(6.dp))
                        .background(barColor),
                )
            }
        }
        if (isEditMode && !statsLoading) {
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
    palette: InsightsUiColors,
    statsLoading: Boolean,
    isEditMode: Boolean,
    onValueChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 16.5.sp,
            fontWeight = FontWeight.Medium,
            color = palette.textPrimary,
        )
        if (statsLoading) {
            ReelInsightsSkeletonBlock(modifier = Modifier.width(40.dp).height(15.dp), cornerRadius = 4.dp)
        } else if (isEditMode) {
            InlineEditableNumber(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.textPrimary,
                    textAlign = TextAlign.End,
                    fontFeatureSettings = "tnum",
                ),
                modifier = Modifier.width(90.dp),
            )
        } else {
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.textPrimary,
                style = TextStyle(fontFeatureSettings = "tnum"),
            )
        }
    }
}

@Composable
private fun SectionHeaderWithInfo(
    title: String,
    palette: InsightsUiColors = rememberCleanLightInsightsPalette(),
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = palette.textPrimary,
        )
        Spacer(Modifier.width(6.dp))
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = "$title info",
            tint = palette.axisText,
            modifier = Modifier.size(16.dp),
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
