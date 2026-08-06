package com.sigmaflux.market.ui.news

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sigmaflux.market.ui.components.NewsMetaRow
import com.sigmaflux.market.ui.theme.Graphite

/**
 * Детальный экран новости.
 * Слово «Неподтверждено» показывается ТОЛЬКО здесь (или в фильтрах).
 * Красный цвет не используется — он зарезервирован для критических событий.
 */
@Composable
fun NewsDetailScreen(
    newsId: String,
    onBack: () -> Unit,
    vm: NewsDetailViewModel = viewModel()
) {
    LaunchedEffect(newsId) { vm.load(newsId) }
    val item by vm.item.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Graphite.Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = Graphite.Text)
        }

        val n = item
        if (n == null) {
            Text(
                text = "Новость не найдена",
                style = MaterialTheme.typography.bodyMedium,
                color = Graphite.Muted,
                modifier = Modifier.padding(top = 24.dp)
            )
            return@Column
        }

        Text(
            text = n.title,
            style = MaterialTheme.typography.headlineSmall,
            color = Graphite.Text
        )
        Spacer(modifier = Modifier.height(10.dp))
        NewsMetaRow(n)
        Spacer(modifier = Modifier.height(8.dp))

        if (!n.isConfirmed) {
            Text(
                text = "Неподтверждено",
                style = MaterialTheme.typography.labelMedium,
                color = Graphite.Warning,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Graphite.Warning.copy(alpha = 0.14f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Информация из непроверенного источника. Не рассматривайте её как факт.",
                style = MaterialTheme.typography.bodySmall,
                color = Graphite.Warning
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Text(
            text = n.summary,
            style = MaterialTheme.typography.bodyMedium,
            color = Graphite.Text
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (n.url != null) {
            Text(
                text = "Источник: ${n.sourceName}",
                style = MaterialTheme.typography.bodySmall,
                color = Graphite.Muted
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (n.isDemo) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Демонстрационные данные — не является индивидуальной инвестиционной рекомендацией.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Graphite.Warning
                )
            }
        }
    }
}
