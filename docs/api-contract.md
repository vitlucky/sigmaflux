# API Contract — SigmaFlux

Версия: 0.1.0 (MVP). Base URL (Android-эмулятор): `http://10.0.2.2:8000/`.
Все ответы — JSON. Демо-данные честно помечаются `is_demo=true`; приложение никогда не выдаёт их за real-time.

## Общие поля ошибок

| Код | Значение | Что показывает UI |
|-----|----------|-------------------|
| 429 | rate limit провайдера | «Источник временно ограничил запросы. Показываем сохранённые данные.» |
| 5xx / сеть | недоступность | offline-кэш, при отсутствии — demo с меткой |
| 4xx (клиент) | неверный запрос | сообщение об ошибке |

HTTP 429 НЕ показывается пользователю как «429» — только дружелюбная фраза выше.

## Endpoints

### GET /health
```json
{ "status": "ok", "version": "0.1.0", "ai_enabled": false }
```

### GET /v1/market/quotes?symbols=IMOEX,RTSI,BTC
```json
{
  "quotes": [
    {
      "symbol": "IMOEX",
      "name": "Индекс МосБиржи",
      "price": 3100.5,
      "prev_close": 3090.0,
      "day_open": 3095.0,
      "day_high": 3112.0,
      "day_low": 3088.0,
      "volume": 14000000000.0,
      "currency": "RUB",
      "change_pct": 0.34,
      "updated_at_epoch_ms": 1720000000000,
      "is_demo": false,
      "provider": "moex_iss",
      "decimals": 1
    }
  ],
  "is_demo": false,
  "updated_at_epoch_ms": 1720000000000
}
```

### GET /v1/news?limit=20
```json
{
  "news": [
    {
      "id": "demo-1",
      "title": "…",
      "summary": "…",
      "source_name": "ЦБ РФ",
      "source_tier": "OFFICIAL",   // OFFICIAL | MEDIA | TELEGRAM
      "published_at_epoch_ms": 1720000000000,
      "url": null,
      "is_demo": true,
      "is_confirmed": true
    }
  ],
  "is_demo": true
}
```

### GET /v1/market/overview
```json
{ "overview": { "market_status": "OPEN", "updated_at_epoch_ms": 0, "is_demo": true } }
```

### GET/POST/DELETE /v1/alerts
В MVP алерты — локальное состояние на устройстве (DataStore). Backend-эндпоинты — заглушки для будущей синхронизации аккаунта.

### GET /v1/instruments
Каталог: `symbol`, `name`, `exchange` (MOEX|CRYPTO), `kind` (INDEX|STOCK|BOND|CURRENCY|CRYPTO), `board`, `currency`, `decimals`.

### GET /v1/ai
```json
{ "enabled": false, "note": "AI подключится позже и только через backend." }
```

## Принципы

- Числовые показатели и сигналы — детерминированные (без AI).
- API-ключи — только backend secrets / GitHub Secrets, никогда в APK/Git.
- Крипто через CCXT: источник — биржа (коннектор не является вендором данных).
