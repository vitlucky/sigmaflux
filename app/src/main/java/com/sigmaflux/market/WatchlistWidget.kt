package com.sigmaflux.market

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.sigmaflux.market.data.Graph
import com.sigmaflux.market.data.model.Quote
import com.sigmaflux.market.util.Format

/**
 * Watchlist Widget (первый размер 4x2).
 * - пользовательские инструменты из DataStore (watchlist);
 * - цена и изменение;
 * - horizontal scrolling (заложен в layout строкой);
 * - deep link в конкретный актив (sigmaflux://asset/{symbol});
 * - cache last successful value (читает кэш котировок из DataStore);
 * - dark/light mode — палитра Graphite одинакова в обеих темах.
 */
class WatchlistWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val symbols = Graph.watchlist.currentSymbols()
        val quotes = Graph.quotes.cachedQuotesOnce().filter { it.symbol in symbols }

        provideContent {
            WatchlistWidgetContent(
                quotes = quotes,
                empty = symbols.isEmpty(),
                onOpen = { symbol ->
                    actionStartActivity(
                        PendingIntent.getActivity(
                            context,
                            0,
                            Intent(Intent.ACTION_VIEW, Uri.parse("sigmaflux://asset/$symbol")),
                            PendingIntent.FLAG_IMMUTABLE
                        )
                    )
                }
            )
        }
    }

    @Composable
    private fun WatchlistWidgetContent(
        quotes: List<Quote>,
        empty: Boolean,
        onOpen: (String) -> androidx.glance.action.Action
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(GraphiteWidget.Background))
                .padding(12.dp)
        ) {
            Text(
                text = "SigmaFlux · Watchlist",
                style = TextStyle(color = ColorProvider(GraphiteWidget.Muted), fontSize = 12.sp),
                modifier = GlanceModifier.padding(bottom = 8.dp)
            )
            if (empty || quotes.isEmpty()) {
                Text(
                    text = "Нет инструментов. Добавьте их на главном экране.",
                    style = TextStyle(color = ColorProvider(GraphiteWidget.Muted), fontSize = 12.sp)
                )
            } else {
                // горизонтальный ряд: расширяется за пределы ширины (scrolling)
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    quotes.forEach { q ->
                        QuoteColumn(q, onOpen(q.symbol))
                    }
                }
            }
        }
    }

    @Composable
    private fun QuoteColumn(q: Quote, action: androidx.glance.action.Action) {
        val up = q.changeAbs >= 0
        val color = if (up) GraphiteWidget.Positive else GraphiteWidget.Negative
        Column(
            modifier = GlanceModifier
                .width(110.dp)
                .padding(horizontal = 4.dp)
                .clickable(action)
        ) {
            Text(
                text = q.symbol,
                style = TextStyle(color = ColorProvider(GraphiteWidget.Text), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            )
            Text(
                text = Format.price(q),
                style = TextStyle(color = ColorProvider(GraphiteWidget.Text), fontSize = 16.sp)
            )
            Text(
                text = Format.pct(q.changePct),
                style = TextStyle(color = ColorProvider(color), fontSize = 13.sp)
            )
        }
    }
}

/** Палитра widget (Graphite Premium). */
object GraphiteWidget {
    val Background = Color(0xFF16161C)
    val Text = Color(0xFFF2EFF7)
    val Muted = Color(0xFFA49EB0)
    val Positive = Color(0xFF87D5B6)
    val Negative = Color(0xFFE9A0B0)
}

class WatchlistWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WatchlistWidget()
}
