package com.sigmaflux.market.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sigmaflux.market.data.model.Quote
import com.sigmaflux.market.ui.theme.Graphite
import com.sigmaflux.market.util.Format

/** Строка котировки для списков (Рынок, Watchlist). */
@Composable
fun QuoteRow(quote: Quote, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(quote.symbol, style = MaterialTheme.typography.titleMedium, color = Graphite.Text)
            Text(
                quote.name,
                style = MaterialTheme.typography.labelSmall,
                color = Graphite.Muted,
                maxLines = 1
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = Format.price(quote),
                style = MaterialTheme.typography.titleMedium,
                color = Graphite.Text
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = Format.pct(quote.changePct),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (quote.isUp) Graphite.Positive else Graphite.Negative
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (quote.isDemo) {
                    Text(
                        text = "demo",
                        style = MaterialTheme.typography.labelSmall,
                        color = Graphite.Warning.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}
