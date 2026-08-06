package com.sigmaflux.market.ui.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sigmaflux.market.data.Graph
import com.sigmaflux.market.data.model.NewsItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class NewsDetailViewModel : ViewModel() {

    private val repo = Graph.news

    private val _item = MutableStateFlow<NewsItem?>(null)
    val item: StateFlow<NewsItem?> = _item

    fun load(id: String) {
        viewModelScope.launch {
            _item.value = repo.cachedOnce().firstOrNull { it.id == id }
            if (_item.value == null) {
                _item.value = repo.refresh(limit = 30).getOrNull()?.firstOrNull { it.id == id }
            }
        }
    }
}
