"""Провайдер крипто через CCXT (async).

CCXT — коннектор, не вендор данных: источником является конкретная биржа.
В MVP тикеры берутся с Binance public API (без ключей). private API — позже, только через backend.
"""

import asyncio
import time
from typing import Any

from .base import ProviderError
from .demo_data import demo_candles, demo_quote

# symbol -> (ccxt_id, биржа)
CRYPTO_MAP: dict[str, tuple[str, str]] = {
    "BTC": ("BTC/USDT", "binance"),
    "ETH": ("ETH/USDT", "binance"),
    "SOL": ("SOL/USDT", "binance"),
}


class CcxtCryptoProvider:
    name = "ccxt"

    def __init__(self, timeout: float = 8.0):
        self.timeout = timeout

    async def fetch_quote(self, symbol: str) -> dict[str, Any]:
        symbol = symbol.upper()
        mapping = CRYPTO_MAP.get(symbol)
        if mapping is None:
            return demo_quote(symbol, provider="demo")
        market, exchange_id = mapping
        try:
            import ccxt.async_support as ccxt_async

            exchange_class = getattr(ccxt_async, exchange_id)
            exchange = exchange_class({"enableRateLimit": True, "timeout": int(self.timeout * 1000)})
            try:
                ticker = await exchange.fetch_ticker(market)
            finally:
                await exchange.close()

            last = ticker.get("last") or ticker.get("close")
            if last is None:
                raise ProviderError("no ticker price")
            prev = ticker.get("previousClose") or (ticker.get("open") or last)
            change = (last - prev) / prev * 100 if prev else 0.0
            return {
                "symbol": symbol,
                "name": {"BTC": "Bitcoin", "ETH": "Ethereum", "SOL": "Solana"}[symbol],
                "price": round(last, 2),
                "prev_close": round(prev, 2),
                "day_open": ticker.get("open") or prev,
                "day_high": ticker.get("high") or last,
                "day_low": ticker.get("low") or last,
                "volume": ticker.get("quoteVolume") or 0.0,
                "currency": "USD",
                "change_pct": round(change, 2),
                "updated_at_epoch_ms": int(time.time() * 1000),
                "is_demo": False,
                "provider": self.name,
                "decimals": 2,
            }
        except Exception as exc:  # noqa: BLE001 — сеть/лимиты/биржа: честный demo fallback
            return demo_quote(symbol, provider="demo")

    def fetch_quote_sync(self, symbol: str) -> dict[str, Any]:
        return asyncio.run(self.fetch_quote(symbol))

    async def fetch_candles(self, symbol: str, interval_sec: int = 3600, limit: int = 48) -> dict[str, Any]:
        """Свечи через CCXT (Binance public). Возвращает {"candles": [...], "is_demo": bool}."""
        symbol = symbol.upper()
        mapping = CRYPTO_MAP.get(symbol)
        if mapping is None:
            return {"candles": demo_candles(symbol, interval_sec, limit), "is_demo": True}
        market, exchange_id = mapping
        try:
            import ccxt.async_support as ccxt_async

            exchange_class = getattr(ccxt_async, exchange_id)
            exchange = exchange_class({"enableRateLimit": True, "timeout": int(self.timeout * 1000)})
            try:
                timeframe = {
                    60: "1m", 300: "5m", 900: "15m", 3600: "1h", 86400: "1d",
                }.get(interval_sec, "1h")
                raw = await exchange.fetch_ohlcv(market, timeframe=timeframe, limit=limit)
            finally:
                await exchange.close()

            candles: list[dict[str, Any]] = []
            for row in raw:
                candles.append({
                    "t": int(row[0]),
                    "o": row[1],
                    "h": row[2],
                    "l": row[3],
                    "c": row[4],
                    "v": row[5],
                })
            return {"candles": candles, "is_demo": False}
        except Exception:
            return {"candles": demo_candles(symbol, interval_sec, limit), "is_demo": True}

    def fetch_candles_sync(self, symbol: str, interval_sec: int = 3600, limit: int = 48) -> dict[str, Any]:
        return asyncio.run(self.fetch_candles(symbol, interval_sec, limit))
