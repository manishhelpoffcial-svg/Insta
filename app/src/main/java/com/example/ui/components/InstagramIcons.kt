package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R

private fun Modifier.semanticsLabel(label: String?): Modifier =
    if (label != null) this.semantics { contentDescription = label } else this

/**
 * Bottom Bar Home tab icon - uses authentic Instagram Home outline/filled icons,
 * automatically switching between Light mode (black) and Dark mode (white).
 */
@Composable
fun IgHomeIcon(
    filled: Boolean,
    tint: Color,
    size: Dp = 25.dp,
    modifier: Modifier = Modifier,
) {
    val resId = if (filled) R.drawable.ic_ig_home_filled else R.drawable.ic_ig_home
    Icon(
        painter = painterResource(resId),
        contentDescription = "Home",
        tint = tint,
        modifier = modifier
            .size(size)
            .semanticsLabel("Home"),
    )
}

/**
 * Bottom Bar Reels tab icon - uses authentic Instagram Reels outline/filled icons.
 */
@Composable
fun IgReelsIcon(
    filled: Boolean,
    tint: Color,
    size: Dp = 24.dp,
    modifier: Modifier = Modifier,
) {
    val resId = if (filled) R.drawable.ic_ig_reels_filled else R.drawable.ic_ig_reels
    Icon(
        painter = painterResource(resId),
        contentDescription = "Reels",
        tint = tint,
        modifier = modifier
            .size(size)
            .semanticsLabel("Reels"),
    )
}

/**
 * Profile Tab 1: Authentic 3x3 Instagram post grid icon.
 */
@Composable
fun IgGridTabIcon(
    active: Boolean = true,
    tint: Color,
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
 * Profile Tab 2: Authentic Instagram Reels tab squircle icon.
 */
@Composable
fun IgReelsSquircleIcon(
    tint: Color,
    size: Dp = 23.dp,
    contentDescription: String? = "Reels",
    modifier: Modifier = Modifier,
) {
    Icon(
        painter = painterResource(R.drawable.ic_ig_reels_tab),
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier
            .size(size)
            .semanticsLabel(contentDescription),
    )
}

/**
 * Profile Tab 3 & Engagement Bar Repost Icon:
 * Two vertical rounded arrows forming a loop (top-right pointing right, bottom-left pointing left).
 */
@Composable
fun IgRepostLoopIcon(
    tint: Color,
    size: Dp = 23.dp,
    strokeWidthDp: Dp = 2.dp,
    contentDescription: String? = "Repost",
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(size).semanticsLabel(contentDescription)) {
        val w = this.size.width
        val h = this.size.height
        val strokeWidth = strokeWidthDp.toPx()
        val strokeStyle = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)

        // Left vertical arrow pointing UP
        val leftArrowStem = Path().apply {
            moveTo(w * 0.56f, h * 0.80f)
            lineTo(w * 0.39f, h * 0.80f)
            quadraticTo(w * 0.27f, h * 0.80f, w * 0.27f, h * 0.67f)
            lineTo(w * 0.27f, h * 0.25f)
        }
        drawPath(leftArrowStem, color = tint, style = strokeStyle)

        val leftArrowHead = Path().apply {
            moveTo(w * 0.15f, h * 0.37f)
            lineTo(w * 0.27f, h * 0.24f)
            lineTo(w * 0.39f, h * 0.37f)
        }
        drawPath(leftArrowHead, color = tint, style = strokeStyle)

        // Right vertical arrow pointing DOWN
        val rightArrowStem = Path().apply {
            moveTo(w * 0.44f, h * 0.20f)
            lineTo(w * 0.61f, h * 0.20f)
            quadraticTo(w * 0.73f, h * 0.20f, w * 0.73f, h * 0.33f)
            lineTo(w * 0.73f, h * 0.75f)
        }
        drawPath(rightArrowStem, color = tint, style = strokeStyle)

        val rightArrowHead = Path().apply {
            moveTo(w * 0.61f, h * 0.63f)
            lineTo(w * 0.73f, h * 0.76f)
            lineTo(w * 0.85f, h * 0.63f)
        }
        drawPath(rightArrowHead, color = tint, style = strokeStyle)
    }
}

/**
 * Profile Tab 4: Authentic Instagram Tagged profile badge icon.
 */
@Composable
fun IgTaggedPersonIcon(
    tint: Color,
    size: Dp = 23.dp,
    contentDescription: String? = "Tagged",
    modifier: Modifier = Modifier,
) {
    Icon(
        painter = painterResource(R.drawable.ic_ig_tagged),
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier
            .size(size)
            .semanticsLabel(contentDescription),
    )
}

/**
 * Post Engagement 1: Authentic Instagram heart outline or filled red / monochrome when liked.
 */
@Composable
fun IgHeartStrokeIcon(
    filled: Boolean,
    tint: Color,
    size: Dp = 24.dp,
    strokeWidthDp: Dp = 2.dp,
    contentDescription: String? = "Like",
    modifier: Modifier = Modifier,
) {
    val isRed = tint == Color(0xFFED4956)
    val resId = when {
        filled && isRed -> R.drawable.ic_ig_heart_red
        filled -> R.drawable.ic_ig_heart_filled
        else -> R.drawable.ic_ig_heart
    }
    Icon(
        painter = painterResource(resId),
        contentDescription = contentDescription,
        tint = if (filled && isRed) Color.Unspecified else tint,
        modifier = modifier
            .size(size)
            .semanticsLabel(contentDescription),
    )
}

/**
 * Post Engagement 2: Authentic Instagram speech comment bubble icon.
 */
@Composable
fun IgCommentBubbleIcon(
    tint: Color,
    size: Dp = 23.dp,
    strokeWidthDp: Dp = 2.dp,
    contentDescription: String? = "Comment",
    modifier: Modifier = Modifier,
) {
    Icon(
        painter = painterResource(R.drawable.ic_ig_comment),
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier
            .size(size)
            .semanticsLabel(contentDescription),
    )
}

/**
 * Post Engagement 4 / Bottom Bar Share Icon:
 * Authentic Instagram paper airplane share icon.
 */
@Composable
fun IgSharePlaneIcon(
    filled: Boolean = false,
    tint: Color,
    size: Dp = 23.dp,
    strokeWidthDp: Dp = 2.dp,
    contentDescription: String? = "Share",
    modifier: Modifier = Modifier,
) {
    Icon(
        painter = painterResource(R.drawable.ic_ig_share_plane),
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier
            .size(size)
            .semanticsLabel(contentDescription),
    )
}

/**
 * Post Engagement 5: Authentic Instagram bookmark save icon.
 */
@Composable
fun IgBookmarkStrokeIcon(
    filled: Boolean,
    tint: Color,
    size: Dp = 23.dp,
    strokeWidthDp: Dp = 2.dp,
    contentDescription: String? = "Save",
    modifier: Modifier = Modifier,
) {
    val resId = if (filled) R.drawable.ic_ig_bookmark_filled else R.drawable.ic_ig_bookmark
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
 * Bottom Bar Search / Explore icon.
 */
@Composable
fun IgSearchIcon(
    bold: Boolean = false,
    tint: Color,
    size: Dp = 24.dp,
    modifier: Modifier = Modifier,
) {
    val resId = if (bold) R.drawable.ic_ig_search_bold else R.drawable.ic_ig_search
    Icon(
        painter = painterResource(resId),
        contentDescription = "Search",
        tint = tint,
        modifier = modifier
            .size(size)
            .semanticsLabel("Search"),
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
        painter = painterResource(R.drawable.ic_ig_eye),
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

        // Open-top rounded tray box
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

        // Vertical arrow pointing up
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
    tint: Color = Color.White,
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
