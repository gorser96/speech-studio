# com.logoped_plus.platform.video

Общий контракт выбора и просмотра видео и его платформенные адаптеры: Android SAF/`ACTION_VIEW`, iOS document picker/security-scoped bookmark/системный просмотр. [Общий указатель](../../../../../../../../PACKAGES.md).

| Файл | Назначение |
|---|---|
| [VideoEnvironment.kt](VideoEnvironment.kt) | `VideoPickResult`, `VideoAccessError`, `VideoPicker`, `VideoAccess`, `VideoCapabilities(deviceScan)` и `expect @Composable rememberVideoEnvironment()` |
| [VideoEnvironment.android.kt](../../../../../../androidMain/kotlin/com/logoped_plus/platform/video/VideoEnvironment.android.kt) | SAF `OpenMultipleDocuments`, persistable URI permission, `READ_MEDIA_VIDEO` через `RequestPermission`, имя через `OpenableColumns`, просмотр через `ACTION_VIEW`; ошибки типизированы |
| [VideoEnvironment.ios.kt](../../../../../../iosMain/kotlin/com/logoped_plus/platform/video/VideoEnvironment.ios.kt) | `UIDocumentPickerViewController` для `UTType.movie` (несколько файлов, `asCopy = false`), Base64 security-scoped bookmark, имя из пути, `UIDocumentInteractionController` (preview, затем open-in) с освобождением доступа; `deviceScan = false`, `requestDeviceScanAccess` сразу false |

Потребители — [VideoAttachmentsEditor](../../ui/screen/schedule/component/PACKAGE.md) и [VideoAttachmentLink](../../ui/screen/schedule/component/PACKAGE.md). Автопоиск по устройству показывается только при `capabilities.deviceScan = true`; на iOS он скрыт. Ссылка хранится строкой: Android — `content://`, iOS — Base64 bookmark; между платформами ссылки не переносятся. Файл не копируется и не удаляется, открепление удаляет только запись в БД.

## Проверки

- `FindLessonVideosUseCaseTest` (4 теста) входит в `:shared:testAndroidHostTest` — 37 тестов без сбоев; Android-ветка адаптера проверяется сборкой `:app:assembleDebug`.
- iOS: klib компилируется (`:shared:compileKotlinIosArm64`, `:shared:compileKotlinIosSimulatorArm64`); системный выбор, bookmark после перезапуска, просмотр и недоступный файл проверяются только на macOS/устройстве и остаются открытыми.
- Android: ручная приёмка выбора, сохранения grant после перезапуска и открытия системным проигрывателем.
