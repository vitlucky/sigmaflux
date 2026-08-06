package com.sigmaflux.market.ui.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sigmaflux.market.R
import com.sigmaflux.market.data.instrument.InstrumentCatalog
import com.sigmaflux.market.data.model.Quote
import com.sigmaflux.market.ui.components.GraphiteCard
import com.sigmaflux.market.ui.components.MiniChart
import com.sigmaflux.market.ui.theme.Graphite
import com.sigmaflux.market.util.Format

/**
 * Детальная карточка актива: котировка, мини-график, статистика дня,
 * добавление в watchlist, кнопка создания алерта.
 * На графике — immersive (bottom bar скрывается).
 */
@Composable
fun AssetDetailScreen(
    symbol: String,
    onBack: () -> Unit,
    onCreateAlert: (String) -> Unit,
    vm: AssetDetailViewModel = viewModel()
) {
    LaunchedEffect(symbol) { vm.load(symbol) }
    val quote by vm.quote.collectAsState()
    val inWatchlist by vm.inWatchlist.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Graphite.Background)
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = Graphite.Text)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(symbol, style = MaterialTheme.typography.titleLarge, color = Graphite.Text)
                val instr = InstrumentCatalog.bySymbol(symbol)
                if (instr != null) {
                    Text(
                        "${instr.name} · ${InstrumentCatalog.kindLabel(instr.kind)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Graphite.Muted
                    )
                }
            }
            IconButton(onClick = { vm.toggleWatchlist(symbol) }) {
                Icon(
                    if (inWatchlist) Icons.Default.Check else Icons.Default.Add,
                    contentDescription = "В watchlist",
                    tint = if (inWatchlist) Graphite.Positive else Graphite.Accent
                )
            }
        }

        val q = quote
        if (q != null) {
            QuoteHero(q)
            Spacer(modifier = Modifier.height(16.dp))
            GraphiteCard(modifier = Modifier.fillMaxWidth()) {
                Box {
                    // Graphite Premium фон графика (графитовый, диагональный, с едва видимой сеткой)
                    Image(
                        painter = painterResource(R.drawable.graphite_material_bg),
                        contentDescription = null,
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.Crop
                    )
                    Column {
                        Text(
                            text = "Мини-график · demo",
                            style = MaterialTheme.typography.labelSmall,
                            color = Graphite.Muted
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        MiniChart(quote = q, heightDp = 120)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            StatGrid(q)
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = { onCreateAlert(symbol) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Создать алерт на $symbol")
            }
        } else {
            Text(
                "Загружаем…",
                style = MaterialTheme.typography.bodyMedium,
                color = Graphite.Muted,
                modifier = Modifier.padding(top = 32.dp)
            )
        }
    }
}

@Composable
private fun QuoteHero(q: Quote) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = Format.price(q),
                style = MaterialTheme.typography.headlineSmall,
                color = Graphite.Text
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = Format.pct(q.changePct),
                style = MaterialTheme.typography.titleMedium,
                color = if (q.isUp) Graphite.Positive else Graphite.Negative
            )
        }
        Text(
            text = if (q.isDemo) "Демо-данные · не real-time · ${q.provider}"
            else "Источник: ${q.provider} · обновлено ${Format.ago(q.updatedAtEpochMs)}",
            style = MaterialTheme.typography.labelSmall,
            color = Graphite.Warning
        )
    }
}

@Composable
private fun StatGrid(q: Quote) {
    val stats = listOf(
        "Открытие" to Format.price(q.dayOpen),
        "Максимум" to Format.price(q.dayHigh),
        "Минимум" to Format.price(q.dayLow),
        "Объём" to Format.compact(q.volume),
        "Пред. закрытие" to Format.price(q.prevClose),
        "Валюта" to q.currency
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        stats.chunked(2).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEach { (label, value) ->
                    GraphiteCard(modifier = Modifier.weight(1f)) {
                        Column {
                            Text(label, style = MaterialTheme.typography.labelSmall, color = Graphite.Muted)
                            Text(value, style = MaterialTheme.typography.bodyMedium, color = Graphite.Text)
                        }
                    }
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}
