package com.sigmaflux.market.data.quote

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sigmaflux.market.data.model.Quote
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.priceHistoryStore by preferencesDataStore(name = "price_history")

@Serializable
data class PriceTick(
    val t: Long,
    val price: Double,
    val volume: Double = 0.0
)

/**
 * Персистентная rolling-история цен (15 мин окно) для Smart-алертов.
 * Хранится в DataStore (JSON), переживает перезапуск приложения.
 * В памяти — кэш для быстрого доступа, синхронизируется с диском при record().
 */
class PriceHistoryRepository(private val context: Context) {

    private val keyHistoryJson = stringPreferencesKey("price_history_json")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val mutex = Mutex()
    private val memory = mutableMapOf<String, MutableList<PriceTick>>()
    private var loaded = false

    companion object {
        const val WINDOW_MS = 15 * 60_000L
        const val MAX_TICKS_PER_SYMBOL = 900 // ~1 per second for 15m, safety cap
    }

    private suspend fun ensureLoaded() {
        if (loaded) return
        mutex.withLock {
            if (loaded) return
            val prefs = context.priceHistoryStore.data.first()
            val raw = prefs[keyHistoryJson]
            if (raw != null) {
                runCatching {
                    val map = json.decodeFromString(
                        MapSerializer(kotlinx.serialization.serializer<String>(), ListSerializer(PriceTick.serializer())),
                        raw
                    )
                    map.forEach { (sym, ticks) -> memory[sym] = ticks.toMutableList() }
                }
            }
            // prune old on load
            val now = System.currentTimeMillis()
            val cutoff = now - WINDOW_MS
            memory.values.forEach { list -> list.removeIf { it.t < cutoff } }
            loaded = true
        }
    }

    suspend fun record(quotes: List<Quote>) {
        ensureLoaded()
        val now = System.currentTimeMillis()
        val cutoff = now - WINDOW_MS
        mutex.withLock {
            quotes.forEach { q ->
                val list = memory.getOrPut(q.symbol) { mutableListOf() }
                list.add(PriceTick(t = now, price = q.price, volume = q.volume))
                // keep only window
                while (list.isNotEmpty() && list.first().t < cutoff) list.removeAt(0)
                // safety cap
                while (list.size > MAX_TICKS_PER_SYMBOL) list.removeAt(0)
            }
            // prune all symbols
            memory.values.forEach { list -> list.removeIf { it.t < cutoff } }
            persistLocked()
        }
    }

    suspend fun getHistory(symbol: String, windowMs: Long = WINDOW_MS): List<Pair<Long, Double>> {
        ensureLoaded()
        val cutoff = System.currentTimeMillis() - windowMs
        return mutex.withLock {
            memory[symbol]?.filter { it.t >= cutoff }?.map { it.t to it.price }?.toList() ?: emptyList()
        }
    }

    suspend fun getTicks(symbol: String, windowMs: Long = WINDOW_MS): List<PriceTick> {
        ensureLoaded()
        val cutoff = System.currentTimeMillis() - windowMs
        return mutex.withLock {
            memory[symbol]?.filter { it.t >= cutoff }?.toList() ?: emptyList()
        }
    }

    suspend fun getAllTicks(windowMs: Long = WINDOW_MS): Map<String, List<PriceTick>> {
        ensureLoaded()
        val cutoff = System.currentTimeMillis() - windowMs
        return mutex.withLock {
            memory.mapValues { (_, v) -> v.filter { it.t >= cutoff }.toList() }
        }
    }

    suspend fun clear() {
        mutex.withLock {
            memory.clear()
            context.priceHistoryStore.edit { it[keyHistoryJson] = json.encodeToString(
                MapSerializer(kotlinx.serialization.serializer<String>(), ListSerializer(PriceTick.serializer())),
                emptyMap()
            ) }
        }
    }

    private suspend fun persistLocked() {
        // must be called with mutex held
        val snapshot: Map<String, List<PriceTick>> = memory.mapValues { it.value.toList() }
        val raw = json.encodeToString(
            MapSerializer(kotlinx.serialization.serializer<String>(), ListSerializer(PriceTick.serializer())),
            snapshot
        )
        context.priceHistoryStore.edit { it[keyHistoryJson] = raw }
    }
}
