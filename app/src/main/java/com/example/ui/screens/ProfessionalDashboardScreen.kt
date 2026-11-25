package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material.icons.outlined.MovieCreation
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AccountInsightsData
import com.example.data.model.ProfileData
import com.example.ui.components.AccountInsightsEditModal
import com.example.ui.components.IgBackIcon
import com.example.ui.components.MetaVerifiedRosetteIcon
import com.example.ui.components.rememberInsightsPalette
import com.example.ui.theme.IgBoostBlue
import com.example.ui.theme.IgLinkBlue
import com.example.ui.theme.IgTypography

@Composable
fun ProfessionalDashboardScreen(
    profile: ProfileData,
    accountInsights: AccountInsightsData,
    onBack: () -> Unit,
    onOpenInsightsTab: (Int) -> Unit,
    onUpdateAccountInsights: (AccountInsightsData) -> Unit,
) {
    BackHandler(onBack = onBack)
    val palette = rememberInsightsPalette()
    var isEditMode by remember { mutableStateOf(false) }
    var activeToolDialog by remember { mutableStateOf<Pair<String, String>?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // Header: <- Your dashboard , gear icon
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
                    modifier = Modifier.testTag("dashboard_back_button"),
                ) {
                    IgBackIcon(
                        size = 24.dp,
                        tint = palette.textPrimary,
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Your dashboard",
                    style = IgTypography.screenHeading,
                    color = palette.textPrimary,
                )
            }

            IconButton(
                onClick = { isEditMode = !isEditMode },
                modifier = Modifier.testTag("dashboard_settings_button"),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = if (isEditMode) "Done editing" else "Edit dashboard insights",
                    tint = if (isEditMode) com.example.ui.theme.IgInsightsMagenta else palette.textPrimary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        if (isEditMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(com.example.ui.theme.IgInsightsMagenta.copy(alpha = 0.14f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Inline Edit Mode · Tap values to edit",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = com.example.ui.theme.IgInsightsMagenta,
                )
                Text(
                    text = "Done",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = com.example.ui.theme.IgInsightsMagenta,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isEditMode = false }
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(10.dp))

            // Insights Header + Date Range
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Insights",
                    style = IgTypography.insightsSectionHeading,
                    color = palette.textPrimary,
                )
                if (isEditMode) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = accountInsights.dateRange,
                        onValueChange = { next ->
                            onUpdateAccountInsights(accountInsights.copy(dateRange = next))
                        },
                        singleLine = true,
                        textStyle = IgTypography.secondaryMeta.copy(
                            color = palette.textPrimary,
                            fontWeight = FontWeight.Medium,
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(palette.card)
                            .border(1.dp, com.example.ui.theme.IgInsightsMagenta.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                } else {
                    Text(
                        text = accountInsights.dateRange,
                        style = IgTypography.secondaryMeta,
                        color = palette.textSecondary,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            DashboardMetricRow(
                label = "Views",
                value = accountInsights.dashboardViews,
                palette = palette,
                isEditMode = isEditMode,
                onValueChange = { next ->
                    onUpdateAccountInsights(accountInsights.copy(dashboardViews = next))
                },
                modifier = Modifier.testTag("dashboard_row_views"),
                onClick = { onOpenInsightsTab(0) },
            )
            DashboardMetricRow(
                label = "New followers",
                value = accountInsights.newFollowers,
                palette = palette,
                isEditMode = isEditMode,
                onValueChange = { next ->
                    val cleaned = next.filter { it.isDigit() || it == ',' }
                    onUpdateAccountInsights(accountInsights.copy(newFollowers = cleaned))
                },
                modifier = Modifier.testTag("dashboard_row_followers"),
                onClick = { onOpenInsightsTab(2) },
            )
            DashboardMetricRow(
                label = "Content you shared",
                value = accountInsights.contentShared,
                palette = palette,
                isEditMode = isEditMode,
                onValueChange = { next ->
                    val cleaned = next.filter { it.isDigit() || it == ',' }
                    onUpdateAccountInsights(accountInsights.copy(contentShared = cleaned))
                },
                modifier = Modifier.testTag("dashboard_row_content"),
                onClick = { onOpenInsightsTab(1) },
            )

            Spacer(Modifier.height(18.dp))

            // Next steps
            Text(
                text = "Next steps",
                style = IgTypography.insightsSectionHeading,
                color = palette.textPrimary,
            )
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(palette.card)
                    .border(1.dp, palette.border, RoundedCornerShape(16.dp))
                    .clickable {
                        activeToolDialog = "Meta Verified" to
                            "Verify @${profile.username} to display the blue creator badge, receive proactive impersonation protection, and unlock direct support."
                    }
                    .padding(horizontal = 14.dp, vertical = 14.dp)
                    .testTag("dashboard_meta_verified_card"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette.bg)
                        .border(1.dp, palette.border, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    MetaVerifiedRosetteIcon(
                        tint = palette.textPrimary,
                        size = 24.dp,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Get Meta Verified",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.textPrimary,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Build credibility with a verified badge and more.",
                        fontSize = 13.sp,
                        color = palette.textSecondary,
                        lineHeight = 17.sp,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = palette.textSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(Modifier.height(24.dp))

            // Your tools + See all
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Your tools",
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary,
                )
                Text(
                    text = "See all",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = IgLinkBlue,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            activeToolDialog = "All Creator Tools" to
                                "Monthly recap, Best practices, Inspiration on Edits, Ad tools, Trial reels, and Saved replies are enabled for @${profile.username}."
                        }
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                )
            }
            Spacer(Modifier.height(8.dp))

            DashboardToolRow(
                icon = Icons.Outlined.CalendarMonth,
                title = "Monthly recap",
                showNewBadge = true,
                palette = palette,
                onClick = {
                    activeToolDialog = "Monthly recap (${accountInsights.dateRange})" to
                        "You reached ${accountInsights.overviewViews} total views across ${accountInsights.contentShared} shared reels with ${accountInsights.interactions} interactions."
                },
            )
            DashboardToolRow(
                icon = Icons.Outlined.AutoAwesome,
                title = "Best practices",
                showNewBadge = true,
                palette = palette,
                onClick = {
                    activeToolDialog = "Best practices" to
                        "Hook viewers in the first 1.5 seconds, keep reels under 60 seconds, and post when your followers are most active (12 am - 3 am)."
                },
            )
            DashboardToolRow(
                icon = Icons.Outlined.MovieCreation,
                title = "Inspiration on Edits",
                isExternalLink = true,
                palette = palette,
                onClick = {
                    activeToolDialog = "Inspiration on Edits" to
                        "Browse trending cinema cuts, pacing templates, and audio transitions curated for movie & TV reel creators."
                },
            )
            DashboardToolRow(
                icon = Icons.Outlined.Campaign,
                title = "Ad tools",
                palette = palette,
                onClick = {
                    activeToolDialog = "Ad tools" to
                        "Boost top-performing reels directly from Post Detail or Insights to expand non-follower reach."
                },
            )
            DashboardToolRow(
                icon = Icons.Outlined.Science,
                title = "Trial reels",
                palette = palette,
                onClick = {
                    activeToolDialog = "Trial reels" to
                        "Test new editing styles with non-followers first before sharing to your main profile grid."
                },
            )
            DashboardToolRow(
                icon = Icons.Outlined.Forum,
                title = "Saved replies",
                palette = palette,
                onClick = {
                    activeToolDialog = "Saved replies" to
                        "Create quick DM shortcuts so you can respond to cinema scene requests in one tap."
                },
            )

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(thickness = 0.6.dp, color = palette.border)
            Spacer(Modifier.height(16.dp))

            // Monetisation
            Text(
                text = "Monetisation",
                fontSize = 16.5.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary,
            )
            Spacer(Modifier.height(6.dp))
            DashboardToolRow(
                icon = Icons.Outlined.MonetizationOn,
                title = "Monetisation tools",
                palette = palette,
                onClick = {
                    activeToolDialog = "Monetisation tools" to
                        "Your account is in good standing and eligible to track bonuses, gifts, and branded content partnerships."
                },
            )

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(thickness = 0.6.dp, color = palette.border)
            Spacer(Modifier.height(16.dp))

            // Tips and resources
            Text(
                text = "Tips and resources",
                fontSize = 16.5.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary,
            )
            Spacer(Modifier.height(6.dp))
            DashboardToolRow(
                icon = Icons.Outlined.GraphicEq,
                title = "Trending audio",
                showNewBadge = true,
                palette = palette,
                onClick = {
                    activeToolDialog = "Trending audio" to
                        "Cinema dialogue overlays and moody ambient tracks are trending +38% this week across Reels."
                },
            )
            DashboardToolRow(
                icon = Icons.AutoMirrored.Outlined.HelpOutline,
                title = "Other helpful resources",
                palette = palette,
                onClick = {
                    activeToolDialog = "Creator resources" to
                        "Tap the top-right gear icon anytime to customize dashboard and account insights figures."
                },
            )

            Spacer(Modifier.height(28.dp))
        }
    }

    activeToolDialog?.let { (title, message) ->
        AlertDialog(
            onDismissRequest = { activeToolDialog = null },
            containerColor = palette.card,
            titleContentColor = palette.textPrimary,
            textContentColor = palette.textSecondary,
            title = { Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp) },
            text = { Text(message, fontSize = 14.sp, lineHeight = 20.sp) },
            confirmButton = {
                TextButton(onClick = { activeToolDialog = null }) {
                    Text("Got it", color = IgBoostBlue, fontWeight = FontWeight.SemiBold)
                }
            },
        )
    }
}

@Composable
private fun DashboardMetricRow(
    label: String,
    value: String,
    palette: com.example.ui.components.InsightsUiColors,
    modifier: Modifier = Modifier,
    isEditMode: Boolean = false,
    onValueChange: ((String) -> Unit)? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = !isEditMode, onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = IgTypography.displayName,
            color = palette.textPrimary,
            modifier = Modifier.weight(1f),
        )
        if (isEditMode && onValueChange != null) {
            androidx.compose.foundation.text.BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = IgTypography.usernameMedium.copy(
                    color = palette.textPrimary,
                    fontFeatureSettings = "tnum",
                ),
                modifier = Modifier
                    .width(96.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(palette.card)
                    .border(1.dp, com.example.ui.theme.IgInsightsMagenta.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        } else {
            Text(
                text = value,
                style = IgTypography.usernameMedium.copy(fontFeatureSettings = "tnum"),
                color = palette.textSecondary,
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Open $label insights",
            tint = palette.textSecondary,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun DashboardToolRow(
    icon: ImageVector,
    title: String,
    palette: com.example.ui.components.InsightsUiColors,
    showNewBadge: Boolean = false,
    isExternalLink: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = palette.textPrimary,
            modifier = Modifier.size(23.dp),
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = title,
            fontSize = 15.5.sp,
            color = palette.textPrimary,
            modifier = Modifier.weight(1f),
        )
        if (showNewBadge) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(IgBoostBlue)
                    .padding(horizontal = 9.dp, vertical = 3.dp),
            ) {
                Text(
                    text = "New",
                    color = Color.White,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.width(8.dp))
        }
        Icon(
            imageVector = if (isExternalLink) {
                Icons.AutoMirrored.Filled.OpenInNew
            } else {
                Icons.AutoMirrored.Filled.KeyboardArrowRight
            },
            contentDescription = null,
            tint = palette.textSecondary,
            modifier = Modifier.size(if (isExternalLink) 18.dp else 20.dp),
        )
    }
}
