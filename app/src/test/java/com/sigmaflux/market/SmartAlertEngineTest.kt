package com.sigmaflux.market

import com.sigmaflux.market.data.alert.SmartAlertEngine
import com.sigmaflux.market.data.model.Alert
import com.sigmaflux.market.data.model.AlertType
import com.sigmaflux.market.data.model.Quote
import com.sigmaflux.market.data.quote.PriceTick
import org.junit.Assert.*
import org.junit.Test

class SmartAlertEngineTest {

    private fun quote(
        symbol: String = "SBER",
        price: Double,
        prevClose: Double = price,
        changePct: Double = 0.0,
        volume: Double = 5_000_000.0,
        isDemo: Boolean = false,
        updatedAt: Long = System.currentTimeMillis()
    ) = Quote(
        symbol = symbol,
        name = symbol,
        price = price,
        prevClose = prevClose,
        dayOpen = prevClose,
        dayHigh = price * 1.02,
        dayLow = price * 0.98,
        volume = volume,
        currency = "RUB",
        changePct = changePct,
        updatedAtEpochMs = updatedAt,
        isDemo = isDemo,
        provider = if (isDemo) "demo" else "moex_iss"
    )

    private fun alert(type: AlertType, threshold: Double, isSmart: Boolean) = Alert(
        id = "test",
        symbol = "SBER",
        type = type,
        threshold = threshold,
        createdAtEpochMs = 1L,
        label = "test",
        isSmart = isSmart
    )

    private fun ticks(prices: List<Double>, startMs: Long = 1_700_000_000_000L): List<PriceTick> {
        return prices.mapIndexed { i, p -> PriceTick(t = startMs + i * 60_000L, price = p, volume = 2_000_000.0) }
    }

    @Test
    fun `smart drop requires ATR or volatility or volume spike`() {
        // history: prices falling from 100 to 95 (5% drop), threshold 3%, with high volatility
        val history = ticks(listOf(100.0, 99.0, 97.0, 96.0, 95.0))
        val q = quote(price = 95.0, volume = 4_000_000.0, changePct = -5.0)
        val alertSmart = alert(AlertType.DROP_PCT_15M, threshold = 3.0, isSmart = true)
        // isMarketOpen = true, no unconfirmed news
        val hit = SmartAlertEngine.evaluateSmart(alertSmart, q, history, isMarketOpen = true, hasUnconfirmedNews = false)
        assertTrue(hit)
    }

    @Test
    fun `smart drop blocked when market closed`() {
        val history = ticks(listOf(100.0, 95.0))
        val q = quote(price = 95.0, changePct = -5.0)
        val alertSmart = alert(AlertType.DROP_PCT_15M, threshold = 3.0, isSmart = true)
        val hit = SmartAlertEngine.evaluateSmart(alertSmart, q, history, isMarketOpen = false)
        assertFalse(hit)
        // manual should still hit even when market closed
        val alertManual = alert(AlertType.DROP_PCT_15M, threshold = 3.0, isSmart = false)
        // manual uses same engine? Actually manual path in AlertRepository doesn't check marketOpen, but SmartEngine does.
        // For manual we expect true via direct check, but SmartEngine with isSmart=false still goes through manual? 
        // Here we test SmartEngine with isSmart=false should still use smart logic? It will return false for market closed only if isSmart=true.
        // So manual smart=false should be true if drop >= threshold
        // But SmartEngine.evaluateSmart with isSmart=false will still check marketOpen only if isSmart, so it should return true
        // Actually our SmartEngine checks marketOpen only if alert.isSmart, so with isSmart=false it should ignore marketOpen
        val hitManualViaSmart = SmartAlertEngine.evaluateSmart(alertManual, q, history, isMarketOpen = false)
        // isSmart=false, so marketOpen is ignored, and drop 5% >= 3% with history size >=2 should be true via SmartEngine's manual branch?
        // For DROP_PCT_15M with isSmart=false, SmartEngine still evaluates via the DROP_PCT_15M branch which checks dropPct >= threshold and then if !isSmart return true
        // So it should be true
        assertTrue(hitManualViaSmart)
    }

    @Test
    fun `demo quote never triggers smart`() {
        val history = ticks(listOf(100.0, 95.0))
        val q = quote(price = 95.0, isDemo = true)
        val alertSmart = alert(AlertType.DROP_PCT_15M, threshold = 1.0, isSmart = true)
        assertFalse(SmartAlertEngine.evaluateSmart(alertSmart, q, history, isMarketOpen = true))
    }

    @Test
    fun `unconfirmed news blocks smart`() {
        val history = ticks(listOf(100.0, 95.0))
        val q = quote(price = 95.0)
        val alertSmart = alert(AlertType.DROP_PCT_15M, threshold = 1.0, isSmart = true)
        assertFalse(SmartAlertEngine.evaluateSmart(alertSmart, q, history, isMarketOpen = true, hasUnconfirmedNews = true))
    }

    @Test
    fun `atr calculation`() {
        val history = ticks(listOf(100.0, 102.0, 101.0, 103.0))
        val atr = SmartAlertEngine.atr(history, period = 3)
        assertTrue(atr > 0)
        assertEquals(1.33, atr, 0.5)
    }

    @Test
    fun `volatility calculation`() {
        val history = ticks(listOf(100.0, 100.5, 99.5, 101.0, 100.0))
        val vol = SmartAlertEngine.volatilityPct(history)
        assertTrue(vol >= 0)
    }

    @Test
    fun `smart price above requires volume spike or volatility`() {
        val history = ticks(listOf(100.0, 100.1, 100.2))
        val qLowVol = quote(price = 101.0, volume = 1_000_000.0)
        val qHighVol = quote(price = 101.0, volume = 5_000_000.0)
        val alert = alert(AlertType.PRICE_ABOVE, threshold = 100.0, isSmart = true)
        // low vol, low volatility -> should not hit smart price above (needs spike)
        // history avg vol ~2M, lowVol 1M -> no spike, vol ~0.1 -> no hit
        assertFalse(SmartAlertEngine.evaluateSmart(alert, qLowVol, history, isMarketOpen = true))
        // high vol -> should hit
        assertTrue(SmartAlertEngine.evaluateSmart(alert, qHighVol, history, isMarketOpen = true))
    }
}
