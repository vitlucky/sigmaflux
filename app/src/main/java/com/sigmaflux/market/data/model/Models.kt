package com.sigmaflux.market.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Источник новости: определяет цвет точки и глубину доверия. */
enum class SourceTier {
    /** Официальный источник (ЦБ РФ, Мосбиржа и т.п.) — muted blue/neutral dot. */
    OFFICIAL,

    /** Профессиональное СМИ — gray-blue dot. */
    MEDIA,

    /** Telegram и менее проверенные каналы — tiny amber dot. */
    TELEGRAM
}

enum class ExchangeKind { INDEX, STOCK, BOND, CURRENCY, CRYPTO }

enum class MarketStatus { OPEN, CLOSED, UNKNOWN }

/** Инструмент из каталога (MOEX + крипто). */
@Serializable
data class Instrument(
    val symbol: String,
    val name: String,
    val exchange: String,        // "MOEX" | "CRYPTO"
    val kind: ExchangeKind,
    val board: String? = null,   // MOEX ISS board (TQBR, CETS, ...)
    val currency: String,
    val isin: String? = null,
    val demoBasePrice: Double,
    val demoVolume: Double,
    val searchable: String,      // lowercase alias для поиска
    val decimals: Int = 2
)

/** Котировка. Всегда несёт время получения и пометку demo/реальная. */
@Serializable
data class Quote(
    val symbol: String,
    val name: String,
    val price: Double,
    @SerialName("prev_close") val prevClose: Double,
    @SerialName("day_open") val dayOpen: Double,
    @SerialName("day_high") val dayHigh: Double,
    @SerialName("day_low") val dayLow: Double,
    val volume: Double,
    val currency: String,
    @SerialName("change_pct") val changePct: Double, // % к prevClose
    @SerialName("updated_at_epoch_ms") val updatedAtEpochMs: Long,
    @SerialName("is_demo") val isDemo: Boolean,      // true = демо-значение, НЕ real-time
    val provider: String,                            // "moex_iss" | "ccxt" | "demo"
    val decimals: Int = 2
) {
    val changeAbs: Double get() = price - prevClose
    val isUp: Boolean get() = changeAbs >= 0
}

/** Новость. */
@Serializable
data class NewsItem(
    val id: String,
    val title: String,
    val summary: String,
    @SerialName("source_name") val sourceName: String,
    @SerialName("source_tier") val sourceTier: SourceTier,
    @SerialName("published_at_epoch_ms") val publishedAtEpochMs: Long,
    val url: String? = null,
    @SerialName("is_demo") val isDemo: Boolean,
    @SerialName("is_confirmed") val isConfirmed: Boolean // неподтверждённое — только на detail
)

/** Алерт пользователя (локальный). */
@Serializable
data class Alert(
    val id: String,
    val symbol: String,
    val type: AlertType,
    val threshold: Double,
    @SerialName("created_at_epoch_ms") val createdAtEpochMs: Long,
    @SerialName("fired_at_epoch_ms") val firedAtEpochMs: Long? = null,
    val active: Boolean = true,
    val label: String
)

@Serializable
enum class AlertType {
    PRICE_ABOVE,      // цена выше уровня
    PRICE_BELOW,      // цена ниже уровня
    DROP_PCT_DAY,     // падение за день, %
    RISE_PCT_DAY,     // рост за день, %
    DROP_PCT_15M      // падение за 15 минут, % (ручной порог)
}

/** Позиция в демо-портфеле. */
@Serializable
data class Position(
    val symbol: String,
    val name: String,
    val qty: Double,
    val avgPrice: Double,
    val realizedPnl: Double
)

@Serializable
enum class OrderSide { BUY, SELL }

@Serializable
enum class OrderType { MARKET, LIMIT }

@Serializable
enum class OrderStatus { FILLED, OPEN }

@Serializable
data class Order(
    val id: String,
    val symbol: String,
    val name: String,
    val side: OrderSide,
    val type: OrderType,
    val qty: Double,
    val price: Double,
    val commission: Double,
    val slippage: Double,
    val status: OrderStatus,
    val tsEpochMs: Long
)

/** Демо-портфель. */
@Serializable
data class Portfolio(
    val name: String,
    val currency: String,
    val startBalance: Double,
    val cash: Double,
    val commissionPct: Double = 0.3,
    val positions: List<Position> = emptyList(),
    val orders: List<Order> = emptyList(),
    val createdAtEpochMs: Long
)

/** Сигнал — всегда детерминированный, с объяснением. */
@Serializable
data class Signal(
    val symbol: String,
    val title: String,
    val direction: Direction,
    val score: Double,           // 0..1
    val explanation: String,
    val tsEpochMs: Long
)

enum class Direction { POSITIVE, NEGATIVE, NEUTRAL }

/** Обзор рынка. */
@Serializable
data class MarketOverview(
    @SerialName("market_status") val marketStatus: MarketStatus,
    @SerialName("updated_at_epoch_ms") val updatedAtEpochMs: Long,
    @SerialName("is_demo") val isDemo: Boolean
)

/** Свеча для графика. */
@Serializable
data class Candle(
    val t: Long,   // epoch ms открытия
    val o: Double,
    val h: Double,
    val l: Double,
    val c: Double,
    val v: Double
)

/** Свежесть данных для UI-метки. */
data class Freshness(
    val updatedAgoMinutes: Long,
    val stale: Boolean,
    val sourceLabel: String,
    val isDemo: Boolean,
    val rateLimited: Boolean = false
)
