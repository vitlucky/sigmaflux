package com.sigmaflux.market.data

import android.content.Context
import com.sigmaflux.market.data.alert.AlertRepository
import com.sigmaflux.market.data.news.NewsRepository
import com.sigmaflux.market.data.network.ApiClient
import com.sigmaflux.market.data.portfolio.PortfolioRepository
import com.sigmaflux.market.data.quote.CandlesRepository
import com.sigmaflux.market.data.quote.PriceHistoryRepository
import com.sigmaflux.market.data.quote.QuoteRepository
import com.sigmaflux.market.data.watchlist.WatchlistRepository

/**
 * Простой DI-граф (без Hilt в MVP, чтобы не усложнять сборку).
 * Singleton-репозитории на процесс.
 */
object Graph {

    lateinit var watchlist: WatchlistRepository
        private set
    lateinit var quotes: QuoteRepository
        private set
    lateinit var priceHistory: PriceHistoryRepository
        private set
    lateinit var news: NewsRepository
        private set
    lateinit var alerts: AlertRepository
        private set
    lateinit var portfolio: PortfolioRepository
        private set
    lateinit var candles: CandlesRepository
        private set

    fun init(context: Context) {
        if (::watchlist.isInitialized) return
        watchlist = WatchlistRepository(context)
        priceHistory = PriceHistoryRepository(context)
        quotes = QuoteRepository(context, ApiClient.api, priceHistory)
        news = NewsRepository(context, ApiClient.api)
        alerts = AlertRepository(context)
        portfolio = PortfolioRepository(context)
        candles = CandlesRepository(context, ApiClient.api)
    }
}
