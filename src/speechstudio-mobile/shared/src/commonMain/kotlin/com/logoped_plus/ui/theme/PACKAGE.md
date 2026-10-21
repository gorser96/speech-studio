# com.logoped_plus.ui.theme

Тема Material 3 для Compose. [Общий указатель](../../../../../../../../PACKAGES.md).

| Файл / символ | Ответственность |
|---|---|
| [Theme.kt](Theme.kt) — `SpeechStudioTheme` | Выбор динамической, тёмной или светлой схемы и передача `Typography` в `MaterialTheme` |
| [Theme.kt](Theme.kt) — `expect platformColorScheme` | Точка расширения для системной палитры |
| [Theme.android.kt](../../../../../../androidMain/kotlin/com/logoped_plus/ui/theme/Theme.android.kt) | Android 12+ возвращает `dynamicDarkColorScheme`/`dynamicLightColorScheme`, иначе `null` |
| [Theme.ios.kt](../../../../../../iosMain/kotlin/com/logoped_plus/ui/theme/Theme.ios.kt) | Всегда `null`: общие светлая/тёмная схемы |
| [Color.kt](Color.kt) | Цвета `Purple80`, `PurpleGrey80`, `Pink80`, `Purple40`, `PurpleGrey40`, `Pink40` |
| [Type.kt](Type.kt) — `Typography` | Настройка `bodyLarge`; остальные стили используют значения Material по умолчанию |

Тема подключена в Android-оболочке [MainActivity.kt](../../../../../../../../app/src/main/java/com/logoped_plus/MainActivity.kt). При `dynamicColor = true` на Android 12+ цвета берутся из системы, поэтому изменение `Color.kt` может не менять вид приложения. Тёмный режим по умолчанию следует системе.

XML-тема Android по-прежнему в `app/src/main/res/values/themes.xml`, её подключает manifest. Логотип drawer — общий ресурс Compose `shared/src/commonMain/composeResources/drawable/icon1024.png`. Специализированных тестов темы нет.
