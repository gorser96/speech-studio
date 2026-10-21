## Why

Продукт переименовывается в **SpeechStudio**, а каталог мобильного проекта — в `speechstudio-mobile`. Прежние пользовательские названия «Логопед+» и «Кабинет речи» больше не используются, поэтому нужно обновить системные метки, общий интерфейс, производные имена символов и графику.

## What Changes

- Установить отображаемое название «SpeechStudio» на Android и iOS и в шапке бокового меню.
- Заменить в коде имена, производные от `LogopedPlus`, на `SpeechStudio`, включая Gradle rootProject, Application, Compose-тему, Android-стиль и iOS `App`.
- Переименовать каталог `src/slt-mobile` в `src/speechstudio-mobile` и обновить ссылки в документации.
- Заменить логотип и иконку магазина на `docs/logo.png` без старой надписи.
- Актуализировать текущую продуктовую документацию и контракт `app-identity`. Архивы изменений сохраняют исторические названия.

## Capabilities

### New Capabilities

Нет.

### Modified Capabilities

- `app-identity`: название приложения и запасное обозначение меняются на «SpeechStudio» и «SS»; технические идентификаторы установки сохраняются.

## Impact

Android: `strings.xml`, манифест, `themes.xml`, `SpeechStudioApplication`, `SpeechStudioTheme`, launcher-графика. iOS: `Info.plist`, `SpeechStudioApp`. Общий код: `AppDrawer`, `App.kt`, Compose Resources. Сборка: `settings.gradle.kts`, сгенерированный пакет ресурсов `speechstudio.shared.generated.resources`. Документация и карты пакетов.

`applicationId`/`namespace` `com.logoped_plus`, iOS bundle id `com.logopedplus.iosApp`, package, файл `children.db` и ключи настроек не меняются, чтобы обновление сохраняло данные.
