package com.sigmaflux.market

import com.sigmaflux.market.data.model.Direction
import com.sigmaflux.market.data.model.Quote
import com.sigmaflux.market.data.signal.SignalEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SignalEngineTest {

    private fun quote(
        symbol: String,
        price: Double,
        prevClose: Double,
        high: Double,
        low: Double,
        isDemo: Boolean = false
    ) = Quote(
        symbol = symbol,
        name = symbol,
        price = price,
        prevClose = prevClose,
        dayOpen = prevClose,
        dayHigh = high,
        dayLow = low,
        volume = 1_000_000.0,
        currency = "RUB",
        changePct = (price - prevClose) / prevClose * 100,
        updatedAtEpochMs = 1_700_000_000_000L,
        isDemo = isDemo,
        provider = if (isDemo) "demo" else "moex_iss"
    )

    @Test
    fun `strong drop produces negative signal`() {
        val q = quote("SBER", 270.0, 285.0, 286.0, 268.0) // -5.3%, range 6.3%
        val signals = SignalEngine.compute(listOf(q))
        val s = signals.firstOrNull { it.symbol == "SBER" }
        assertNotNull(s)
        assertEquals(Direction.NEGATIVE, s!!.direction)
        assertTrue(s.explanation.isNotBlank())
    }

    @Test
    fun `strong rise produces positive signal`() {
        val q = quote("GAZP", 140.0, 128.0, 141.0, 127.0) // +9.4%
        val signals = SignalEngine.compute(listOf(q))
        val s = signals.firstOrNull { it.symbol == "GAZP" }
        assertEquals(Direction.POSITIVE, s!!.direction)
    }

    @Test
    fun `demo quote never produces directional signal`() {
        val q = quote("IMOEX", 3000.0, 3100.0, 3110.0, 2990.0, isDemo = true)
        val signals = SignalEngine.compute(listOf(q))
        val s = signals.firstOrNull { it.symbol == "IMOEX" }
        assertNotNull(s)
        assertEquals(Direction.NEUTRAL, s!!.direction)
        assertTrue(s.score < 0.5)
    }

    @Test
    fun `flat quote produces no signal`() {
        val q = quote("VTBR", 0.095, 0.095, 0.096, 0.094)
        assertTrue(SignalEngine.compute(listOf(q)).isEmpty())
    }

    @Test
    fun `empty list produces empty signals`() {
        assertTrue(SignalEngine.compute(emptyList()).isEmpty())
    }
}
