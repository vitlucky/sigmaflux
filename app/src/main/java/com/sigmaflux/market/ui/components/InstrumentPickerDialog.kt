package com.sigmaflux.market.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sigmaflux.market.data.instrument.InstrumentCatalog
import com.sigmaflux.market.data.model.Instrument
import com.sigmaflux.market.ui.theme.Graphite

/**
 * Поисковый диалог выбора инструмента (immediate task #2).
 * Поиск по символу/названию; добавление в watchlist/strip.
 */
@Composable
fun InstrumentPickerDialog(
    title: String,
    onDismiss: () -> Unit,
    onPick: (Instrument) -> Unit,
    isPicked: (Instrument) -> Boolean
) {
    var query by remember { mutableStateOf("") }
    val results = remember(query) { InstrumentCatalog.search(query) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = Graphite.Text) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("IMOEX, SBER, BTC…", color = Graphite.Muted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                LazyColumn(modifier = Modifier.height(320.dp).padding(top = 8.dp)) {
                    items(results, key = { it.symbol }) { instr ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${instr.symbol} · ${instr.name}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Graphite.Text
                                )
                                Text(
                                    text = "${instr.exchange} · ${InstrumentCatalog.kindLabel(instr.kind)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Graphite.Muted
                                )
                            }
                            TextButton(
                                onClick = { onPick(instr) },
                                enabled = !isPicked(instr)
                            ) {
                                Text(if (isPicked(instr)) "Добавлен" else "Добавить", color = Graphite.Accent)
                            }
                        }
                    }
                    if (results.isEmpty()) {
                        item {
                            Text(
                                text = "Ничего не найдено",
                                style = MaterialTheme.typography.bodySmall,
                                color = Graphite.Muted,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Закрыть", color = Graphite.Accent) }
        }
    )
}
