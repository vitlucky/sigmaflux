package com.sigmaflux.market.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sigmaflux.market.data.model.NewsItem
import com.sigmaflux.market.data.model.Quote
import com.sigmaflux.market.ui.BottomBarBehavior
import com.sigmaflux.market.ui.components.FreshnessLabel
import com.sigmaflux.market.ui.components.GraphiteCard
import com.sigmaflux.market.ui.components.InstrumentPickerDialog
import com.sigmaflux.market.ui.components.MarketStrip
import com.sigmaflux.market.ui.components.NewsMetaRow
import com.sigmaflux.market.ui.components.QuoteRow
import com.sigmaflux.market.ui.components.SectionHeader
import com.sigmaflux.market.ui.components.SignalRadar
import com.sigmaflux.market.ui.components.SkeletonBlock
import com.sigmaflux.market.ui.theme.Graphite
import com.sigmaflux.market.util.Format

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    behavior: BottomBarBehavior,
    onOpenInstrument: (String) -> Unit,
    onOpenNews: (String) -> Unit,
    onShowAllNews: () -> Unit,
    vm: HomeViewModel = viewModel()
) {
    // onOpenNews(id): открывает news detail (route "news/{id}")
    val symbols by vm.watchlistSymbols.collectAsState()
    val quotes by vm.quotes.collectAsState()
    val freshness by vm.freshness.collectAsState()
    val loading by vm.loading.collectAsState()
    val news by vm.news.collectAsState()
    val signals by vm.signals.collectAsState()

    var editing by remember { mutableStateOf(false) }
    var showPicker by remember { mutableStateOf(false) }

    val bySymbol = quotes.associateBy { it.symbol }

    PullToRefreshBox(
        isRefreshing = loading,
        onRefresh = { vm.refresh() },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(behavior.connection),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            item {
                GreetingBlock(
                    loading = loading,
                    freshnessLabel = { FreshnessLabel(freshness) },
                    onRefresh = { vm.refresh() }
                )
            }
            item {
                MarketStrip(
                    quotes = quotes,
                    selectedSymbols = symbols,
                    editing = editing,
                    onToggleEdit = { editing = !editing },
                    onRemove = { vm.removeFromWatchlist(it) },
                    onAddClick = { showPicker = true },
                    onOpen = onOpenInstrument
                )
            }
            item {
                SectionHeader("Market Pulse")
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    symbols.take(5).forEach { sym ->
                        val q = bySymbol[sym]
                        if (q != null) PulseRow(q)
                    }
                }
            }
            item {
                SectionHeader("Главные новости", trailing = {
                    Text(
                        text = "Все",
                        style = MaterialTheme.typography.labelMedium,
                        color = Graphite.Accent,
                        modifier = Modifier
                            .clickable { onShowAllNews() }
                            .padding(4.dp)
                    )
                })
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    news.take(3).forEach { n -> NewsCard(n, onClick = { onOpenNews(n.id) }) }
                    if (news.isEmpty() && loading) {
                        repeat(3) { SkeletonBlock(heightDp = 84) }
                    }
                }
            }
            item { SectionHeader("Watchlist") }
            items(symbols) { sym ->
                val q = bySymbol[sym]
                if (q != null) QuoteRow(q, onClick = { onOpenInstrument(sym) })
            }
            item {
                SectionHeader("Signal Radar")
                Spacer(modifier = Modifier.height(4.dp))
                SignalRadar(signals)
            }
            item {
                Text(
                    text = "Данные демонстрационные. Не является индивидуальной инвестиционной рекомендацией.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Graphite.Muted,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }

    if (showPicker) {
        InstrumentPickerDialog(
            title = "Добавить инструмент",
            onDismiss = { showPicker = false },
            onPick = { vm.addToWatchlist(it.symbol); showPicker = false },
            isPicked = { it.symbol in symbols }
        )
    }
}

@Composable
private fun GreetingBlock(
    loading: Boolean,
    freshnessLabel: @Composable () -> Unit,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "SigmaFlux",
                style = MaterialTheme.typography.headlineSmall,
                color = Graphite.Text
            )
            Spacer(modifier = Modifier.height(2.dp))
            freshnessLabel()
        }
        IconButton(onClick = onRefresh, enabled = !loading) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = "Обновить",
                tint = Graphite.Accent
            )
        }
    }
}

@Composable
private fun PulseRow(q: Quote) {
    GraphiteCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(q.symbol, style = MaterialTheme.typography.titleMedium, color = Graphite.Text)
                Text(
                    text = "День ${Format.pct(q.changePct)} · Объём ${Format.compact(q.volume)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Graphite.Muted
                )
            }
            Text(
                text = Format.price(q),
                style = MaterialTheme.typography.titleLarge,
                color = if (q.isUp) Graphite.Positive else Graphite.Negative
            )
        }
    }
}

@Composable
fun NewsCard(item: NewsItem, onClick: () -> Unit) {
    GraphiteCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                color = Graphite.Text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            NewsMetaRow(item)
        }
    }
}
