package com.sigmaflux.market.ui.portfolio

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sigmaflux.market.data.model.OrderSide
import com.sigmaflux.market.data.model.Portfolio
import com.sigmaflux.market.data.portfolio.PortfolioRepository
import com.sigmaflux.market.ui.BottomBarBehavior
import com.sigmaflux.market.ui.components.DemoBadge
import com.sigmaflux.market.ui.components.GraphiteCard
import com.sigmaflux.market.ui.theme.Graphite
import com.sigmaflux.market.util.Format

@Composable
fun PortfolioScreen(
    behavior: BottomBarBehavior,
    onOpenInstrument: (String) -> Unit,
    vm: PortfolioViewModel = viewModel()
) {
    val portfolio by vm.portfolio.collectAsState()
    val equity by vm.equity.collectAsState()
    val unrealized by vm.unrealized.collectAsState()

    if (portfolio == null) {
        CreatePortfolioForm(onCreate = { name, cur, bal -> vm.create(name, cur, bal) })
        return
    }

    val p = portfolio ?: return
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { PortfolioHeader(p, equity, unrealized, onDelete = { vm.delete() }) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Позиции",
                    style = MaterialTheme.typography.titleMedium,
                    color = Graphite.Text,
                    modifier = Modifier.weight(1f)
                )
                DemoBadge()
            }
        }
        items(p.positions, key = { it.symbol }) { pos ->
            PositionRow(pos, p.currency, onOpenInstrument)
        }
        if (p.positions.isEmpty()) {
            item {
                Text(
                    text = "Позиций нет. Купите инструмент через «Рынок» или Market Strip.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Graphite.Muted
                )
            }
        }
        item {
            Text("История сделок", style = MaterialTheme.typography.titleMedium, color = Graphite.Text)
        }
        items(p.orders.sortedByDescending { it.tsEpochMs }.take(20), key = { it.id }) { order ->
            OrderRow(order, p.currency)
        }
    }
}

@Composable
private fun PortfolioHeader(p: Portfolio, equity: Double, unrealized: Double, onDelete: () -> Unit) {
    GraphiteCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(p.name, style = MaterialTheme.typography.titleLarge, color = Graphite.Text, modifier = Modifier.weight(1f))
                DemoBadge()
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Стоимость: ${Format.money(equity, p.currency)}",
                style = MaterialTheme.typography.titleMedium,
                color = Graphite.Text
            )
            Text(
                text = "Нереализованный P&L: ${Format.signedMoney(unrealized, p.currency)}",
                style = MaterialTheme.typography.bodyMedium,
                color = if (unrealized >= 0) Graphite.Positive else Graphite.Negative
            )
            Text(
                text = "Свободно: ${Format.money(p.cash, p.currency)} · Старт: ${Format.money(p.startBalance, p.currency)}",
                style = MaterialTheme.typography.labelSmall,
                color = Graphite.Muted
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Симуляция. Операции не исполняются на бирже.",
                style = MaterialTheme.typography.labelSmall,
                color = Graphite.Warning
            )
            TextButton(onClick = onDelete) {
                Text("Удалить портфель", color = Graphite.Negative)
            }
        }
    }
}

@Composable
private fun PositionRow(pos: com.sigmaflux.market.data.model.Position, currency: String, onOpenInstrument: (String) -> Unit) {
    GraphiteCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenInstrument(pos.symbol) }
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(pos.symbol, style = MaterialTheme.typography.titleMedium, color = Graphite.Text)
                    Text(
                        "${pos.qty} шт · ср. ${Format.price(pos.avgPrice)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Graphite.Muted
                    )
                }
                Text(
                    text = "P&L ${Format.signedMoney(pos.realizedPnl, currency)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (pos.realizedPnl >= 0) Graphite.Positive else Graphite.Negative
                )
            }
        }
    }
}

@Composable
private fun OrderRow(order: com.sigmaflux.market.data.model.Order, currency: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${order.side.name} ${order.qty} ${order.symbol} · ${Format.price(order.price)}",
                style = MaterialTheme.typography.bodySmall,
                color = Graphite.Text
            )
            Text(
                "комиссия ${Format.price(order.commission)} · ${order.status.name.lowercase()}",
                style = MaterialTheme.typography.labelSmall,
                color = Graphite.Muted
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreatePortfolioForm(onCreate: (String, String, Double) -> Unit) {
    var name by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("RUB") }
    var balance by remember { mutableStateOf("100000") }
    var menuOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Демо-портфель", style = MaterialTheme.typography.headlineSmall, color = Graphite.Text)
        Text(
            "Создайте виртуальный портфель: валюта, стартовый баланс, название. Все операции — симуляция.",
            style = MaterialTheme.typography.bodyMedium,
            color = Graphite.Muted
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Название") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        ExposedDropdownMenuBox(
            expanded = menuOpen,
            onExpandedChange = { menuOpen = it }
        ) {
            OutlinedTextField(
                value = currency,
                onValueChange = {},
                readOnly = true,
                label = { Text("Валюта") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuOpen) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                PortfolioRepository.SUPPORTED_CURRENCIES.forEach { c ->
                    DropdownMenuItem(text = { Text(c) }, onClick = { currency = c; menuOpen = false })
                }
            }
        }
        OutlinedTextField(
            value = balance,
            onValueChange = { balance = it.filter { ch -> ch.isDigit() } },
            label = { Text("Стартовый баланс") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { onCreate(name.ifBlank { "Мой портфель" }, currency, balance.toDoubleOrNull() ?: 0.0) },
            enabled = balance.toDoubleOrNull()?.let { it > 0 } == true,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Создать портфель")
        }
    }
}
