package com.nosdois.app.ui

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nosdois.app.Dates
import kotlin.math.sin

// ---------- Cartões e títulos ----------

/** Cartão padrão: cantos bem arredondados, sombra rosada suave e borda fininha. */
@Composable
fun SectionCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier
            .fillMaxWidth()
            .shadow(14.dp, shape, ambientColor = love.shadow.copy(alpha = 0.10f), spotColor = love.shadow.copy(alpha = 0.18f))
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f), shape),
    ) { content() }
}

@Composable
fun ScreenTitle(title: String, subtitle: String) {
    Column(Modifier.padding(top = 12.dp, bottom = 6.dp, start = 4.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Rótulo pequeno em caixa alta, com espaçamento entre letras. */
@Composable
fun Eyebrow(text: String, color: Color = MaterialTheme.colorScheme.primary) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.6.sp),
        color = color,
    )
}

/** Quadradinho arredondado com emoji, usado como ícone de seção. */
@Composable
fun EmojiBadge(emoji: String, pastel: Pastel, size: Dp = 42.dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size * 0.32f)).background(pastel.bg),
        contentAlignment = Alignment.Center,
    ) { Text(emoji, fontSize = (size.value * 0.48f).sp) }
}

@Composable
fun EmptyState(emoji: String, title: String, text: String) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 28.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(88.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) { Text(emoji, fontSize = 40.sp) }
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = { onConfirm(); onDismiss() }) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

// ---------- Foto ----------

@Composable
fun CouplePhoto(photo: Bitmap?, size: Dp, ringColor: Color = Color.White) {
    Box(
        Modifier
            .size(size)
            .shadow(18.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.35f))
            .clip(CircleShape)
            .background(ringColor)
            .padding(4.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (photo != null) {
            Image(
                bitmap = photo.asImageBitmap(),
                contentDescription = "Foto do casal",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size),
            )
        } else {
            PulsingHeart(size / 2.6f, MaterialTheme.colorScheme.primary)
        }
    }
}

// ---------- Corações ----------

/** Desenha um coração ocupando o retângulo [size] a partir de [topLeft]. */
fun heartPath(topLeft: Offset, size: Size): Path = Path().apply {
    val (x, y) = topLeft
    val w = size.width
    val h = size.height
    moveTo(x + w / 2, y + h * 0.95f)
    cubicTo(x - w * 0.15f, y + h * 0.5f, x + w * 0.05f, y - h * 0.15f, x + w / 2, y + h * 0.25f)
    cubicTo(x + w * 0.95f, y - h * 0.15f, x + w * 1.15f, y + h * 0.5f, x + w / 2, y + h * 0.95f)
    close()
}

@Composable
fun PulsingHeart(size: Dp, color: Color) {
    val t = rememberInfiniteTransition(label = "pulso")
    val scale by t.animateFloat(
        initialValue = 1f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(tween(650, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "escala",
    )
    Icon(Icons.Filled.Favorite, null, tint = color, modifier = Modifier.size(size).scale(scale))
}

private data class Floater(val x: Float, val size: Float, val speed: Float, val phase: Float, val sway: Float)

private val floaters = listOf(
    Floater(0.08f, 14f, 1.0f, 0.10f, 10f),
    Floater(0.20f, 9f, 1.4f, 0.55f, 6f),
    Floater(0.33f, 18f, 0.8f, 0.30f, 12f),
    Floater(0.70f, 12f, 1.2f, 0.80f, 8f),
    Floater(0.82f, 20f, 0.7f, 0.05f, 14f),
    Floater(0.92f, 10f, 1.3f, 0.45f, 6f),
    Floater(0.55f, 8f, 1.6f, 0.68f, 5f),
)

/** Coraçõezinhos subindo devagar, como decoração de fundo. */
@Composable
fun FloatingHearts(modifier: Modifier, color: Color) {
    val t = rememberInfiniteTransition(label = "coracoes")
    val progress by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing)),
        label = "subida",
    )
    Canvas(modifier) {
        floaters.forEach { f ->
            val p = (progress * f.speed + f.phase) % 1f
            val y = size.height * (1.05f - p * 1.1f)
            val x = size.width * f.x + sin(p * 6.28f * 2) * f.sway.dp.toPx()
            val s = f.size.dp.toPx()
            val alpha = when {
                p < 0.15f -> p / 0.15f
                p > 0.8f -> (1f - p) / 0.2f
                else -> 1f
            }
            drawPath(heartPath(Offset(x, y), Size(s, s)), color.copy(alpha = color.alpha * alpha))
        }
    }
}

// ---------- Números ----------

/** Número que "conta" de 0 até o valor ao aparecer. */
@Composable
fun AnimatedCount(target: Long, style: TextStyle, color: Color = Color.Unspecified, modifier: Modifier = Modifier) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(target) {
        anim.animateTo(target.toFloat(), tween(1400, easing = FastOutSlowInEasing))
    }
    Text(Dates.num(anim.value.toLong()), style = style, color = color, modifier = modifier)
}

/** Anel de progresso com degradê. */
@Composable
fun ProgressRing(
    fraction: Float,
    size: Dp,
    stroke: Dp,
    track: Color,
    brush: Brush,
    content: @Composable () -> Unit,
) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(fraction) { anim.animateTo(fraction, tween(1400, easing = FastOutSlowInEasing)) }
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val sw = stroke.toPx()
            val inset = sw / 2
            val arcSize = Size(this.size.width - sw, this.size.height - sw)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(sw))
            drawArc(brush, -90f, 360f * anim.value, false, Offset(inset, inset), arcSize, style = Stroke(sw, cap = StrokeCap.Round))
        }
        content()
    }
}

/** Pequeno desenho de disco de vinil (sulcos), usado na tela de música. */
fun DrawScope.vinylGrooves(color: Color) {
    val r = size.minDimension / 2
    drawCircle(Color(0xFF1C1116), r)
    var rr = r * 0.95f
    while (rr > r * 0.42f) {
        drawCircle(color, rr, style = Stroke(1.dp.toPx()))
        rr -= 4.dp.toPx()
    }
    // brilho diagonal
    drawArc(Color.White.copy(alpha = 0.08f), 200f, 50f, true)
    drawArc(Color.White.copy(alpha = 0.08f), 20f, 50f, true)
}

