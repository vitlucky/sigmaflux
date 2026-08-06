package com.sigmaflux.market.ui.news

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import com.sigmaflux.market.data.model.SourceTier
import com.sigmaflux.market.ui.BottomBarBehavior
import com.sigmaflux.market.ui.components.GraphiteCard
import com.sigmaflux.market.ui.components.NewsMetaRow
import com.sigmaflux.market.ui.components.SkeletonBlock
import com.sigmaflux.market.ui.theme.Graphite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsScreen(
    behavior: BottomBarBehavior,
    onOpenNews: (String) -> Unit,
    vm: NewsViewModel = viewModel()
) {
    val items by vm.items.collectAsState()
    val loading by vm.loading.collectAsState()
    var filter by remember { mutableStateOf<SourceTier?>(null) }
    val visible = if (filter == null) items else items.filter { it.sourceTier == filter }

    PullToRefreshBox(
        isRefreshing = loading,
        onRefresh = { vm.refresh() },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Новости",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Graphite.Text,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { vm.refresh() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Обновить", tint = Graphite.Accent)
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filter == null,
                    onClick = { filter = null },
                    label = { Text("Все") }
                )
                FilterChip(
                    selected = filter == SourceTier.OFFICIAL,
                    onClick = { filter = SourceTier.OFFICIAL },
                    label = { Text("Официальные") }
                )
                FilterChip(
                    selected = filter == SourceTier.MEDIA,
                    onClick = { filter = SourceTier.MEDIA },
                    label = { Text("СМИ") }
                )
                FilterChip(
                    selected = filter == SourceTier.TELEGRAM,
                    onClick = { filter = SourceTier.TELEGRAM },
                    label = { Text("Telegram") }
                )
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(behavior.connection),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(visible, key = { it.id }) { item -> NewsListItem(item, onClick = { onOpenNews(item.id) }) }
                if (visible.isEmpty() && loading) {
                    items(6) { SkeletonBlock(heightDp = 92) }
                }
                if (visible.isEmpty() && !loading) {
                    item {
                        Text(
                            text = "Новостей нет",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Graphite.Muted,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NewsListItem(item: NewsItem, onClick: () -> Unit) {
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
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            if (item.summary.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = Graphite.Muted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            NewsMetaRow(item)
        }
    }
}
