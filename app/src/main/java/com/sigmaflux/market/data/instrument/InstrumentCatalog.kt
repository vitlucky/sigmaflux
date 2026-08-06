package com.sigmaflux.market.data.instrument

import com.sigmaflux.market.data.model.ExchangeKind
import com.sigmaflux.market.data.model.Instrument

/**
 * Каталог инструментов. Расширяемый: MOEX (индексы, акции, облигации, валюты)
 * + крипто через CCXT. Зарубежные акции/ETF не входят в первую версию.
 *
 * MOEX ISS board mapping:
 *  - TQBR  — акции (основной режим)
 *  - TQCB  — облигации
 *  - CETS  — валютный рынок
 *  - IMOEX / RTSI / RGBI — индексы (board = "SNDX"/спец. индексы)
 */
object InstrumentCatalog {

    val DEFAULT_STRIP = listOf("IMOEX", "RTSI", "RGBI", "USDRUB", "BTC")

    val all: List<Instrument> = listOf(
        // Индексы MOEX
        Instrument("IMOEX", "Индекс МосБиржи", "MOEX", ExchangeKind.INDEX, "SNDX", "RUB", demoBasePrice = 3100.0, demoVolume = 1.4e10, searchable = "imoex индекс мосбиржи", decimals = 1),
        Instrument("RTSI", "Индекс РТС", "MOEX", ExchangeKind.INDEX, "SNDX", "USD", demoBasePrice = 1000.0, demoVolume = 8e9, searchable = "rtsi ртс индекс", decimals = 1),
        Instrument("RGBI", "Индекс гособлигаций", "MOEX", ExchangeKind.INDEX, "SNDX", "RUB", demoBasePrice = 105.0, demoVolume = 5e8, searchable = "rgbi гособлигации индекс", decimals = 2),

        // Акции MOEX (TQBR)
        Instrument("SBER", "Сбербанк", "MOEX", ExchangeKind.STOCK, "TQBR", "RUB", demoBasePrice = 285.0, demoVolume = 9e9, searchable = "sber сбербанк сбер", decimals = 2),
        Instrument("GAZP", "Газпром", "MOEX", ExchangeKind.STOCK, "TQBR", "RUB", demoBasePrice = 128.0, demoVolume = 6e9, searchable = "gazp газпром", decimals = 2),
        Instrument("LKOH", "Лукойл", "MOEX", ExchangeKind.STOCK, "TQBR", "RUB", demoBasePrice = 6900.0, demoVolume = 4e9, searchable = "lkoh лукойл", decimals = 2),
        Instrument("ROSN", "Роснефть", "MOEX", ExchangeKind.STOCK, "TQBR", "RUB", demoBasePrice = 520.0, demoVolume = 3e9, searchable = "rosn роснефть", decimals = 2),
        Instrument("VTBR", "ВТБ", "MOEX", ExchangeKind.STOCK, "TQBR", "RUB", demoBasePrice = 0.095, demoVolume = 5e9, searchable = "vtbr втб", decimals = 4),
        Instrument("NVTK", "Новатэк", "MOEX", ExchangeKind.STOCK, "TQBR", "RUB", demoBasePrice = 1050.0, demoVolume = 2e9, searchable = "nvtk новатэк", decimals = 2),
        Instrument("PLZL", "Полюс", "MOEX", ExchangeKind.STOCK, "TQBR", "RUB", demoBasePrice = 13500.0, demoVolume = 3e9, searchable = "plzl полюс золото", decimals = 2),
        Instrument("MGNT", "Магнит", "MOEX", ExchangeKind.STOCK, "TQBR", "RUB", demoBasePrice = 4600.0, demoVolume = 1e9, searchable = "mgnt магнит", decimals = 2),

        // Облигации (TQCB)
        Instrument("OFZ26240", "ОФЗ 26240", "MOEX", ExchangeKind.BOND, "TQCB", "RUB", demoBasePrice = 82.5, demoVolume = 2e8, searchable = "ofz26240 офз облигация", decimals = 3),

        // Валюты (CETS)
        Instrument("USDRUB", "Доллар/Рубль", "MOEX", ExchangeKind.CURRENCY, "CETS", "RUB", demoBasePrice = 88.5, demoVolume = 1.2e11, searchable = "usdrub доллар рубль usd", decimals = 2),
        Instrument("EURRUB", "Евро/Рубль", "MOEX", ExchangeKind.CURRENCY, "CETS", "RUB", demoBasePrice = 96.0, demoVolume = 5e10, searchable = "eurrub евро рубль eur", decimals = 2),

        // Крипто (CCXT — данные биржи, не вендора)
        Instrument("BTC", "Bitcoin", "CRYPTO", ExchangeKind.CRYPTO, null, "USD", demoBasePrice = 62000.0, demoVolume = 2.5e10, searchable = "btc bitcoin биткоин", decimals = 0),
        Instrument("ETH", "Ethereum", "CRYPTO", ExchangeKind.CRYPTO, null, "USD", demoBasePrice = 3200.0, demoVolume = 1.2e10, searchable = "eth ethereum эфир", decimals = 1),
        Instrument("SOL", "Solana", "CRYPTO", ExchangeKind.CRYPTO, null, "USD", demoBasePrice = 145.0, demoVolume = 4e9, searchable = "sol solana солана", decimals = 2)
    )

    private val bySymbol = all.associateBy { it.symbol }

    fun bySymbol(symbol: String): Instrument? = bySymbol[symbol]

    fun search(query: String): List<Instrument> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return all
        return all.filter {
            it.symbol.lowercase().contains(q) || it.searchable.contains(q) || it.name.lowercase().contains(q)
        }
    }

    fun kindLabel(kind: ExchangeKind): String = when (kind) {
        ExchangeKind.INDEX -> "Индекс"
        ExchangeKind.STOCK -> "Акция"
        ExchangeKind.BOND -> "Облигация"
        ExchangeKind.CURRENCY -> "Валюта"
        ExchangeKind.CRYPTO -> "Крипто"
    }
}
