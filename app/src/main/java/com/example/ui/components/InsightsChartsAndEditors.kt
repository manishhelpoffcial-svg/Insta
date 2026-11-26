package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AccountInsightsData
import com.example.data.model.ReelInsights
import com.example.data.model.ViewsCurvePresets
import com.example.ui.theme.IgBoostBlue
import com.example.ui.theme.IgInsightsAxisText
import com.example.ui.theme.IgInsightsBgDark
import com.example.ui.theme.IgInsightsBorderDark
import com.example.ui.theme.IgInsightsCardDark
import com.example.ui.theme.IgInsightsMagenta
import com.example.ui.theme.IgInsightsMutedText
import com.example.ui.theme.IgInsightsSoftPink
import com.example.ui.theme.IgInsightsViolet
import com.example.ui.theme.InstagramTheme
import java.util.Locale
import kotlin.math.roundToInt

@Immutable
data class InsightsUiColors(
    val bg: Color,
    val card: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val axisText: Color,
    val pillSelected: Color,
    val track: Color,
    val activeCardBorder: Color,
)

@Composable
fun rememberInsightsPalette(): InsightsUiColors {
    val isDark = InstagramTheme.colors.isDark
    return remember(isDark) {
        if (isDark) {
            InsightsUiColors(
                bg = IgInsightsBgDark,
                card = IgInsightsCardDark,
                border = IgInsightsBorderDark,
                textPrimary = Color.White,
                textSecondary = IgInsightsMutedText,
                axisText = IgInsightsAxisText,
                pillSelected = Color(0xFF262930),
                track = Color(0xFF262930),
                activeCardBorder = Color.White,
            )
        } else {
            InsightsUiColors(
                bg = Color(0xFFFFFFFF),
                card = Color(0xFFF7F8FA),
                border = Color(0xFFDBDBDB),
                textPrimary = Color(0xFF000000),
                textSecondary = Color(0xFF666666),
                axisText = Color(0xFF737373),
                pillSelected = Color(0xFFF0F2F5),
                track = Color(0xFFF0F0F5),
                activeCardBorder = Color(0xFFD1D5DB),
            )
        }
    }
}

@Composable
fun rememberCleanLightInsightsPalette(): InsightsUiColors = remember {
    InsightsUiColors(
        bg = Color(0xFFFFFFFF),
        card = Color(0xFFF7F8FA),
        border = Color(0xFFDBDBDB),
        textPrimary = Color(0xFF000000),
        textSecondary = Color(0xFF666666),
        axisText = Color(0xFF737373),
        pillSelected = Color(0xFFF0F2F5),
        track = Color(0xFFF0F0F5),
        activeCardBorder = Color(0xFFD1D5DB),
    )
}

@Composable
fun ReelInsightsSkeletonBlock(
    modifier: Modifier = Modifier,
    cornerRadius: androidx.compose.ui.unit.Dp = 6.dp,
) {
    val transition = rememberInfiniteTransition(label = "reel_skeleton")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "reel_skeleton_offset",
    )
    val base = Color(0xFFECEEF2)
    val highlight = Color(0xFFF6F7F9)
    val brush = Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(progress * 450f - 220f, 0f),
        end = Offset(progress * 450f + 160f, 120f),
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(brush),
    )
}

fun formatAxisLabels(rawMax: String, fallbackViews: String): Pair<String, String> {
    val parsedMax = rawMax.replace(",", "").trim().toDoubleOrNull()
    val maxVal = parsedMax ?: run {
        val cleaned = fallbackViews.replace(",", "").trim()
        val parsedViews = cleaned.toDoubleOrNull() ?: 4000.0
        if (parsedViews <= 0.0) 4000.0 else parsedViews
    }
    fun compact(v: Double): String = when {
        v >= 1_000_000 -> String.format(Locale.US, "%.1fM", v / 1_000_000.0).removeSuffix(".0")
        v >= 1_000 -> {
            val k = v / 1_000.0
            if (k % 1.0 == 0.0) "${k.toInt()}K" else String.format(Locale.US, "%.1fK", k)
        }
        else -> v.roundToInt().toString()
    }
    return compact(maxVal) to compact(maxVal / 2.0)
}

@Composable
fun ShimmerSummaryAndGraphPlaceholder(palette: InsightsUiColors) {
    val transition = rememberInfiniteTransition(label = "insights_shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 580, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_offset",
    )
    val base = palette.card
    val highlight = if (InstagramTheme.colors.isDark) Color(0xFF282C35) else Color(0xFFE5E7EB)
    val brush = Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(progress * 600f - 300f, 0f),
        end = Offset(progress * 600f + 220f, 180f),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 18.dp),
    ) {
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(brush),
        )
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            repeat(2) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(78.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(brush),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            repeat(2) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(78.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(brush),
                )
            }
        }
        Spacer(Modifier.height(26.dp))
        Box(
            modifier = Modifier
                .width(150.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(brush),
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(56.dp, 90.dp, 118.dp).forEach { w ->
                Box(
                    modifier = Modifier
                        .width(w)
                        .height(34.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(brush),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(155.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(brush),
        )
    }
}

@Composable
fun ViewsOverTimeLineChart(
    points: List<Float>,
    startDate: String,
    midDate: String,
    endDate: String,
    yMaxLabel: String,
    yMidLabel: String,
    palette: InsightsUiColors,
    modifier: Modifier = Modifier,
    isEditMode: Boolean = false,
    onUpdatePoint: ((Int, Float) -> Unit)? = null,
) {
    var selectedIdx by remember(points.size, isEditMode) { mutableIntStateOf(0) }
    val safeSelected = selectedIdx.coerceIn(0, (points.size - 1).coerceAtLeast(0))

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(152.dp),
        ) {
            Column(
                modifier = Modifier
                    .width(42.dp)
                    .fillMaxHeight()
                    .padding(bottom = 2.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = yMaxLabel,
                    color = palette.axisText,
                    fontSize = 11.5.sp,
                    style = TextStyle(fontFeatureSettings = "tnum"),
                )
                Text(
                    text = yMidLabel,
                    color = palette.axisText,
                    fontSize = 11.5.sp,
                    style = TextStyle(fontFeatureSettings = "tnum"),
                )
                Text(
                    text = "0",
                    color = palette.axisText,
                    fontSize = 11.5.sp,
                    style = TextStyle(fontFeatureSettings = "tnum"),
                )
            }

            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(vertical = 7.dp)
                    .then(
                        if (isEditMode && points.isNotEmpty()) {
                            Modifier.pointerInput(points.size, isEditMode) {
                                detectTapGestures { tap ->
                                    if (points.size == 1) {
                                        selectedIdx = 0
                                    } else {
                                        val w = size.width.toFloat().coerceAtLeast(1f)
                                        val nearest = ((tap.x / w) * (points.size - 1))
                                            .roundToInt()
                                            .coerceIn(0, points.lastIndex)
                                        selectedIdx = nearest
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
                        color = Color(0xFFF2F2F2),
                        start = Offset(0f, h * frac),
                        end = Offset(w, h * frac),
                        strokeWidth = 1.dp.toPx(),
                    )
                }

                // Typical reel dashed reference line
                val typicalValues = listOf(5f, 24f, 44f, 68f)
                val typicalCoords = typicalValues.mapIndexed { idx, value ->
                    val x = w * (idx.toFloat() / (typicalValues.size - 1))
                    val y = h * (1f - (value / 100f).coerceIn(0f, 1f))
                    Offset(x, y)
                }
                val typicalPath = Path().apply {
                    typicalCoords.forEachIndexed { idx, pt ->
                        if (idx == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                    }
                }
                drawPath(
                    path = typicalPath,
                    color = Color(0xFF6B7280),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                            floatArrayOf(12f, 10f),
                            0f,
                        ),
                    ),
                )

                if (points.size >= 2) {
                    val coords = points.mapIndexed { idx, value ->
                        val x = w * (idx.toFloat() / (points.size - 1))
                        val y = h * (1f - (value / 100f).coerceIn(0f, 1f))
                        Offset(x, y)
                    }
                    val path = Path().apply {
                        coords.forEachIndexed { idx, pt ->
                            if (idx == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                        }
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
                            val isSel = idx == safeSelected
                            if (isSel) {
                                drawCircle(
                                    color = palette.bg,
                                    radius = 7.5.dp.toPx(),
                                    center = pt,
                                )
                            }
                            drawCircle(
                                color = if (isSel) Color(0xFF4F46E5) else Color(0xFFD300C5),
                                radius = if (isSel) 5.5.dp.toPx() else 3.8.dp.toPx(),
                                center = pt,
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 42.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(startDate, color = palette.axisText, fontSize = 12.sp)
            Text(midDate, color = palette.axisText, fontSize = 12.sp)
            Text(endDate, color = palette.axisText, fontSize = 12.sp)
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.padding(start = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD300C5)),
                )
                Spacer(Modifier.width(6.dp))
                Text("This reel", fontSize = 12.sp, color = palette.textSecondary)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF6B7280)),
                )
                Spacer(Modifier.width(6.dp))
                Text("Your typical reel", fontSize = 12.sp, color = palette.textSecondary)
            }
        }

        if (isEditMode && onUpdatePoint != null && points.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            InlineChartPointEditor(
                points = points,
                selectedIdx = safeSelected,
                onSelectIdx = { selectedIdx = it },
                onUpdatePoint = onUpdatePoint,
                palette = palette,
            )
        }
    }
}

@Composable
fun InlineChartPointEditor(
    points: List<Float>,
    selectedIdx: Int,
    onSelectIdx: (Int) -> Unit,
    onUpdatePoint: (Int, Float) -> Unit,
    palette: InsightsUiColors,
    minVal: Float = 0f,
    maxVal: Float = 100f,
) {
    val safeIdx = selectedIdx.coerceIn(0, (points.size - 1).coerceAtLeast(0))
    val currentVal = points.getOrElse(safeIdx) { 0f }
    var draftText by remember(safeIdx, currentVal) {
        mutableStateOf(currentVal.roundToInt().toString())
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(palette.card)
            .border(1.dp, palette.border, RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Day ${safeIdx + 1} point value",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.textPrimary,
            )
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(palette.bg)
                    .border(1.dp, IgInsightsMagenta.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                BasicTextField(
                    value = draftText,
                    onValueChange = { raw ->
                        val cleaned = raw.filter { it.isDigit() || it == '-' || it == '.' }
                        draftText = cleaned
                        cleaned.toFloatOrNull()?.let { parsed ->
                            onUpdatePoint(safeIdx, parsed.coerceIn(minVal, maxVal))
                        }
                    },
                    singleLine = true,
                    cursorBrush = SolidColor(IgInsightsMagenta),
                    textStyle = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.textPrimary,
                        textAlign = TextAlign.End,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            points.indices.forEach { idx ->
                val active = idx == safeIdx
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (active) IgInsightsMagenta else palette.bg)
                        .border(1.dp, if (active) IgInsightsMagenta else palette.border, CircleShape)
                        .clickable { onSelectIdx(idx) }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "D${idx + 1}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (active) Color.White else palette.textPrimary,
                    )
                }
            }
        }
        Slider(
            value = currentVal.coerceIn(minVal, maxVal),
            onValueChange = { v ->
                draftText = v.roundToInt().toString()
                onUpdatePoint(safeIdx, v)
            },
            valueRange = minVal..maxVal,
            colors = SliderDefaults.colors(
                thumbColor = IgInsightsMagenta,
                activeTrackColor = IgInsightsMagenta,
            ),
            modifier = Modifier.height(30.dp),
        )
    }
}

@Composable
fun AccountOverviewLineChart(
    points: List<Float>,
    startDate: String,
    midDate: String,
    endDate: String,
    topLabel: String,
    midLabel: String,
    bottomLabel: String,
    palette: InsightsUiColors,
    modifier: Modifier = Modifier,
    isEditMode: Boolean = false,
    onUpdatePoint: ((Int, Float) -> Unit)? = null,
) {
    var selectedIdx by remember(points.size, isEditMode) { mutableIntStateOf(0) }
    val safeSelected = selectedIdx.coerceIn(0, (points.size - 1).coerceAtLeast(0))

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(145.dp),
        ) {
            Column(
                modifier = Modifier
                    .width(42.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(topLabel, color = palette.axisText, fontSize = 11.5.sp, style = TextStyle(fontFeatureSettings = "tnum"))
                Text(midLabel, color = palette.axisText, fontSize = 11.5.sp, style = TextStyle(fontFeatureSettings = "tnum"))
                Text(bottomLabel, color = palette.axisText, fontSize = 11.5.sp, style = TextStyle(fontFeatureSettings = "tnum"))
            }

            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(vertical = 7.dp)
                    .then(
                        if (isEditMode && points.isNotEmpty()) {
                            Modifier.pointerInput(points.size, isEditMode) {
                                detectTapGestures { tap ->
                                    if (points.size > 1) {
                                        val w = size.width.toFloat().coerceAtLeast(1f)
                                        selectedIdx = ((tap.x / w) * (points.size - 1))
                                            .roundToInt()
                                            .coerceIn(0, points.lastIndex)
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
                        color = palette.border,
                        start = Offset(0f, h * frac),
                        end = Offset(w, h * frac),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
                if (points.size >= 2) {
                    val coords = points.mapIndexed { idx, value ->
                        val x = w * (idx.toFloat() / (points.size - 1))
                        val y = h * (1f - (value / 100f).coerceIn(0f, 1f))
                        Offset(x, y)
                    }
                    val path = Path()
                    coords.forEachIndexed { idx, pt ->
                        if (idx == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
                    }
                    drawPath(
                        path = path,
                        color = IgInsightsMagenta,
                        style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Miter),
                    )
                    if (isEditMode) {
                        coords.forEachIndexed { idx, pt ->
                            val isSel = idx == safeSelected
                            if (isSel) {
                                drawCircle(color = palette.bg, radius = 6.8.dp.toPx(), center = pt)
                            }
                            drawCircle(
                                color = if (isSel) IgInsightsViolet else IgInsightsMagenta,
                                radius = if (isSel) 5.dp.toPx() else 3.2.dp.toPx(),
                                center = pt,
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 42.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(startDate, color = palette.axisText, fontSize = 11.5.sp)
            Text(midDate, color = palette.axisText, fontSize = 11.5.sp)
            Text(endDate, color = palette.axisText, fontSize = 11.5.sp)
        }
        if (isEditMode && onUpdatePoint != null && points.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            InlineChartPointEditor(
                points = points,
                selectedIdx = safeSelected,
                onSelectIdx = { selectedIdx = it },
                onUpdatePoint = onUpdatePoint,
                palette = palette,
            )
        }
    }
}

@Composable
fun FollowerGrowthChart(
    points: List<Float>,
    startDate: String,
    midDate: String,
    endDate: String,
    palette: InsightsUiColors,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp),
        ) {
            Column(
                modifier = Modifier
                    .width(34.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("2", color = palette.axisText, fontSize = 11.5.sp, style = TextStyle(fontFeatureSettings = "tnum"))
                Text("0", color = palette.axisText, fontSize = 11.5.sp, style = TextStyle(fontFeatureSettings = "tnum"))
                Text("-2", color = palette.axisText, fontSize = 11.5.sp, style = TextStyle(fontFeatureSettings = "tnum"))
            }

            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(vertical = 7.dp),
            ) {
                val w = size.width
                val h = size.height
                listOf(0f, 0.5f, 1f).forEach { frac ->
                    drawLine(
                        color = palette.border,
                        start = Offset(0f, h * frac),
                        end = Offset(w, h * frac),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
                if (points.size >= 2) {
                    val path = Path()
                    points.forEachIndexed { idx, value ->
                        val x = w * (idx.toFloat() / (points.size - 1))
                        val normalized = ((value + 2f) / 4f).coerceIn(0f, 1f)
                        val y = h * (1f - normalized)
                        if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(
                        path = path,
                        color = IgInsightsMagenta,
                        style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 34.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(startDate, color = palette.axisText, fontSize = 11.5.sp)
            Text(midDate, color = palette.axisText, fontSize = 11.5.sp)
            Text(endDate, color = palette.axisText, fontSize = 11.5.sp)
        }
    }
}

@Composable
fun FollowerActiveTimesBarChart(
    bars: List<Float>,
    palette: InsightsUiColors,
    modifier: Modifier = Modifier,
) {
    val labels = listOf("12a", "3a", "6a", "9a", "12p", "3p", "6p", "9p")
    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp),
        ) {
            val count = bars.size.coerceAtLeast(1)
            val slotW = size.width / count
            val barW = (slotW * 0.44f).coerceIn(10.dp.toPx(), 20.dp.toPx())
            val h = size.height

            drawLine(
                color = palette.border,
                start = Offset(0f, h),
                end = Offset(size.width, h),
                strokeWidth = 1.dp.toPx(),
            )

            bars.forEachIndexed { idx, v ->
                val frac = v.coerceIn(0.08f, 1f)
                val barH = h * frac
                val left = idx * slotW + (slotW - barW) / 2f
                drawRoundRect(
                    color = IgInsightsMagenta,
                    topLeft = Offset(left, h - barH),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            labels.forEach { lbl ->
                Text(
                    text = lbl,
                    color = palette.axisText,
                    fontSize = 11.5.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
fun StackedContentBarRow(
    label: String,
    countText: String,
    fillFraction: Float,
    followersPortion: Float,
    palette: InsightsUiColors,
) {
    val animFill by animateFloatAsState(fillFraction.coerceIn(0f, 1f), label = "stacked_fill")
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, color = palette.textPrimary, fontSize = 14.5.sp, fontWeight = FontWeight.Normal)
            Text(
                text = countText,
                color = palette.textPrimary,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Medium,
                style = TextStyle(fontFeatureSettings = "tnum"),
            )
        }
        Spacer(Modifier.height(7.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(palette.track),
        ) {
            if (animFill > 0f) {
                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animFill.coerceAtLeast(0.01f))
                        .clip(RoundedCornerShape(4.dp)),
                ) {
                    val followerWeight = followersPortion.coerceIn(0.02f, 0.98f)
                    if (followersPortion > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(followerWeight)
                                .background(IgInsightsViolet),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight((1f - followerWeight).coerceAtLeast(0.02f))
                            .background(IgInsightsMagenta),
                    )
                }
            }
        }
    }
}

@Composable
fun DualToneAgeBarRow(
    label: String,
    totalPct: Double,
    womenShareRatio: Double,
    palette: InsightsUiColors,
) {
    val totalFraction by animateFloatAsState((totalPct.toFloat() / 100f).coerceIn(0f, 1f), label = "age_bar")
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = palette.textPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
        )
        Spacer(Modifier.height(5.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(palette.track),
            ) {
                if (totalFraction > 0.002f) {
                    Row(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(totalFraction.coerceAtLeast(0.015f))
                            .clip(RoundedCornerShape(4.dp)),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        val wWeight = womenShareRatio.toFloat().coerceIn(0.15f, 0.85f)
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(wWeight)
                                .clip(RoundedCornerShape(4.dp))
                                .background(IgInsightsMagenta),
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(1f - wWeight)
                                .clip(RoundedCornerShape(4.dp))
                                .background(IgInsightsSoftPink),
                        )
                    }
                }
            }
            Spacer(Modifier.width(14.dp))
            Text(
                text = String.format(Locale.US, "%.1f%%", totalPct),
                color = palette.textPrimary,
                fontSize = 14.sp,
                textAlign = TextAlign.End,
                style = TextStyle(fontFeatureSettings = "tnum"),
                modifier = Modifier.width(54.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelInsightsEditModal(
    insights: ReelInsights,
    audioTitle: String = "",
    likedByUsername: String = "",
    isInlineEditMode: Boolean,
    onToggleInlineEditMode: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (ReelInsights) -> Unit,
    onSavePostMetadata: ((audioTitle: String, likedByUsername: String) -> Unit)? = null,
) {
    val palette = rememberInsightsPalette()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var audioDraft by remember(audioTitle) { mutableStateOf(audioTitle) }
    var likedByDraft by remember(likedByUsername) { mutableStateOf(likedByUsername) }
    var views by remember(insights) { mutableStateOf(insights.views) }
    var viewers by remember(insights) { mutableStateOf(insights.viewers) }
    var avgWatch by remember(insights) { mutableStateOf(insights.averageWatchTime) }
    var follows by remember(insights) { mutableStateOf(insights.follows) }
    var likes by remember(insights) { mutableStateOf(insights.likes) }
    var comments by remember(insights) { mutableStateOf(insights.comments) }
    var reposts by remember(insights) { mutableStateOf(insights.reposts) }
    var shares by remember(insights) { mutableStateOf(insights.shares) }
    var saves by remember(insights) { mutableStateOf(insights.saves) }
    var profileVisits by remember(insights) { mutableStateOf(insights.profileVisits) }

    var selectedPreset by remember(insights) { mutableStateOf(insights.viewsCurvePreset) }
    var activeSeries by remember { mutableStateOf("All") }
    var startDate by remember(insights) { mutableStateOf(insights.viewsStartDate) }
    var midDate by remember(insights) { mutableStateOf(insights.viewsMidDate) }
    var endDate by remember(insights) { mutableStateOf(insights.viewsEndDate) }
    var yAxisMax by remember(insights) { mutableStateOf(insights.viewsYAxisMax) }
    var ptsAll by remember(insights) { mutableStateOf(insights.viewsOverTimeAll) }
    var ptsFollowers by remember(insights) { mutableStateOf(insights.viewsOverTimeFollowers) }
    var ptsNonFollowers by remember(insights) { mutableStateOf(insights.viewsOverTimeNonFollowers) }

    val currentPts = when (activeSeries) {
        "Followers" -> ptsFollowers
        "Non-followers" -> ptsNonFollowers
        else -> ptsAll
    }

    fun setCurrentSeriesPoint(index: Int, value: Float) {
        selectedPreset = ViewsCurvePresets.CUSTOM
        val clamped = value.coerceIn(0f, 100f)
        when (activeSeries) {
            "Followers" -> ptsFollowers = ptsFollowers.toMutableList().also { if (index in it.indices) it[index] = clamped }
            "Non-followers" -> ptsNonFollowers = ptsNonFollowers.toMutableList().also { if (index in it.indices) it[index] = clamped }
            else -> ptsAll = ptsAll.toMutableList().also { if (index in it.indices) it[index] = clamped }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.card,
        contentColor = palette.textPrimary,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Edit Reel Insights & Views Graph",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary,
            )

            OutlinedButton(
                onClick = {
                    onToggleInlineEditMode()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = if (isInlineEditMode) "Disable Inline Retention/Audience Editing" else "Enable Inline Retention & Audience Editing",
                    color = IgInsightsMagenta,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            HorizontalDivider(color = palette.border)

            if (onSavePostMetadata != null) {
                Text("Post Audio & Social Proof", fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp, color = palette.textPrimary)
                OutlinedTextField(
                    value = audioDraft,
                    onValueChange = { audioDraft = it },
                    label = { Text("Audio Track Title") },
                    placeholder = { Text("e.g. LANY · Stuck") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_post_audio_input"),
                )
                OutlinedTextField(
                    value = likedByDraft,
                    onValueChange = { likedByDraft = it },
                    label = { Text("Liked by Username (blank = auto rotation)") },
                    placeholder = { Text("e.g. catchthis.us") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_post_liker_input"),
                )
                HorizontalDivider(color = palette.border)
            }

            Text("Summary Metrics", fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp, color = palette.textPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = views, onValueChange = { views = it }, label = { Text("Views") }, singleLine = true, modifier = Modifier.weight(1f).testTag("edit_reel_views"))
                OutlinedTextField(value = viewers, onValueChange = { viewers = it }, label = { Text("Viewers") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = avgWatch, onValueChange = { avgWatch = it }, label = { Text("Avg watch") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = follows, onValueChange = { follows = it }, label = { Text("Follows") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = likes, onValueChange = { likes = it }, label = { Text("Likes") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = comments, onValueChange = { comments = it }, label = { Text("Comments") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = reposts, onValueChange = { reposts = it }, label = { Text("Reposts") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = shares, onValueChange = { shares = it }, label = { Text("Shares") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = saves, onValueChange = { saves = it }, label = { Text("Saves") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = profileVisits, onValueChange = { profileVisits = it }, label = { Text("Visits") }, singleLine = true, modifier = Modifier.weight(1f))
            }

            HorizontalDivider(color = palette.border)

            Text("Views Over Time Graph Controls", fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp, color = IgInsightsMagenta)

            // Curve Preset Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ViewsCurvePresets.allPresets.forEach { preset ->
                    val selected = selectedPreset == preset
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (selected) IgInsightsMagenta.copy(alpha = 0.2f) else palette.bg)
                            .border(1.dp, if (selected) IgInsightsMagenta else palette.border, RoundedCornerShape(18.dp))
                            .clickable {
                                selectedPreset = preset
                                if (preset != ViewsCurvePresets.CUSTOM) {
                                    ptsAll = ViewsCurvePresets.pointsForPreset(preset, "All")
                                    ptsFollowers = ViewsCurvePresets.pointsForPreset(preset, "Followers")
                                    ptsNonFollowers = ViewsCurvePresets.pointsForPreset(preset, "Non-followers")
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                            .testTag("preset_${preset.lowercase().replace(" ", "_")}"),
                    ) {
                        Text(
                            text = preset,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (selected) IgInsightsMagenta else palette.textPrimary,
                        )
                    }
                }
            }

            // Date Axis + Y Max
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = startDate, onValueChange = { startDate = it }, label = { Text("Start Date") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = midDate, onValueChange = { midDate = it }, label = { Text("Mid Date") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = endDate, onValueChange = { endDate = it }, label = { Text("End Date") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            OutlinedTextField(
                value = yAxisMax,
                onValueChange = { yAxisMax = it },
                label = { Text("Y-axis Max Scale (blank = auto)") },
                placeholder = { Text("e.g. 4000") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // Series Toggle for 7 Custom Points
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("All", "Followers", "Non-followers").forEach { series ->
                    val selected = activeSeries == series
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (selected) palette.pillSelected else Color.Transparent)
                            .border(1.dp, palette.border, RoundedCornerShape(16.dp))
                            .clickable { activeSeries = series }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    ) {
                        Text(series, fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = palette.textPrimary)
                    }
                }
            }

            // 7 Curve Point Sliders (0-100)
            currentPts.forEachIndexed { idx, ptVal ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Pt ${idx + 1}",
                        fontSize = 12.sp,
                        color = palette.textSecondary,
                        modifier = Modifier.width(38.dp),
                    )
                    Slider(
                        value = ptVal.coerceIn(0f, 100f),
                        onValueChange = { v -> setCurrentSeriesPoint(idx, v) },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = IgInsightsMagenta,
                            activeTrackColor = IgInsightsMagenta,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${ptVal.roundToInt()}",
                        fontSize = 12.sp,
                        color = palette.textPrimary,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(32.dp),
                    )
                }
            }

            Button(
                onClick = {
                    onSavePostMetadata?.invoke(audioDraft.trim(), likedByDraft.trim())
                    onSave(
                        insights.copy(
                            views = views.trim().ifBlank { insights.views },
                            viewers = viewers.trim().ifBlank { insights.viewers },
                            averageWatchTime = avgWatch.trim().ifBlank { insights.averageWatchTime },
                            follows = follows.trim().ifBlank { insights.follows },
                            engagementFollows = follows.trim().ifBlank { insights.engagementFollows },
                            likes = likes.trim().ifBlank { insights.likes },
                            comments = comments.trim().ifBlank { insights.comments },
                            reposts = reposts.trim().ifBlank { insights.reposts },
                            shares = shares.trim().ifBlank { insights.shares },
                            saves = saves.trim().ifBlank { insights.saves },
                            profileVisits = profileVisits.trim().ifBlank { insights.profileVisits },
                            viewsCurvePreset = selectedPreset,
                            viewsStartDate = startDate.trim().ifBlank { "26 Sept" },
                            viewsMidDate = midDate.trim().ifBlank { "6 Oct" },
                            viewsEndDate = endDate.trim().ifBlank { "17 Oct" },
                            viewsYAxisMax = yAxisMax.trim(),
                            viewsOverTimeAll = ptsAll,
                            viewsOverTimeFollowers = ptsFollowers,
                            viewsOverTimeNonFollowers = ptsNonFollowers,
                        )
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = IgBoostBlue, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_reel_insights_modal_button"),
            ) {
                Text("Save Insights & Graph", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }

            Spacer(Modifier.height(14.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostOptionsAndEditSheet(
    audioTitle: String,
    likedByUsername: String,
    onDismiss: () -> Unit,
    onOpenInsights: () -> Unit,
    onSave: (audioTitle: String, likedByUsername: String) -> Unit,
) {
    val palette = rememberInsightsPalette()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var audioDraft by remember(audioTitle) { mutableStateOf(audioTitle) }
    var likedByDraft by remember(likedByUsername) { mutableStateOf(likedByUsername) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.card,
        contentColor = palette.textPrimary,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Post Options & Customization",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary,
            )

            OutlinedButton(
                onClick = {
                    onDismiss()
                    onOpenInsights()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("sheet_open_insights_button"),
            ) {
                Text("View Reel Insights", color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
            }

            HorizontalDivider(color = palette.border)

            Text(
                text = "Customize Audio & Social Proof",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = IgInsightsMagenta,
            )

            OutlinedTextField(
                value = audioDraft,
                onValueChange = { audioDraft = it },
                label = { Text("Audio Track Title") },
                placeholder = { Text("e.g. LANY · Stuck") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("post_sheet_audio_input"),
            )

            OutlinedTextField(
                value = likedByDraft,
                onValueChange = { likedByDraft = it },
                label = { Text("Liked by Username (clear for auto rotation)") },
                placeholder = { Text("e.g. catchthis.us") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("post_sheet_liker_input"),
            )

            Button(
                onClick = {
                    onSave(audioDraft.trim(), likedByDraft.trim())
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = IgBoostBlue, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("post_sheet_save_button"),
            ) {
                Text("Save Audio & Liked By", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }

            Spacer(Modifier.height(14.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountInsightsEditModal(
    accountInsights: AccountInsightsData,
    onDismiss: () -> Unit,
    onSave: (AccountInsightsData) -> Unit,
) {
    val palette = rememberInsightsPalette()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var dateRange by remember(accountInsights) { mutableStateOf(accountInsights.dateRange) }
    var dashboardViews by remember(accountInsights) { mutableStateOf(accountInsights.dashboardViews) }
    var newFollowers by remember(accountInsights) { mutableStateOf(accountInsights.newFollowers) }
    var contentShared by remember(accountInsights) { mutableStateOf(accountInsights.contentShared) }
    var overviewViews by remember(accountInsights) { mutableStateOf(accountInsights.overviewViews) }
    var netFollowers by remember(accountInsights) { mutableStateOf(accountInsights.netFollowers) }
    var interactions by remember(accountInsights) { mutableStateOf(accountInsights.interactions) }
    var viewersTotal by remember(accountInsights) { mutableStateOf(accountInsights.viewersTotal) }
    var reelsViews by remember(accountInsights) { mutableStateOf(accountInsights.reelsViews) }
    var profileVisits by remember(accountInsights) { mutableStateOf(accountInsights.profileVisits) }
    var followersPct by remember(accountInsights) { mutableStateOf(accountInsights.followersPct.toString()) }
    var audienceFollowersTotal by remember(accountInsights) { mutableStateOf(accountInsights.audienceFollowersTotal) }
    var audienceChangeSubtitle by remember(accountInsights) { mutableStateOf(accountInsights.audienceChangeSubtitle) }
    var startDate by remember(accountInsights) { mutableStateOf(accountInsights.overviewChartStartDate) }
    var midDate by remember(accountInsights) { mutableStateOf(accountInsights.overviewChartMidDate) }
    var endDate by remember(accountInsights) { mutableStateOf(accountInsights.overviewChartEndDate) }
    var yAxisMax by remember(accountInsights) { mutableStateOf(accountInsights.overviewViewsYAxisMax) }
    var overviewCurve by remember(accountInsights) { mutableStateOf(accountInsights.overviewViewsCurve) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.card,
        contentColor = palette.textPrimary,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Edit Dashboard & Account Insights",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary,
            )

            Text("Dashboard Summary", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = IgInsightsMagenta)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = dateRange, onValueChange = { dateRange = it }, label = { Text("Date Range") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = dashboardViews, onValueChange = { dashboardViews = it }, label = { Text("Dashboard Views") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = newFollowers, onValueChange = { newFollowers = it }, label = { Text("New followers") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = contentShared, onValueChange = { contentShared = it }, label = { Text("Content shared") }, singleLine = true, modifier = Modifier.weight(1f))
            }

            HorizontalDivider(color = palette.border)
            Text("Account Insights Overview & Audience", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = IgInsightsMagenta)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = overviewViews, onValueChange = { overviewViews = it }, label = { Text("Views") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = netFollowers, onValueChange = { netFollowers = it }, label = { Text("Net followers") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = interactions, onValueChange = { interactions = it }, label = { Text("Interactions") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = viewersTotal, onValueChange = { viewersTotal = it }, label = { Text("Viewers") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = reelsViews, onValueChange = { reelsViews = it }, label = { Text("Reels views") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = profileVisits, onValueChange = { profileVisits = it }, label = { Text("Profile visits") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = followersPct, onValueChange = { followersPct = it }, label = { Text("Followers %") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = audienceFollowersTotal, onValueChange = { audienceFollowersTotal = it }, label = { Text("Total Followers") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            OutlinedTextField(value = audienceChangeSubtitle, onValueChange = { audienceChangeSubtitle = it }, label = { Text("Growth Subtitle") }, singleLine = true, modifier = Modifier.fillMaxWidth())

            HorizontalDivider(color = palette.border)
            Text("Overview Graph Scale & Curve", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = IgInsightsMagenta)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = startDate, onValueChange = { startDate = it }, label = { Text("Start") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = midDate, onValueChange = { midDate = it }, label = { Text("Mid") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = endDate, onValueChange = { endDate = it }, label = { Text("End") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            OutlinedTextField(value = yAxisMax, onValueChange = { yAxisMax = it }, label = { Text("Overview Y-Axis Max") }, singleLine = true, modifier = Modifier.fillMaxWidth())

            // Preset quick-apply buttons for Overview graph
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(ViewsCurvePresets.VIRAL_SPIKE, ViewsCurvePresets.STEADY_GROWTH, ViewsCurvePresets.DELAYED_SPIKE).forEach { preset ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(palette.bg)
                            .border(1.dp, palette.border, RoundedCornerShape(16.dp))
                            .clickable {
                                val pts = ViewsCurvePresets.pointsForPreset(preset, "All")
                                overviewCurve = pts
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(preset, fontSize = 12.sp, color = palette.textPrimary)
                    }
                }
            }

            Button(
                onClick = {
                    val fPct = (followersPct.toDoubleOrNull() ?: accountInsights.followersPct).coerceIn(0.0, 100.0)
                    onSave(
                        accountInsights.copy(
                            dateRange = dateRange.trim().ifBlank { accountInsights.dateRange },
                            dashboardViews = dashboardViews.trim().ifBlank { accountInsights.dashboardViews },
                            newFollowers = newFollowers.trim().ifBlank { accountInsights.newFollowers },
                            contentShared = contentShared.trim().ifBlank { accountInsights.contentShared },
                            overviewViews = overviewViews.trim().ifBlank { accountInsights.overviewViews },
                            netFollowers = netFollowers.trim().ifBlank { accountInsights.netFollowers },
                            interactions = interactions.trim().ifBlank { accountInsights.interactions },
                            followersPct = fPct,
                            nonFollowersPct = ((100.0 - fPct) * 10.0).roundToInt() / 10.0,
                            viewersTotal = viewersTotal.trim().ifBlank { accountInsights.viewersTotal },
                            reelsViews = reelsViews.trim().ifBlank { accountInsights.reelsViews },
                            interactionsReels = interactions.trim().ifBlank { accountInsights.interactionsReels },
                            profileVisits = profileVisits.trim().ifBlank { accountInsights.profileVisits },
                            audienceFollowersTotal = audienceFollowersTotal.trim().ifBlank { accountInsights.audienceFollowersTotal },
                            audienceChangeSubtitle = audienceChangeSubtitle.trim().ifBlank { accountInsights.audienceChangeSubtitle },
                            overviewChartStartDate = startDate.trim().ifBlank { "16 Oct" },
                            overviewChartMidDate = midDate.trim().ifBlank { "31 Oct" },
                            overviewChartEndDate = endDate.trim().ifBlank { "14 Nov" },
                            overviewViewsYAxisMax = yAxisMax.trim().ifBlank { "5800" },
                            overviewViewsCurve = overviewCurve,
                        )
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = IgBoostBlue, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_account_insights_button"),
            ) {
                Text("Save Dashboard & Insights", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }

            Spacer(Modifier.height(14.dp))
        }
    }
}
