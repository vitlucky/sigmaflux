package com.sigmaflux.market.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sigmaflux.market.data.Graph
import com.sigmaflux.market.data.model.Quote
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AssetDetailViewModel : ViewModel() {

    private val quoteRepo = Graph.quotes
    private val watchlistRepo = Graph.watchlist

    private val _quote = MutableStateFlow<Quote?>(null)
    val quote: StateFlow<Quote?> = _quote

    private val _inWatchlist = MutableStateFlow(false)
    val inWatchlist: StateFlow<Boolean> = _inWatchlist

    fun load(symbol: String) {
        viewModelScope.launch {
            _quote.value = quoteRepo.cachedQuotesOnce().firstOrNull { it.symbol == symbol }
            _inWatchlist.value = watchlistRepo.contains(symbol)
            // свежая одиночная котировка
            val res = quoteRepo.refresh(listOf(symbol))
            _quote.value = when (res) {
                is com.sigmaflux.market.data.quote.RefreshResult.Success -> res.quotes.firstOrNull()
                is com.sigmaflux.market.data.quote.RefreshResult.UsingCache -> res.quotes.firstOrNull()
                is com.sigmaflux.market.data.quote.RefreshResult.Demo -> res.quotes.firstOrNull()
            }
        }
    }

    fun toggleWatchlist(symbol: String) {
        viewModelScope.launch {
            if (watchlistRepo.contains(symbol)) watchlistRepo.remove(symbol)
            else watchlistRepo.add(symbol)
            _inWatchlist.value = watchlistRepo.contains(symbol)
        }
    }
}
