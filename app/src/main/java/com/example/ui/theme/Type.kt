package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

/**
 * Script wordmark reserved exclusively for the Instagram top brand logo.
 */
val GrandHotelFontFamily = FontFamily(
    Font(R.font.grand_hotel, FontWeight.Normal)
)

/**
 * Primary Android system font (Roboto).
 */
val IgSystemFontFamily = FontFamily.SansSerif

/**
 * Official Instagram typography tokens following platform native Roboto scale and weights:
 * - Regular (400): captions, descriptions, secondary metadata, statistic labels
 * - Medium (500): navigation labels, subtle secondary handles/tabs
 * - Semibold (600): headings, buttons, usernames, profile stats, insights section headings
 * - Bold (700): prominent Insights numbers & key numeric figures
 */
object IgTypography {
    val screenHeading = TextStyle(
        fontFamily = IgSystemFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.2).sp,
    )
    val username = TextStyle(
        fontFamily = IgSystemFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    )
    val usernameMedium = TextStyle(
        fontFamily = IgSystemFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    )
    val displayName = TextStyle(
        fontFamily = IgSystemFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    )
    val postCaption = TextStyle(
        fontFamily = IgSystemFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 19.sp,
    )
    val navLabel = TextStyle(
        fontFamily = IgSystemFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    )
    val profileStatValue = TextStyle(
        fontFamily = IgSystemFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        fontFeatureSettings = "tnum",
    )
    val profileStatLabel = TextStyle(
        fontFamily = IgSystemFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    )
    val button = TextStyle(
        fontFamily = IgSystemFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    )
    val secondaryMeta = TextStyle(
        fontFamily = IgSystemFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    )
    val insightsMainNumber = TextStyle(
        fontFamily = IgSystemFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.2).sp,
        fontFeatureSettings = "tnum",
    )
    val insightsSectionHeading = TextStyle(
        fontFamily = IgSystemFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    )
    val chartLabel = TextStyle(
        fontFamily = IgSystemFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.5.sp,
        lineHeight = 14.sp,
        fontFeatureSettings = "tnum",
    )
}

val Typography = Typography(
    displaySmall = TextStyle(
        fontFamily = GrandHotelFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 30.sp,
        lineHeight = 34.sp,
    ),
    titleLarge = IgTypography.screenHeading,
    titleMedium = IgTypography.insightsSectionHeading,
    bodyLarge = TextStyle(
        fontFamily = IgSystemFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    bodyMedium = IgTypography.postCaption,
    labelLarge = IgTypography.button,
    labelMedium = IgTypography.navLabel,
    labelSmall = IgTypography.secondaryMeta,
)
