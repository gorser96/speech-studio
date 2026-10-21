# Тестирование и эмулятор

Процедура запуска эмулятора и проверки приложения из командной строки.
Команды выполняются из корня модуля `src/speechstudio-mobile/`, если не указано иное.
Выбор уровня проверки по риску — в [AGENTS.md](../../AGENTS.md), раздел «Проверки после изменений».

## Среда

| Компонент | Путь / значение |
|---|---|
| Android SDK | `C:\Users\Gorser\AppData\Local\Android\Sdk` (задан в `local.properties`) |
| JDK | `D:\programms\AndroidStudio\jbr` (JBR, OpenJDK 25; задан в пользовательском `JAVA_HOME`) |
| adb | `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe` |
| emulator | `%LOCALAPPDATA%\Android\Sdk\emulator\emulator.exe` |
| AVD | `Pixel_7a` (x86_64, API 34, образ `google_apis_playstore`) |

`JAVA_HOME` (пользовательская переменная) и каталог `platform-tools` с `adb` уже добавлены в PATH:
в новом терминале `adb` и `java` работают без настройки.
`emulator` в PATH не входит, для сессии PowerShell удобно задать:

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$emu = "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe"
```

## Запуск эмулятора

```powershell
Start-Process $emu -ArgumentList "-avd","Pixel_7a"
& $adb devices   # дождаться строки "emulator-5554  device"
```

Ожидание полной загрузки системы:

```powershell
& $adb wait-for-device shell 'while [ -z $(getprop sys.boot_completed) ]; do sleep 1; done; echo boot-ok'
```

Запуск без окна — добавить `-no-window` в аргументы.
Остановка: `& $adb emu kill` (при единственном эмуляторе) или закрыть окно эмулятора.

## Сборка и установка

```powershell
.\gradlew.bat :app:assembleDebug
& $adb install -r app\build\outputs\apk\debug\app-debug.apk
```

## Запуск приложения и проверки

Пакет — `com.logoped_plus`, стартовый activity — `com.logoped_plus.MainActivity`.

```powershell
# Запуск стартового экрана
& $adb shell monkey -p com.logoped_plus -c android.intent.category.LAUNCHER 1

# Активный activity (ожидается com.logoped_plus/...)
& $adb shell dumpsys activity activities | Select-String topResumedActivity

# Скриншот текущего экрана
& $adb shell screencap -p /sdcard/screen.png
& $adb pull /sdcard/screen.png "$env:TEMP\screen.png"

# Критические ошибки в логе (ожидается пусто)
& $adb shell logcat -d | Select-String -Pattern "FATAL|AndroidRuntime"
```

## Тесты

- Unit-тесты (эмулятор не нужен):

  ```powershell
  .\gradlew.bat :app:testDebugUnitTest --tests "полное.ИмяТеста"
  ```

  Полный набор `:app:testDebugUnitTest` — при широком влиянии изменения или неясных границах регрессии.
- Инструментированные `:app:connectedDebugAndroidTest` — только при существенном Android/Room/UI-риске, который нельзя проверить локально; нужен запущенный эмулятор:

  ```powershell
  .\gradlew.bat :app:connectedDebugAndroidTest
  ```

  Gradle находит запущенное устройство автоматически через adb.
