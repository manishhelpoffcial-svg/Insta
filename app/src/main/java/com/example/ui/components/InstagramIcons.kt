package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun IgHomeIcon(
    filled: Boolean,
    tint: Color,
    size: Dp = 25.dp,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeWidth = w * 0.085f
        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.1f)
            lineTo(w * 0.12f, h * 0.43f)
            lineTo(w * 0.12f, h * 0.88f)
            lineTo(w * 0.39f, h * 0.88f)
            lineTo(w * 0.39f, h * 0.6f)
            lineTo(w * 0.61f, h * 0.6f)
            lineTo(w * 0.61f, h * 0.88f)
            lineTo(w * 0.88f, h * 0.88f)
            lineTo(w * 0.88f, h * 0.43f)
            close()
        }
        if (filled) {
            drawPath(path, color = tint, style = Fill)
        } else {
            drawPath(
                path,
                color = tint,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

@Composable
fun IgReelsIcon(
    filled: Boolean,
    tint: Color,
    size: Dp = 24.dp,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeWidth = w * 0.08f
        val corner = CornerRadius(w * 0.24f, w * 0.24f)

        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.08f, h * 0.08f),
            size = Size(w * 0.84f, h * 0.84f),
            cornerRadius = corner,
            style = Stroke(width = if (filled) strokeWidth * 1.15f else strokeWidth),
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.1f, h * 0.32f),
            end = Offset(w * 0.9f, h * 0.32f),
            strokeWidth = strokeWidth,
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.34f, h * 0.09f),
            end = Offset(w * 0.44f, h * 0.32f),
            strokeWidth = strokeWidth,
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.62f, h * 0.09f),
            end = Offset(w * 0.72f, h * 0.32f),
            strokeWidth = strokeWidth,
        )
        val playPath = Path().apply {
            moveTo(w * 0.42f, h * 0.46f)
            lineTo(w * 0.67f, h * 0.60f)
            lineTo(w * 0.42f, h * 0.74f)
            close()
        }
        drawPath(playPath, color = tint, style = Fill)
    }
}

private fun Modifier.semanticsLabel(label: String?): Modifier =
    if (label != null) this.semantics { contentDescription = label } else this

/**
 * Profile Tab 1: 3x3 grid of 9 small rounded squares.
 */
@Composable
fun IgGridTabIcon(
    active: Boolean,
    tint: Color,
    size: Dp = 22.dp,
    contentDescription: String? = "Posts grid",
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(size).semanticsLabel(contentDescription)) {
        val w = this.size.width
        val cell = w * 0.22f
        val gap = w * 0.09f
        val total = cell * 3f + gap * 2f
        val start = (w - total) / 2f
        val corner = CornerRadius(w * 0.055f, w * 0.055f)

        for (r in 0 until 3) {
            for (c in 0 until 3) {
                val left = start + c * (cell + gap)
                val top = start + r * (cell + gap)
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(left, top),
                    size = Size(cell, cell),
                    cornerRadius = corner,
                    style = Fill,
                )
            }
        }
        if (false && active) Unit
    }
}

/**
 * Profile Tab 2: Rounded squircle frame with an outlined rounded play triangle centered inside.
 */
@Composable
fun IgReelsSquircleIcon(
    tint: Color,
    size: Dp = 23.dp,
    contentDescription: String? = "Reels",
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(size).semanticsLabel(contentDescription)) {
        val w = this.size.width
        val h = this.size.height
        val strokeWidth = 2.dp.toPx()

        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.11f, h * 0.11f),
            size = Size(w * 0.78f, h * 0.78f),
            cornerRadius = CornerRadius(w * 0.27f, w * 0.27f),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )

        val playPath = Path().apply {
            moveTo(w * 0.41f, h * 0.35f)
            lineTo(w * 0.65f, h * 0.50f)
            lineTo(w * 0.41f, h * 0.65f)
            close()
        }
        drawPath(
            path = playPath,
            color = tint,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
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

        // Left vertical arrow pointing UP (curves smoothly from bottom-center up the left side)
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

        // Right vertical arrow pointing DOWN (curves smoothly from top-center down the right side)
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
 * Profile Tab 4: Rounded square frame with a pointed badge notch at top center,
 * containing a circle head and curved shoulders silhouette inside.
 */
@Composable
fun IgTaggedPersonIcon(
    tint: Color,
    size: Dp = 23.dp,
    contentDescription: String? = "Tagged",
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(size).semanticsLabel(contentDescription)) {
        val w = this.size.width
        val h = this.size.height
        val strokeWidth = 2.dp.toPx()
        val strokeStyle = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)

        val frame = Path().apply {
            moveTo(w * 0.23f, h * 0.21f)
            lineTo(w * 0.40f, h * 0.21f)
            lineTo(w * 0.485f, h * 0.115f)
            quadraticTo(w * 0.50f, h * 0.10f, w * 0.515f, h * 0.115f)
            lineTo(w * 0.60f, h * 0.21f)
            lineTo(w * 0.77f, h * 0.21f)
            quadraticTo(w * 0.87f, h * 0.21f, w * 0.87f, h * 0.31f)
            lineTo(w * 0.87f, h * 0.77f)
            quadraticTo(w * 0.87f, h * 0.87f, w * 0.77f, h * 0.87f)
            lineTo(w * 0.23f, h * 0.87f)
            quadraticTo(w * 0.13f, h * 0.87f, w * 0.13f, h * 0.77f)
            lineTo(w * 0.13f, h * 0.31f)
            quadraticTo(w * 0.13f, h * 0.21f, w * 0.23f, h * 0.21f)
            close()
        }
        drawPath(frame, color = tint, style = strokeStyle)

        // Head circle
        drawCircle(
            color = tint,
            radius = w * 0.115f,
            center = Offset(w * 0.50f, h * 0.45f),
            style = Stroke(width = strokeWidth),
        )

        // Shoulders arc
        val shoulders = Path().apply {
            moveTo(w * 0.29f, h * 0.86f)
            cubicTo(
                w * 0.30f, h * 0.65f,
                w * 0.70f, h * 0.65f,
                w * 0.71f, h * 0.86f,
            )
        }
        drawPath(shoulders, color = tint, style = strokeStyle)
    }
}

/**
 * Post Engagement 1: Standard Instagram heart outline or filled when liked.
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
    Canvas(modifier = modifier.size(size).semanticsLabel(contentDescription)) {
        val w = this.size.width
        val h = this.size.height
        val strokeWidth = strokeWidthDp.toPx()

        val heartPath = Path().apply {
            moveTo(w * 0.50f, h * 0.85f)
            cubicTo(
                w * 0.26f, h * 0.69f,
                w * 0.11f, h * 0.50f,
                w * 0.11f, h * 0.34f,
            )
            cubicTo(
                w * 0.11f, h * 0.20f,
                w * 0.22f, h * 0.13f,
                w * 0.34f, h * 0.14f,
            )
            cubicTo(
                w * 0.42f, h * 0.15f,
                w * 0.47f, h * 0.21f,
                w * 0.50f, h * 0.27f,
            )
            cubicTo(
                w * 0.53f, h * 0.21f,
                w * 0.58f, h * 0.15f,
                w * 0.66f, h * 0.14f,
            )
            cubicTo(
                w * 0.78f, h * 0.13f,
                w * 0.89f, h * 0.20f,
                w * 0.89f, h * 0.34f,
            )
            cubicTo(
                w * 0.89f, h * 0.50f,
                w * 0.74f, h * 0.69f,
                w * 0.50f, h * 0.85f,
            )
            close()
        }

        if (filled) {
            drawPath(heartPath, color = tint, style = Fill)
        } else {
            drawPath(
                heartPath,
                color = tint,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

/**
 * Post Engagement 2: Smooth circular speech bubble with small pointed tail at bottom-right.
 */
@Composable
fun IgCommentBubbleIcon(
    tint: Color,
    size: Dp = 23.dp,
    strokeWidthDp: Dp = 2.dp,
    contentDescription: String? = "Comment",
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(size).semanticsLabel(contentDescription)) {
        val w = this.size.width
        val h = this.size.height
        val strokeWidth = strokeWidthDp.toPx()
        val rect = Rect(
            left = w * 0.13f,
            top = h * 0.13f,
            right = w * 0.87f,
            bottom = h * 0.87f,
        )
        val path = Path().apply {
            arcTo(
                rect = rect,
                startAngleDegrees = 60f,
                sweepAngleDegrees = 332f,
                forceMoveTo = true,
            )
            lineTo(w * 0.86f, h * 0.86f)
            close()
        }
        drawPath(
            path = path,
            color = tint,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

/**
 * Post Engagement 4 / Bottom Bar Share Icon:
 * Rounded triangular paper airplane pointing top-right with diagonal inner fold line.
 */
@Composable
fun IgSharePlaneIcon(
    filled: Boolean,
    tint: Color,
    size: Dp = 23.dp,
    strokeWidthDp: Dp = 2.dp,
    contentDescription: String? = "Share",
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(size).semanticsLabel(contentDescription)) {
        val w = this.size.width
        val h = this.size.height
        val strokeWidth = strokeWidthDp.toPx()
        val path = Path().apply {
            moveTo(w * 0.14f, h * 0.21f)
            lineTo(w * 0.86f, h * 0.21f)
            lineTo(w * 0.49f, h * 0.85f)
            lineTo(w * 0.40f, h * 0.50f)
            close()
        }
        if (filled) {
            drawPath(path, color = tint, style = Fill)
            drawPath(
                path,
                color = tint,
                style = Stroke(width = strokeWidth * 0.6f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        } else {
            drawPath(
                path,
                color = tint,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            drawLine(
                color = tint,
                start = Offset(w * 0.85f, h * 0.22f),
                end = Offset(w * 0.40f, h * 0.50f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}

/**
 * Post Engagement 5: Classic vertical ribbon bookmark outline with inverted V-notch at bottom.
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
    Canvas(modifier = modifier.size(size).semanticsLabel(contentDescription)) {
        val w = this.size.width
        val h = this.size.height
        val strokeWidth = strokeWidthDp.toPx()
        val path = Path().apply {
            moveTo(w * 0.21f, h * 0.14f)
            lineTo(w * 0.79f, h * 0.14f)
            lineTo(w * 0.79f, h * 0.86f)
            lineTo(w * 0.50f, h * 0.64f)
            lineTo(w * 0.21f, h * 0.86f)
            close()
        }
        if (filled) {
            drawPath(path, color = tint, style = Fill)
            drawPath(
                path,
                color = tint,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        } else {
            drawPath(
                path,
                color = tint,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

@Composable
fun IgSearchIcon(
    bold: Boolean,
    tint: Color,
    size: Dp = 24.dp,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeWidth = if (bold) w * 0.115f else w * 0.082f
        val radius = w * 0.31f
        val center = Offset(w * 0.44f, h * 0.44f)
        drawCircle(
            color = tint,
            radius = radius,
            center = center,
            style = Stroke(width = strokeWidth),
        )
        drawLine(
            color = tint,
            start = Offset(center.x + radius * 0.72f, center.y + radius * 0.72f),
            end = Offset(w * 0.88f, h * 0.88f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

/**
 * Exact Instagram Views eye icon:
 * Bold curved upper eyelid arch ONLY (no bottom eyelid) + solid circular pupil centered underneath
 * with a clean top-right circular "bite" cutout for Instagram's glossy pupil glint.
 */
@Composable
fun IgEyeIcon(
    tint: Color = Color.White,
    size: Dp = 14.dp,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = (w * 0.125f).coerceAtLeast(1.8.dp.toPx())

        // Upper eyelid arch only
        val lidPath = Path().apply {
            moveTo(w * 0.10f, h * 0.48f)
            cubicTo(
                w * 0.28f, h * 0.16f,
                w * 0.72f, h * 0.16f,
                w * 0.90f, h * 0.48f,
            )
        }
        drawPath(
            path = lidPath,
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )

        // Solid pupil with circular bite cutout in top-right quadrant
        val pupilCenter = Offset(w * 0.50f, h * 0.62f)
        val pupilRadius = w * 0.22f
        val pupilCircle = Path().apply {
            addOval(
                Rect(
                    left = pupilCenter.x - pupilRadius,
                    top = pupilCenter.y - pupilRadius,
                    right = pupilCenter.x + pupilRadius,
                    bottom = pupilCenter.y + pupilRadius,
                )
            )
        }
        val biteCenter = Offset(pupilCenter.x + pupilRadius * 0.56f, pupilCenter.y - pupilRadius * 0.48f)
        val biteRadius = pupilRadius * 0.46f
        val biteCircle = Path().apply {
            addOval(
                Rect(
                    left = biteCenter.x - biteRadius,
                    top = biteCenter.y - biteRadius,
                    right = biteCenter.x + biteRadius,
                    bottom = biteCenter.y + biteRadius,
                )
            )
        }
        val glintedPupil = Path.combine(PathOperation.Difference, pupilCircle, biteCircle)
        drawPath(path = glintedPupil, color = tint, style = Fill)
    }
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
 * Two horizontal rounded lines menu icon (=) shown on the top-right of Post Detail video overlays.
 */
@Composable
fun IgTwoLineMenuIcon(
    tint: Color = Color.White,
    size: Dp = 20.dp,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = 2.dp.toPx()
        drawLine(
            color = tint,
            start = Offset(w * 0.14f, h * 0.36f),
            end = Offset(w * 0.86f, h * 0.36f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.14f, h * 0.64f),
            end = Offset(w * 0.86f, h * 0.64f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}
