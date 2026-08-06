package com.sigmaflux.market.ui.more

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.sigmaflux.market.BuildConfig
import com.sigmaflux.market.ui.alerts.AlertsViewModel
import com.sigmaflux.market.data.model.Alert
import com.sigmaflux.market.data.model.AlertType
import com.sigmaflux.market.ui.BottomBarBehavior
import com.sigmaflux.market.ui.components.GraphiteCard
import com.sigmaflux.market.ui.components.InstrumentPickerDialog
import com.sigmaflux.market.ui.theme.Graphite
import com.sigmaflux.market.util.Format

/** «Ещё»: алерты, статус AI, о приложении. */
@Composable
fun MoreScreen(
    behavior: BottomBarBehavior,
    onOpenInstrument: (String) -> Unit,
    vm: AlertsViewModel = viewModel()
) {
    val alerts by vm.alerts.collectAsState()
    var showCreate by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Ещё",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Graphite.Text,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { showCreate = true }) {
                    Text("+ Алерт", color = Graphite.Accent)
                }
            }
        }
        item {
            SectionTitle("Smart-алерты")
        }
        items(alerts, key = { it.id }) { alert -> AlertRow(alert, onDelete = { vm.delete(alert.id) }) }
        if (alerts.isEmpty()) {
            item {
                Text(
                    "Алертов нет. Создайте: цена выше/ниже уровня, падение/рост за день.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Graphite.Muted
                )
            }
        }
        item { SectionTitle("AI") }
        item {
            GraphiteCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        text = "AI_ENABLED = ${BuildConfig.AI_ENABLED}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Graphite.Text
                    )
                    Text(
                        text = "В MVP AI выключен: сигналы считаются детерминированным кодом. Ключи API никогда не хранятся в приложении.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Graphite.Muted
                    )
                }
            }
        }
        item { SectionTitle("О приложении") }
        item {
            Text(
                text = "SigmaFlux v0.1.0-alpha · аналитический терминал (MOEX + крипто).\nНе является индивидуальной инвестиционной рекомендацией. Данные в прототипе — демонстрационные.",
                style = MaterialTheme.typography.labelSmall,
                color = Graphite.Muted
            )
        }
    }

    if (showCreate) {
        AlertCreateDialog(
            onDismiss = { showCreate = false },
            onCreate = { type, symbol, threshold, label, isSmart ->
                vm.add(type, symbol, threshold, label, isSmart)
                showCreate = false
            },
            defaultPriceFor = { vm.priceFor(it) }
        )
    }
}

@Composable
private fun SectionTitle(t: String) {
    Text(t, style = MaterialTheme.typography.titleMedium, color = Graphite.Text)
}

@Composable
private fun TypeButton(t: AlertType, label: String, selected: AlertType, onSelect: (AlertType) -> Unit) {
    TextButton(
        onClick = { onSelect(t) },
        modifier = Modifier.weight(1f)
    ) {
        Text(
            label,
            color = if (selected == t) Graphite.Accent else Graphite.Muted,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun AlertRow(alert: Alert, onDelete: () -> Unit) {
    GraphiteCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${alert.symbol} · ${alert.label}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Graphite.Text,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (alert.isSmart) {
                        Text(
                            " · Smart",
                            style = MaterialTheme.typography.labelSmall,
                            color = Graphite.Accent,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
                Text(
                    text = if (alert.firedAtEpochMs != null) {
                        "Сработал ${Format.ago(alert.firedAtEpochMs)}"
                    } else if (alert.active) (if (alert.isSmart) "Smart · Активен" else "Активен") else "Неактивен",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (alert.firedAtEpochMs != null) Graphite.Warning else Graphite.Muted
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = Graphite.Negative)
            }
        }
    }
}

@Composable
private fun AlertCreateDialog(
    onDismiss: () -> Unit,
    onCreate: (AlertType, String, Double, String, Boolean) -> Unit,
    defaultPriceFor: (String) -> Double?
) {
    var type by remember { mutableStateOf(AlertType.PRICE_ABOVE) }
    var symbol by remember { mutableStateOf("IMOEX") }
    var threshold by remember { mutableStateOf("") }
    var isSmart by remember { mutableStateOf(false) }
    var showPicker by remember { mutableStateOf(false) }

    val priceHint = defaultPriceFor(symbol)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый алерт", color = Graphite.Text) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            AlertType.PRICE_ABOVE to "Выше цены",
                            AlertType.PRICE_BELOW to "Ниже цены",
                            AlertType.DROP_PCT_DAY to "Падение дня"
                        ).forEach { (t, label) -> TypeButton(t, label, type) { type = t } }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            AlertType.RISE_PCT_DAY to "Рост дня",
                            AlertType.DROP_PCT_15M to "Падение 15м"
                        ).forEach { (t, label) -> TypeButton(t, label, type) { type = t } }
                    }
                }
                OutlinedTextField(
                    value = symbol,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Инструмент") },
                    trailingIcon = { TextButton(onClick = { showPicker = true }) { Text("…", color = Graphite.Accent) } },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = threshold,
                    onValueChange = { threshold = it.replace(',', '.') },
                    label = {
                        Text(
                            if (type == AlertType.PRICE_ABOVE || type == AlertType.PRICE_BELOW) "Уровень цены"
                            else "Порог, %"
                        )
                    },
                    placeholder = {
                        Text(
                            when {
                                type == AlertType.PRICE_ABOVE || type == AlertType.PRICE_BELOW ->
                                    priceHint?.let { Format.price(it) } ?: "3100.0"
                                type == AlertType.DROP_PCT_DAY || type == AlertType.RISE_PCT_DAY -> "3.0"
                                else -> "1.5"
                            },
                            color = Graphite.Muted
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Smart-режим", style = MaterialTheme.typography.bodySmall, color = Graphite.Text)
                        Text(
                            "ATR/волатильность/объём/торговое время. Demo и неподтверждённые новости не триггерят.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Graphite.Muted
                        )
                    }
                    Switch(
                        checked = isSmart,
                        onCheckedChange = { isSmart = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Graphite.Accent)
                    )
                }
                Text(
                    "Детерминированная проверка по обновлениям котировок. Smart = доп. фильтры. Не является торговой рекомендацией.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Graphite.Muted
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = threshold.toDoubleOrNull() != null,
                onClick = {
                    val t = threshold.toDoubleOrNull() ?: 0.0
                    val baseLabel = when (type) {
                        AlertType.PRICE_ABOVE -> "Цена ≥ ${Format.price(t)}"
                        AlertType.PRICE_BELOW -> "Цена ≤ ${Format.price(t)}"
                        AlertType.DROP_PCT_DAY -> "Падение ≥ ${Format.pct(-t)}"
                        AlertType.RISE_PCT_DAY -> "Рост ≥ ${Format.pct(t)}"
                        AlertType.DROP_PCT_15M -> "Падение за 15 мин ≥ ${Format.pct(-t)}"
                    }
                    val label = if (isSmart) "$baseLabel · Smart" else baseLabel
                    onCreate(type, symbol, t, label, isSmart)
                }
            ) { Text("Создать", color = Graphite.Accent) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена", color = Graphite.Muted) } }
    )

    if (showPicker) {
        InstrumentPickerDialog(
            title = "Инструмент для алерта",
            onDismiss = { showPicker = false },
            onPick = { symbol = it.symbol; showPicker = false },
            isPicked = { false }
        )
    }
}
