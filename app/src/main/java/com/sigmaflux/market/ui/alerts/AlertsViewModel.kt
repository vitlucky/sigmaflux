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

    fun add(type: AlertType, symbol: String, threshold: Double, label: String) {
        viewModelScope.launch { repo.add(type, symbol, threshold, label) }
    }

    fun delete(id: String) {
        viewModelScope.launch { repo.delete(id) }
    }

    fun evaluate() {
        viewModelScope.launch {
            repo.evaluate(_lastQuotes.value) { symbol -> quoteRepo.historyFor(symbol) }
        }
    }

    fun priceFor(symbol: String): Double? =
        _lastQuotes.value.firstOrNull { it.symbol == symbol }?.price
}
