package com.abdulbasir.vibecord.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun isDarkTheme(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

@Composable
fun statusBarTop(): Dp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

/**
 * The "liquid glass" surface: a translucent tinted fill, a soft sheen that is
 * brighter at the top, and a thin bright rim that fades along the edge.
 *
 * [fill] is how strong the tinted base is. Floating bars use a higher value so
 * the text under them stays readable.
 */
@Composable
fun Modifier.glass(
    shape: Shape = RoundedCornerShape(24.dp),
    dark: Boolean = isDarkTheme(),
    fill: Float? = null,
): Modifier {
    val baseColor = if (dark) Color(0xFF2C2C2E) else Color.White
    val baseAlpha = (fill ?: if (dark) 0.42f else 0.55f).coerceIn(0f, 1f)
    val sheenTop = Color.White.copy(alpha = if (dark) 0.14f else 0.50f)
    val sheenBottom = Color.White.copy(alpha = if (dark) 0.03f else 0.08f)
    val rimBright = Color.White.copy(alpha = if (dark) 0.50f else 0.95f)
    val rimDim = Color.White.copy(alpha = if (dark) 0.06f else 0.30f)
    val rimEnd = Color.White.copy(alpha = if (dark) 0.28f else 0.60f)
    return this
        .clip(shape)
        .background(baseColor.copy(alpha = baseAlpha))
        .background(Brush.verticalGradient(listOf(sheenTop, sheenBottom)))
        .border(1.dp, Brush.linearGradient(listOf(rimBright, rimDim, rimEnd)), shape)
}

/** Soft coloured glow behind everything, so the glass has something to catch. */
@Composable
fun AuroraBackground(modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val base = MaterialTheme.colorScheme.background
    val strength = if (isDarkTheme()) 0.36f else 0.28f
    Box(
        modifier
            .fillMaxSize()
            .background(base)
            .drawBehind {
                val w = size.width
                val h = size.height
                drawRect(
                    Brush.radialGradient(
                        listOf(accent.copy(alpha = strength), Color.Transparent),
                        center = Offset(w * 0.15f, h * 0.08f),
                        radius = w * 1.0f,
                    ),
                )
                drawRect(
                    Brush.radialGradient(
                        listOf(Color(0xFF5E5CE6).copy(alpha = strength * 0.9f), Color.Transparent),
                        center = Offset(w * 0.95f, h * 0.38f),
                        radius = w * 0.9f,
                    ),
                )
                drawRect(
                    Brush.radialGradient(
                        listOf(Color(0xFF64D2FF).copy(alpha = strength * 0.7f), Color.Transparent),
                        center = Offset(w * 0.2f, h * 0.85f),
                        radius = w * 0.95f,
                    ),
                )
            },
    )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(26.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.glass(shape), content = content)
}

@Composable
fun GlassIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dark: Boolean = isDarkTheme(),
    tint: Color = MaterialTheme.colorScheme.onSurface,
    size: Dp = 44.dp,
) {
    Box(
        modifier
            .size(size)
            .glass(CircleShape, dark = dark)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
fun GlassButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
) {
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(20.dp)
    val surface = if (filled) {
        Modifier
            .clip(shape)
            .background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.95f), accent.copy(alpha = 0.75f))))
            .border(1.dp, Color.White.copy(alpha = 0.35f), shape)
    } else {
        Modifier.glass(shape)
    }
    Row(
        modifier
            .then(surface)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val content = if (filled) Color.White else accent
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = content, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

@Composable
fun GlassSegmented(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(18.dp))
            .padding(4.dp),
    ) {
        options.forEachIndexed { index, label ->
            val isSelected = index == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isSelected) accent else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    maxLines = 1,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

/** The Vibecord mark: the app icon, drawn in code so it stays sharp at any size. */
@Composable
fun VibeLogo(modifier: Modifier = Modifier, logoSize: Dp = 52.dp) {
    Box(
        modifier
            .size(logoSize)
            .clip(RoundedCornerShape(logoSize * 0.28f))
            .background(Brush.linearGradient(listOf(Color(0xFFFF375F), Color(0xFFBF5AF2)))),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val k = 1.45f / 108f
            fun at(x: Float, y: Float) = Offset(w * (0.5f + (x - 54f) * k), w * (0.5f + (y - 54f) * k))
            val stroke = 6f * k * w
            val bars = listOf(
                Triple(34f, 48f, 60f),
                Triple(44f, 41f, 67f),
                Triple(54f, 34f, 74f),
                Triple(64f, 43f, 65f),
                Triple(74f, 47f, 61f),
            )
            for ((x, top, bottom) in bars) {
                drawLine(Color.White, at(x, top), at(x, bottom), strokeWidth = stroke, cap = StrokeCap.Round)
            }
        }
    }
}

/**
 * The bottom bar: a glass pill with the main tabs, and one round glass button
 * beside it (Settings). [selected] is -1 when none of the pill tabs is active.
 */
@Composable
fun FloatingNavBar(
    tabs: List<Pair<String, ImageVector>>,
    selected: Int,
    onSelect: (Int) -> Unit,
    sideIcon: ImageVector,
    sideLabel: String,
    sideSelected: Boolean,
    onSide: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    val active = MaterialTheme.colorScheme.onSurface
    val idle = MaterialTheme.colorScheme.onSurfaceVariant
    val fill = if (isDarkTheme()) 0.62f else 0.72f

    Row(
        modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier
                .weight(1f)
                .glass(RoundedCornerShape(40.dp), fill = fill)
                .padding(6.dp),
        ) {
            tabs.forEachIndexed { index, (label, icon) ->
                val isSelected = index == selected
                val capsule = RoundedCornerShape(34.dp)
                Column(
                    Modifier
                        .weight(1f)
                        .clip(capsule)
                        .then(
                            if (isSelected) {
                                Modifier
                                    .background(accent.copy(alpha = 0.20f))
                                    .border(1.dp, Color.White.copy(alpha = 0.28f), capsule)
                            } else {
                                Modifier
                            },
                        )
                        .clickable { onSelect(index) }
                        .padding(vertical = 9.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isSelected) active else idle,
                        modifier = Modifier.size(26.dp),
                    )
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        maxLines = 1,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) active else idle,
                    )
                }
            }
        }

        Box(
            Modifier
                .size(72.dp)
                .glass(CircleShape, fill = fill)
                .then(if (sideSelected) Modifier.background(accent.copy(alpha = 0.20f)) else Modifier)
                .clickable(onClick = onSide),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = sideIcon,
                contentDescription = sideLabel,
                tint = if (sideSelected) accent else active,
                modifier = Modifier.size(30.dp),
            )
        }
    }
}
