"""SigmaFlux Backend API.

MVP: работает без API-ключей (AI_ENABLED=false), demo-fallback у провайдеров.
Эндпоинты соответствуют контракту docs/api-contract.md.
"""

import time

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware

from .providers.ccxt_crypto import CcxtCryptoProvider
from .providers.demo_data import demo_news
from .providers.moex_iss import MoexIssProvider, ProviderError

VERSION = "0.1.0"
AI_ENABLED = False

app = FastAPI(
    title="SigmaFlux API",
    description="Мобильный аналитический терминал (MOEX + крипто). Не финансовый совет.",
    version=VERSION,
)

# CORS для локальной отладки (web-клиенты). В проде — строгий allowlist.
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

moex = MoexIssProvider()
crypto = CcxtCryptoProvider()

# криптосимволы идут через CCXT, остальные — MOEX ISS
CRYPTO_SYMBOLS = {"BTC", "ETH", "SOL"}


@app.get("/health", response_model=dict)
def health() -> dict:
    return {"status": "ok", "version": VERSION, "ai_enabled": AI_ENABLED}


@app.get("/v1/instruments")
def instruments() -> list[dict]:
    """Каталог инструментов (соответствует InstrumentCatalog в Android)."""
    return [
        {"symbol": "IMOEX", "name": "Индекс МосБиржи", "exchange": "MOEX", "kind": "INDEX", "board": "SNDX", "currency": "RUB", "decimals": 1},
        {"symbol": "RTSI", "name": "Индекс РТС", "exchange": "MOEX", "kind": "INDEX", "board": "SNDX", "currency": "USD", "decimals": 1},
        {"symbol": "RGBI", "name": "Индекс гособлигаций", "exchange": "MOEX", "kind": "INDEX", "board": "SNDX", "currency": "RUB", "decimals": 2},
        {"symbol": "SBER", "name": "Сбербанк", "exchange": "MOEX", "kind": "STOCK", "board": "TQBR", "currency": "RUB", "decimals": 2},
        {"symbol": "GAZP", "name": "Газпром", "exchange": "MOEX", "kind": "STOCK", "board": "TQBR", "currency": "RUB", "decimals": 2},
        {"symbol": "LKOH", "name": "Лукойл", "exchange": "MOEX", "kind": "STOCK", "board": "TQBR", "currency": "RUB", "decimals": 2},
        {"symbol": "OFZ26240", "name": "ОФЗ 26240", "exchange": "MOEX", "kind": "BOND", "board": "TQCB", "currency": "RUB", "decimals": 3},
        {"symbol": "USDRUB", "name": "Доллар/Рубль", "exchange": "MOEX", "kind": "CURRENCY", "board": "CETS", "currency": "RUB", "decimals": 2},
        {"symbol": "BTC", "name": "Bitcoin", "exchange": "CRYPTO", "kind": "CRYPTO", "board": None, "currency": "USD", "decimals": 2},
        {"symbol": "ETH", "name": "Ethereum", "exchange": "CRYPTO", "kind": "CRYPTO", "board": None, "currency": "USD", "decimals": 2},
    ]


@app.get("/v1/market/overview")
def market_overview() -> dict:
    # торговое время MOEX: пн-пт 10:00–18:50 (MSK). Упрощённо.
    now = time.localtime()
    is_weekday = now.tm_wday < 5
    hour_min = now.tm_hour * 100 + now.tm_min
    status = "OPEN" if is_weekday and 1000 <= hour_min <= 1850 else "CLOSED"
    return {
        "overview": {
            "market_status": status,
            "updated_at_epoch_ms": int(time.time() * 1000),
            "is_demo": True,
        }
    }


@app.get("/v1/market/quotes")
def market_quotes(symbols: str = "IMOEX,RTSI,BTC") -> dict:
    requested = [s.strip().upper() for s in symbols.split(",") if s.strip()]
    out = []
    any_demo = False
    for sym in requested:
        try:
            if sym in CRYPTO_SYMBOLS:
                q = crypto.fetch_quote_sync(sym)
            else:
                q = moex.fetch_quote(sym)
        except ProviderError:
            from .providers.demo_data import demo_quote
            q = demo_quote(sym)
        out.append(q)
        if q.get("is_demo"):
            any_demo = True
    return {"quotes": out, "is_demo": any_demo, "updated_at_epoch_ms": int(time.time() * 1000)}


# допустимые интервалы свечей (сек): 1 мин, 5 мин, 15 мин, 1 ч, 1 день
CANDLE_INTERVALS = {60, 300, 900, 3600, 86400}


@app.get("/v1/market/candles")
def market_candles(symbol: str = "IMOEX", interval: int = 3600, limit: int = 48) -> dict:
    """Свечи для мини-графика. Реальные (ISS) с честным demo fallback."""
    if interval not in CANDLE_INTERVALS:
        raise HTTPException(status_code=422, detail=f"interval должен быть одним из {sorted(CANDLE_INTERVALS)}")
    limit = max(5, min(limit, 200))
    symbol = symbol.upper()
    try:
        if symbol in CRYPTO_SYMBOLS:
            result = crypto.fetch_candles_sync(symbol, interval_sec=interval, limit=limit)
        else:
            result = moex.fetch_candles(symbol, interval_sec=interval, limit=limit)
        candles = result["candles"]
        is_demo = result.get("is_demo", False)
    except Exception:
        from .providers.demo_data import demo_candles
        candles = demo_candles(symbol, interval, limit)
        is_demo = True
    return {
        "symbol": symbol,
        "interval": interval,
        "candles": candles,
        "is_demo": is_demo,
        "updated_at_epoch_ms": int(time.time() * 1000),
    }


@app.get("/v1/news")
def news(limit: int = 20) -> dict:
    limit = max(1, min(limit, 100))
    return {"news": demo_news()[:limit], "is_demo": True}


@app.get("/v1/alerts")
def get_alerts() -> dict:
    # В MVP алерты живут локально на устройстве (DataStore).
    return {"alerts": []}


@app.post("/v1/alerts")
def create_alert(payload: dict) -> dict:
    # Заглушка: локальное состояние алертов — на устройстве.
    return {"alerts": []}


@app.delete("/v1/alerts/{alert_id}")
def delete_alert(alert_id: str) -> dict:
    return {"alerts": []}


@app.get("/v1/portfolio/demo")
def portfolio_demo() -> dict:
    return {
        "name": "Демо-портфель",
        "currency": "RUB",
        "start_balance": 100000.0,
        "cash": 100000.0,
        "positions": [],
        "is_demo": True,
    }


@app.get("/v1/ai")
def ai_status() -> dict:
    """AI выключен в MVP: числовые показатели детерминированные."""
    return {"enabled": AI_ENABLED, "note": "AI подключится позже и только через backend."}
