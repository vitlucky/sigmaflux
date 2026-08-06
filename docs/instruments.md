# MOEX ISS: символы и board mapping

Дизайн маппинга для провайдера `moex_iss` и каталога Android (`InstrumentCatalog`).

## Правила

- **Индексы** (`engine=stock, market=index, board=SNDX`): IMOEX, RTSI, RGBI.
- **Акции** (`engine=stock, market=shares, board=TQBR`): SBER, GAZP, LKOH, ROSN, VTBR, NVTK, PLZL, MGNT.
- **Облигации** (`engine=stock, market=bonds, board=TQCB`): OFZ26240.
- **Валюты** (`engine=currency, market=selt, board=CETS`): USDRUB, EURRUB.
- **Крипто**: не MOEX; идёт через CCXT (Binance public): BTC/USDT, ETH/USDT, SOL/USDT.

## Запрос ISS

```
GET https://iss.moex.com/iss/engines/{engine}/markets/{market}/securities.json
    ?iss.meta=off&iss.only=marketdata
    &marketdata.columns=SECID,LAST,OPEN,HIGH,LOW,VALTODAY
```

Строка с `SECID == symbol` даёт `LAST` (последняя), `OPEN`, `HIGH`, `LOW`, `VALTODAY` (объём в деньгах).

## Fallback

Любая ошибка/429/недоступность → детерминированные demo-данные с `is_demo=true` и `provider="demo"`.
Нет данных по символу → 404-эквивалент на уровне списка (символ пропускается) или demo.

## Расширяемость

Зарубежные акции/ETF не входят в первую версию. Для добавления нового инструмента:
1. добавить запись в `InstrumentCatalog` (Android) и `DEMO_BASE` (backend);
2. добавить board mapping в `MOEX_BOARDS` (backend);
3. обновить `docs/api-contract.md` при изменении формата.
