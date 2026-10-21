## 1. Переименование

- [x] 1.1 Установить «SpeechStudio» в Android `app_name`, iOS `CFBundleDisplayName`, шапке и `contentDescription` drawer; запасное обозначение — «SS».
- [x] 1.2 Переименовать `LogopedPlusApplication`, `LogopedPlusTheme`, `Theme.LogopedPlus`, `LogopedPlusApp` и Gradle rootProject; обновить импорт сгенерированных Compose Resources.
- [x] 1.3 Перенести `src/slt-mobile` → `src/speechstudio-mobile`, обновить пути в текущей документации и OpenSpec.
- [x] 1.4 Заменить `ic_launcher-playstore.png` и обе копии `icon1024.png` на `docs/logo.png`; проверить, что используемая графика не содержит старых надписей.
- [x] 1.5 Обновить текущие описания продукта, `app-identity`, `openspec/config.yaml` и verification; архивы оставить историческими.

## 2. Проверка

- [x] 2.1 `:app:assembleDebug` и проверка `application-label` в APK; `:app:testDebugUnitTest` (13) и `:shared:testAndroidHostTest` (37) без сбоев; drawer-ресурс в APK совпадает с новым логотипом; `git diff --check` и поиск остатков старого имени.
- [ ] 2.2 iOS: Xcode-сборка и визуальная проверка имени на macOS недоступны в текущей среде; конфигурация проверена статически.
