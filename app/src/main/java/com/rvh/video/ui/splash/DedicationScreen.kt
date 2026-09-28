package com.rvh.video.ui.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rvh.video.ui.theme.AccentTeal
import com.rvh.video.ui.theme.DedicationFontFamily
import com.rvh.video.ui.theme.Surface0
import kotlinx.coroutines.delay
import kotlin.math.min

/**
 * RVH's branded launch moment: a lightweight, fully local Compose mark with
 * a breathing ring, play glyph, wordmark and dedication. It deliberately
 * avoids external images so startup remains fast and offline.
 */
@Composable
fun DedicationScreen(onFinished: () -> Unit) {
    var visible by remember { mutableStateOf(true) }
    val transition = rememberInfiniteTransition(label = "rvhSplash")
    val pulse by transition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val glowAlpha by transition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.42f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    LaunchedEffect(Unit) {
        delay(2600)
        visible = false
        delay(600)
        onFinished()
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(450)),
        exit = fadeOut(animationSpec = tween(600)),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Surface0),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(156.dp)) {
                    Canvas(modifier = Modifier.size(156.dp)) {
                        val radius = min(size.width, size.height) * 0.34f * pulse
                        drawCircle(
                            color = AccentTeal.copy(alpha = glowAlpha),
                            radius = radius + 16.dp.toPx(),
                            style = Stroke(width = 3.dp.toPx())
                        )
                        drawCircle(
                            color = AccentTeal.copy(alpha = 0.78f),
                            radius = radius,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(Color(0xFF1D2126)),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(92.dp).alpha(0.98f)) {
                            val left = size.width * 0.38f
                            val top = size.height * 0.31f
                            val right = size.width * 0.70f
                            val bottom = size.height * 0.69f
                            val path = androidx.compose.ui.graphics.Path().apply {
                                moveTo(left, top)
                                lineTo(right, size.height * 0.50f)
                                lineTo(left, bottom)
                                close()
                            }
                            drawPath(path = path, color = AccentTeal)
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                Text(
                    text = "RVH VIDEO",
                    style = TextStyle(
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp,
                        color = Color.White
                    )
                )

                Spacer(Modifier.height(7.dp))

                Text(
                    text = "Your media. Your world.",
                    style = TextStyle(
                        fontSize = 13.sp,
                        letterSpacing = 0.5.sp,
                        color = Color.White.copy(alpha = 0.62f)
                    )
                )

                Spacer(Modifier.height(30.dp))

                Text(
                    text = "In dedication to Hellen",
                    style = TextStyle(
                        fontFamily = DedicationFontFamily,
                        fontStyle = FontStyle.Italic,
                        fontSize = 19.sp,
                        color = Color.White.copy(alpha = 0.90f),
                    )
                )
            }
        }
    }
}
