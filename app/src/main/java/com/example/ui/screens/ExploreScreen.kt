package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MediaPost
import com.example.ui.components.IgEyeIcon
import com.example.ui.components.MetaAiRingIcon
import com.example.ui.components.PostMediaImage
import com.example.ui.theme.InstagramTheme

@Composable
fun ExploreScreen(
    posts: List<MediaPost>,
    onPostClick: (String) -> Unit,
) {
    val colors = InstagramTheme.colors
    var query by remember { mutableStateOf("") }

    val exploreItems = remember(posts, query) {
        val base = posts.filter { it.showInExplore }.ifEmpty { posts }
        if (query.isBlank()) base
        else {
            val q = query.trim().lowercase()
            base.filter {
                it.caption.lowercase().contains(q) ||
                    it.overlayTopCaption.lowercase().contains(q) ||
                    it.authorUsername.lowercase().contains(q)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding(),
    ) {
        // Search with Meta AI pill bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(colors.pillBackground)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MetaAiRingIcon(size = 19.dp)
                Spacer(Modifier.width(10.dp))
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Search with Meta AI",
                            color = colors.textSecondary,
                            fontSize = 15.sp,
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        cursorBrush = SolidColor(colors.textPrimary),
                        textStyle = TextStyle(color = colors.textPrimary, fontSize = 15.sp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("explore_search_input"),
                    )
                }
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = { query = "" },
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Clear search",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }

        // 3-Column tight 1.5dp gap Explore grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(1.5.dp),
            verticalArrangement = Arrangement.spacedBy(1.5.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(exploreItems, key = { it.id }) { post ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.62f)
                        .background(Color.Black)
                        .clickable { onPostClick(post.id) }
                        .testTag("explore_tile_${post.id}"),
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

                    // Bottom readability scrim
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                                )
                            ),
                    )

                    // Eye icon + formatted view count
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 6.dp, vertical = 6.dp),
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
            }
        }
    }
}
