"""Детерминированные demo-данные.

Используются только как fallback, когда провайдер недоступен.
Все demo-ответы несут is_demo=True — приложение никогда не выдаёт их за real-time.
Значения плавно «двигаются» во времени, но не имитируют реальный рынок.
"""

import math
import time
from typing import Any

# Стабильная база (примерные порядки величин, НЕ реальные котировки)
DEMO_BASE: dict[str, dict[str, Any]] = {
    "IMOEX": {"name": "Индекс МосБиржи", "base": 3100.0, "prev": 3090.0, "currency": "RUB", "volume": 1.4e10, "decimals": 1},
    "RTSI": {"name": "Индекс РТС", "base": 1000.0, "prev": 1010.0, "currency": "USD", "volume": 8e9, "decimals": 1},
    "RGBI": {"name": "Индекс гособлигаций", "base": 105.0, "prev": 105.2, "currency": "RUB", "volume": 5e8, "decimals": 2},
    "SBER": {"name": "Сбербанк", "base": 285.0, "prev": 283.0, "currency": "RUB", "volume": 9e9, "decimals": 2},
    "GAZP": {"name": "Газпром", "base": 128.0, "prev": 129.5, "currency": "RUB", "volume": 6e9, "decimals": 2},
    "LKOH": {"name": "Лукойл", "base": 6900.0, "prev": 6850.0, "currency": "RUB", "volume": 4e9, "decimals": 2},
    "ROSN": {"name": "Роснефть", "base": 520.0, "prev": 517.0, "currency": "RUB", "volume": 3e9, "decimals": 2},
    "VTBR": {"name": "ВТБ", "base": 0.095, "prev": 0.094, "currency": "RUB", "volume": 5e9, "decimals": 4},
    "NVTK": {"name": "Новатэк", "base": 1050.0, "prev": 1045.0, "currency": "RUB", "volume": 2e9, "decimals": 2},
    "PLZL": {"name": "Полюс", "base": 13500.0, "prev": 13400.0, "currency": "RUB", "volume": 3e9, "decimals": 2},
    "MGNT": {"name": "Магнит", "base": 4600.0, "prev": 4620.0, "currency": "RUB", "volume": 1e9, "decimals": 2},
    "OFZ26240": {"name": "ОФЗ 26240", "base": 82.5, "prev": 82.4, "currency": "RUB", "volume": 2e8, "decimals": 3},
    "USDRUB": {"name": "Доллар/Рубль", "base": 88.5, "prev": 88.3, "currency": "RUB", "volume": 1.2e11, "decimals": 2},
    "EURRUB": {"name": "Евро/Рубль", "base": 96.0, "prev": 96.2, "currency": "RUB", "volume": 5e10, "decimals": 2},
    "BTC": {"name": "Bitcoin", "base": 62000.0, "prev": 61500.0, "currency": "USD", "volume": 2.5e10, "decimals": 0},
    "ETH": {"name": "Ethereum", "base": 3200.0, "prev": 3180.0, "currency": "USD", "volume": 1.2e10, "decimals": 1},
    "SOL": {"name": "Solana", "base": 145.0, "prev": 143.0, "currency": "USD", "volume": 4e9, "decimals": 2},
}

# Провайдеры по группам (для маркировки)
DEMO_PROVIDER: dict[str, str] = {
    "BTC": "ccxt", "ETH": "ccxt", "SOL": "ccxt",
}

def _round(v: float, decimals: int) -> float:
    f = 10 ** decimals
    return round(v * f) / f


def demo_quote(symbol: str, provider: str = "demo") -> dict[str, Any]:
    cfg = DEMO_BASE.get(symbol.upper())
    if cfg is None:
        raise KeyError(f"no demo config for {symbol}")
    t = time.time()
    seed = abs(hash(symbol)) % 10000
    wave = math.sin(seed * 0.001 + t * 0.02)
    wave2 = math.cos(seed * 0.0007 + t * 0.011)
    move_pct = wave * 0.9 + wave2 * 0.4
    prev = cfg["prev"]
    price = cfg["base"] * (1 + move_pct / 100)
    high = prev * (1 + (abs(wave) + 0.3) / 100)
    low = prev * (1 - (abs(wave2) + 0.3) / 100)
    dec = cfg["decimals"]
    return {
        "symbol": symbol.upper(),
        "name": cfg["name"],
        "price": _round(price, dec),
        "prev_close": prev,
        "day_open": prev,
        "day_high": _round(high, dec),
        "day_low": _round(low, dec),
        "volume": cfg["volume"] * (1 + wave * 0.15),
        "currency": cfg["currency"],
        "change_pct": round(move_pct, 2),
        "updated_at_epoch_ms": int(time.time() * 1000),
        "is_demo": True,
        "provider": provider,
        "decimals": dec,
    }


def demo_news() -> list[dict[str, Any]]:
    now = int(time.time() * 1000)
    m = 60_000
    return [
        {
            "id": "demo-1",
            "title": "Демо: ЦБ РФ сохранил ключевую ставку",
            "summary": "Демонстрационная новость. Данные не реальные.",
            "source_name": "ЦБ РФ",
            "source_tier": "OFFICIAL",
            "published_at_epoch_ms": now - 18 * m,
            "url": None,
            "is_demo": True,
            "is_confirmed": True,
        },
        {
            "id": "demo-2",
            "title": "Демо: нефть корректируется после роста",
            "summary": "Демонстрационная новость. Данные не реальные.",
            "source_name": "Финансовое СМИ",
            "source_tier": "MEDIA",
            "published_at_epoch_ms": now - 34 * m,
            "url": None,
            "is_demo": True,
            "is_confirmed": True,
        },
        {
            "id": "demo-3",
            "title": "Демо: слухи о сделке в телеком-секторе",
            "summary": "Демонстрационная новость. Информация не подтверждена.",
            "source_name": "Telegram",
            "source_tier": "TELEGRAM",
            "published_at_epoch_ms": now - 49 * m,
            "url": None,
            "is_demo": True,
            "is_confirmed": False,
        },
        {
            "id": "demo-4",
            "title": "Демо: рубль стабилен к основным валютам",
            "summary": "Демонстрационная новость. Данные не реальные.",
            "source_name": "Финансовое СМИ",
            "source_tier": "MEDIA",
            "published_at_epoch_ms": now - 71 * m,
            "url": None,
            "is_demo": True,
            "is_confirmed": True,
        },
        {
            "id": "demo-5",
            "title": "Демо: индекс МосБиржи в боковике",
            "summary": "Демонстрационная новость. Данные не реальные.",
            "source_name": "Мосбиржа",
            "source_tier": "OFFICIAL",
            "published_at_epoch_ms": now - 95 * m,
            "url": None,
            "is_demo": True,
            "is_confirmed": True,
        },
    ]


def demo_candles(symbol: str, interval_sec: int, limit: int = 48) -> list[dict[str, Any]]:
    """Детерминированные demo-свечи (random walk от базовой цены).

    Возвращает list свечей: {"t": epoch_ms, "o","h","l","c","v"} по возрастанию времени.
    Для неизвестных символов базовая цена выводится из хэша (детерминированно).
    """
    cfg = DEMO_BASE.get(symbol.upper())
    if cfg is None:
        seed_base = abs(hash(symbol.upper())) % 9000 + 100
        base = float(seed_base)
        volume_base = 1.0e6
        dec = 2
    else:
        base = cfg["base"]
        volume_base = cfg["volume"]
        dec = cfg["decimals"]
    now = int(time.time())
    seed = abs(hash(symbol)) % 10000
    candles: list[dict[str, Any]] = []
    price = base
    for i in range(limit):
        t = now - (limit - i) * interval_sec
        # псевдослучайный шаг, стабильный во времени
        step = math.sin(seed * 0.001 + i * 0.7) * 0.008 + math.cos(seed * 0.0009 + i * 0.31) * 0.005
        open_ = price
        close = max(0.01, price * (1 + step))
        high = max(open_, close) * (1 + abs(math.sin(i * 0.5 + seed)) * 0.006)
        low = min(open_, close) * (1 - abs(math.cos(i * 0.4 + seed)) * 0.006)
        volume = volume_base * (0.5 + 0.5 * abs(math.sin(i * 1.3 + seed)))
        candles.append({
            "t": t * 1000,
            "o": _round(open_, dec),
            "h": _round(high, dec),
            "l": _round(low, dec),
            "c": _round(close, dec),
            "v": round(volume, 2),
        })
        price = close
    return candles
