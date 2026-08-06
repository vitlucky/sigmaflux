package com.sigmaflux.market.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.sigmaflux.market.data.model.Direction
import com.sigmaflux.market.data.model.Signal
import com.sigmaflux.market.ui.theme.Graphite
import com.sigmaflux.market.util.Format

/**
 * Signal Radar — детерминированные, объяснимые сигналы.
 * Никогда не показываются как рекомендации.
 */
@Composable
fun SignalRadar(signals: List<Signal>, modifier: Modifier = Modifier) {
    if (signals.isEmpty()) {
        Text(
            text = "Сигналов нет. Данные демонстрационные — оценки не являются рекомендациями.",
            style = MaterialTheme.typography.bodySmall,
            color = Graphite.Muted,
            modifier = modifier.padding(vertical = 4.dp)
        )
        return
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        signals.forEach { s ->
            SignalRow(s)
        }
    }
}

@Composable
private fun SignalRow(s: Signal) {
    val color = when (s.direction) {
        Direction.POSITIVE -> Graphite.Positive
        Direction.NEGATIVE -> Graphite.Negative
        Direction.NEUTRAL -> Graphite.Warning
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Graphite.Elevated.copy(alpha = 0.45f))
            .padding(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ColorDot(color)
                Text(
                    text = "  ${s.symbol}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Graphite.Text
                )
            }
            Text(
                text = s.title,
                style = MaterialTheme.typography.bodySmall,
                color = Graphite.Text,
                modifier = Modifier.padding(top = 2.dp)
            )
            Text(
                text = s.explanation,
                style = MaterialTheme.typography.labelSmall,
                color = Graphite.Muted,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Text(
            text = "${Math.round(s.score * 100)}",
            style = MaterialTheme.typography.titleMedium,
            color = color
        )
    }
}
