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
import com.sigmaflux.market.data.model.Quote
import com.sigmaflux.market.ui.theme.Graphite
import kotlin.math.sin

/**
 * Мини-график (спарклайн). В MVP — детерминированная серия на основе
 * символа и текущего времени; помечается как demo рядом с карточкой.
 */
@Composable
fun MiniChart(quote: Quote, modifier: Modifier = Modifier, heightDp: Int = 64) {
    val up = quote.isUp
    Canvas(modifier = modifier.fillMaxWidth().height(heightDp.dp)) {
        val n = 48
        val points = ArrayList<Offset>(n)
        val t = System.currentTimeMillis() / 5000.0
        val seed = quote.symbol.hashCode()
        var v = 0.0
        for (i in 0 until n) {
            v += sin(seed + i * 0.55 + t * 0.35) * 0.018 + sin(seed * 0.7 + i * 0.21 - t * 0.17) * 0.008
            val x = i / (n - 1f) * size.width
            val y = size.height / 2f + (v * size.height * 0.9f).toFloat()
            points += Offset(x, y)
        }
        val path = Path()
        points.forEachIndexed { i, p -> if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y) }
        drawPath(
            path = path,
            color = if (up) Graphite.Positive else Graphite.Negative,
            style = Stroke(width = 2f)
        )
        // заливка под линией
        val fill = Path().apply {
            moveTo(points.first().x, size.height)
            points.forEach { lineTo(it.x, it.y) }
            lineTo(points.last().x, size.height)
            close()
        }
        drawPath(
            path = fill,
            color = (if (up) Graphite.Positive else Graphite.Negative).copy(alpha = 0.08f)
        )
    }
}
