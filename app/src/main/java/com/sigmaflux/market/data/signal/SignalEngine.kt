package com.sigmaflux.market.data.signal

import com.sigmaflux.market.data.model.Direction
import com.sigmaflux.market.data.model.Quote
import com.sigmaflux.market.data.model.Signal
import kotlin.math.abs

/**
 * Детерминированный движок сигналов (MVP): без AI.
 * Использует: движение за день, размах high/low (волатильность), объём.
 * Сигналы — вероятностные оценки, НЕ рекомендации. Объяснимы (explanation).
 */
object SignalEngine {

    fun compute(quotes: List<Quote>): List<Signal> = quotes.mapNotNull { q -> signalFor(q) }

    private fun signalFor(q: Quote): Signal? {
        if (q.isDemo) return Signal(
            symbol = q.symbol,
            title = "Демо-оценка",
            direction = Direction.NEUTRAL,
            score = 0.3,
            explanation = "Данные демонстрационные — сигнал не вычисляется на реальном потоке.",
            tsEpochMs = q.updatedAtEpochMs
        )

        val dayMove = q.changePct
        val rangePct = if (q.prevClose > 0) (q.dayHigh - q.dayLow) / q.prevClose * 100 else 0.0
        val score = minOf(1.0, abs(dayMove) / 4.0 + rangePct / 6.0)

        return when {
            dayMove <= -2.5 && rangePct >= 3.0 -> Signal(q.symbol, "Необычное падение с высокой волатильностью", Direction.NEGATIVE, score,
                "Падение за день ${fmt(dayMove)}% при размахе ${fmt(rangePct)}% (ATR-подобная оценка).", q.updatedAtEpochMs)
            dayMove <= -1.2 -> Signal(q.symbol, "Заметное снижение", Direction.NEGATIVE, score,
                "Снижение за день ${fmt(dayMove)}%. Следите за уровнем и новостями.", q.updatedAtEpochMs)
            dayMove >= 2.5 && rangePct >= 3.0 -> Signal(q.symbol, "Необычный рост с высокой волатильностью", Direction.POSITIVE, score,
                "Рост за день ${fmt(dayMove)}% при размахе ${fmt(rangePct)}%.", q.updatedAtEpochMs)
            dayMove >= 1.2 -> Signal(q.symbol, "Заметный рост", Direction.POSITIVE, score,
                "Рост за день ${fmt(dayMove)}%. Проверьте контекст новостей.", q.updatedAtEpochMs)
            rangePct >= 4.0 -> Signal(q.symbol, "Повышенная волатильность", Direction.NEUTRAL, score,
                "Размах дня ${fmt(rangePct)}% выше обычного. Риск движения в обе стороны.", q.updatedAtEpochMs)
            else -> null
        }
    }

    private fun fmt(v: Double): String = String.format("%.2f", v)
}
