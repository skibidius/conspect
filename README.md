<img src="docs/icon.png" width="88" align="right" alt="Иконка Conspect">

# Conspect

Conspect — нативное Android-приложение для конспектов: включаете запись перед парой, приложение слушает лекцию, отбрасывает отступления и собирает структурированный конспект с формулами LaTeX и PDF.

По умолчанию всё обрабатывается на устройстве. При желании можно подключить DeepSeek по API-ключу — тогда текст лекции будет отправлен в облако для более качественного структурирования.

**[Скачать подписанный APK](https://github.com/skibidius/conspect/releases/latest)**

<p align="center">
  <img src="docs/screenshots/home.png" width="31%" alt="Главный экран Conspect">
  <img src="docs/screenshots/record.png" width="31%" alt="Запись лекции">
  <img src="docs/screenshots/note.png" width="31%" alt="Conspect с LaTeX">
</p>

## Как работает

- **Запись** — системное офлайн-распознавание речи на русском, живой транскрипт и таймер.
- **Локальный режим** — фильтрация отступлений и структурирование выполняются на телефоне. Интернет не нужен, аудио не сохраняется.
- **DeepSeek** — необязательный режим: ключ шифруется через Android Keystore, в облако уходит только текст транскрипта. При ошибке конспект автоматически собирается локально.
- **LaTeX** — определения, теоремы, примеры и формулы рендерятся нативно.
- **PDF** — A4 с формулами и полями; файл сохраняется в `Downloads/Conspect` и открывается в меню «Поделиться».
- **Материалы** — предметы, преподаватели, расписание, библиотека, поиск и настройки.

Для офлайн-распознавания на телефоне должен быть установлен русский офлайн-пакет в настройках распознавания речи Android.

В debug-сборке есть демонстрационная лекция для проверки сценария без микрофона. В release она недоступна.

## Дизайн

Строгий Material 3 / Pixel: системная динамическая палитра Android 12+, стандартные `TopAppBar`, `NavigationBar`, FAB, segmented buttons, карточки и корректные отступы под status bar и gesture bar. Есть светлая и тёмная темы и четыре резервные палитры.

Стек: Kotlin · Jetpack Compose · Material 3 · Navigation Compose · SpeechRecognizer · DeepSeek API (опционально) · Compose LaTeX · Android PdfDocument · KaTeX.

Минимальная версия — Android 8.0 (API 26), target SDK 35, compile SDK 36.

## Сборка

```bash
./gradlew :app:assembleDebug
```

Нужны JDK 21 и Android SDK 36. APK появится в `app/build/outputs/apk/debug/`.

## Релизы и подпись

При push тега `v*` GitHub Actions собирает **подписанный release APK** и создаёт GitHub Release:

```bash
git tag v1.1.0
git push origin v1.1.0
```

Workflow: `.github/workflows/release.yml`. Секреты репозитория:

| Секрет | Назначение |
|---|---|
| `KEYSTORE_BASE64` | release-keystore в base64 (PKCS12) |
| `KEYSTORE_PASSWORD` | пароль хранилища |
| `KEY_ALIAS` | alias ключа |
| `KEY_PASSWORD` | пароль ключа |

Локальная подпись — через `keystore.properties` (шаблон `keystore.properties.example`). Keystore и пароли в git не попадают.

Отпечаток release-сертификата SHA-256: `70016433004fc2e48d8f59b47e46189fabdbc56a73c44e42e9005b43fe2a90b8`.

## Структура

```
app/src/main/java/ru/konspekt/app/
├── data/          модели, хранилище, локальный структуратор, DeepSeek
├── recording/     офлайн-распознавание речи
├── pdf/           PDF с KaTeX
└── ui/            Compose-экраны и тема
```
