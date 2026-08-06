package com.sigmaflux.market.ui.market

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sigmaflux.market.data.Graph
import com.sigmaflux.market.data.instrument.InstrumentCatalog
import com.sigmaflux.market.data.model.Instrument
import com.sigmaflux.market.data.model.Quote
import com.sigmaflux.market.data.quote.demoQuotes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MarketViewModel : ViewModel() {

    private val watchlistRepo = Graph.watchlist

    private val _quotes = MutableStateFlow<List<Quote>>(emptyList())
    val quotes: StateFlow<List<Quote>> = _quotes

    private val _search = MutableStateFlow("")
    val search: StateFlow<String> = _search

    /** Каталог с фильтром по поиску (символ/имя/тип). */
    val instruments: StateFlow<List<Instrument>> = kotlinx.coroutines.flow.combine(
        _search, _quotes
    ) { q, _ -> InstrumentCatalog.search(q) }
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), InstrumentCatalog.all)

    val watchlistSymbols: StateFlow<List<String>> = watchlistRepo.symbols
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            _quotes.value = Graph.quotes.cachedQuotesOnce().ifEmpty { demoQuotes(InstrumentCatalog.all.map { it.symbol }) }
            refresh()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val allSymbols = InstrumentCatalog.all.map { it.symbol }
            val res = Graph.quotes.refresh(allSymbols)
            _quotes.value = when (res) {
                is com.sigmaflux.market.data.quote.RefreshResult.Success -> res.quotes
                is com.sigmaflux.market.data.quote.RefreshResult.UsingCache -> res.quotes
                is com.sigmaflux.market.data.quote.RefreshResult.Demo -> res.quotes
            }
        }
    }

    fun onSearchChange(q: String) { _search.value = q }

    fun addToWatchlist(symbol: String) {
        viewModelScope.launch { watchlistRepo.add(symbol) }
    }
}
