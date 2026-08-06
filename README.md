# SigmaFlux

Мобильный аналитический терминал для российского рынка (MOEX) и криптоактивов (CCXT).

**Sigma** = агрегирование, статистика, риск. **Flux** = поток и движение рынка.

> ⚠️ SigmaFlux не обещает доходность и не является индивидуальной инвестиционной рекомендацией.
> Все данные в прототипе помечаются как demo до подключения реальных провайдеров.

## Статус

Прототип (MVP-скелетон): Android-приложение (Kotlin + Jetpack Compose + Material 3, Graphite Premium theme)
и backend-скелет (FastAPI + провайдеры MOEX ISS / CCXT). AI выключен: `AI_ENABLED=false`.

## Структура

```
app/                          # Android-приложение (com.sigmaflux.market)
  src/main/java/com/sigmaflux/market/
    MainActivity.kt           # scaffold, 5 вкладок, deep links, скрытие bottom bar
    WatchlistWidget.kt        # Glance widget 4x2
    data/                     # DataStore, репозитории, сеть, модели
    ui/                       # экраны и компоненты (Graphite theme)
backend/                      # FastAPI + провайдеры данных
docs/                         # контракты, дизайн, CI, алармы и портфель
.github/workflows/            # GitHub Actions (build APK, проверка backend)
```

## Быстрый старт

```bash
# Backend (без API-ключей, demo-фолбэк)
cd backend
python3 -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000

# Android (нужны JDK 17 + Android SDK)
./gradlew :app:assembleDebug
```

## Ключевые решения

- Kotlin, Jetpack Compose, Material 3, minSdk 28 (Android 9–11 и новее).
- Bottom navigation: Главная | Новости | Рынок | Портфель | Ещё.
- Graphite Premium: фон `#16161C`, акцент `#B7A1FF`, mint `#87D5B6`, rose `#E9A0A0`, amber `#D8B875`.
- Новости: тихая строка метаданных с цветной точкой источника; «Неподтверждено» — только на detail.
- Сигналы — детерминированные (ATR, волатильность, объём). AI в MVP выключен.
- Ключи API никогда не попадают в APK или Git — только backend secrets / GitHub Secrets.
- Демо-портфель: market/limit заявки, комиссия, проскальзывание, P&L. Всё помечено simulation/demo.

Подробнее: `docs/` (api-contract.md, visual-design.md, alerts-and-portfolio.md, github-setup.md, instruments.md).
