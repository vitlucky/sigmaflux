package com.sigmaflux.market.data.network

import com.sigmaflux.market.BuildConfig
import com.sigmaflux.market.data.model.Alert
import com.sigmaflux.market.data.model.MarketOverview
import com.sigmaflux.market.data.model.NewsItem
import com.sigmaflux.market.data.model.Quote
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- DTO-обёртки ответов backend (контракт: docs/api-contract.md) ---

@Serializable
data class QuotesResponse(val quotes: List<Quote>, val is_demo: Boolean, val updated_at_epoch_ms: Long)

@Serializable
data class NewsResponse(val news: List<NewsItem>, val is_demo: Boolean)

@Serializable
data class AlertsResponse(val alerts: List<Alert>)

@Serializable
data class OverviewResponse(val overview: MarketOverview)

@Serializable
data class HealthResponse(val status: String, val version: String, val ai_enabled: Boolean)

// --- Retrofit API ---

interface BackendApi {
    @GET("health")
    suspend fun health(): HealthResponse

    @GET("v1/market/quotes")
    suspend fun getQuotes(@Query("symbols") symbols: String): QuotesResponse

    @GET("v1/market/overview")
    suspend fun getOverview(): OverviewResponse

    @GET("v1/news")
    suspend fun getNews(@Query("limit") limit: Int = 20): NewsResponse

    @GET("v1/alerts")
    suspend fun getAlerts(): AlertsResponse

    @POST("v1/alerts")
    suspend fun postAlert(@Body alert: Alert): AlertsResponse

    @DELETE("v1/alerts/{id}")
    suspend fun deleteAlert(@Path("id") id: String): AlertsResponse
}

/**
 * Сетевой слой Android → backend.
 * BACKEND_BASE_URL задаётся в BuildConfig (эмулятор: http://10.0.2.2:8000/).
 * Ошибки провайдера мапятся на стороне репозиториев (429 → rate limit и т.п.).
 */
object ApiClient {

    val json = Json { ignoreUnknownKeys = true }

    private val okHttp = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    val api: BackendApi by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BACKEND_BASE_URL)
            .client(okHttp)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(BackendApi::class.java)
    }
}
