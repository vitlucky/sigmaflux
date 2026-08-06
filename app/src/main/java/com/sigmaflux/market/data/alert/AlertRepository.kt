package com.sigmaflux.market.data.alert

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sigmaflux.market.data.model.Alert
import com.sigmaflux.market.data.model.AlertType
import com.sigmaflux.market.data.model.Quote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.util.UUID

private val Context.alertStore by preferencesDataStore(name = "alerts")

/**
 * Локальные smart/manual алерты. Хранятся в DataStore.
 * Immediate next task #4: "Add alert creation UI and local alert state."
 */
class AlertRepository(private val context: Context) {

    private val keyAlertsJson = stringPreferencesKey("alerts_json")
    private val json = Json { ignoreUnknownKeys = true }

    val alerts: Flow<List<Alert>> = context.alertStore.data.map { prefs ->
        prefs[keyAlertsJson]?.let { raw ->
            runCatching { json.decodeFromString(ListSerializer(Alert.serializer()), raw) }.getOrNull()
        } ?: emptyList()
    }

    suspend fun all(): List<Alert> = alerts.first()

    suspend fun add(type: AlertType, symbol: String, threshold: Double, label: String): Alert {
        val alert = Alert(
            id = UUID.randomUUID().toString(),
            symbol = symbol,
            type = type,
            threshold = threshold,
            createdAtEpochMs = System.currentTimeMillis(),
            firedAtEpochMs = null,
            active = true,
            label = label
        )
        save(all() + alert)
        return alert
    }

    suspend fun delete(id: String) {
        save(all().filterNot { it.id == id })
    }

    suspend fun markFired(id: String) {
        save(all().map { if (it.id == id) it.copy(firedAtEpochMs = System.currentTimeMillis(), active = false) else it })
    }

    /**
     * Детерминированная проверка условий по свежим котировкам.
     * Возвращает сработавшие алерты (одноразовые — active=false после срабатывания).
     */
    suspend fun evaluate(quotes: List<Quote>): List<Alert> {
        val bySymbol = quotes.associateBy { it.symbol }
        val current = all()
        val fired = mutableListOf<Alert>()
        val updated = mutableListOf<Alert>()
        for (a in current) {
            if (!a.active) { updated += a; continue }
            val q = bySymbol[a.symbol] ?: run { updated += a; continue }
            val hit = when (a.type) {
                AlertType.PRICE_ABOVE -> q.price >= a.threshold
                AlertType.PRICE_BELOW -> q.price <= a.threshold
                AlertType.DROP_PCT_DAY -> q.changePct <= -a.threshold
                AlertType.RISE_PCT_DAY -> q.changePct >= a.threshold
                AlertType.DROP_PCT_15M -> false // 15-минутный срез требует истории тиков — в MVP не оценивается
            }
            if (hit) {
                val f = a.copy(firedAtEpochMs = System.currentTimeMillis(), active = false)
                fired += f
                updated += f
            } else {
                updated += a
            }
        }
        if (fired.isNotEmpty()) save(updated)
        return fired
    }

    private suspend fun save(list: List<Alert>) {
        context.alertStore.edit { prefs ->
            prefs[keyAlertsJson] = json.encodeToString(ListSerializer(Alert.serializer()), list)
        }
    }
}
