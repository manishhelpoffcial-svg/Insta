package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.IgDarkTextSecondary
import com.example.ui.theme.IgInsightsMutedText
import com.example.ui.theme.IgLightTextSecondary
import com.example.ui.theme.IgNotifRed
import com.example.ui.theme.InstagramTheme

private fun Modifier.semanticsLabel(label: String?): Modifier =
    if (label != null) this.semantics { contentDescription = label } else this

/**
 * Resolves whether white icons should be displayed based on the active theme or explicit white tint.
 */
@Composable
private fun shouldUseWhiteIcon(tint: Color, forceWhite: Boolean): Boolean {
    if (forceWhite || tint == Color.White) return true
    if (tint == Color.Black) return false
    return InstagramTheme.colors.isDark
}

/**
 * Renders one of the exact supplied transparent PNG icons from the uploaded icon packs.
 * Automatically switches between the white pack (dark backgrounds) and black pack (light backgrounds).
 * Optical scaling (~1.30x inside the fixed Box) compensates for the transparent canvas margins in the supplied PNGs
 * so every glyph aligns cleanly with standard 24dp Android touch/icon dimensions.
 */
@Composable
private fun IgSuppliedIcon(
    @DrawableRes whiteRes: Int,
    @DrawableRes blackRes: Int,
    size: Dp,
    contentDescription: String?,
    tint: Color = InstagramTheme.colors.textPrimary,
    forceWhite: Boolean = false,
    applyColorTint: ColorFilter? = null,
    opticalScale: Float = 1.30f,
    modifier: Modifier = Modifier,
) {
    val useWhite = shouldUseWhiteIcon(tint = tint, forceWhite = forceWhite)
    val resId = if (useWhite) whiteRes else blackRes
    val alpha = when {
        applyColorTint != null -> 1f
        tint == IgLightTextSecondary || tint == IgDarkTextSecondary || tint == IgInsightsMutedText ->
            if (useWhite) 0.78f else 0.72f
        else -> tint.alpha
    }

    Box(
        modifier = modifier
            .size(size)
            .semanticsLabel(contentDescription),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(resId),
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            alpha = alpha,
            colorFilter = applyColorTint,
            modifier = Modifier.requiredSize(size * opticalScale),
        )
    }
}

/**
 * Minimal Instagram-style rotating circular spinner adapted automatically to Dark and Light themes.
 */
@Composable
fun InstagramCircularSpinner(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    strokeWidth: Dp = 2.4.dp,
    color: Color = InstagramTheme.colors.spinnerTint,
) {
    CircularProgressIndicator(
        color = color,
        strokeWidth = strokeWidth,
        strokeCap = StrokeCap.Round,
        modifier = modifier.size(size),
    )
}

/**
 * Bottom Bar Home tab icon - supplied white/black home outline & filled PNGs.
 */
@Composable
fun IgHomeIcon(
    filled: Boolean,
    tint: Color = InstagramTheme.colors.textPrimary,
    size: Dp = 24.dp,
    forceWhite: Boolean = false,
    modifier: Modifier = Modifier,
) {
    IgSuppliedIcon(
        whiteRes = if (filled) R.drawable.ic_zip_w_home_filled else R.drawable.ic_zip_w_home_outline,
        blackRes = if (filled) R.drawable.ic_zip_b_home_filled else R.drawable.ic_zip_b_home_outline,
        size = size,
        contentDescription = "Home",
        tint = tint,
        forceWhite = forceWhite,
        modifier = modifier,
    )
}

/**
 * Bottom Bar Reels tab icon - supplied video_outline / video_filled PNGs matching official Instagram Reels navigation.
 */
@Composable
fun IgReelsIcon(
    filled: Boolean,
    tint: Color = InstagramTheme.colors.textPrimary,
    size: Dp = 24.dp,
    forceWhite: Boolean = false,
    modifier: Modifier = Modifier,
) {
    IgSuppliedIcon(
        whiteRes = if (filled) R.drawable.ic_zip_w_video_filled else R.drawable.ic_zip_w_video_outline,
        blackRes = if (filled) R.drawable.ic_zip_b_video_filled else R.drawable.ic_zip_b_video_outline,
        size = size,
        contentDescription = "Reels",
        tint = tint,
        forceWhite = forceWhite,
        modifier = modifier,
    )
}

/**
 * Classic clapperboard Reels icon from supplied ZIP pack.
 */
@Composable
fun IgReelsClapperIcon(
    tint: Color = InstagramTheme.colors.textPrimary,
    size: Dp = 24.dp,
    forceWhite: Boolean = false,
    modifier: Modifier = Modifier,
) {
    IgSuppliedIcon(
        whiteRes = R.drawable.ic_zip_w_reels,
        blackRes = R.drawable.ic_zip_b_reels,
        size = size,
        contentDescription = "Reels",
        tint = tint,
        forceWhite = forceWhite,
        modifier = modifier,
    )
}

/**
 * Profile Tab 1: Authentic 3x3 Instagram post grid wireframe icon.
 */
@Composable
fun IgGridTabIcon(
    active: Boolean = true,
    tint: Color = InstagramTheme.colors.textPrimary,
    size: Dp = 22.dp,
    contentDescription: String? = "Posts grid",
    modifier: Modifier = Modifier,
) {
    val resId = if (active) R.drawable.ic_ig_grid_filled else R.drawable.ic_ig_grid
    Icon(
        painter = painterResource(resId),
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier
            .size(size)
            .semanticsLabel(contentDescription),
    )
}

/**
 * Profile Tab 2 & Reel Grid Overlay Badge: supplied video_outline icon.
 */
@Composable
fun IgReelsSquircleIcon(
    tint: Color = InstagramTheme.colors.textPrimary,
    size: Dp = 22.dp,
    filled: Boolean = false,
    forceWhite: Boolean = false,
    contentDescription: String? = "Reels",
    modifier: Modifier = Modifier,
) {
    IgSuppliedIcon(
        whiteRes = if (filled) R.drawable.ic_zip_w_video_filled else R.drawable.ic_zip_w_video_outline,
        blackRes = if (filled) R.drawable.ic_zip_b_video_filled else R.drawable.ic_zip_b_video_outline,
        size = size,
        contentDescription = contentDescription,
        tint = tint,
        forceWhite = forceWhite,
        modifier = modifier,
    )
}

/**
 * Profile Tab 3 & Engagement Bar Repost Icon: supplied repost PNGs.
 */
@Composable
fun IgRepostLoopIcon(
    tint: Color = InstagramTheme.colors.textPrimary,
    size: Dp = 23.dp,
    @Suppress("UNUSED_PARAMETER") strokeWidthDp: Dp = 2.dp,
    forceWhite: Boolean = false,
    contentDescription: String? = "Repost",
    modifier: Modifier = Modifier,
) {
    val isAccentGreen = tint == Color(0xFF22C55E) || tint == Color(0xFF4ADE80)
    IgSuppliedIcon(
        whiteRes = R.drawable.ic_zip_w_repost,
        blackRes = R.drawable.ic_zip_b_repost,
        size = size,
        contentDescription = contentDescription,
        tint = tint,
        forceWhite = forceWhite,
        applyColorTint = if (isAccentGreen) ColorFilter.tint(tint) else null,
        modifier = modifier,
    )
}

/**
 * Profile Tab 4: Tagged profile card icon from supplied ZIP pack (`profile_card.png`).
 */
@Composable
fun IgTaggedPersonIcon(
    tint: Color = InstagramTheme.colors.textPrimary,
    size: Dp = 23.dp,
    forceWhite: Boolean = false,
    contentDescription: String? = "Tagged",
    modifier: Modifier = Modifier,
) {
    IgSuppliedIcon(
        whiteRes = R.drawable.ic_zip_w_profile_card,
        blackRes = R.drawable.ic_zip_b_profile_card,
        size = size,
        contentDescription = contentDescription,
        tint = tint,
        forceWhite = forceWhite,
        modifier = modifier,
    )
}

/**
 * Post Engagement 1 & Header Notifications: supplied heart outline / filled red / monochrome PNGs.
 */
@Composable
fun IgHeartStrokeIcon(
    filled: Boolean,
    tint: Color = InstagramTheme.colors.textPrimary,
    size: Dp = 24.dp,
    @Suppress("UNUSED_PARAMETER") strokeWidthDp: Dp = 2.dp,
    forceWhite: Boolean = false,
    contentDescription: String? = "Like",
    modifier: Modifier = Modifier,
) {
    val isRed = tint == Color(0xFFED4956) || tint == IgNotifRed
    val (whiteRes, blackRes) = when {
        filled && isRed -> R.drawable.ic_zip_w_heart_filled_red to R.drawable.ic_zip_b_heart_filled_red
        filled -> R.drawable.ic_zip_w_heart_filled to R.drawable.ic_zip_b_heart_filled
        else -> R.drawable.ic_zip_w_heart_outline to R.drawable.ic_zip_b_heart_outline
    }
    IgSuppliedIcon(
        whiteRes = whiteRes,
        blackRes = blackRes,
        size = size,
        contentDescription = contentDescription,
        tint = if (filled && isRed) Color.Unspecified else tint,
        forceWhite = forceWhite,
        modifier = modifier,
    )
}

/**
 * Post Engagement 2: supplied speech comment bubble PNG (`comment.png`).
 */
@Composable
fun IgCommentBubbleIcon(
    tint: Color = InstagramTheme.colors.textPrimary,
    size: Dp = 23.dp,
    @Suppress("UNUSED_PARAMETER") strokeWidthDp: Dp = 2.dp,
    forceWhite: Boolean = false,
    contentDescription: String? = "Comment",
    modifier: Modifier = Modifier,
) {
    IgSuppliedIcon(
        whiteRes = R.drawable.ic_zip_w_comment,
        blackRes = R.drawable.ic_zip_b_comment,
        size = size,
        contentDescription = contentDescription,
        tint = tint,
        forceWhite = forceWhite,
        modifier = modifier,
    )
}

/**
 * Post Engagement 4 / Bottom Bar Messages Icon:
 * Uses supplied `send_fold` (outline) and `send_filled` (filled/active) PNGs matching Instagram UI.
 */
@Composable
fun IgSharePlaneIcon(
    filled: Boolean = false,
    tint: Color = InstagramTheme.colors.textPrimary,
    size: Dp = 23.dp,
    @Suppress("UNUSED_PARAMETER") strokeWidthDp: Dp = 2.dp,
    forceWhite: Boolean = false,
    contentDescription: String? = "Share",
    modifier: Modifier = Modifier,
) {
    IgSuppliedIcon(
        whiteRes = if (filled) R.drawable.ic_zip_w_send_filled else R.drawable.ic_zip_w_send_fold,
        blackRes = if (filled) R.drawable.ic_zip_b_send_filled else R.drawable.ic_zip_b_send_fold,
        size = size,
        contentDescription = contentDescription,
        tint = tint,
        forceWhite = forceWhite,
        modifier = modifier,
    )
}

/**
 * Post Engagement 5: Bookmark icon (`bookmark.png` outline from ZIP; crisp matching filled vector when saved).
 */
@Composable
fun IgBookmarkStrokeIcon(
    filled: Boolean,
    tint: Color = InstagramTheme.colors.textPrimary,
    size: Dp = 23.dp,
    @Suppress("UNUSED_PARAMETER") strokeWidthDp: Dp = 2.dp,
    forceWhite: Boolean = false,
    contentDescription: String? = "Save",
    modifier: Modifier = Modifier,
) {
    if (filled) {
        val resolvedTint = if (forceWhite) Color.White else tint
        Icon(
            painter = painterResource(R.drawable.ic_ig_bookmark_filled),
            contentDescription = contentDescription,
            tint = resolvedTint,
            modifier = modifier
                .size(size)
                .semanticsLabel(contentDescription),
        )
    } else {
        IgSuppliedIcon(
            whiteRes = R.drawable.ic_zip_w_bookmark,
            blackRes = R.drawable.ic_zip_b_bookmark,
            size = size,
            contentDescription = contentDescription,
            tint = tint,
            forceWhite = forceWhite,
            modifier = modifier,
        )
    }
}

/**
 * Bottom Bar Search / Explore icon from supplied ZIP pack (`search.png`).
 */
@Composable
fun IgSearchIcon(
    @Suppress("UNUSED_PARAMETER") bold: Boolean = false,
    tint: Color = InstagramTheme.colors.textPrimary,
    size: Dp = 24.dp,
    forceWhite: Boolean = false,
    modifier: Modifier = Modifier,
) {
    IgSuppliedIcon(
        whiteRes = R.drawable.ic_zip_w_search,
        blackRes = R.drawable.ic_zip_b_search,
        size = size,
        contentDescription = "Search",
        tint = tint,
        forceWhite = forceWhite,
        modifier = modifier,
    )
}

/**
 * Header `+` create icon from supplied ZIP pack (`plus.png`).
 */
@Composable
fun IgPlusIcon(
    tint: Color = InstagramTheme.colors.textPrimary,
    size: Dp = 22.dp,
    forceWhite: Boolean = false,
    contentDescription: String? = "Create",
    modifier: Modifier = Modifier,
) {
    IgSuppliedIcon(
        whiteRes = R.drawable.ic_zip_w_plus,
        blackRes = R.drawable.ic_zip_b_plus,
        size = size,
        contentDescription = contentDescription,
        tint = tint,
        forceWhite = forceWhite,
        modifier = modifier,
    )
}

/**
 * Camera icon from supplied ZIP pack (`camera.png`).
 */
@Composable
fun IgCameraIcon(
    tint: Color = InstagramTheme.colors.textPrimary,
    size: Dp = 23.dp,
    forceWhite: Boolean = false,
    contentDescription: String? = "Camera",
    modifier: Modifier = Modifier,
) {
    IgSuppliedIcon(
        whiteRes = R.drawable.ic_zip_w_camera,
        blackRes = R.drawable.ic_zip_b_camera,
        size = size,
        contentDescription = contentDescription,
        tint = tint,
        forceWhite = forceWhite,
        modifier = modifier,
    )
}

/**
 * New message / compose icon from supplied ZIP pack (`compose.png`).
 */
@Composable
fun IgComposeIcon(
    tint: Color = InstagramTheme.colors.textPrimary,
    size: Dp = 23.dp,
    forceWhite: Boolean = false,
    contentDescription: String? = "New message",
    modifier: Modifier = Modifier,
) {
    IgSuppliedIcon(
        whiteRes = R.drawable.ic_zip_w_compose,
        blackRes = R.drawable.ic_zip_b_compose,
        size = size,
        contentDescription = contentDescription,
        tint = tint,
        forceWhite = forceWhite,
        modifier = modifier,
    )
}

/**
 * Add friend / suggested user icon from supplied ZIP pack (`add_user.png`).
 */
@Composable
fun IgAddUserIcon(
    tint: Color = InstagramTheme.colors.textPrimary,
    size: Dp = 15.dp,
    forceWhite: Boolean = false,
    contentDescription: String? = "Follow suggestion",
    modifier: Modifier = Modifier,
) {
    IgSuppliedIcon(
        whiteRes = R.drawable.ic_zip_w_add_user,
        blackRes = R.drawable.ic_zip_b_add_user,
        size = size,
        contentDescription = contentDescription,
        tint = tint,
        forceWhite = forceWhite,
        modifier = modifier,
    )
}

/**
 * Profile circle fallback icon from supplied ZIP pack (`profile_circle.png`).
 */
@Composable
fun IgProfileCircleIcon(
    tint: Color = InstagramTheme.colors.textPrimary,
    size: Dp = 26.dp,
    forceWhite: Boolean = false,
    contentDescription: String? = "Profile",
    modifier: Modifier = Modifier,
) {
    IgSuppliedIcon(
        whiteRes = R.drawable.ic_zip_w_profile_circle,
        blackRes = R.drawable.ic_zip_b_profile_circle,
        size = size,
        contentDescription = contentDescription,
        tint = tint,
        forceWhite = forceWhite,
        modifier = modifier,
    )
}

/**
 * Clean back arrow vector icon automatically tinted to active theme.
 */
@Composable
fun IgBackIcon(
    tint: Color = InstagramTheme.colors.textPrimary,
    size: Dp = 22.dp,
    contentDescription: String? = "Back",
    modifier: Modifier = Modifier,
) {
    Icon(
        painter = painterResource(R.drawable.ic_ig_back),
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier
            .size(size)
            .semanticsLabel(contentDescription),
    )
}

/**
 * Three-dot overflow menu vector icon.
 */
@Composable
fun IgMoreDotsIcon(
    tint: Color = InstagramTheme.colors.textPrimary,
    size: Dp = 20.dp,
    contentDescription: String? = "More",
    modifier: Modifier = Modifier,
) {
    Icon(
        painter = painterResource(R.drawable.ic_ig_more_dots),
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier
            .size(size)
            .semanticsLabel(contentDescription),
    )
}

/**
 * Authentic Instagram Views eye icon.
 */
@Composable
fun IgEyeIcon(
    tint: Color = Color.White,
    size: Dp = 14.dp,
    modifier: Modifier = Modifier,
) {
    Icon(
        painter = painterResource(R.drawable.ic_views_eye),
        contentDescription = "Views",
        tint = tint,
        modifier = modifier
            .size(size)
            .semanticsLabel("Views"),
    )
}

/**
 * Share tray / export icon [↑] shown in Account Insights top bar.
 */
@Composable
fun IgShareBoxUpIcon(
    tint: Color,
    size: Dp = 22.dp,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeWidth = 2.dp.toPx()
        val strokeStyle = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)

        val tray = Path().apply {
            moveTo(w * 0.34f, h * 0.36f)
            lineTo(w * 0.24f, h * 0.36f)
            quadraticTo(w * 0.15f, h * 0.36f, w * 0.15f, h * 0.46f)
            lineTo(w * 0.15f, h * 0.78f)
            quadraticTo(w * 0.15f, h * 0.87f, w * 0.24f, h * 0.87f)
            lineTo(w * 0.76f, h * 0.87f)
            quadraticTo(w * 0.85f, h * 0.87f, w * 0.85f, h * 0.78f)
            lineTo(w * 0.85f, h * 0.46f)
            quadraticTo(w * 0.85f, h * 0.36f, w * 0.76f, h * 0.36f)
            lineTo(w * 0.66f, h * 0.36f)
        }
        drawPath(tray, color = tint, style = strokeStyle)

        drawLine(
            color = tint,
            start = Offset(w * 0.50f, h * 0.62f),
            end = Offset(w * 0.50f, h * 0.16f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        val head = Path().apply {
            moveTo(w * 0.35f, h * 0.30f)
            lineTo(w * 0.50f, h * 0.15f)
            lineTo(w * 0.65f, h * 0.30f)
        }
        drawPath(head, color = tint, style = strokeStyle)
    }
}

/**
 * Verified scalloped rosette badge icon for "Get Meta Verified" card.
 */
@Composable
fun MetaVerifiedRosetteIcon(
    tint: Color,
    size: Dp = 24.dp,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f
        val points = 16
        val outerR = w * 0.42f
        val innerR = w * 0.34f
        val rosette = Path()
        for (i in 0 until points) {
            val angle = (i * 2.0 * Math.PI / points)
            val r = if (i % 2 == 0) outerR else innerR
            val x = cx + (r * kotlin.math.cos(angle)).toFloat()
            val y = cy + (r * kotlin.math.sin(angle)).toFloat()
            if (i == 0) rosette.moveTo(x, y) else rosette.lineTo(x, y)
        }
        rosette.close()
        drawPath(
            path = rosette,
            color = tint,
            style = Stroke(width = 1.9.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        val check = Path().apply {
            moveTo(cx - w * 0.14f, cy + h * 0.01f)
            lineTo(cx - w * 0.03f, cy + h * 0.12f)
            lineTo(cx + w * 0.17f, cy - h * 0.10f)
        }
        drawPath(
            path = check,
            color = tint,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

/**
 * Authentic Instagram hamburger menu icon.
 */
@Composable
fun IgTwoLineMenuIcon(
    tint: Color = InstagramTheme.colors.textPrimary,
    size: Dp = 20.dp,
    modifier: Modifier = Modifier,
) {
    Icon(
        painter = painterResource(R.drawable.ic_ig_menu),
        contentDescription = "Menu",
        tint = tint,
        modifier = modifier
            .size(size)
            .semanticsLabel("Menu"),
    )
}
