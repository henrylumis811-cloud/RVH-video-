package com.rvh.video.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * Dark theme done properly means surfaces get LIGHTER as they rise in
 * elevation (closer to the user), not just "black with a card on top."
 * This mirrors Material's elevation-overlay approach but tuned darker
 * and cooler to match the mockup's near-black background.
 *
 * Background  -> Surface0 (base)
 * Cards/grid  -> Surface1
 * Sheets/nav  -> Surface2
 * Glass panel -> Surface2 + blur + border, never a flat fill alone
 */
val Surface0 = Color(0x660E1013)   // app background
val Surface1 = Color(0xAA16191D)   // grid cards, list rows
val Surface2 = Color(0xB81D2126)   // bottom nav, sheets, floating chips
val Surface3 = Color(0xCC262B31)   // topmost floating elements (mini-player)

// Glass panels are a translucent tint layer + border, not a blur (see
// Glass.kt for why). Alpha raised from the original blur-backed design
// (which relied on blur to add contrast) so panels stay legible against
// bright or busy content behind them — video thumbnails vary a lot in
// brightness, and there's no blur doing any of that work anymore.
val GlassTintLight = Color(0x330D2B2D) // ~20% white, for panels over bright content
val GlassTintDark = Color(0x66040D0F)  // ~25% black, for panels over dark content
val GlassBorder = Color(0x6634D8CF)    // slightly stronger hairline to still read as an edge without blur separating it

// Accent: appears in exactly 3 places by design — active nav tab,
// primary action (play/pause, send), and progress indicators.
val AccentTeal = Color(0xFF2DD4C8)
val AccentTealDim = Color(0xFF1A8F86) // pressed/disabled state of accent

val TextPrimary = Color(0xFFF2F3F5)
val TextSecondary = Color(0xFFA7ADB5)
val TextTertiary = Color(0xFF6E747C)

val DangerRed = Color(0xFFE5484D) // like/heart fill, kept out of the 3 accent slots on purpose
