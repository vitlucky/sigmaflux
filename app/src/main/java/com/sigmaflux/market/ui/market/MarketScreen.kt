package com.sigmaflux.market.ui.market

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sigmaflux.market.data.instrument.InstrumentCatalog
import com.sigmaflux.market.ui.BottomBarBehavior
import com.sigmaflux.market.ui.components.QuoteRow
import com.sigmaflux.market.ui.theme.Graphite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketScreen(
    behavior: BottomBarBehavior,
    onOpenInstrument: (String) -> Unit,
    vm: MarketViewModel = viewModel()
) {
    val instruments by vm.instruments.collectAsState()
    val quotes by vm.quotes.collectAsState()
    val search by vm.search.collectAsState()
    val quotesBySymbol = quotes.associateBy { it.symbol }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Рынок",
            style = MaterialTheme.typography.headlineSmall,
            color = Graphite.Text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        OutlinedTextField(
            value = search,
            onValueChange = { vm.onSearchChange(it) },
            placeholder = { Text("Поиск: IMOEX, Сбербанк, BTC…", color = Graphite.Muted) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Graphite.Muted) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 8.dp, bottom = 96.dp)
        ) {
            items(instruments, key = { it.symbol }) { instr ->
                val q = quotesBySymbol[instr.symbol]
                if (q != null) {
                    QuoteRow(q, onClick = { onOpenInstrument(instr.symbol) })
                } else {
                    Row(modifier = Modifier.padding(vertical = 10.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(instr.symbol, style = MaterialTheme.typography.titleMedium, color = Graphite.Text)
                            Text(
                                "${instr.name} · ${InstrumentCatalog.kindLabel(instr.kind)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Graphite.Muted
                            )
                        }
                    }
                }
            }
        }
    }
}
