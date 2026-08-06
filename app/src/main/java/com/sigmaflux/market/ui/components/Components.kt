package com.sigmaflux.market.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sigmaflux.market.data.model.Freshness
import com.sigmaflux.market.data.model.NewsItem
import com.sigmaflux.market.data.model.SourceTier
import com.sigmaflux.market.ui.theme.Graphite
import com.sigmaflux.market.util.Format

/** Точка-индикатор источника (тихая, маленькая). */
@Composable
fun SourceDot(tier: SourceTier, modifier: Modifier = Modifier) {
    val color = when (tier) {
        SourceTier.OFFICIAL -> Graphite.DotOfficial
        SourceTier.MEDIA -> Graphite.DotMedia
        SourceTier.TELEGRAM -> Graphite.DotTelegram
    }
    Box(
        modifier = modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(color)
    )
}

/**
 * Тихая строка метаданных новости: «ЦБ РФ · 18 мин •»
 * Без тяжёлых badges; слово «Неподтверждено» — только на detail.
 */
@Composable
fun NewsMetaRow(item: NewsItem, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = item.sourceName,
            style = MaterialTheme.typography.labelSmall,
            color = Graphite.Muted
        )
        Text(
            text = "·",
            style = MaterialTheme.typography.labelSmall,
            color = Graphite.Muted.copy(alpha = 0.6f)
        )
        Text(
            text = Format.ago(item.publishedAtEpochMs),
            style = MaterialTheme.typography.labelSmall,
            color = Graphite.Muted
        )
        SourceDot(item.sourceTier)
        if (item.isDemo) {
            Text(
                text = "demo",
                style = MaterialTheme.typography.labelSmall,
                color = Graphite.Warning.copy(alpha = 0.8f)
            )
        }
    }
}

/** Строка свежести данных: «обновлено 3 мин назад · MOEX ISS» / «stale» / rate limit. */
@Composable
fun FreshnessLabel(freshness: Freshness?, modifier: Modifier = Modifier) {
    if (freshness == null) return
    val (text, color) = when {
        freshness.rateLimited -> "Источник временно ограничил запросы. Показываем сохранённые данные." to Graphite.Warning
        freshness.isDemo -> "Демо-данные · не real-time" to Graphite.Warning
        freshness.stale -> "Данные устарели (${freshness.updatedAgoMinutes} мин) · ${freshness.sourceLabel}" to Graphite.Warning
        else -> "Обновлено ${freshness.updatedAgoMinutes} мин назад · ${freshness.sourceLabel}" to Graphite.Muted
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier
    )
}

/** Карточка-поверхность Graphite. */
@Composable
fun GraphiteCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Graphite.Surface,
        tonalElevation = 0.dp
    ) {
        Box(modifier = Modifier.padding(14.dp)) { content() }
    }
}

/** Скелетон-заглушка (loading). */
@Composable
fun SkeletonBlock(modifier: Modifier = Modifier, heightDp: Int = 64) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(heightDp.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Graphite.Elevated.copy(alpha = 0.55f))
    )
}

/** Заголовок секции с опциональным экшеном. */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = Graphite.Text,
            modifier = Modifier.weight(1f)
        )
        trailing?.invoke()
    }
}

@Composable
fun DemoBadge(modifier: Modifier = Modifier) {
    Text(
        text = "SIMULATION",
        style = MaterialTheme.typography.labelSmall,
        color = Graphite.Warning,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Graphite.Warning.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
fun ColorDot(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
fun HorizontalSpacer(w: Int = 8) = Spacer(modifier = Modifier.width(w.dp))
@Composable
fun VerticalSpacer(h: Int = 8) = Spacer(modifier = Modifier.height(h.dp))
