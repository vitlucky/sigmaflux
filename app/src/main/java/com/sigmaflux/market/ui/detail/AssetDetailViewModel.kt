package com.sigmaflux.market.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sigmaflux.market.data.Graph
import com.sigmaflux.market.data.model.Candle
import com.sigmaflux.market.data.model.Quote
import com.sigmaflux.market.ui.components.demoCandles
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AssetDetailViewModel : ViewModel() {

    private val quoteRepo = Graph.quotes
    private val candleRepo = Graph.candles
    private val watchlistRepo = Graph.watchlist

    private val _quote = MutableStateFlow<Quote?>(null)
    val quote: StateFlow<Quote?> = _quote

    private val _candles = MutableStateFlow<List<Candle>>(emptyList())
    val candles: StateFlow<List<Candle>> = _candles

    private val _candlesDemo = MutableStateFlow(true)
    val candlesDemo: StateFlow<Boolean> = _candlesDemo

    private val _inWatchlist = MutableStateFlow(false)
    val inWatchlist: StateFlow<Boolean> = _inWatchlist

    fun load(symbol: String) {
        viewModelScope.launch {
            _quote.value = quoteRepo.cachedQuotesOnce().firstOrNull { it.symbol == symbol }
            _inWatchlist.value = watchlistRepo.contains(symbol)

            // свежие одиночные котировки
            val res = quoteRepo.refresh(listOf(symbol))
            _quote.value = when (res) {
                is com.sigmaflux.market.data.quote.RefreshResult.Success -> res.quotes.firstOrNull()
                is com.sigmaflux.market.data.quote.RefreshResult.UsingCache -> res.quotes.firstOrNull()
                is com.sigmaflux.market.data.quote.RefreshResult.Demo -> res.quotes.firstOrNull()
            }

            // свечи: сеть → кэш → детерминированный demo-спарклайн
            val cached = candleRepo.cached(symbol)
            cached.collect { _candles.value = it }
            val fresh = candleRepo.refresh(symbol, interval = 3600, limit = 48)
            if (fresh.candles.isNotEmpty()) {
                _candles.value = fresh.candles
                _candlesDemo.value = fresh.isDemo
            } else {
                _candles.value = demoCandles(symbol)
                _candlesDemo.value = true
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
