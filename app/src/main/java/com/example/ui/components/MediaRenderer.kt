package com.example.ui.components

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.network.MediaClient
import com.example.ui.theme.IgNotifRed
import com.example.ui.theme.IgVerifiedBlue
import com.example.ui.theme.MetaAiRingColors
import com.example.ui.theme.StoryRingColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.cos
import kotlin.math.sin

fun resolveLocalDrawable(key: String): Int {
    return when (key) {
        "cineverxhub" -> R.drawable.avatar_cineverxhub
        "themovietimez" -> R.drawable.avatar_themovietimez
        "blox_ibakke" -> R.drawable.avatar_blox_ibakke
        "nightvibe_rbx" -> R.drawable.avatar_nightvibe_rbx
        "tom_autumn" -> R.drawable.avatar_tom_autumn
        "peter_frames" -> R.drawable.avatar_peter_frames
        "aarav_rides" -> R.drawable.avatar_aarav_rides
        "beth" -> R.drawable.img_reel_beth_1794738710844
        "cute" -> R.drawable.img_reel_cute_1794738711867
        else -> R.drawable.img_feed_cinema_1794738710393
    }
}

fun resolveRawVideoRes(videoUrl: String?): Int? {
    return when (videoUrl) {
        "raw://reel_mentalist_cineverx" -> R.raw.reel_mentalist_cineverx
        "raw://reel_roblox_mailbox" -> R.raw.reel_roblox_mailbox
        "raw://reel_nyc_paradox" -> R.raw.reel_nyc_paradox
        "raw://reel_wolfs_subway" -> R.raw.reel_wolfs_subway
        "raw://reel_music_hits" -> R.raw.reel_music_hits
        else -> null
    }
}

private val videoThumbnailCache = ConcurrentHashMap<Int, ImageBitmap>()

private suspend fun extractRawVideoThumbnail(context: Context, rawResId: Int): ImageBitmap? {
    videoThumbnailCache[rawResId]?.let { return it }
    return withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            context.resources.openRawResourceFd(rawResId)?.use { afd ->
                retriever.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                val frame = retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: retriever.getFrameAtTime(500_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: retriever.frameAtTime
                frame?.asImageBitmap()?.also { bmp ->
                    videoThumbnailCache[rawResId] = bmp
                }
            }
        } catch (_: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }
}

@Composable
fun AvatarView(
    avatarUrl: String?,
    avatarKey: String,
    size: Dp,
    modifier: Modifier = Modifier,
    showGradientRing: Boolean = false,
    ringStrokeWidth: Dp = 2.5.dp,
    contentDescription: String = "User avatar",
) {
    val context = LocalContext.current
    val imageLoader = remember(context) { MediaClient.getImageLoader(context) }

    val outerSize = if (showGradientRing) size + 8.dp else size

    Box(
        modifier = modifier.size(outerSize),
        contentAlignment = Alignment.Center,
    ) {
        if (showGradientRing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .border(
                        width = ringStrokeWidth,
                        brush = Brush.sweepGradient(StoryRingColors),
                        shape = CircleShape,
                    )
            )
        }

        when {
            avatarKey == "meta_ai" -> {
                MetaAiAvatar(size = size)
            }
            avatarKey == "screentime" -> {
                Box(
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(0.5.dp, Color(0xFFE0E0E0), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .background(Color(0xFFEC008C), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "SCREENTIME",
                            color = Color.White,
                            fontSize = (size.value * 0.13f).sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                        )
                    }
                }
            }
            avatarKey == "placeholder" && avatarUrl.isNullOrBlank() -> {
                Box(
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape)
                        .background(Color(0xFFD9DCE0)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = contentDescription,
                        tint = Color.White,
                        modifier = Modifier.size(size * 0.62f),
                    )
                }
            }
            !avatarUrl.isNullOrBlank() -> {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(avatarUrl)
                        .crossfade(true)
                        .build(),
                    imageLoader = imageLoader,
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape)
                        .background(Color(0xFF1A1A1A)),
                    error = {
                        Image(
                            painter = painterResource(id = resolveLocalDrawable(avatarKey)),
                            contentDescription = contentDescription,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(size)
                                .clip(CircleShape),
                        )
                    },
                )
            }
            else -> {
                Image(
                    painter = painterResource(id = resolveLocalDrawable(avatarKey)),
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape),
                )
            }
        }
    }
}

@Composable
fun MetaAiAvatar(size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.White)
            .border(0.5.dp, Color(0xFFE5E7EB), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(size * 0.7f)) {
            val stroke = this.size.minDimension * 0.18f
            drawArc(
                brush = Brush.sweepGradient(MetaAiRingColors),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
    }
}

@Composable
fun MetaAiRingIcon(size: Dp = 20.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val stroke = this.size.minDimension * 0.22f
        drawArc(
            brush = Brush.sweepGradient(MetaAiRingColors),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
}

@Composable
fun VerifiedBlueBadge(size: Dp = 15.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(IgVerifiedBlue),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = "Verified",
            tint = Color.White,
            modifier = Modifier.size(size * 0.7f),
        )
    }
}

@Composable
fun CreatorDashedBadge(size: Dp = 16.dp, tint: Color = Color(0xFF8E9AAF)) {
    Canvas(modifier = Modifier.size(size)) {
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val radius = this.size.minDimension * 0.44f
        val dotCount = 10
        for (i in 0 until dotCount) {
            val angle = (2.0 * Math.PI * i / dotCount).toFloat()
            val x = center.x + radius * cos(angle)
            val y = center.y + radius * sin(angle)
            drawCircle(
                color = tint,
                radius = this.size.minDimension * 0.08f,
                center = Offset(x, y),
            )
        }
        val checkStart = Offset(center.x - radius * 0.35f, center.y)
        val checkMid = Offset(center.x - radius * 0.08f, center.y + radius * 0.28f)
        val checkEnd = Offset(center.x + radius * 0.38f, center.y - radius * 0.24f)
        drawLine(tint, checkStart, checkMid, strokeWidth = this.size.minDimension * 0.11f, cap = StrokeCap.Round)
        drawLine(tint, checkMid, checkEnd, strokeWidth = this.size.minDimension * 0.11f, cap = StrokeCap.Round)
    }
}

@Composable
fun PostMediaImage(
    mediaUrl: String?,
    drawableKey: String,
    videoUrl: String? = null,
    overlayTopCaption: String = "",
    overlaySubtitle: String = "",
    useLetterboxMemeStyle: Boolean = false,
    contentDescription: String = "Post media",
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val imageLoader = remember(context) { MediaClient.getImageLoader(context) }
    val rawVideoResId = remember(videoUrl) { resolveRawVideoRes(videoUrl) }
    var videoThumb by remember(rawVideoResId) {
        mutableStateOf(rawVideoResId?.let { videoThumbnailCache[it] })
    }

    LaunchedEffect(rawVideoResId) {
        if (rawVideoResId != null && videoThumb == null) {
            videoThumb = extractRawVideoThumbnail(context, rawVideoResId)
        }
    }

    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        if (!mediaUrl.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(mediaUrl)
                    .crossfade(true)
                    .build(),
                imageLoader = imageLoader,
                contentDescription = contentDescription,
                contentScale = if (useLetterboxMemeStyle) ContentScale.Fit else ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                error = {
                    Image(
                        painter = painterResource(id = resolveLocalDrawable(drawableKey)),
                        contentDescription = contentDescription,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                },
            )
        } else if (videoThumb != null) {
            Image(
                bitmap = videoThumb!!,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else if (useLetterboxMemeStyle && overlayTopCaption.isNotBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = overlayTopCaption,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    lineHeight = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Image(
                        painter = painterResource(id = resolveLocalDrawable(drawableKey)),
                        contentDescription = contentDescription,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    if (overlaySubtitle.isNotBlank()) {
                        Text(
                            text = overlaySubtitle,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            modifier = Modifier
                                .padding(bottom = 8.dp, start = 6.dp, end = 6.dp)
                                .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }
        } else {
            Image(
                painter = painterResource(id = resolveLocalDrawable(drawableKey)),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (overlaySubtitle.isNotBlank()) {
                Text(
                    text = overlaySubtitle,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 20.dp, start = 16.dp, end = 16.dp)
                        .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun InstagramVideoPlayer(
    videoUrl: String,
    isMuted: Boolean,
    isPlaying: Boolean = true,
    shouldPrepare: Boolean = true,
    resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT,
    onVideoAspectRatioChanged: (Float) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (!shouldPrepare) {
        return
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentIsPlaying by rememberUpdatedState(isPlaying)
    val currentRatioCallback by rememberUpdatedState(onVideoAspectRatioChanged)

    val resolvedUri = remember(videoUrl, context) {
        val rawRes = resolveRawVideoRes(videoUrl)
        if (rawRes != null) {
            Uri.parse("android.resource://${context.packageName}/$rawRes")
        } else {
            Uri.parse(videoUrl)
        }
    }

    var isFirstFrameRendered by remember(resolvedUri) { mutableStateOf(false) }

    val exoPlayer = remember(resolvedUri) {
        val okHttpFactory = OkHttpDataSource.Factory(MediaClient.getOkHttpClient())
        val dataSourceFactory = DefaultDataSource.Factory(context, okHttpFactory)
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(resolvedUri))
                repeatMode = Player.REPEAT_MODE_ONE
                volume = if (isMuted) 0f else 1f
                playWhenReady = isPlaying
                prepare()
            }
    }

    LaunchedEffect(exoPlayer, isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    LaunchedEffect(exoPlayer, isPlaying) {
        exoPlayer.playWhenReady = isPlaying
        if (isPlaying) {
            exoPlayer.play()
        } else {
            exoPlayer.pause()
        }
    }

    DisposableEffect(lifecycleOwner, exoPlayer) {
        val videoListener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                isFirstFrameRendered = true
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    val ratio = (videoSize.width.toFloat() * videoSize.pixelWidthHeightRatio) /
                        videoSize.height.toFloat()
                    if (ratio in 0.35f..2.2f) {
                        currentRatioCallback(ratio)
                    }
                }
            }
        }
        exoPlayer.addListener(videoListener)
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    exoPlayer.pause()
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (currentIsPlaying) {
                        exoPlayer.playWhenReady = true
                        exoPlayer.play()
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            isFirstFrameRendered = false
            exoPlayer.removeListener(videoListener)
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { ctx: Context ->
            (LayoutInflater.from(ctx).inflate(R.layout.ig_texture_player_view, null, false) as PlayerView).apply {
                player = exoPlayer
                useController = false
                setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                this.resizeMode = resizeMode
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            }
        },
        update = { playerView ->
            if (playerView.player !== exoPlayer) {
                playerView.player = exoPlayer
            }
            if (playerView.resizeMode != resizeMode) {
                playerView.resizeMode = resizeMode
            }
        },
        modifier = modifier.alpha(if (isFirstFrameRendered) 1f else 0f),
    )
}

@Composable
fun DoubleTapHeartOverlay(
    burstTrigger: Int,
    modifier: Modifier = Modifier,
) {
    if (burstTrigger <= 0) return

    val scale = remember(burstTrigger) { Animatable(0.3f) }
    val alpha = remember(burstTrigger) { Animatable(1f) }

    LaunchedEffect(burstTrigger) {
        val scaleJob = launch {
            scale.animateTo(1.22f, animationSpec = tween(160, easing = FastOutSlowInEasing))
            scale.animateTo(0.95f, animationSpec = tween(110, easing = FastOutSlowInEasing))
            scale.animateTo(1.04f, animationSpec = tween(90, easing = FastOutSlowInEasing))
        }
        delay(420)
        alpha.animateTo(0f, animationSpec = tween(240))
        scaleJob.join()
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        IgHeartStrokeIcon(
            filled = true,
            tint = IgNotifRed,
            size = 96.dp,
            contentDescription = null,
            modifier = Modifier
                .scale(scale.value)
                .alpha(alpha.value),
        )
    }
}
