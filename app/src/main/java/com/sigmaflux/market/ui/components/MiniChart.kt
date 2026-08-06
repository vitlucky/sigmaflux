package com.sigmaflux.market.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.sigmaflux.market.data.model.Candle
import com.sigmaflux.market.ui.theme.Graphite
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Мини-график (спарклайн) по свечам.
 * Свечи — реальные (backend) или demo-фолбэк; рядом с карточкой всегда
 * показывается источник («Свечи · demo» / «Свечи · MOEX ISS»).
 */
@Composable
fun MiniChart(
    candles: List<Candle>,
    modifier: Modifier = Modifier,
    heightDp: Int = 64
) {
    val up = candles.size >= 2 && candles.last().c >= candles.first().c
    val color = if (up) Graphite.Positive else Graphite.Negative
    Canvas(modifier = modifier.fillMaxWidth().height(heightDp.dp)) {
        if (candles.size < 2) return@Canvas
        val min = candles.minOf { it.l }
        val max = candles.maxOf { it.h }
        val range = (max - min).takeIf { it > 0 } ?: 1.0
        val n = candles.size
        val points = ArrayList<Offset>(n)
        candles.forEachIndexed { i, c ->
            val x = i / (n - 1f) * size.width
            val t = ((c.c - min) / range).toFloat()
            val y = size.height * (1f - t * 0.92f) - size.height * 0.04f
            points += Offset(x, y)
        }
        val path = Path()
        points.forEachIndexed { i, p -> if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y) }
        drawPath(path, color, style = Stroke(width = 2f))
        val fill = Path().apply {
            moveTo(points.first().x, size.height)
            points.forEach { lineTo(it.x, it.y) }
            lineTo(points.last().x, size.height)
            close()
        }
        drawPath(fill, color.copy(alpha = 0.08f))
    }
}

/** Детерминированный demo-спарклайн (fallback, когда нет ни сети, ни кэша). */
fun demoCandles(symbol: String, count: Int = 48): List<Candle> {
    val seed = symbol.hashCode()
    val now = System.currentTimeMillis()
    val stepMs = 3600_000L
    var price = 100.0 + (seed % 9900)
    val out = ArrayList<Candle>(count)
    for (i in 0 until count) {
        val wave = sin(seed * 0.001 + i * 0.7) * 0.008 + sin(seed * 0.0009 + i * 0.31) * 0.005
        val open = price
        val close = maxOf(0.01, price * (1 + wave))
        val high = maxOf(open, close) * (1 + abs(sin(i * 0.5 + seed)) * 0.006)
        val low = minOf(open, close) * (1 - abs(cos(i * 0.4 + seed)) * 0.006)
        out += Candle(now - (count - i) * stepMs, open, high, low, close, 1e6)
        price = close
    }
    return out
}
