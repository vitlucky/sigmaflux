package com.sigmaflux.market.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sigmaflux.market.data.model.Quote
import com.sigmaflux.market.ui.theme.Graphite
import com.sigmaflux.market.util.Format

/**
 * Горизонтальный Market Strip — пользователь управляет составом
 * (добавление/удаление, сортировка позже). Показывает цену и изменение.
 */
@Composable
fun MarketStrip(
    quotes: List<Quote>,
    selectedSymbols: List<String>,
    editing: Boolean,
    onToggleEdit: () -> Unit,
    onRemove: (String) -> Unit,
    onAddClick: () -> Unit,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Рынок сейчас",
                style = MaterialTheme.typography.titleMedium,
                color = Graphite.Text,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (editing) "Готово" else "Изменить",
                style = MaterialTheme.typography.labelMedium,
                color = Graphite.Accent,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onToggleEdit() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val bySymbol = quotes.associateBy { it.symbol }
            selectedSymbols.forEach { sym ->
                val q = bySymbol[sym] ?: return@forEach
                StripChip(
                    quote = q,
                    editing = editing,
                    onRemove = { onRemove(sym) },
                    onClick = { onOpen(sym) }
                )
            }
            if (editing) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Graphite.Elevated.copy(alpha = 0.6f),
                    modifier = Modifier
                        .width(64.dp)
                        .height(72.dp)
                        .clickable { onAddClick() }
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = "Добавить", tint = Graphite.Accent)
                        Text("Добавить", style = MaterialTheme.typography.labelSmall, color = Graphite.Muted)
                    }
                }
            }
        }
    }
}

@Composable
private fun StripChip(
    quote: Quote,
    editing: Boolean,
    onRemove: () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Graphite.Surface,
        modifier = Modifier
            .width(if (editing) 108.dp else 128.dp)
            .height(72.dp)
            .clickable(enabled = !editing) { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = quote.symbol,
                    style = MaterialTheme.typography.labelMedium,
                    color = Graphite.Muted,
                    modifier = Modifier.weight(1f)
                )
                if (editing) {
                    IconButton(onClick = onRemove, modifier = Modifier.height(20.dp).width(20.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Удалить ${quote.symbol}",
                            tint = Graphite.Negative,
                            modifier = Modifier.height(14.dp).width(14.dp)
                        )
                    }
                }
            }
            Text(
                text = Format.price(quote),
                style = MaterialTheme.typography.titleMedium,
                color = Graphite.Text
            )
            Text(
                text = Format.pct(quote.changePct),
                style = MaterialTheme.typography.labelSmall,
                color = if (quote.isUp) Graphite.Positive else Graphite.Negative
            )
        }
    }
}
