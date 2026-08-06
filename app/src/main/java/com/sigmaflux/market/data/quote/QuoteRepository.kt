package com.sigmaflux.market.data.quote

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sigmaflux.market.data.instrument.InstrumentCatalog
import com.sigmaflux.market.data.model.Freshness
import com.sigmaflux.market.data.model.Quote
import com.sigmaflux.market.data.network.BackendApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.quoteStore by preferencesDataStore(name = "quote_cache")

/**
 * Поставщик котировок:
 * 1) сеть (backend → MOEX ISS / CCXT);
 * 2) при недоступности — кэш последних успешных данных (offline cache);
 * 3) без кэша — детерминированные demo-значения с честной пометкой isDemo=true.
 * Никогда не выдаём demo за real-time.
 */
class QuoteRepository(
    private val context: Context,
    private val api: BackendApi,
    private val priceHistory: PriceHistoryRepository? = null
) {

    private val keyQuotesJson = stringPreferencesKey("quotes_json")
    private val keyUpdatedAt = longPreferencesKey("updated_at_epoch_ms")

    private val json = Json { ignoreUnknownKeys = true }

    /** Последние успешные котировки (кэш). */
    val cachedQuotes: Flow<List<Quote>> = context.quoteStore.data.map { prefs ->
        prefs[keyQuotesJson]?.let { raw ->
            runCatching { json.decodeFromString(ListSerializer(Quote.serializer()), raw) }.getOrNull()
        } ?: demoQuotes(InstrumentCatalog.DEFAULT_STRIP)
    }

    suspend fun cachedQuotesOnce(): List<Quote> = cachedQuotes.first()

    /**
     * Rolling-история цен — теперь персистентная (DataStore) через PriceHistoryRepository.
     * Fallback — in-memory если репозиторий не инжектирован (тесты).
     */
    private val fallbackHistory = mutableMapOf<String, MutableList<Pair<Long, Double>>>()

    suspend fun historyFor(symbol: String): List<Pair<Long, Double>> =
        priceHistory?.getHistory(symbol) ?: (fallbackHistory[symbol]?.toList() ?: emptyList())

    /** Синхронная версия для совместимости (читает из памяти, если есть). */
    fun historyForSync(symbol: String): List<Pair<Long, Double>> =
        fallbackHistory[symbol]?.toList() ?: emptyList()

    private suspend fun recordHistory(quotes: List<Quote>) {
        if (priceHistory != null) {
            priceHistory.record(quotes)
        } else {
            val now = System.currentTimeMillis()
            val cutoff = now - HISTORY_WINDOW_MS
            quotes.forEach { q ->
                val list = fallbackHistory.getOrPut(q.symbol) { mutableListOf() }
                list.add(now to q.price)
                while (list.isNotEmpty() && list.first().first < cutoff) list.removeAt(0)
            }
        }
    }

    suspend fun refresh(symbols: List<String>): RefreshResult {
        return try {
            val dto = api.getQuotes(symbols.joinToString(","))
            val quotes = dto.quotes
            cache(quotes)
            recordHistory(quotes)
            RefreshResult.Success(quotes, freshness(quotes.firstOrNull()?.updatedAtEpochMs ?: 0L, isDemo = false))
        } catch (e: Exception) {
            handleFailure(symbols, e)
        }
    }

    private suspend fun handleFailure(symbols: List<String>, e: Exception): RefreshResult {
        val rateLimited = e.message?.contains("429", ignoreCase = true) == true
        val cached = cachedQuotesOnce()
        if (cached.isNotEmpty()) {
            return RefreshResult.UsingCache(cached, freshness(cached.first().updatedAtEpochMs, isDemo = cached.first().isDemo, rateLimited = rateLimited))
        }
        val demo = demoQuotes(symbols)
        return RefreshResult.Demo(demo, freshness(0L, isDemo = true))
    }

    private suspend fun cache(quotes: List<Quote>) {
        context.quoteStore.edit { prefs ->
            prefs[keyQuotesJson] = json.encodeToString(ListSerializer(Quote.serializer()), quotes)
            prefs[keyUpdatedAt] = System.currentTimeMillis()
        }
    }

    private fun freshness(updatedEpochMs: Long, isDemo: Boolean, rateLimited: Boolean = false): Freshness {
        val minutes = if (updatedEpochMs > 0) (System.currentTimeMillis() - updatedEpochMs) / 60_000L else 0L
        return Freshness(
            updatedAgoMinutes = minutes,
            stale = minutes > STALE_AFTER_MINUTES,
            sourceLabel = if (isDemo) "Демо-данные" else "MOEX ISS / CCXT",
            isDemo = isDemo,
            rateLimited = rateLimited
        )
    }

    companion object {
        const val STALE_AFTER_MINUTES = 15L
        const val HISTORY_WINDOW_MS = 15 * 60_000L
    }
}

sealed class RefreshResult {
    data class Success(val quotes: List<Quote>, val freshness: Freshness) : RefreshResult()
    data class UsingCache(val quotes: List<Quote>, val freshness: Freshness) : RefreshResult()
    data class Demo(val quotes: List<Quote>, val freshness: Freshness) : RefreshResult()
}

/** Детерминированные demo-котировки: стабильны внутри минуты, помечены isDemo=true. */
fun demoQuotes(symbols: List<String>): List<Quote> {
    val now = System.currentTimeMillis()
    return symbols.mapNotNull { sym ->
        val instr = InstrumentCatalog.bySymbol(sym) ?: return@mapNotNull null
        val seed = sym.hashCode()
        // плавное псевдослучайное движение на основе времени (5-сек баскет)
        val t = now / 5_000
        val wave = kotlin.math.sin(seed.toDouble() * 0.001 + t * 0.02)
        val wave2 = kotlin.math.cos(seed.toDouble() * 0.0007 + t * 0.011)
        val movePct = (wave * 0.9 + wave2 * 0.4) // примерно ±1.3%
        val prevClose = instr.demoBasePrice
        val price = prevClose * (1 + movePct / 100)
        val dayHigh = prevClose * (1 + (kotlin.math.abs(wave) + 0.3) / 100)
        val dayLow = prevClose * (1 - (kotlin.math.abs(wave2) + 0.3) / 100)
        Quote(
            symbol = sym,
            name = instr.name,
            price = round(price, instr.decimals),
            prevClose = prevClose,
            dayOpen = prevClose,
            dayHigh = round(dayHigh, instr.decimals),
            dayLow = round(dayLow, instr.decimals),
            volume = instr.demoVolume * (1 + wave * 0.15),
            currency = instr.currency,
            changePct = round(movePct, 2),
            updatedAtEpochMs = now,
            isDemo = true,
            provider = "demo",
            decimals = instr.decimals
        )
    }
}

private fun round(v: Double, decimals: Int): Double {
    val f = Math.pow(10.0, decimals.toDouble())
    return Math.round(v * f) / f
}
