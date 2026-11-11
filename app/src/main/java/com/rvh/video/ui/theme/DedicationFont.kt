package com.rvh.video.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.rvh.video.R

/**
 * Used for the dedication message only — deliberately not part of the
 * app's everyday type scale (RvhType), which stays sans-serif throughout
 * the rest of the UI. Crimson Pro Italic, OFL-licensed (bundled at
 * res/font/crimson_pro_italic.ttf; license text kept at the project root
 * as CrimsonPro-OFL.txt for attribution).
 */
val DedicationFontFamily = FontFamily(Font(R.font.crimson_pro_italic))
