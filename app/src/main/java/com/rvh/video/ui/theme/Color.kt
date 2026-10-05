package com.rvh.video.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * RVH visual language:
 * - near-black translucent surfaces
 * - one warm gold action/accent family
 * - white/neutral text
 *
 * Keep accent decisions centralized here so players, gestures, telemetry,
 * library controls, dialogs and navigation do not drift into different colors.
 */
val Surface0 = Color(0x660E1013)
val Surface1 = Color(0xAA16191D)
val Surface2 = Color(0xB81D2126)
val Surface3 = Color(0xCC262B31)

val GlassTintLight = Color(0x33000000)
val GlassTintDark = Color(0x66000000)
val GlassBorder = Color(0x66E0B35A)

val RvhGold = Color(0xFFE0B35A)
val RvhGoldSoft = Color(0xFFE8D9B9)
val RvhGoldDim = Color(0xFF8F7040)

// Kept as the existing source-level name so older screens inherit the new
// RVH palette without needing duplicate accent declarations.
val AccentTeal = RvhGold
val AccentTealDim = RvhGoldDim

val TextPrimary = Color(0xFFF2F3F5)
val TextSecondary = Color(0xFFA7ADB5)
val TextTertiary = Color(0xFF6E747C)

val DangerRed = Color(0xFFE5484D)

// Shared player chrome. The black is intentionally translucent so video remains
// visible underneath while the controls/telemetry still read as one surface.
val PlayerControlBlack = Color.Black.copy(alpha = 0.72f)
val PlayerControlBlackRaised = Color.Black.copy(alpha = 0.80f)
