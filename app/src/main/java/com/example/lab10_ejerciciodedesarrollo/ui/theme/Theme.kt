package com.example.lab10_ejerciciodedesarrollo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Claro = lightColorScheme(
    primary=Color(0xFFAF431D), onPrimary=Color.White,
    primaryContainer=Color(0xFFFCE6D9), onPrimaryContainer=Color(0xFF60260E),
    secondary=Color(0xFF206356), onSecondary=Color.White,
    secondaryContainer=Color(0xFFDAEDE5), onSecondaryContainer=Color(0xFF164B40),
    background=Color(0xFFF5F4F0), onBackground=Color(0xFF182638),
    surface=Color(0xFFF5F4F0), onSurface=Color(0xFF182638),
    surfaceContainer=Color.White, surfaceContainerHigh=Color(0xFFECEBE6),
    onSurfaceVariant=Color(0xFF54606E),outline=Color(0xFF7B8189),outlineVariant=Color(0xFFDADDDC),
    error=Color(0xFFAD2929),onError=Color.White,errorContainer=Color(0xFFFFE6E1),onErrorContainer=Color(0xFF71201D),
)
private val Oscuro = darkColorScheme(
    primary=Color(0xFFFFB38D),onPrimary=Color(0xFF53200B),
    primaryContainer=Color(0xFF63361F),onPrimaryContainer=Color(0xFFFFDDCB),
    secondary=Color(0xFF9AD8C5),onSecondary=Color(0xFF073A2E),
    secondaryContainer=Color(0xFF254D42),onSecondaryContainer=Color(0xFFC5F0DF),
    background=Color(0xFF111A23),onBackground=Color(0xFFE7EBED),
    surface=Color(0xFF111A23),onSurface=Color(0xFFE7EBED),
    surfaceContainer=Color(0xFF1D2A35),surfaceContainerHigh=Color(0xFF293742),
    onSurfaceVariant=Color(0xFFB7C2CC),outline=Color(0xFF84929D),outlineVariant=Color(0xFF43505B),
    error=Color(0xFFFFB4A8),onError=Color(0xFF65120C),errorContainer=Color(0xFF59302D),onErrorContainer=Color(0xFFFFDAD5),
)
private val Texto = Typography(
    headlineLarge=TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.Bold,fontSize=32.sp,lineHeight=36.sp,letterSpacing=(-1).sp),
    headlineSmall=TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.Bold,fontSize=24.sp,lineHeight=29.sp),
    titleLarge=TextStyle(fontWeight=FontWeight.Bold,fontSize=21.sp,lineHeight=27.sp),
    titleMedium=TextStyle(fontWeight=FontWeight.SemiBold,fontSize=17.sp,lineHeight=23.sp),
    bodyLarge=TextStyle(fontSize=16.sp,lineHeight=24.sp),
    bodyMedium=TextStyle(fontSize=14.sp,lineHeight=21.sp),
    labelLarge=TextStyle(fontWeight=FontWeight.SemiBold,fontSize=14.sp,lineHeight=20.sp),
)
@Composable
fun EventFlowTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme=if(dark) Oscuro else Claro,typography=Texto,content=content)
}
