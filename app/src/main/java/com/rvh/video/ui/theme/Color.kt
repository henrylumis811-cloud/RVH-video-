package com.rvh.video.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * RVH visual language:
 * - deep charcoal surfaces with restrained tonal separation
 * - desaturated blue-cyan as the shared RVH family accent
 * - soft white and neutral-grey typography
 *
 * Accent colour is intentionally calm at rest. Reserve stronger contrast for
 * genuinely important or selected states rather than outlining everything.
 * Keep these decisions centralized so screens and player chrome stay coherent.
 */
val Surface0 = Color(0x660E1013)
val Surface1 = Color(0xAA16191D)
val Surface2 = Color(0xB81D2126)
val Surface3 = Color(0xCC262B31)

val GlassTintLight = Color(0x33000000)
val GlassTintDark = Color(0x66000000)
val GlassBorder = Color(0x6676AEB7)

// Legacy names are retained for source compatibility. The palette is now a
// muted blue-cyan: recognisable as RVH, without the glare of electric neon.
val RvhGold = Color(0xFF76AEB7)
val RvhGoldSoft = Color(0xFFB4CDD1)
val RvhGoldDim = Color(0xFF456F78)

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
