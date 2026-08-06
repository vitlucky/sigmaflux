"""Smoke-тесты API (без внешних ключей; demo-fallback, сеть не требуется)."""

from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_health():
    r = client.get("/health")
    assert r.status_code == 200
    body = r.json()
    assert body["status"] == "ok"
    assert body["ai_enabled"] is False


def test_quotes_demo_or_real():
    r = client.get("/v1/market/quotes", params={"symbols": "IMOEX,RTSI,BTC"})
    assert r.status_code == 200
    body = r.json()
    assert len(body["quotes"]) == 3
    for q in body["quotes"]:
        assert q["symbol"] in {"IMOEX", "RTSI", "BTC"}
        assert q["price"] > 0
        assert "is_demo" in q
        assert q["updated_at_epoch_ms"] > 0


def test_news():
    r = client.get("/v1/news", params={"limit": 5})
    assert r.status_code == 200
    news = r.json()["news"]
    assert len(news) == 5
    tiers = {n["source_tier"] for n in news}
    assert tiers <= {"OFFICIAL", "MEDIA", "TELEGRAM"}


def test_overview():
    r = client.get("/v1/market/overview")
    assert r.status_code == 200
    assert r.json()["overview"]["market_status"] in {"OPEN", "CLOSED"}


def test_instruments():
    r = client.get("/v1/instruments")
    assert r.status_code == 200
    assert any(i["symbol"] == "IMOEX" for i in r.json())


def test_ai_disabled():
    r = client.get("/v1/ai")
    assert r.json()["enabled"] is False


def test_candles_default():
    """Свечи: 48 баров, валидные OHLC, отметки времени по возрастанию."""
    r = client.get("/v1/market/candles", params={"symbol": "IMOEX", "interval": 3600, "limit": 48})
    assert r.status_code == 200
    body = r.json()
    assert body["symbol"] == "IMOEX"
    assert len(body["candles"]) == 48
    ts = [c["t"] for c in body["candles"]]
    assert ts == sorted(ts)
    for c in body["candles"]:
        assert c["l"] <= c["o"] <= c["h"]
        assert c["l"] <= c["c"] <= c["h"]
        assert c["v"] >= 0


def test_candles_bad_interval():
    r = client.get("/v1/market/candles", params={"symbol": "IMOEX", "interval": 12345})
    assert r.status_code == 422


def test_candles_crypto():
    r = client.get("/v1/market/candles", params={"symbol": "BTC", "interval": 900, "limit": 20})
    assert r.status_code == 200
    assert len(r.json()["candles"]) == 20


def test_candles_unknown_symbol_demo():
    r = client.get("/v1/market/candles", params={"symbol": "UNKNOWN_SYM", "interval": 3600, "limit": 10})
    assert r.status_code == 200
    body = r.json()
    assert body["is_demo"] is True
    assert len(body["candles"]) == 10


def test_demo_candles_consistency():
    """Свечи из demo_data: OHLC консистентны для всех символов."""
    from app.providers.demo_data import demo_candles

    for symbol in ("IMOEX", "SBER", "USDRUB", "BTC", "VTBR"):
        candles = demo_candles(symbol, 3600, 30)
        assert len(candles) == 30
        for c in candles:
            assert c["l"] <= c["o"] <= c["h"]
            assert c["l"] <= c["c"] <= c["h"]
            assert c["t"] > 0
