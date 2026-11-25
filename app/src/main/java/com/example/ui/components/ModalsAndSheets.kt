package com.example.ui.components

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.DefaultSampleData
import com.example.data.model.MediaPost
import com.example.data.model.ProfileData
import com.example.data.model.StoryItem
import com.example.ui.theme.IgBoostBlue
import com.example.ui.theme.IgLinkBlue
import com.example.ui.theme.IgNotifRed
import com.example.ui.theme.InstagramTheme
import com.example.ui.theme.ThemeMode
import com.example.ui.viewmodel.FetchAccountUiState
import kotlinx.coroutines.delay

internal val PROFILE_LOADING_MESSAGES = listOf(
    "Loading...",
    "Collecting posts...",
    "Almost done...",
)

@Composable
fun rememberProgressiveLoadingLabel(isLoading: Boolean): String {
    var stepIndex by remember(isLoading) { mutableIntStateOf(0) }
    LaunchedEffect(isLoading) {
        if (!isLoading) return@LaunchedEffect
        stepIndex = 0
        delay(1800L)
        stepIndex = 1
        delay(2200L)
        stepIndex = 2
    }
    return PROFILE_LOADING_MESSAGES[stepIndex.coerceIn(0, PROFILE_LOADING_MESSAGES.lastIndex)]
}

@Composable
fun FetchInstagramAccountModal(
    fetchState: FetchAccountUiState,
    onDismiss: () -> Unit,
    onSubmitUsername: (username: String) -> Unit,
    onInputChanged: () -> Unit = {},
    onLogoutClick: (() -> Unit)? = null,
    themeMode: ThemeMode? = null,
    onSelectThemeMode: ((ThemeMode) -> Unit)? = null,
) {
    val colors = InstagramTheme.colors
    val keyboardController = LocalSoftwareKeyboardController.current
    var username by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    val isLoading = fetchState is FetchAccountUiState.Loading
    val loadingLabel = rememberProgressiveLoadingLabel(isLoading)

    fun submit() {
        val clean = username.trim().removePrefix("@").trim()
        if (clean.isBlank()) {
            localError = "Enter a username"
            return
        }
        localError = null
        keyboardController?.hide()
        onSubmitUsername(clean)
    }

    Dialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !isLoading,
            dismissOnClickOutside = !isLoading,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.cardBackground)
                .border(0.75.dp, colors.hairline, RoundedCornerShape(16.dp))
                .padding(16.dp)
                .testTag("triple_tap_username_box"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it
                    if (localError != null) localError = null
                    onInputChanged()
                },
                placeholder = {
                    Text(
                        text = "Enter username",
                        color = colors.textSecondary,
                        fontSize = 14.sp,
                    )
                },
                singleLine = true,
                enabled = !isLoading,
                shape = RoundedCornerShape(10.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { submit() }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colors.background,
                    unfocusedContainerColor = colors.background,
                    disabledContainerColor = colors.background,
                    focusedBorderColor = IgBoostBlue,
                    unfocusedBorderColor = colors.hairline,
                    disabledBorderColor = colors.hairline,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    cursorColor = IgBoostBlue,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("fetch_username_input"),
            )

            Button(
                onClick = { submit() },
                enabled = !isLoading,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = IgBoostBlue,
                    contentColor = Color.White,
                    disabledContainerColor = IgBoostBlue.copy(alpha = 0.65f),
                    disabledContentColor = Color.White,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("fetch_submit_button"),
            ) {
                if (isLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = loadingLabel,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            modifier = Modifier.testTag("fetch_loading_label"),
                        )
                    }
                } else {
                    Text(
                        text = "Enter",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                    )
                }
            }

            val errText = localError ?: (fetchState as? FetchAccountUiState.Error)?.message
            if (!errText.isNullOrBlank()) {
                Text(
                    text = errText,
                    color = IgNotifRed,
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("fetch_error_text"),
                )
            }

            if (themeMode != null && onSelectThemeMode != null) {
                HorizontalDivider(thickness = 0.5.dp, color = colors.hairline)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Appearance",
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            val selected = themeMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) colors.textPrimary else colors.pillBackground)
                                    .clickable { onSelectThemeMode(mode) }
                                    .testTag("theme_mode_${mode.name.lowercase()}"),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = when (mode) {
                                        ThemeMode.SYSTEM -> "Auto"
                                        ThemeMode.DARK -> "Dark"
                                        ThemeMode.LIGHT -> "Light"
                                    },
                                    color = if (selected) colors.background else colors.textPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            }

            if (onLogoutClick != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Log Out of Account",
                    color = IgNotifRed,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDismiss()
                            onLogoutClick()
                        }
                        .padding(vertical = 8.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    post: MediaPost,
    currentUser: ProfileData,
    onDismiss: () -> Unit,
    onSendComment: (String) -> Unit,
) {
    val colors = InstagramTheme.colors
    var commentDraft by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.background,
        contentColor = colors.textPrimary,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 340.dp, max = 560.dp)
                .imePadding()
                .navigationBarsPadding(),
        ) {
            Text(
                text = "Comments",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = colors.textPrimary,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 12.dp),
            )
            HorizontalDivider(thickness = 0.5.dp, color = colors.hairline)

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                if (post.commentsList.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = "No comments yet",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = colors.textPrimary,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Start the conversation.",
                                fontSize = 14.sp,
                                color = colors.textSecondary,
                            )
                        }
                    }
                } else {
                    items(post.commentsList, key = { it.id }) { comment ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            AvatarView(
                                avatarUrl = null,
                                avatarKey = if (comment.username == currentUser.username) currentUser.avatarDrawableKey else "placeholder",
                                size = 34.dp,
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = comment.username,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = colors.textPrimary,
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = comment.timeAgo,
                                        fontSize = 12.sp,
                                        color = colors.textSecondary,
                                    )
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = comment.text,
                                    fontSize = 14.sp,
                                    color = colors.textPrimary,
                                )
                            }
                            Icon(
                                imageVector = Icons.Outlined.FavoriteBorder,
                                contentDescription = "Like comment",
                                tint = colors.textSecondary,
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .size(16.dp),
                            )
                        }
                    }
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = colors.hairline)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AvatarView(
                    avatarUrl = currentUser.avatarUrl,
                    avatarKey = currentUser.avatarDrawableKey,
                    size = 34.dp,
                )
                Spacer(Modifier.width(10.dp))
                OutlinedTextField(
                    value = commentDraft,
                    onValueChange = { commentDraft = it },
                    placeholder = { Text("Add a comment for ${post.authorUsername}...", fontSize = 13.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("comment_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.hairline,
                        unfocusedBorderColor = colors.hairline,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                    ),
                )
                Spacer(Modifier.width(8.dp))
                TextButton(
                    onClick = {
                        if (commentDraft.isNotBlank()) {
                            onSendComment(commentDraft)
                            commentDraft = ""
                        }
                    },
                    enabled = commentDraft.isNotBlank(),
                    modifier = Modifier.testTag("post_comment_button"),
                ) {
                    Text(
                        text = "Post",
                        color = if (commentDraft.isNotBlank()) IgLinkBlue else colors.textSecondary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileBottomSheet(
    profile: ProfileData,
    onDismiss: () -> Unit,
    onSave: (displayName: String, username: String, bio: String, dashboardSubtitle: String) -> Unit,
) {
    val colors = InstagramTheme.colors
    var displayName by remember { mutableStateOf(profile.displayName) }
    var username by remember { mutableStateOf(profile.username) }
    var bio by remember { mutableStateOf(profile.bio) }
    var dashboardText by remember { mutableStateOf(profile.dashboardSubtitle) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.background,
        contentColor = colors.textPrimary,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Edit profile",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = colors.textPrimary,
            )
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("Bio") },
                minLines = 3,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = dashboardText,
                onValueChange = { dashboardText = it },
                label = { Text("Dashboard subtitle") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    onSave(displayName, username, bio, dashboardText)
                    onDismiss()
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IgBoostBlue, contentColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text("Done", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun EditNoteDialog(
    initialNote: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var draft by remember { mutableStateOf(initialNote) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Share a note", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = draft,
                onValueChange = { if (it.length <= 60) draft = it },
                placeholder = { Text("Share what's on your mind...") },
                singleLine = false,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(draft.ifBlank { "Unpopular opinion..." })
                onDismiss()
            }) {
                Text("Share", color = IgLinkBlue, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
fun StoryViewerOverlay(
    story: StoryItem,
    ownProfile: ProfileData,
    onClose: () -> Unit,
    onAuthorClick: (String) -> Unit = {},
) {
    BackHandler(onBack = onClose)
    var liked by remember { mutableStateOf(false) }
    val resolvedUsername = if (story.isOwnStory) ownProfile.username else story.username
    val isVerified = remember(resolvedUsername, ownProfile) {
        if (story.isOwnStory) ownProfile.isVerified else DefaultSampleData.isVerifiedCreator(resolvedUsername)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        PostMediaImage(
            mediaUrl = null,
            drawableKey = story.storyImageKey,
            contentDescription = story.username,
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onClose),
        )

        // Top progress + author header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.9f)),
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onAuthorClick(resolvedUsername) }
                        .padding(vertical = 4.dp, horizontal = 2.dp),
                ) {
                    AvatarView(
                        avatarUrl = if (story.isOwnStory) ownProfile.avatarUrl else story.avatarUrl,
                        avatarKey = if (story.isOwnStory) ownProfile.avatarDrawableKey else story.avatarKey,
                        size = 34.dp,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = Color.White)) {
                                append(resolvedUsername)
                            }
                        },
                        fontSize = 14.sp,
                    )
                    if (isVerified) {
                        Spacer(Modifier.width(5.dp))
                        VerifiedBlueBadge(size = 13.dp)
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = story.timeAgo,
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.75f),
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "Close story", tint = Color.White)
                }
            }
        }

        // Bottom caption + heart
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = story.storyCaption.ifBlank { "Story by ${story.username}" },
                color = Color.White,
                fontSize = 15.sp,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { liked = !liked }) {
                Icon(
                    imageVector = if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Like story",
                    tint = if (liked) IgNotifRed else Color.White,
                )
            }
        }
    }
}
