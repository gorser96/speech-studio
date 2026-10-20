# com.logoped_plus.ui

Общая оболочка приложения, боковое меню и идентификаторы разделов. [Общий указатель](../../../../../../../PACKAGES.md).

| Файл | Назначение |
|---|---|
| [App.kt](App.kt) | `App` — общая для Android и iOS оболочка: Drawer, TopAppBar, четыре раздела, сохранение раздела, подтверждение выхода (`onExit`), экранное действие «Назад» (`showSectionBack`), перехват системного Back через `PlatformBackHandler`; логотип по умолчанию — общий ресурс `composeResources/drawable/icon1024.png`, параметр `logo` позволяет подменить |
| [AppDrawer.kt](AppDrawer.kt) | Четыре пункта (Расписание, Дети, Тэги, Настройки): выделение раздела, enabled, семантика disabled и guard callback; логотип передаётся как `Painter?` |
| [AppScreen.kt](AppScreen.kt) | Schedule, Children, Tags, Settings |

Навигацией владеет [App](App.kt). Во время busy (запись/генерация/удаление любого экрана) недоступны открытые пункты drawer; App отдельно проверяет актуальный статус, блокирует кнопку меню, жесты и Back. Вложенные экраны (карточка/редактор, диалоги) перехватывают Back раньше. Navigation Compose не внедрён.

Android-оболочка передаёт `activity.finish()` как `onExit` и оставляет `showSectionBack = false` (системная кнопка уже работает). На iOS `onExit = null`, `PlatformBackHandler` — no-op, поэтому подтверждение выхода недостижимо, а возврат между разделами выполняет экранная стрелка при `showSectionBack = true`.

## Проверки

На диске тестов оболочки нет; навигация проверялась осмотром кода. Сборка Android и адресные ViewModel-тесты — в задачах изменения `cross-platform-kmp`. Результаты и открытые критерии — [статус проверок](../../../../../../../../../openspec/verification/README.md).
