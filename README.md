<img src="docs/icon.png" width="80" align="right" alt="Conspect">

# Conspect

Нативное Android-приложение для конспектов. Записываете лекцию — Conspect распознаёт речь, убирает отступления и собирает конспект с формулами LaTeX и PDF.

**[Скачать APK](https://github.com/skibidius/conspect/releases/latest)** · **[Сайт](https://skibidius.github.io/conspect/)**

<p align="center">
  <img src="docs/screenshots/home.png" width="31%" alt="Главная">
  <img src="docs/screenshots/record.png" width="31%" alt="Запись">
  <img src="docs/screenshots/note.png" width="31%" alt="Конспект">
</p>

## Что умеет

- Распознавание речи: Vosk офлайн (две модели на выбор) или системный движок Android.
- Локальная обработка по умолчанию. Опционально — DeepSeek Flash по API-ключу (ключ шифруется).
- Конспекты с LaTeX-формулами, экспорт в PDF A4, поиск, предметы и библиотека.
- 18 иконок для предметов и свободный выбор цветов.
- Корзина: конспект удаляется долгим нажатием, его можно вернуть или стереть навсегда.
- Удаление предметов долгим нажатием с подтверждением.

Приложение стартует пустым: добавьте предметы и записывайте лекции.

## Сборка

```bash
./gradlew :app:assembleDebug
```

JDK 21, Android SDK 36.

Материалы для RuStore (иконка, скриншоты 16:9 и тексты карточки): `docs/rustore/`.
