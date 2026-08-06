package com.sigmaflux.market.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sigmaflux.market.data.Graph
import com.sigmaflux.market.data.model.Freshness
import com.sigmaflux.market.data.model.NewsItem
import com.sigmaflux.market.data.model.Quote
import com.sigmaflux.market.data.model.Signal
import com.sigmaflux.market.data.quote.RefreshResult
import com.sigmaflux.market.data.signal.SignalEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val watchlistRepo = Graph.watchlist
    private val quoteRepo = Graph.quotes
    private val newsRepo = Graph.news
    private val alertRepo = Graph.alerts

    val watchlistSymbols: StateFlow<List<String>> = watchlistRepo.symbols
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _quotes = MutableStateFlow<List<Quote>>(emptyList())
    val quotes: StateFlow<List<Quote>> = _quotes

    private val _freshness = MutableStateFlow<Freshness?>(null)
    val freshness: StateFlow<Freshness?> = _freshness

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    private val _news = MutableStateFlow<List<NewsItem>>(emptyList())
    val news: StateFlow<List<NewsItem>> = _news

    val signals: StateFlow<List<Signal>> = _quotes.map { SignalEngine.compute(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            // мгновенный offline-cache
            _quotes.value = quoteRepo.cachedQuotesOnce()
            _news.value = newsRepo.cachedOnce()
            refresh()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            val symbols = watchlistRepo.currentSymbols()
            when (val res = quoteRepo.refresh(symbols)) {
                is RefreshResult.Success -> { _quotes.value = res.quotes; _freshness.value = res.freshness }
                is RefreshResult.UsingCache -> { _quotes.value = res.quotes; _freshness.value = res.freshness }
                is RefreshResult.Demo -> { _quotes.value = res.quotes; _freshness.value = res.freshness }
            }
            _news.value = newsRepo.refresh(limit = 3).getOrElse { _news.value }
            // smart alerts: детерминированная проверка по свежим котировкам
            alertRepo.evaluate(_quotes.value)
            _loading.value = false
        }
    }

    fun addToWatchlist(symbol: String) {
        viewModelScope.launch { watchlistRepo.add(symbol); refresh() }
    }

    fun removeFromWatchlist(symbol: String) {
        viewModelScope.launch { watchlistRepo.remove(symbol); refresh() }
    }
}
