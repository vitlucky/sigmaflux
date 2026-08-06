package com.sigmaflux.market.data.news

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sigmaflux.market.data.model.NewsItem
import com.sigmaflux.market.data.model.SourceTier
import com.sigmaflux.market.data.network.BackendApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.newsStore by preferencesDataStore(name = "news_cache")

/**
 * Лента новостей: backend → кэш → demo.
 * Демо-лента явно помечена (isDemo=true); неподтверждённые источники не
 * могут сами по себе создавать сигналы (см. SignalEngine).
 */
class NewsRepository(private val context: Context, private val api: BackendApi) {

    private val keyNewsJson = stringPreferencesKey("news_json")
    private val json = Json { ignoreUnknownKeys = true }

    val cachedNews: Flow<List<NewsItem>> = context.newsStore.data.map { prefs ->
        prefs[keyNewsJson]?.let { raw ->
            runCatching { json.decodeFromString(ListSerializer(NewsItem.serializer()), raw) }.getOrNull()
        } ?: demoNews()
    }

    suspend fun cachedOnce(): List<NewsItem> = cachedNews.first()

    suspend fun refresh(limit: Int = 20): Result<List<NewsItem>> {
        return try {
            val res = api.getNews(limit)
            val items = res.news
            context.newsStore.edit { prefs ->
                prefs[keyNewsJson] = json.encodeToString(ListSerializer(NewsItem.serializer()), items)
            }
            Result.success(items)
        } catch (e: Exception) {
            Result.success(cachedOnce())
        }
    }

    companion object {
        fun demoNews(): List<NewsItem> {
            val now = System.currentTimeMillis()
            val m = 60_000L
            return listOf(
                NewsItem(
                    id = "demo-1",
                    title = "Демо: ЦБ РФ сохранил ключевую ставку",
                    summary = "Демонстрационная новость. Совет директоров Банка России сохранил ключевую ставку на прежнем уровне. Данные не реальные.",
                    sourceName = "ЦБ РФ",
                    sourceTier = SourceTier.OFFICIAL,
                    publishedAtEpochMs = now - 18 * m,
                    url = null,
                    isDemo = true,
                    isConfirmed = true
                ),
                NewsItem(
                    id = "demo-2",
                    title = "Демо: нефть корректируется после роста",
                    summary = "Демонстрационная новость. Котировки нефти снижаются после недельного роста. Это не реальные данные.",
                    sourceName = "Финансовое СМИ",
                    sourceTier = SourceTier.MEDIA,
                    publishedAtEpochMs = now - 34 * m,
                    url = null,
                    isDemo = true,
                    isConfirmed = true
                ),
                NewsItem(
                    id = "demo-3",
                    title = "Демо: слухи о сделке в телеком-секторе",
                    summary = "Демонстрационная новость. Телеграм-каналы обсуждают возможную сделку. Информация не подтверждена.",
                    sourceName = "Telegram",
                    sourceTier = SourceTier.TELEGRAM,
                    publishedAtEpochMs = now - 49 * m,
                    url = null,
                    isDemo = true,
                    isConfirmed = false
                ),
                NewsItem(
                    id = "demo-4",
                    title = "Демо: рубль стабилен к основным валютам",
                    summary = "Демонстрационная новость. Курс рубля незначительно изменился. Данные не реальные.",
                    sourceName = "Финансовое СМИ",
                    sourceTier = SourceTier.MEDIA,
                    publishedAtEpochMs = now - 71 * m,
                    url = null,
                    isDemo = true,
                    isConfirmed = true
                ),
                NewsItem(
                    id = "demo-5",
                    title = "Демо: индекс МосБиржи в боковике",
                    summary = "Демонстрационная новость. IMOEX торгуется в узком диапазоне. Это не реальные данные.",
                    sourceName = "Мосбиржа",
                    sourceTier = SourceTier.OFFICIAL,
                    publishedAtEpochMs = now - 95 * m,
                    url = null,
                    isDemo = true,
                    isConfirmed = true
                )
            )
        }
    }
}
