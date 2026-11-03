package com.abdulbasir.vibecord.ui

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
) {
    Box(
        modifier
            .size(44.dp)
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

/** The floating tab bar at the bottom, a glass pill with the selected tab lit. */
@Composable
fun GlassTabBar(
    tabs: List<Pair<String, ImageVector>>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    val idle = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .glass(RoundedCornerShape(34.dp), fill = if (isDarkTheme()) 0.72f else 0.78f)
            .padding(6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        tabs.forEachIndexed { index, (label, icon) ->
            val isSelected = index == selected
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(28.dp))
                    .background(if (isSelected) accent.copy(alpha = 0.18f) else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isSelected) accent else idle,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = label,
                    fontSize = 11.sp,
                    maxLines = 1,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isSelected) accent else idle,
                )
            }
        }
    }
}
