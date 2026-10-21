<img src="docs/icon.png" width="80" align="right" alt="Conspect">

# Conspect

Нативное Android-приложение для конспектов. Записываете лекцию — Conspect распознаёт речь, убирает отступления и собирает конспект с формулами LaTeX и PDF.

**[Скачать подписанный APK](https://github.com/skibidius/conspect/releases/latest)**

<p align="center">
  <img src="docs/screenshots/home.png" width="31%" alt="Главная">
  <img src="docs/screenshots/record.png" width="31%" alt="Запись">
  <img src="docs/screenshots/note.png" width="31%" alt="Конспект">
</p>

## Что умеет

- Запись и распознавание речи — на устройстве (Android 12+) или через системный распознаватель с офлайн-пакетом.
- Локальная обработка по умолчанию, без сети. Опционально — DeepSeek по API-ключу (ключ шифруется).
- Конспекты с LaTeX-формулами, экспорт в PDF A4, поиск, предметы и библиотека.

Приложение стартует пустым: добавьте предметы и записывайте лекции.

## Сборка

```bash
./gradlew :app:assembleDebug
```

JDK 21, Android SDK 36.

## Релиз

Тег `v*` собирает подписанный APK через GitHub Actions и публикует релиз. Подпись — release-ключ, секреты описаны в workflow.
