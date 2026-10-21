<img src="docs/icon.png" width="88" align="right" alt="Иконка Конспекта">

# Конспект

Нативное Android-приложение для AI-конспектов. Включаете запись перед парой — приложение слушает лекцию, убирает отступления и собирает структурированный конспект с формулами LaTeX и экспортом в PDF.

Всё работает локально: без аккаунта, облака и отправки аудио на сервер.

<p align="center">
  <img src="docs/screenshots/home.png" width="31%" alt="Главный экран">
  <img src="docs/screenshots/record.png" width="31%" alt="Запись лекции">
  <img src="docs/screenshots/note.png" width="31%" alt="Конспект с LaTeX">
</p>

## Возможности

- **Material You** — светлая и тёмная тема, четыре спокойные палитры и системный динамический цвет на Android 12+.
- **Запись и транскрипт** — нативный микрофон, системное офлайн-распознавание и живой транскрипт.
- **Только по делу** — локальный движок помечает отступления и не включает их в конспект.
- **Конспект с LaTeX** — определения, теоремы, формулы и ключевые термины в аккуратной вёрстке.
- **PDF** — экспорт в A4 с формулами; файл сохраняется в `Downloads/Конспект` и открывается в меню «Поделиться».
- **Предметы и библиотека** — курсы, преподаватели, аудитории, поиск и фильтры по конспектам.
- **Приватность** — нет разрешения `INTERNET`, резервные копии отключены, данные хранятся в приватном хранилище приложения.

В демонстрационном режиме доступна встроенная лекция, чтобы посмотреть весь сценарий без микрофона.

## Стек

Kotlin · Jetpack Compose · Material 3 · Navigation Compose · MediaRecorder / SpeechRecognizer · Compose LaTeX · Android PdfDocument · KaTeX.

Минимальная версия — Android 8.0 (API 26), целевая — Android 15. Сборка под Android SDK 36.

## Сборка

```bash
./gradlew :app:assembleDebug
```

Нужны JDK 21 и Android SDK 36. APK появится в `app/build/outputs/apk/debug/`.

## Релизы и подпись

При push тега `v*` GitHub Actions собирает **подписанный release APK** и создаёт GitHub Release:

```bash
git tag v1.0.0
git push origin v1.0.0
```

Workflow: `.github/workflows/release.yml`. В репозитории должны быть настроены секреты:

| Секрет | Назначение |
|---|---|
| `KEYSTORE_BASE64` | release-keystore в base64 (PKCS12) |
| `KEYSTORE_PASSWORD` | пароль хранилища |
| `KEY_ALIAS` | alias ключа |
| `KEY_PASSWORD` | пароль ключа |

Локальная подпись настраивается через `keystore.properties` (шаблон — `keystore.properties.example`). Сам keystore и пароли в git не попадают.

Отпечаток release-сертификата SHA-256: `70016433004fc2e48d8f59b47e46189fabdbc56a73c44e42e9005b43fe2a90b8`.

## Структура

```
app/src/main/java/ru/konspekt/app/
├── data/          модели, локальное хранилище, seed-данные
├── recording/     микрофон и офлайн-распознавание
├── pdf/           генерация PDF с KaTeX
└── ui/            Compose-экраны, тема, компоненты
```
