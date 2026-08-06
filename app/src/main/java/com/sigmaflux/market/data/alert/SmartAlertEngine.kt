package com.sigmaflux.market.data.alert

import com.sigmaflux.market.data.model.Alert
import com.sigmaflux.market.data.model.AlertType
import com.sigmaflux.market.data.model.Quote
import com.sigmaflux.market.data.quote.PriceTick
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Smart-алерты: детерминированная оценка с учётом ATR, волатильности, объёма, ликвидности,
 * торгового времени и новостей. Не является торговой рекомендацией.
 *
 * Логика MVP Smart:
 * - ATR-подобная оценка через (high-low)/close и историю тиков
 * - волатильность как std dev доходностей
 * - объёмный спайк (>1.8x среднего)
 * - ликвидность (VALTODAY proxy через volume)
 * - торговое время MOEX 10:00-18:50 MSK, пн-пт
 * - неподтверждённая новость не может триггерить strong сигнал
 */
object SmartAlertEngine {

    fun isMarketOpen(nowMs: Long = System.currentTimeMillis()): Boolean {
        // MSK = UTC+3
        val utcHours = (nowMs / 3600000L) % 24
        val mskHours = (utcHours + 3) % 24
        val mskMinutes = ((nowMs / 60000L) % 60).toInt()
        val totalMins = (mskHours * 60 + mskMinutes).toInt()
        // пн-пт 10:00-18:50
        val dayOfWeek = ((nowMs / 86400000L) + 3) % 7 // 0=чт, 3=вс? упрощённо: используем Calendar
        // Для детерминизма используем java.util.Calendar
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Europe/Moscow"))
        cal.timeInMillis = nowMs
        val dow = cal.get(java.util.Calendar.DAY_OF_WEEK) // 1=ВС, 2=ПН...
        val isWeekday = dow in 2..6
        val mins = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
        return isWeekday && mins in (10 * 60)..(18 * 60 + 50)
    }

    fun atr(ticks: List<PriceTick>, period: Int = 14): Double {
        if (ticks.size < 2) return 0.0
        val trs = mutableListOf<Double>()
        for (i in 1 until ticks.size) {
            val prev = ticks[i - 1].price
            val cur = ticks[i].price
            // true range упрощённо = |cur - prev| (нет high/low в тиках)
            trs.add(abs(cur - prev))
        }
        if (trs.isEmpty()) return 0.0
        val take = minOf(period, trs.size)
        return trs.takeLast(take).average()
    }

    fun volatilityPct(ticks: List<PriceTick>): Double {
        if (ticks.size < 3) return 0.0
        val returns = mutableListOf<Double>()
        for (i in 1 until ticks.size) {
            val prev = ticks[i - 1].price
            val cur = ticks[i].price
            if (prev > 0) returns.add((cur - prev) / prev * 100)
        }
        if (returns.size < 2) return 0.0
        val mean = returns.average()
        val variance = returns.map { (it - mean) * (it - mean) }.average()
        return sqrt(variance)
    }

    fun avgVolume(ticks: List<PriceTick>): Double {
        if (ticks.isEmpty()) return 0.0
        return ticks.map { it.volume }.average()
    }

    fun evaluateSmart(
        alert: Alert,
        quote: Quote,
        history: List<PriceTick>,
        isMarketOpen: Boolean = isMarketOpen(quote.updatedAtEpochMs),
        hasUnconfirmedNews: Boolean = false
    ): Boolean {
        // demo-данные: smart не триггерит сильный сигнал
        if (quote.isDemo) return false
        // неподтверждённая новость блокирует strong smart-триггер
        if (hasUnconfirmedNews && alert.isSmart) {
            // для smart требуем подтверждение, иначе не триггерим
            // но manual порог всё равно может сработать — здесь блокируем только smart
            return false
        }
        // вне торгов — smart не триггерит (manual — триггерит)
        if (alert.isSmart && !isMarketOpen) return false

        return when (alert.type) {
            AlertType.DROP_PCT_15M -> {
                if (history.size < 2) return false
                val maxPrice = history.maxOf { it.price }
                val dropPct = (maxPrice - quote.price) / maxPrice * 100
                if (dropPct < alert.threshold) return false
                if (!alert.isSmart) return true
                // Smart: требуем ATR, волатильность, объём
                val atrVal = atr(history)
                val atrPct = if (quote.price > 0) atrVal / quote.price * 100 else 0.0
                val vol = volatilityPct(history)
                val avgVol = avgVolume(history)
                val volSpike = if (avgVol > 0) quote.volume / avgVol else 0.0
                // ликвидность: объём в деньгах proxy — если объём слишком мал, считаем неликвидом
                val isLiquid = quote.volume > 1_000_000 || avgVol > 500_000
                // Smart условие: падение >= threshold И (>= ATR*1.2 или волатильность >1% или объёмный спайк)
                val smartCondition = (atrPct > 0 && dropPct >= atrPct * 1.2) || vol >= 1.0 || volSpike >= 1.8
                smartCondition && isLiquid
            }
            AlertType.DROP_PCT_DAY -> {
                if (quote.changePct > -alert.threshold) return false
                if (!alert.isSmart) return true
                val vol = volatilityPct(history)
                val atrVal = atr(history)
                val atrPct = if (quote.price > 0) atrVal / quote.price * 100 else 0.0
                // для дневного падения smart требуем волатильность или ATR
                (vol >= 1.2 || atrPct >= 1.0) && isMarketOpen
            }
            AlertType.RISE_PCT_DAY -> {
                if (quote.changePct < alert.threshold) return false
                if (!alert.isSmart) return true
                val vol = volatilityPct(history)
                vol >= 1.2 && isMarketOpen
            }
            AlertType.PRICE_ABOVE -> {
                if (quote.price < alert.threshold) return false
                if (!alert.isSmart) return true
                // для цены smart — проверяем объёмный спайк и волатильность
                val volSpike = if (history.isNotEmpty()) {
                    val avgVol = avgVolume(history)
                    if (avgVol > 0) quote.volume / avgVol else 0.0
                } else 0.0
                volSpike >= 1.5 || volatilityPct(history) >= 1.0
            }
            AlertType.PRICE_BELOW -> {
                if (quote.price > alert.threshold) return false
                if (!alert.isSmart) return true
                val volSpike = if (history.isNotEmpty()) {
                    val avgVol = avgVolume(history)
                    if (avgVol > 0) quote.volume / avgVol else 0.0
                } else 0.0
                volSpike >= 1.5 || volatilityPct(history) >= 1.0
            }
        }
    }
}
