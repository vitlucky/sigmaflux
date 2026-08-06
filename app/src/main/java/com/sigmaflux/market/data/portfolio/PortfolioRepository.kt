package com.sigmaflux.market.data.portfolio

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sigmaflux.market.data.instrument.InstrumentCatalog
import com.sigmaflux.market.data.model.Order
import com.sigmaflux.market.data.model.OrderSide
import com.sigmaflux.market.data.model.OrderStatus
import com.sigmaflux.market.data.model.OrderType
import com.sigmaflux.market.data.model.Portfolio
import com.sigmaflux.market.data.model.Position
import com.sigmaflux.market.data.model.Quote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.util.UUID

private val Context.portfolioStore by preferencesDataStore(name = "portfolio")

/**
 * Демо-портфель: market/limit заявки, комиссия, проскальзывание,
 * средняя цена, realized/unrealized P&L, история сделок.
 * Всё — симуляция, помечено как demo.
 */
class PortfolioRepository(private val context: Context) {

    private val keyPortfolioJson = stringPreferencesKey("portfolio_json")
    private val json = Json { ignoreUnknownKeys = true }

    val portfolio: Flow<Portfolio?> = context.portfolioStore.data.map { prefs ->
        prefs[keyPortfolioJson]?.let { raw ->
            runCatching { json.decodeFromString(Portfolio.serializer(), raw) }.getOrNull()
        }
    }

    suspend fun current(): Portfolio? = portfolio.first()

    suspend fun create(name: String, currency: String, startBalance: Double): Portfolio {
        val p = Portfolio(
            name = name,
            currency = currency,
            startBalance = startBalance,
            cash = startBalance,
            createdAtEpochMs = System.currentTimeMillis()
        )
        save(p)
        return p
    }

    suspend fun delete() {
        context.portfolioStore.edit { it.clear() }
    }

    /**
     * Размещение demo-заявки.
     * MARKET: исполняется немедленно по последней цене + проскальзывание.
     * LIMIT: исполняется, если лимит пересекает рынок, иначе остаётся OPEN.
     */
    suspend fun placeOrder(side: OrderSide, type: OrderType, symbol: String, qty: Double, limitPrice: Double?, lastPrice: Double): Portfolio? {
        val p = current() ?: return null
        val instr = InstrumentCatalog.bySymbol(symbol) ?: return null
        val slippagePct = if (type == OrderType.MARKET) SLIPPAGE_PCT else 0.0
        val fillPrice = when (type) {
            OrderType.MARKET -> lastPrice * (1 + slippagePct * (if (side == OrderSide.BUY) 1 else -1))
            OrderType.LIMIT -> limitPrice ?: lastPrice // лимитка исполняется по лимитной цене (если пересекает рынок)
        }
        val notional = fillPrice * qty
        val commission = notional * p.commissionPct / 100.0

        // исполнимость лимитной заявки (в MVP: только если пересекает рынок)
        val willFill = when (type) {
            OrderType.MARKET -> true
            OrderType.LIMIT -> {
                val lp = limitPrice ?: lastPrice
                (side == OrderSide.BUY && lp >= lastPrice) || (side == OrderSide.SELL && lp <= lastPrice)
            }
        }

        val order = Order(
            id = UUID.randomUUID().toString(),
            symbol = symbol,
            name = instr.name,
            side = side,
            type = type,
            qty = qty,
            price = Math.round(fillPrice * 100.0) / 100.0,
            commission = Math.round(commission * 100.0) / 100.0,
            slippage = Math.round(notional * slippagePct * 100.0) / 100.0,
            status = if (willFill) OrderStatus.FILLED else OrderStatus.OPEN,
            tsEpochMs = System.currentTimeMillis()
        )

        var cash = p.cash
        var positions = p.positions.toMutableList()
        var realized = 0.0

        if (willFill) {
            if (side == OrderSide.BUY) {
                cash -= notional + commission
                val existing = positions.firstOrNull { it.symbol == symbol }
                if (existing != null) {
                    val newQty = existing.qty + qty
                    val newAvg = (existing.avgPrice * existing.qty + fillPrice * qty) / newQty
                    positions = positions.map { if (it.symbol == symbol) it.copy(qty = newQty, avgPrice = newAvg) else it }.toMutableList()
                } else {
                    positions += Position(symbol, instr.name, qty, fillPrice, 0.0)
                }
            } else {
                val existing = positions.firstOrNull { it.symbol == symbol }
                if (existing != null && existing.qty >= qty) {
                    val sellNotional = fillPrice * qty
                    val cost = existing.avgPrice * qty
                    realized = sellNotional - cost - commission
                    val left = existing.qty - qty
                    positions = positions.map {
                        if (it.symbol == symbol) it.copy(qty = left, realizedPnl = it.realizedPnl + realized) else it
                    }.filter { it.qty > 0.0 }.toMutableList()
                    cash += sellNotional - commission
                } else {
                    // короткая продажа не поддерживается — заявка не исполняется
                    return p.copy(orders = p.orders + order.copy(status = OrderStatus.OPEN))
                }
            }
        }

        val updated = p.copy(cash = cash, positions = positions, orders = p.orders + order)
        save(updated)
        return updated
    }

    /** Непокрытая (unrealized) прибыль по текущим ценам. */
    fun unrealizedPnl(portfolio: Portfolio?, quotes: Map<String, Quote>): Double {
        if (portfolio == null) return 0.0
        return portfolio.positions.sumOf { pos ->
            val last = quotes[pos.symbol]?.price ?: pos.avgPrice
            (last - pos.avgPrice) * pos.qty
        }
    }

    fun equity(portfolio: Portfolio?, quotes: Map<String, Quote>): Double {
        if (portfolio == null) return 0.0
        return portfolio.cash + unrealizedPnl(portfolio, quotes) +
            portfolio.positions.sumOf { it.realizedPnl }
    }

    private suspend fun save(p: Portfolio) {
        context.portfolioStore.edit { prefs ->
            prefs[keyPortfolioJson] = json.encodeToString(Portfolio.serializer(), p)
        }
    }

    companion object {
        const val SLIPPAGE_PCT = 0.05 // 0.05% проскальзывание для market-заявок
        val SUPPORTED_CURRENCIES = listOf("RUB", "USD", "USDT")
    }
}
