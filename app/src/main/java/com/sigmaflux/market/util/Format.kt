package com.sigmaflux.market.util

import com.sigmaflux.market.data.model.Quote
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object Format {

    private val df = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))
    private val df0 = DecimalFormat("#,##0", DecimalFormatSymbols(Locale.US))
    private val df4 = DecimalFormat("#,##0.0000", DecimalFormatSymbols(Locale.US))

    fun price(q: Quote): String = when (q.decimals) {
        0 -> df0.format(q.price)
        4 -> df4.format(q.price)
        else -> df.format(q.price)
    }

    fun price(value: Double, decimals: Int = 2): String = when (decimals) {
        0 -> df0.format(value)
        4 -> df4.format(value)
        else -> df.format(value)
    }

    fun pct(value: Double): String {
        val sign = if (value > 0) "+" else ""
        return "$sign${df.format(value)}%"
    }

    fun money(value: Double, currency: String): String = "${df.format(value)} $currency"

    fun signedMoney(value: Double, currency: String): String {
        val sign = if (value > 0) "+" else ""
        return "$sign${df.format(value)} $currency"
    }

    fun compact(value: Double): String = when {
        value >= 1_000_000_000 -> df.format(value / 1_000_000_000) + "B"
        value >= 1_000_000 -> df.format(value / 1_000_000) + "M"
        value >= 1_000 -> df.format(value / 1_000) + "K"
        else -> df0.format(value)
    }

    /** «18 мин», «2 ч», «только что». */
    fun ago(epochMs: Long): String {
        val diff = System.currentTimeMillis() - epochMs
        val minutes = diff / 60_000L
        return when {
            minutes < 1 -> "только что"
            minutes < 60 -> "$minutes мин"
            minutes < 60 * 24 -> "${minutes / 60} ч"
            else -> "${minutes / (60 * 24)} дн"
        }
    }
}
