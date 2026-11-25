package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ExploreOff
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DmThread
import com.example.data.model.ProfileData
import com.example.ui.components.AvatarView
import com.example.ui.components.IgBackIcon
import com.example.ui.components.IgCameraIcon
import com.example.ui.components.IgComposeIcon
import com.example.ui.components.MetaAiRingIcon
import com.example.ui.components.PostMediaImage
import com.example.ui.components.VerifiedBlueBadge
import com.example.ui.theme.IgBoostBlue
import com.example.ui.theme.IgLinkBlue
import com.example.ui.theme.IgNotifRed
import com.example.ui.theme.IgOnlineGreen
import com.example.ui.theme.IgTypography
import com.example.ui.theme.InstagramTheme

@Composable
fun MessagesScreen(
    profile: ProfileData,
    threads: List<DmThread>,
    onThreadClick: (String) -> Unit,
    onEditNoteClick: () -> Unit,
) {
    val colors = InstagramTheme.colors
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Primary") }

    val filteredThreads = remember(threads, searchQuery, selectedFilter) {
        val byCat = if (selectedFilter == "All") threads else threads.filter { it.category == selectedFilter }
        if (searchQuery.isBlank()) byCat
        else {
            val q = searchQuery.trim().lowercase()
            threads.filter {
                it.username.lowercase().contains(q) ||
                    it.displayName.lowercase().contains(q) ||
                    it.lastMessagePreview.lowercase().contains(q)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding(),
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                    contentDescription = "Inbox Menu",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(24.dp),
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            ) {
                Text(
                    text = if (profile.username.length > 10) profile.username.take(10) + "..." else profile.username,
                    style = IgTypography.screenHeading,
                    color = colors.textPrimary,
                    maxLines = 1,
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Switch account",
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
                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = "Insights",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(23.dp),
                    )
                }
                IconButton(onClick = { onThreadClick("dm_meta_ai") }) {
                    IgComposeIcon(
                        tint = colors.textPrimary,
                        size = 22.dp,
                        contentDescription = "New Message",
                    )
                }
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // Search or ask Meta AI pill
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .height(42.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(colors.pillBackground)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MetaAiRingIcon(size = 19.dp)
                    Spacer(Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search or ask Meta AI",
                                color = colors.textSecondary,
                                fontSize = 15.sp,
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            cursorBrush = SolidColor(colors.textPrimary),
                            textStyle = TextStyle(color = colors.textPrimary, fontSize = 15.sp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dm_search_input"),
                        )
                    }
                }
            }

            // Notes & Map Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    // User Note Bubble + Avatar
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable(onClick = onEditNoteClick),
                    ) {
                        Box(contentAlignment = Alignment.TopCenter) {
                            AvatarView(
                                avatarUrl = profile.avatarUrl,
                                avatarKey = profile.avatarDrawableKey,
                                size = 72.dp,
                                modifier = Modifier.padding(top = 24.dp),
                            )
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                shadowElevation = 3.dp,
                                color = if (colors.isDark) colors.cardBackground else Color.White,
                                modifier = Modifier.widthIn(max = 104.dp),
                            ) {
                                Text(
                                    text = profile.noteText,
                                    color = colors.textPrimary,
                                    fontSize = 12.sp,
                                    lineHeight = 14.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Your note",
                            fontSize = 12.sp,
                            color = colors.textSecondary,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.ExploreOff,
                                contentDescription = "Location off",
                                tint = IgNotifRed,
                                modifier = Modifier.size(11.dp),
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = "Location off",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textPrimary,
                            )
                        }
                    }

                    // Map Globe Icon
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 24.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(Color(0xFFA7F3D0), Color(0xFF93C5FD), Color(0xFF60A5FA))
                                    )
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Public,
                                contentDescription = "Friend Map",
                                tint = Color(0xFF1D4ED8).copy(alpha = 0.55f),
                                modifier = Modifier.size(56.dp),
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Map",
                            fontSize = 12.sp,
                            color = colors.textPrimary,
                        )
                    }
                }
            }

            // Filter Chips Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilterPill(
                        label = "Filters",
                        selected = false,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Tune,
                                contentDescription = null,
                                tint = colors.textPrimary,
                                modifier = Modifier.size(15.dp),
                            )
                        },
                        trailingArrow = true,
                        onClick = { selectedFilter = "All" },
                    )
                    listOf("Primary", "Requests", "General").forEach { filter ->
                        FilterPill(
                            label = filter,
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
            }

            // Chat List
            items(filteredThreads, key = { it.id }) { thread ->
                DmThreadRow(
                    thread = thread,
                    profile = profile,
                    onClick = { onThreadClick(thread.id) },
                )
            }

            // Accounts to follow footer section
            item {
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Accounts to follow",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "See all",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = IgLinkBlue,
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(modifier = Modifier.size(46.dp), contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .rotate(10f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, Color.White, RoundedCornerShape(10.dp)),
                            ) {
                                PostMediaImage(
                                    mediaUrl = null,
                                    drawableKey = "cute",
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .rotate(-8f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, Color.White, RoundedCornerShape(10.dp)),
                            ) {
                                PostMediaImage(
                                    mediaUrl = null,
                                    drawableKey = "feed_cinema",
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun FilterPill(
    label: String,
    selected: Boolean,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingArrow: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = InstagramTheme.colors
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) colors.pillBackground else Color.Transparent)
            .then(
                if (!selected) Modifier.border(1.dp, colors.hairline, RoundedCornerShape(18.dp))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = label,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary,
        )
        if (trailingArrow) {
            Spacer(Modifier.width(3.dp))
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = colors.textPrimary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun DmThreadRow(
    thread: DmThread,
    profile: ProfileData,
    onClick: () -> Unit,
) {
    val colors = InstagramTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("dm_row_${thread.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(56.dp)) {
            AvatarView(
                avatarUrl = if (thread.id == "dm_self") profile.avatarUrl else thread.avatarUrl,
                avatarKey = if (thread.id == "dm_self") profile.avatarDrawableKey else thread.avatarKey,
                size = 54.dp,
            )
            if (thread.isOnline) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-1).dp, y = (-1).dp)
                        .size(15.dp)
                        .clip(CircleShape)
                        .background(colors.background)
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(IgOnlineGreen),
                )
            }
        }

        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = thread.displayName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    color = colors.textPrimary,
                )
                if (thread.isVerifiedBlue) {
                    Spacer(Modifier.width(5.dp))
                    VerifiedBlueBadge(size = 15.dp)
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = thread.lastMessagePreview,
                fontSize = 13.5.sp,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun ChatConversationScreen(
    thread: DmThread,
    profile: ProfileData,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit,
) {
    BackHandler(onBack = onBack)
    val colors = InstagramTheme.colors
    var draft by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(thread.messages.size) {
        if (thread.messages.isNotEmpty()) {
            listState.animateScrollToItem(thread.messages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        // Chat Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("chat_back_button")) {
                IgBackIcon(
                    tint = colors.textPrimary,
                    size = 22.dp,
                    contentDescription = "Back",
                )
            }
            AvatarView(
                avatarUrl = if (thread.id == "dm_self") profile.avatarUrl else thread.avatarUrl,
                avatarKey = if (thread.id == "dm_self") profile.avatarDrawableKey else thread.avatarKey,
                size = 34.dp,
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = thread.displayName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = colors.textPrimary,
                    )
                    if (thread.isVerifiedBlue) {
                        Spacer(Modifier.width(4.dp))
                        VerifiedBlueBadge(size = 14.dp)
                    }
                }
                Text(
                    text = if (thread.isOnline) "Active now" else thread.username,
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                )
            }
            IconButton(onClick = {}) {
                Icon(Icons.Outlined.Call, contentDescription = "Audio call", tint = colors.textPrimary)
            }
            IconButton(onClick = {}) {
                Icon(Icons.Outlined.Videocam, contentDescription = "Video call", tint = colors.textPrimary)
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = colors.hairline)

        // Messages
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(thread.messages, key = { it.id }) { msg ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (msg.senderIsMe) Arrangement.End else Arrangement.Start,
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 270.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (msg.senderIsMe) IgBoostBlue else colors.pillBackground)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Text(
                            text = msg.text,
                            color = if (msg.senderIsMe) Color.White else colors.textPrimary,
                            fontSize = 14.5.sp,
                            lineHeight = 19.sp,
                        )
                    }
                }
            }
        }

        // Composer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(colors.pillBackground)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(IgBoostBlue),
                contentAlignment = Alignment.Center,
            ) {
                IgCameraIcon(
                    tint = Color.White,
                    forceWhite = true,
                    size = 18.dp,
                    contentDescription = "Camera",
                )
            }
            Spacer(Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (draft.isEmpty()) {
                    Text(
                        text = "Message...",
                        color = colors.textSecondary,
                        fontSize = 15.sp,
                    )
                }
                BasicTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    cursorBrush = SolidColor(colors.textPrimary),
                    textStyle = TextStyle(color = colors.textPrimary, fontSize = 15.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("chat_message_input"),
                )
            }
            if (draft.isNotBlank()) {
                IconButton(
                    onClick = {
                        onSendMessage(draft)
                        draft = ""
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("chat_send_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = IgBoostBlue,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}
