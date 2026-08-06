"""Smoke-тесты API (без внешних ключей; demo-fallback)."""

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
