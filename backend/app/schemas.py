"""Схемы API SigmaFlux (контракт: docs/api-contract.md)."""

from typing import Any

from pydantic import BaseModel, Field


class QuoteOut(BaseModel):
    symbol: str
    name: str
    price: float
    prev_close: float
    day_open: float
    day_high: float
    day_low: float
    volume: float
    currency: str
    change_pct: float
    updated_at_epoch_ms: int
    is_demo: bool
    provider: str
    decimals: int = 2


class QuotesResponse(BaseModel):
    quotes: list[QuoteOut]
    is_demo: bool = Field(description="true, если все котировки демо")
    updated_at_epoch_ms: int


class NewsItemOut(BaseModel):
    id: str
    title: str
    summary: str
    source_name: str
    source_tier: str  # OFFICIAL | MEDIA | TELEGRAM
    published_at_epoch_ms: int
    url: str | None = None
    is_demo: bool
    is_confirmed: bool = Field(description="неподтверждённое показывается только на detail-экране")


class NewsResponse(BaseModel):
    news: list[NewsItemOut]
    is_demo: bool


class MarketStatusOut(BaseModel):
    market_status: str  # OPEN | CLOSED | UNKNOWN
    updated_at_epoch_ms: int
    is_demo: bool


class OverviewResponse(BaseModel):
    overview: MarketStatusOut


class AlertIn(BaseModel):
    symbol: str
    type: str  # PRICE_ABOVE | PRICE_BELOW | DROP_PCT_DAY | RISE_PCT_DAY | DROP_PCT_15M
    threshold: float
    label: str = ""


class AlertOut(AlertIn):
    id: str
    created_at_epoch_ms: int
    fired_at_epoch_ms: int | None = None
    active: bool = True


class AlertsResponse(BaseModel):
    alerts: list[AlertOut]


class InstrumentOut(BaseModel):
    symbol: str
    name: str
    exchange: str
    kind: str
    board: str | None = None
    currency: str
    isin: str | None = None
    decimals: int = 2


class HealthResponse(BaseModel):
    status: str
    version: str
    ai_enabled: bool = Field(description="в MVP всегда false: AI выключен")


class PortfolioDemoOut(BaseModel):
    name: str = "Демо-портфель"
    currency: str = "RUB"
    start_balance: float
    cash: float
    positions: list[dict[str, Any]] = Field(default_factory=list)
    is_demo: bool = True
