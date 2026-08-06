package com.sigmaflux.market.data.watchlist

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sigmaflux.market.data.instrument.InstrumentCatalog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "watchlist")

/**
 * DataStore-персистентность для выбранных инструментов Market Strip / Watchlist.
 * Immediate next task #1: "Add DataStore persistence for selected Market Strip instruments."
 */
class WatchlistRepository(private val context: Context) {

    private val keySymbols = stringPreferencesKey("strip_symbols_json")
    private val keyOrder = stringPreferencesKey("strip_order_json")

    /** Поток выбранных символов в порядке отображения. */
    val symbols: Flow<List<String>> = context.dataStore.data.map { prefs ->
        val raw = prefs[keySymbols] ?: return@map InstrumentCatalog.DEFAULT_STRIP
        decode(raw)
    }

    suspend fun currentSymbols(): List<String> = symbols.first()

    suspend fun add(symbol: String) {
        val list = currentSymbols().toMutableList()
        if (symbol !in list) {
            list.add(symbol)
            save(list)
        }
    }

    suspend fun remove(symbol: String) {
        save(currentSymbols().filterNot { it == symbol })
    }

    suspend fun reorder(ordered: List<String>) {
        // сохраняем только известные символы, порядок — пользовательский
        val known = InstrumentCatalog.all.map { it.symbol }.toSet()
        save(ordered.filter { it in known })
    }

    suspend fun contains(symbol: String): Boolean = symbol in currentSymbols()

    private suspend fun save(list: List<String>) {
        context.dataStore.edit { prefs ->
            prefs[keySymbols] = encode(list)
        }
    }

    private fun encode(list: List<String>): String = list.joinToString(",")
    private fun decode(raw: String): List<String> =
        raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}
