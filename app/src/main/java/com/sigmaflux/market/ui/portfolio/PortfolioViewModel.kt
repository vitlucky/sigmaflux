package com.sigmaflux.market.ui.portfolio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sigmaflux.market.data.Graph
import com.sigmaflux.market.data.model.OrderSide
import com.sigmaflux.market.data.model.OrderType
import com.sigmaflux.market.data.model.Portfolio
import com.sigmaflux.market.data.model.Quote
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PortfolioViewModel : ViewModel() {

    private val repo = Graph.portfolio
    private val quoteRepo = Graph.quotes

    private val _quotes = MutableStateFlow<Map<String, Quote>>(emptyMap())

    val portfolio: StateFlow<Portfolio?> = repo.portfolio
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val equity: StateFlow<Double> = combine(portfolio, _quotes) { p, q ->
        repo.equity(p, q)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val unrealized: StateFlow<Double> = combine(portfolio, _quotes) { p, q ->
        repo.unrealizedPnl(p, q)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    init {
        viewModelScope.launch {
            _quotes.value = quoteRepo.cachedQuotesOnce().associateBy { it.symbol }
            quoteRepo.cachedQuotes.collect { quotes ->
                _quotes.value = quotes.associateBy { it.symbol }
            }
        }
    }

    fun create(name: String, currency: String, startBalance: Double) {
        viewModelScope.launch { repo.create(name, currency, startBalance) }
    }

    fun delete() {
        viewModelScope.launch { repo.delete() }
    }

    fun placeMarketOrder(side: OrderSide, symbol: String, qty: Double, lastPrice: Double) {
        viewModelScope.launch { repo.placeOrder(side, OrderType.MARKET, symbol, qty, null, lastPrice) }
    }

    fun placeLimitOrder(side: OrderSide, symbol: String, qty: Double, limitPrice: Double, lastPrice: Double) {
        viewModelScope.launch { repo.placeOrder(side, OrderType.LIMIT, symbol, qty, limitPrice, lastPrice) }
    }

    fun allocation(portfolio: Portfolio?, quotes: Map<String, Quote>): Map<String, Double> {
        if (portfolio == null) return emptyMap()
        val total = portfolio.cash + portfolio.positions.sumOf { (quotes[it.symbol]?.price ?: it.avgPrice) * it.qty }
        if (total <= 0) return emptyMap()
        val map = mutableMapOf("CASH" to portfolio.cash / total)
        portfolio.positions.forEach { pos ->
            map[pos.symbol] = ((quotes[pos.symbol]?.price ?: pos.avgPrice) * pos.qty) / total
        }
        return map
    }
}
