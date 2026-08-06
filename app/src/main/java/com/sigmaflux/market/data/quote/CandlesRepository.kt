package com.sigmaflux.market.data.quote

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sigmaflux.market.data.model.Candle
import com.sigmaflux.market.data.network.BackendApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.candleStore by preferencesDataStore(name = "candle_cache")

/** Результат загрузки свечей: кэш/сеть, честно помеченный demo-признак. */
data class CandlesResult(val candles: List<Candle>, val isDemo: Boolean)

/**
 * Репозиторий свечей для мини-графика.
 * 1) backend (MOEX ISS / CCXT) — реальные свечи;
 * 2) кэш последних успешных;
 * 3) пусто → UI рисует детерминированный demo-спарклайн с пометкой demo.
 */
class CandlesRepository(private val context: Context, private val api: BackendApi) {

    private val json = Json { ignoreUnknownKeys = true }

    fun cached(symbol: String): Flow<List<Candle>> = context.candleStore.data.map { prefs ->
        prefs[key(symbol)]?.let { raw ->
            runCatching { json.decodeFromString(ListSerializer(Candle.serializer()), raw) }.getOrNull()
        } ?: emptyList()
    }

    suspend fun refresh(symbol: String, interval: Int = 3600, limit: Int = 48): CandlesResult {
        return try {
            val res = api.getCandles(symbol, interval, limit)
            val candles = res.candles
            if (candles.isNotEmpty()) {
                context.candleStore.edit { prefs -> prefs[key(symbol)] = json.encodeToString(ListSerializer(Candle.serializer()), candles) }
            }
            CandlesResult(candles, res.isDemo)
        } catch (e: Exception) {
            val cached = cached(symbol).first()
            if (cached.isNotEmpty()) CandlesResult(cached, true)
            else CandlesResult(emptyList(), true)
        }
    }

    private fun key(symbol: String) = stringPreferencesKey("candles_${symbol.uppercase()}")
}
