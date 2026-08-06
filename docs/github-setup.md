# GitHub / CI / Distribution — SigmaFlux

## Интеграция

Репозиторий: `https://github.com/vitlucky/sigmaflux.git`

Разработка ведётся на ветке `arena/019fd5b1-sigmaflux` (сессия Arena), изменения приходят в `main`
через Pull Request. Прямой push в `main` не выполняется.

## GitHub Actions

`.github/workflows/build.yml` — подготовлен (см. `uploads/build.yml` / нижний блок), но пока НЕ добавлен
в репозиторий: подключённый GitHub App в этой сессии не имеет права `workflows`
(`refusing to allow a GitHub App to create or update workflow ... without workflows permission`).
Добавление файла — обычный коммит, как только у интеграции появится право `workflows`
(настройка: GitHub → Settings → Apps → разрешения интеграции → Workflows: Read and write).

Workflow (когда будет добавлен):
- **android**: ubuntu-latest, JDK 17, `./gradlew :app:assembleDebug`, артефакт APK;
- **backend**: Python 3.11, `pip install -r requirements.txt`, `py_compile` + smoke-тесты (TestClient).

Gradle Wrapper включён в репозиторий (`gradle/wrapper/`), поэтому сборка не требует установки Gradle.

## Секреты (GitHub Secrets) — для релизной подписи

| Секрет | Назначение |
|--------|------------|
| `KEYSTORE_BASE64` | подписанный keystore (base64) — никогда в Git |
| `KEYSTORE_PASSWORD` | пароль keystore |
| `KEY_ALIAS` | алиас ключа |
| `KEY_PASSWORD` | пароль ключа |

`keystore.properties`, `*.jks`, `*.keystore` — в `.gitignore`. Ключи нельзя класть в APK/Git.

## Distribution

1. GitHub Releases + подписанный APK для ранних тестов (workflow `release` — добавить при первом релизе);
2. Google Play / closed testing;
3. Автоматический build через GitHub Actions.

## Локальная сборка APK

Требуется JDK 17 + Android SDK (в песочнице Arena SDK отсутствует — сборка идёт через CI).
Для физического устройства адрес backend задаётся в `app/build.gradle.kts`
(`buildConfigField("String", "BACKEND_BASE_URL", ...)`), для эмулятора — `http://10.0.2.2:8000/`.
