package com.example.bookbuddies.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import com.example.bookbuddies.R

// Set of Material typography styles to start with
val MyTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily(Font(R.font.sf_pro_display)),
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.5.sp),

    bodyMedium = TextStyle(
        fontFamily = FontFamily(Font(R.font.sf_pro_display)),
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.5.sp),

    bodySmall = TextStyle(
        fontFamily = FontFamily(Font(R.font.sf_pro_display)),
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp),

    titleLarge = TextStyle(
        fontFamily = FontFamily(Font(R.font.sf_pro_display)),
        fontWeight = FontWeight.Bold,
        fontSize = 50.sp,
        lineHeight = 50.sp,
        letterSpacing = 0.5.sp,
        textAlign = TextAlign.Center
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily(Font(R.font.sf_pro_display)),
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.5.sp,
        textAlign = TextAlign.Center
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily(Font(R.font.sf_pro_display)),
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
        textAlign = TextAlign.Center
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily(Font(R.font.sf_pro_display)),
        fontWeight = FontWeight.Normal,
        textDecoration = TextDecoration.Underline,
        fontSize = 16.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp),

    labelSmall = TextStyle(
        fontFamily = FontFamily(Font(R.font.sf_pro_display)),
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp)
)