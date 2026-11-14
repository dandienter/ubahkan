package com.ubahkan.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubahkan.app.core.AppStrings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ============================================================
// SPLASH SCREEN  -  animasi satu kali jalan, total ±1,4 detik
// Samakan pratinjau splash.html:
// 1) U digambar progresif dari ujung kiri atas (trim path, 0,7 dtk)
// 2) Mata panah biru pop/spring mulai ±0,55 dtk
// 3) Wordmark "Ubahkan." fade + naik mulai ±0,85 dtk
// 4) Tagline letterspaced fade in terakhir mulai ±1,1 dtk
// Sentuhan di mana pun melewati animasi.
// ============================================================

@Composable
fun SplashScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = AppStrings.current

    var finished by remember { mutableStateOf(false) }
    val finish = {
        if (!finished) {
            finished = true
            onFinished()
        }
    }

    val drawProgress = remember { Animatable(0f) }
    val arrowScale = remember { Animatable(0f) }
    val wordAlpha = remember { Animatable(0f) }
    val wordRise = remember { Animatable(10f) }
    val tagAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            drawProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
            )
        }
        launch {
            delay(550)
            arrowScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.5f,
                    stiffness = 380f
                )
            )
        }
        launch {
            delay(850)
            launch {
                wordAlpha.animateTo(1f, tween(durationMillis = 450))
            }
            wordRise.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing)
            )
        }
        launch {
            delay(1100)
            tagAlpha.animateTo(1f, tween(durationMillis = 300))
        }
        delay(1450)
        finish()
    }

    val inkColor = MaterialTheme.colorScheme.onBackground
    val arrowColor = MaterialTheme.colorScheme.primary
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { finish() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Logo U Panah  -  geometri sama dengan drawable logo (kanvas 108)
            Canvas(modifier = Modifier.size(150.dp)) {
                val s = size.width / 108f

                val uPath = Path().apply {
                    moveTo(33f * s, 22f * s)
                    lineTo(33f * s, 64f * s)
                    arcTo(
                        rect = Rect(33f * s, 43f * s, 75f * s, 85f * s),
                        startAngleDegrees = 180f,
                        sweepAngleDegrees = -180f,
                        forceMoveTo = false
                    )
                    lineTo(75f * s, 22f * s)
                }
                val measure = PathMeasure().apply { setPath(uPath, false) }
                val segment = Path()
                measure.getSegment(
                    0f,
                    measure.length * drawProgress.value,
                    segment,
                    true
                )
                if (drawProgress.value > 0f) {
                    drawPath(
                        path = segment,
                        color = inkColor,
                        style = Stroke(width = 10.5f * s, cap = StrokeCap.Round)
                    )
                }

                // Mata panah  -  pop dengan poros di pangkal panah (75, 22)
                if (arrowScale.value > 0f) {
                    val pivot = Offset(75f * s, 22f * s)
                    withTransform({
                        scale(arrowScale.value, arrowScale.value, pivot)
                    }) {
                        val triangle = Path().apply {
                            moveTo(75f * s, 9f * s)
                            lineTo(84f * s, 23f * s)
                            lineTo(66f * s, 23f * s)
                            close()
                        }
                        drawPath(path = triangle, color = arrowColor)
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Wordmark
            Row(
                modifier = Modifier
                    .alpha(wordAlpha.value)
                    .offset(y = wordRise.value.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ubahkan",
                    fontSize = 31.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.6).sp,
                    color = inkColor
                )
                Text(
                    text = ".",
                    fontSize = 31.sp,
                    fontWeight = FontWeight.Medium,
                    color = arrowColor
                )
            }

            Spacer(modifier = Modifier.height(9.dp))

            // Tagline
            Text(
                text = strings.splashTagline.uppercase(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.9.sp,
                color = mutedColor,
                modifier = Modifier.alpha(tagAlpha.value)
            )
        }

        // Footer kecil  -  penanda offline
        Text(
            text = strings.homeKicker,
            fontSize = 11.5.sp,
            color = mutedColor.copy(alpha = 0.6f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 30.dp)
                .alpha(tagAlpha.value)
        )
    }
}
