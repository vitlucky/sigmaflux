# SigmaFlux Backend

FastAPI-скелет для Android-приложения SigmaFlux. Работает без API-ключей: `AI_ENABLED=false`,
при недоступности провайдеров отдаёт честно помеченные demo-данные (`is_demo=true`).

## Запуск

```bash
python3 -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

## Эндпоинты (v1)

| Метод | Путь | Описание |
|-------|------|----------|
| GET | `/health` | Статус, версия, `ai_enabled` |
| GET | `/v1/market/overview` | Статус рынка (торговое время) |
| GET | `/v1/market/quotes?symbols=IMOEX,RTSI,BTC` | Котировки |
| GET | `/v1/news?limit=20` | Лента новостей с тирами источников |
| GET/POST/DELETE | `/v1/alerts` | Алерты (local state — основной слой на устройстве) |
| GET | `/v1/portfolio/demo` | Демо-портфель |
| GET | `/v1/instruments` | Каталог инструментов (MOEX + крипто) |

Контракт: `docs/api-contract.md` в корне репозитория.

## Провайдеры

- `providers/moex_iss.py` — MOEX ISS (реальный HTTP → demo fallback).
- `providers/ccxt_crypto.py` — CCXT async (ticker/OHLCV → demo fallback).
- `providers/demo_data.py` — детерминированные demo-значения (не выдаются за real-time).

## Правила

- Никаких API-ключей в коде. Ключи — только env/GitHub Secrets.
- Числовые показатели — детерминированные (никакого AI в MVP).
- 429/ошибки провайдера: ответ помечается, UI показывает «Источник временно ограничил запросы…».
