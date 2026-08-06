package com.sigmaflux.market.ui.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sigmaflux.market.data.Graph
import com.sigmaflux.market.data.model.Alert
import com.sigmaflux.market.data.model.AlertType
import com.sigmaflux.market.data.model.Quote
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlertsViewModel : ViewModel() {

    private val repo = Graph.alerts
    private val quoteRepo = Graph.quotes

    val alerts: StateFlow<List<Alert>> = repo.alerts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _lastQuotes = MutableStateFlow<List<Quote>>(emptyList())
    val lastQuotes: StateFlow<List<Quote>> = _lastQuotes

    init {
        viewModelScope.launch {
            _lastQuotes.value = quoteRepo.cachedQuotesOnce()
            quoteRepo.cachedQuotes.collect { _lastQuotes.value = it }
        }
    }

    fun add(type: AlertType, symbol: String, threshold: Double, label: String, isSmart: Boolean = false) {
        viewModelScope.launch { repo.add(type, symbol, threshold, label, isSmart) }
    }

    fun delete(id: String) {
        viewModelScope.launch { repo.delete(id) }
    }

    fun evaluate() {
        viewModelScope.launch {
            val quotes = _lastQuotes.value
            // предзагружаем историю для всех символов (suspend)
            val historyMap = mutableMapOf<String, List<Pair<Long, Double>>>()
            val ticksMap = mutableMapOf<String, List<com.sigmaflux.market.data.quote.PriceTick>>()
            for (q in quotes) {
                historyMap[q.symbol] = quoteRepo.historyFor(q.symbol)
                ticksMap[q.symbol] = Graph.priceHistory.getTicks(q.symbol)
            }
            repo.evaluate(
                quotes,
                history = { sym -> historyMap[sym] ?: emptyList() },
                priceTicks = { sym -> ticksMap[sym] ?: emptyList() }
            )
        }
    }

    fun priceFor(symbol: String): Double? =
        _lastQuotes.value.firstOrNull { it.symbol == symbol }?.price
}
