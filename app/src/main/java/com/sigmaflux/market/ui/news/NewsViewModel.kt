package com.sigmaflux.market.ui.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sigmaflux.market.data.Graph
import com.sigmaflux.market.data.model.NewsItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class NewsViewModel : ViewModel() {

    private val repo = Graph.news

    private val _items = MutableStateFlow<List<NewsItem>>(emptyList())
    val items: StateFlow<List<NewsItem>> = _items

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    init {
        viewModelScope.launch {
            _items.value = repo.cachedOnce()
            refresh()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            _items.value = repo.refresh(limit = 30).getOrElse { _items.value }
            _loading.value = false
        }
    }
}
