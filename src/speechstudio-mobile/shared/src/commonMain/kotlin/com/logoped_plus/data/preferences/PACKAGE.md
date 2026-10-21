# com.logoped_plus.data.preferences

Настройки приложения: тумблер «Скрывать выходные», диапазон часов сетки и число показываемых тэгов. [Общий указатель](../../../../../../../../PACKAGES.md).

| Файл | Назначение |
|---|---|
| [SettingsRepository.kt](SettingsRepository.kt) | Общий контракт: `hideWeekend: StateFlow<Boolean>`, `gridStartHour`/`gridEndHour`/`visibleTagsLimit: StateFlow<Int>` и setters. `SettingsKeys` хранит прежние ключи `hide_weekend` (дефолт `false`), `grid_start_hour` (дефолт `0`), `grid_end_hour` (дефолт `24`), `visible_tags_limit` (дефолт `3`) и имя файла `settings` |
| [SettingsRepository.android.kt](../../../../../../androidMain/kotlin/com/logoped_plus/data/preferences/SettingsRepository.android.kt) | `AndroidSettingsRepository` на SharedPreferences: прежний файл `settings` и прежние ключи, поэтому существующие установки читаются без миграции |
| [SettingsRepository.ios.kt](../../../../../../iosMain/kotlin/com/logoped_plus/data/preferences/SettingsRepository.ios.kt) | `IosSettingsRepository` на `NSUserDefaults` с теми же ключами и значениями по умолчанию |

Создаётся платформенной оболочкой: Android — `createSettingsRepository(context)` в [AppContainer](../../../../../../../../app/src/main/java/com/logoped_plus/AppContainer.kt); iOS — `createSettingsRepository()` в [IosAppContainer](../../../../../../../../shared/src/iosMain/kotlin/com/logoped_plus/ios/IosAppContainer.kt). Потребители — [экран настроек](../../ui/screen/PACKAGE.md), [расписание](../../ui/screen/schedule/PACKAGE.md) (подписка на диапазон часов сетки и число тэгов) и [дети](../../ui/screen/children/PACKAGE.md) (подписка на число тэгов). Не Repository и не Use Case: одиночное значение не оправдывает предметный контракт ([ARCHITECTURE.md](../../../../../../../../ARCHITECTURE.md), §25). DataStore не введён.

## Проверки

- [AndroidSettingsRepositoryTest.kt](../../../../../../../../app/src/test/java/com/logoped_plus/data/preferences/AndroidSettingsRepositoryTest.kt) — Robolectric: чтение прежних ключей из файла `settings` и сохранение изменений обратно в тот же файл.
- [IosSettingsRepositoryTest.kt](../../../../../../iosTest/kotlin/com/logoped_plus/data/preferences/IosSettingsRepositoryTest.kt) — iOS: сохранённые значения `NSUserDefaults` читаются новым экземпляром репозитория после `synchronize()`.
- iOS-ветка использует те же ключи; сохранение проверено `IosSettingsRepositoryTest`. Ручной UI-сценарий задачи 5.2 закрыт без фактической проверки — [статус проверок](../../../../../../../../../../openspec/verification/README.md).
