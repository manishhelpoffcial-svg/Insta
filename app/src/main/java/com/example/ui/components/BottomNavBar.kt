package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.ProfileData
import com.example.ui.theme.IgDarkBackground
import com.example.ui.theme.IgDarkHairline
import com.example.ui.theme.IgDarkPill
import com.example.ui.theme.IgNotifRed
import com.example.ui.theme.InstagramTheme
import com.example.ui.viewmodel.MainTab

@Composable
fun InstagramBottomBar(
    activeTab: MainTab,
    profile: ProfileData,
    onSelectTab: (MainTab) -> Unit,
    onProfileTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isReelsDark = activeTab == MainTab.REELS
    val colors = InstagramTheme.colors
    val barBg = if (isReelsDark) IgDarkBackground else colors.background
    val iconTint = if (isReelsDark) Color.White else colors.textPrimary
    val activePillBg = if (isReelsDark) IgDarkPill else colors.pillBackground
    val dividerColor = if (isReelsDark) IgDarkHairline.copy(alpha = 0.5f) else colors.hairline

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(barBg)
            .navigationBarsPadding(),
    ) {
        HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavTabItem(
                selected = activeTab == MainTab.HOME,
                pillColor = activePillBg,
                showPillWhenSelected = false,
                testTag = "nav_home",
                onClick = { onSelectTab(MainTab.HOME) },
            ) {
                IgHomeIcon(
                    filled = activeTab == MainTab.HOME,
                    tint = iconTint,
                )
            }

            NavTabItem(
                selected = activeTab == MainTab.REELS,
                pillColor = activePillBg,
                showPillWhenSelected = true,
                testTag = "nav_reels",
                onClick = { onSelectTab(MainTab.REELS) },
            ) {
                IgReelsIcon(
                    filled = activeTab == MainTab.REELS,
                    tint = iconTint,
                )
            }

            NavTabItem(
                selected = activeTab == MainTab.MESSAGES,
                pillColor = activePillBg,
                showPillWhenSelected = true,
                testTag = "nav_messages",
                onClick = { onSelectTab(MainTab.MESSAGES) },
            ) {
                IgSharePlaneIcon(
                    filled = activeTab == MainTab.MESSAGES,
                    tint = iconTint,
                )
            }

            NavTabItem(
                selected = activeTab == MainTab.EXPLORE,
                pillColor = activePillBg,
                showPillWhenSelected = true,
                testTag = "nav_explore",
                onClick = { onSelectTab(MainTab.EXPLORE) },
            ) {
                IgSearchIcon(
                    bold = activeTab == MainTab.EXPLORE,
                    tint = iconTint,
                )
            }

            NavTabItem(
                selected = activeTab == MainTab.PROFILE,
                pillColor = activePillBg,
                showPillWhenSelected = true,
                testTag = "nav_profile",
                onClick = onProfileTap,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    AvatarView(
                        avatarUrl = profile.avatarUrl,
                        avatarKey = profile.avatarDrawableKey,
                        size = 27.dp,
                        contentDescription = "Profile",
                        modifier = if (activeTab == MainTab.PROFILE) {
                            Modifier.border(1.5.dp, iconTint, CircleShape)
                        } else Modifier,
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 3.dp, y = 2.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(barBg)
                            .padding(1.dp)
                            .clip(CircleShape)
                            .background(IgNotifRed),
                    )
                }
            }
        }
    }
}

@Composable
private fun NavTabItem(
    selected: Boolean,
    pillColor: Color,
    showPillWhenSelected: Boolean,
    testTag: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .testTag(testTag)
            .width(64.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected && showPillWhenSelected) {
            Box(
                modifier = Modifier
                    .width(56.dp)
                    .height(38.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(pillColor),
            )
        }
        content()
    }
}
