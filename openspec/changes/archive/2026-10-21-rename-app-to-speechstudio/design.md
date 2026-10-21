## Context

Мотивация и границы — в [proposal.md](proposal.md); контракт — в [app-identity](specs/app-identity/spec.md). Предыдущее переименование в «Кабинет речи» зафиксировано в [архиве](../archive/2026-10-21-rename-app-to-kabinet-rechi/design.md).

Найденная цепочка: Android `AndroidManifest.xml` → `@string/app_name` → `strings.xml`; iOS `Info.plist` → `CFBundleDisplayName`; общий `App` → `AppDrawer`; тема `SpeechStudioTheme` → `MainActivity` и `MainViewController`; `settings.gradle.kts` → сгенерированный пакет Compose Resources.

## Goals / Non-Goals

**Goals:** единое отображаемое название SpeechStudio, отсутствие старых надписей в используемой графике, рабочая сборка после переноса каталога, сохранение установки и данных.

**Non-Goals:** смена package, `applicationId`, iOS bundle id, схемы БД, ключей настроек и подписи; публикация в магазинах.

## Decisions

1. **Сохранить технические идентификаторы.** `com.logoped_plus`, `com.logopedplus.iosApp`, package, `children.db` и ключи настроек остаются прежними: это сохраняет идентичность установки и доступ к данным. Меняются только отображаемые имена и производные от названия символы.
2. **Переименовать символы и rootProject.** `LogopedPlusApplication` → `SpeechStudioApplication`, `LogopedPlusTheme` → `SpeechStudioTheme`, `Theme.LogopedPlus` → `Theme.SpeechStudio`, `LogopedPlusApp` → `SpeechStudioApp`, `rootProject.name` → `SpeechStudio`. Пакет Compose Resources, производный от rootProject, становится `speechstudio.shared.generated.resources`.
3. **Каталог проекта — `src/speechstudio-mobile`.** Перенос выполняется `git mv`, относительные ссылки внутри модуля не меняются; пути в текущей документации обновляются, включая ссылки OpenSpec.
4. **Графика — `docs/logo.png`.** Файл заменяет `ic_launcher-playstore.png` и обе копии `icon1024.png` (Android drawable и Compose Resources), потому что прежний растр содержит надпись «Логопед+». Launcher-WebP уже не содержат старого текста и сохраняются.
5. **Документация.** Текущие описания переводятся на SpeechStudio; архивы и прежние отчёты не переписываются, кроме путей к перенесённому каталогу. Verification получает новый раздел.

## Risks / Trade-offs

- [Старое имя остаётся в технических идентификаторах] → это осознанно: смена сломала бы обновление и потребовала бы миграции данных.
- [Перенос каталога ломает ссылки] → проверка поиском и `git diff --check`.
- [Графика выглядит иначе] → используется согласованный логотип без текста; drawer и Play Store используют один источник.

## Migration Plan

Обычное обновление с прежними идентификаторами и подписью. Миграции данных не нужны. Откат — обратная замена имён и графики без очистки данных.

## Verification

Минимальные проверки: `:app:assembleDebug`, `:app:testDebugUnitTest`, `:shared:testAndroidHostTest`, проверка `application-label` в собранном APK, побайтное совпадение drawer-ресурса в APK с новым логотипом, поиск остатков `LogopedPlus`/`slt-mobile`, `git diff --check`. iOS-сборка на Windows недоступна; `Info.plist` проверяется статически.
