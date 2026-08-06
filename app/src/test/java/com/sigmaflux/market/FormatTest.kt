package com.sigmaflux.market

import com.sigmaflux.market.util.Format
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {

    @Test
    fun `pct adds plus sign for positive`() {
        assertEquals("+1.50%", Format.pct(1.5))
        assertEquals("-1.50%", Format.pct(-1.5))
        assertEquals("0.00%", Format.pct(0.0))
    }

    @Test
    fun `compact scales values`() {
        assertEquals("1.50B", Format.compact(1_500_000_000.0))
        assertEquals("1.20B", Format.compact(1_200_000_000.0))
        assertEquals("900.00K", Format.compact(900_000.0))
        assertEquals("123", Format.compact(123.0))
    }

    @Test
    fun `ago formats recent times`() {
        val now = System.currentTimeMillis()
        assertEquals("только что", Format.ago(now - 10_000))
        assertEquals("5 мин", Format.ago(now - 5 * 60_000))
        assertEquals("2 ч", Format.ago(now - 2 * 3_600_000))
    }

    @Test
    fun `money formatting`() {
        assertEquals("1,000.00 RUB", Format.money(1000.0, "RUB"))
        assertEquals("+50.00 USD", Format.signedMoney(50.0, "USD"))
        assertEquals("-50.00 USD", Format.signedMoney(-50.0, "USD"))
    }
}
