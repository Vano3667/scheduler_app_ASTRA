package ru.dayplanner.app.ui.effects

import android.animation.ValueAnimator
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.*
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ru.dayplanner.app.R
import ru.dayplanner.app.ui.theme.AppStyle
import ru.dayplanner.app.ui.theme.LocalAppStyle
import kotlin.math.*
import kotlin.random.Random

interface EffectsController { fun onTaskCompleted(allDone: Boolean) }
object NoEffects : EffectsController { override fun onTaskCompleted(allDone: Boolean) {} }
val LocalEffects = staticCompositionLocalOf<EffectsController> { NoEffects }
private data class Celebration(val key: Long, val text: String, val all: Boolean)
private data class Particle(val x: Float, val speed: Float, val sway: Float, val phase: Float)

@Composable
fun EffectsHost(style: AppStyle, content: @Composable () -> Unit) {
    val phrases = stringArrayResource(R.array.praise)
    val finale = stringResource(R.string.all_done)
    var event by remember(style) { mutableStateOf<Celebration?>(null) }
    val controller: EffectsController = remember(style, phrases.contentHashCode(), finale) {
        if (style != AppStyle.EFFECTS) NoEffects else object : EffectsController {
            override fun onTaskCompleted(allDone: Boolean) {
                if (ValueAnimator.areAnimatorsEnabled()) event = Celebration(System.nanoTime(), if (allDone) finale else phrases.random(), allDone)
            }
        }
    }
    val palette = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.tertiary)
    val glow = palette.first().copy(alpha = .08f)
    CompositionLocalProvider(LocalEffects provides controller) {
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().drawWithContent {
                drawContent()
                if (style == AppStyle.EFFECTS) drawRect(Brush.verticalGradient(listOf(glow, Color.Transparent), endY = 84.dp.toPx()), size = Size(size.width, minOf(size.height, 84.dp.toPx())))
            }) { content() }
            event?.let { e -> key(e.key) {
                val progress = remember { Animatable(0f) }
                val particles = remember { val rng = Random(e.key); List(if (e.all) 56 else 28) { Particle(rng.nextFloat(), .65f + rng.nextFloat() * .55f, rng.nextFloat() * .08f, rng.nextFloat() * 6.28f) } }
                LaunchedEffect(Unit) {
                    progress.animateTo(1f, tween(if (e.all) 1300 else 950, easing = LinearEasing)); delay(650)
                    if (event == e) event = null
                }
                Canvas(Modifier.fillMaxSize()) {
                    val t = progress.value
                    particles.forEachIndexed { i, p ->
                        val center = Offset((p.x + p.sway * sin(9f*t + p.phase))*size.width, (t*t*p.speed - .025f)*size.height)
                        drawCircle(palette[i % palette.size].copy(alpha = 1f-t), (if (e.all) 4.dp else 3.dp).toPx(), center)
                    }
                }
                // Нет интерактивной поверхности над приложением: карточка не перехватывает касания.
                Text(e.text, Modifier.align(Alignment.Center).padding(16.dp).then(Modifier.drawWithContent {
                    drawRoundRect(palette[0], cornerRadius = CornerRadius(16.dp.toPx())); drawContent()
                }).padding(16.dp), color = MaterialTheme.colorScheme.onPrimary, style = if (e.all) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineSmall)
            } }
        }
    }
}

fun Modifier.pressBurst(): Modifier = composed {
    if (LocalAppStyle.current != AppStyle.EFFECTS) return@composed this
    var origin by remember { mutableStateOf(Offset.Zero) }; var tick by remember { mutableLongStateOf(0L) }
    val progress = remember { Animatable(1f) }; val ink = MaterialTheme.colorScheme.primary
    LaunchedEffect(tick) { if (tick != 0L) { progress.snapTo(0f); progress.animateTo(1f, tween(260)) } }
    this.pointerInput(Unit) { awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        if (ValueAnimator.areAnimatorsEnabled()) { origin = down.position; tick++ }
    } }.drawWithContent {
        drawContent(); val t = progress.value
        if (t < 1f) {
            val color = ink.copy(alpha=(1f-t)*.65f); val r = (5f+17f*t).dp.toPx()
            drawCircle(color, r, origin, style=Stroke(1.5.dp.toPx()))
            repeat(6) { i -> val angle=i*Math.PI/3; drawCircle(color, 2.dp.toPx(), origin+Offset(cos(angle).toFloat(), sin(angle).toFloat())*(r+4.dp.toPx())) }
        }
    }
}
