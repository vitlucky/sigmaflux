"""Провайдер MOEX ISS (реальный HTTP → demo fallback).

Mapping symbol → board:
- Индексы: IMOEX, RTSI, RGBI (engine=stock, market=index, board=SNDX)
- Акции:   TQBR (engine=stock, market=shares)
- Облигации: TQCB (engine=stock, market=bonds)
- Валюты:  CETS (engine=currency, market=selt)
Документация mapping: docs/instruments.md
"""

import time
from datetime import datetime, timedelta, timezone
from typing import Any

import requests

from .base import BaseProvider, ProviderError
from .demo_data import demo_candles, demo_quote

ISS_BASE = "https://iss.moex.com/iss"


def iso_epoch_ms(value: str) -> int:
    """'2026-08-06 10:00:00' (MSK) → epoch ms (UTC)."""
    dt = datetime.strptime(value, "%Y-%m-%d %H:%M:%S")
    # ISS отдаёт время в московском часовом поясе
    dt_msk = dt.replace(tzinfo=timezone(timedelta(hours=3)))
    return int(dt_msk.timestamp() * 1000)

# symbol -> (engine, market, board)
MOEX_BOARDS: dict[str, tuple[str, str, str]] = {
    "IMOEX": ("stock", "index", "SNDX"),
    "RTSI": ("stock", "index", "SNDX"),
    "RGBI": ("stock", "index", "SNDX"),
    "SBER": ("stock", "shares", "TQBR"),
    "GAZP": ("stock", "shares", "TQBR"),
    "LKOH": ("stock", "shares", "TQBR"),
    "ROSN": ("stock", "shares", "TQBR"),
    "VTBR": ("stock", "shares", "TQBR"),
    "NVTK": ("stock", "shares", "TQBR"),
    "PLZL": ("stock", "shares", "TQBR"),
    "MGNT": ("stock", "shares", "TQBR"),
    "OFZ26240": ("stock", "bonds", "TQCB"),
    "USDRUB": ("currency", "selt", "CETS"),
    "EURRUB": ("currency", "selt", "CETS"),
}


class MoexIssProvider(BaseProvider):
    name = "moex_iss"

    def __init__(self, timeout: float = 5.0):
        self.timeout = timeout
        self.session = requests.Session()
        self.session.headers.update({"User-Agent": "SigmaFlux/0.1 (+research)"})

    def _request(self, url: str, params: dict[str, Any]) -> dict[str, Any]:
        try:
            resp = self.session.get(url, params=params, timeout=self.timeout)
            if resp.status_code == 429:
                raise ProviderError("rate_limited")
            resp.raise_for_status()
            return resp.json()
        except requests.RequestException as exc:
            raise ProviderError(f"moex_iss unreachable: {exc}") from exc

    def fetch_quote(self, symbol: str) -> dict[str, Any]:
        symbol = symbol.upper()
        board = MOEX_BOARDS.get(symbol)
        if board is None:
            # неизвестный символ — честный demo fallback
            return demo_quote(symbol, provider="demo")
        engine, market, _board = board
        try:
            data = self._request(
                f"{ISS_BASE}/engines/{engine}/markets/{market}/securities.json",
                params={"iss.meta": "off", "iss.only": "marketdata", "marketdata.columns": "SECID,LAST,OPEN,HIGH,LOW,VALTODAY"},
            )
            rows = (data.get("marketdata") or {}).get("data") or []
            for row in rows:
                if row[0] == symbol:
                    last, open_, high, low = row[1], row[2], row[3], row[4]
                    if last is None or last == 0:
                        raise ProviderError("no last price")
                    cfg = demo_quote(symbol, provider=self.name)  # для имени/валюты (стабильно)
                    change = (last - cfg["prev_close"]) / cfg["prev_close"] * 100 if cfg["prev_close"] else 0.0
                    return {
                        "symbol": symbol,
                        "name": cfg["name"],
                        "price": round(last, cfg["decimals"]),
                        "prev_close": cfg["prev_close"],
                        "day_open": open_ or cfg["prev_close"],
                        "day_high": high or last,
                        "day_low": low or last,
                        "volume": row[5] or 0.0,
                        "currency": cfg["currency"],
                        "change_pct": round(change, 2),
                        "updated_at_epoch_ms": int(time.time() * 1000),
                        "is_demo": False,
                        "provider": self.name,
                        "decimals": cfg["decimals"],
                    }
            raise ProviderError(f"symbol {symbol} not found")
        except ProviderError:
            raise
        except Exception as exc:  # любой сбой — честный demo fallback
            return demo_quote(symbol, provider="demo")

    def fetch_candles(self, symbol: str, interval_sec: int = 3600, limit: int = 48) -> dict[str, Any]:
        """Свечи MOEX ISS (interval: 60|300|900|3600|86400) → demo fallback.

        Возвращает {"candles": [...], "is_demo": bool}.
        """
        symbol = symbol.upper()
        board = MOEX_BOARDS.get(symbol)
        if board is None:
            return {"candles": demo_candles(symbol, interval_sec, limit), "is_demo": True}
        engine, market, _board = board
        # ISS принимает 'from'; берём окно с запасом на выходные
        window_days = max(1, (limit * interval_sec) // 86400 + 2)
        from_date = (datetime.now(timezone.utc) - timedelta(days=window_days)).strftime("%Y-%m-%d")
        try:
            data = self._request(
                f"{ISS_BASE}/engines/{engine}/markets/{market}/securities/{symbol}/candles.json",
                params={
                    "iss.meta": "off",
                    "from": from_date,
                    "interval": str(interval_sec),
                    "limit": str(min(limit, 200)),
                },
            )
            columns = (data.get("candles") or {}).get("columns") or []
            rows = (data.get("candles") or {}).get("data") or []
            idx = {name: i for i, name in enumerate(columns)}
            out: list[dict[str, Any]] = []
            for row in rows[-limit:]:
                out.append({
                    "t": int(iso_epoch_ms(row[idx.get("begin", 0)])) if idx.get("begin") is not None else 0,
                    "o": row[idx.get("open", 1)],
                    "h": row[idx.get("high", 3)],
                    "l": row[idx.get("low", 4)],
                    "c": row[idx.get("close", 2)],
                    "v": row[idx.get("volume", 6)] or 0.0,
                })
            if not out:
                return {"candles": demo_candles(symbol, interval_sec, limit), "is_demo": True}
            return {"candles": out, "is_demo": False}
        except Exception:
            return {"candles": demo_candles(symbol, interval_sec, limit), "is_demo": True}
